# ADR-0024: Phát và tải trước video ở tab Short bằng một ExoPlayer và PreloadManager

**Ngày**: 2026-10-10
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Tab Short (mục 3.4.4 đặc tả) cho vuốt dọc qua các video ngắn. Mỗi video phát qua stream OneDrive: phải lấy link tải (Graph trả `302` sang URL đã ký), rồi mới bắt đầu buffer. Không tải trước thì mỗi lần vuốt chờ khoảng 1 đến 2 giây, mất cảm giác "short" (SV-13). Ngoài ra:
- Video HEVC 10-bit trên một số máy phải dùng decoder FFmpeg phần mềm (ADR-0018), tốn CPU.
- Video dọc quay bằng điện thoại thường lưu dạng 1920×1080 kèm cờ xoay, nên `video.width/height` từ Graph có thể không phản ánh hướng thật (SV-08).
- Cache video đã có trần riêng theo ADR-0021.

## Quyết định
1. Tab Short dùng **một** `ExoPlayer` cho video đang hiện, cộng với **`DefaultPreloadManager`** của Media3 để tải trước **video kế tiếp và video trước** (cửa sổ ±1). Mỗi video chỉ tải trước một đoạn đầu ngắn (ước khoảng 3 đến 5 giây, chốt khi thử máy), không tải cả tệp.
2. Nguồn dữ liệu tải trước đi qua cùng `CacheDataSource` với cache video hiện có, nên phần tải trước tính vào trần cache video (BN-01) và phát lại từ cache được khi offline. Link stream lấy qua `GetStreamUrlUseCase`, cơ chế tự lấy link mới khi hết hạn của VD-14 dùng lại.
3. Renderer giữ nguyên cấu hình của màn Xem video (gồm FFmpeg dự phòng, ADR-0018).
4. Khung hình (SV-08) chọn theo **kích thước hình báo về từ player sau khi giải mã** (đã tính cờ xoay). `video.width/height` trong Room chỉ dùng làm phỏng đoán ban đầu để tránh nháy khung lớn.
5. Thứ tự xáo (SV-02) được tạo từ một **seed** lưu trong saved state, cùng chỉ số video đang xem và vị trí phát, để khôi phục đúng danh sách sau khi hệ điều hành thu hồi tiến trình. Danh sách ID lấy một lần từ Room theo bộ lọc thời lượng, không giữ cả bản ghi trong bộ nhớ.
6. Trình phát và PreloadManager **tạm dừng** (không giải phóng) khi rời tab, app xuống nền hoặc bị khóa (SV-11), và giải phóng khi Màn chính bị bỏ (ADR-0023).

## Phương án đã cân nhắc
### Không tải trước
- Ưu: đơn giản, ít tốn mạng.
- Nhược: chờ mỗi lần vuốt.
- Lý do không chọn: trái mục tiêu của tính năng.
### Một nhóm 2 đến 3 ExoPlayer luân phiên (player pool)
- Ưu: chuyển video gần như tức thì vì player kế tiếp đã chuẩn bị sẵn.
- Nhược: mỗi player giữ một decoder; nhiều máy giới hạn số decoder phần cứng, decoder FFmpeg phần mềm thì càng tốn CPU và bộ nhớ; quản lý vòng đời phức tạp.
- Lý do không chọn: PreloadManager là cách Media3 khuyến nghị cho dạng short-form và chỉ cần một decoder.
### Tải trước cả tệp video ngắn
- Ưu: chắc chắn không giật.
- Nhược: tốn mạng và cache cho video người dùng có thể vuốt qua ngay.
- Lý do không chọn: đoạn đầu ngắn là đủ để bắt đầu phát.

## Hệ quả
### Tích cực
- Vuốt sang video kế tiếp phát gần như ngay khi có mạng ổn định.
- Không thêm thư viện ngoài Media3.
### Tiêu cực
- Tốn thêm băng thông cho video tải trước nhưng không xem.
- Cần phiên bản Media3 có `DefaultPreloadManager` (từ 1.4 trở lên); kiểm phiên bản trong version catalog khi bắt đầu Lát 8.
### Rủi ro
- Video phải giải mã bằng FFmpeg phần mềm có thể giật khi vuốt nhanh; cần thử sớm với clip HEVC 10-bit trên máy thật.
- Link đã ký hết hạn trong lúc nằm ở cửa sổ tải trước lâu (người dùng dừng ở một video quá 1 giờ): dựa vào cơ chế VD-14, cần kiểm tay.