# ADR-0026: Tải trước video của tab Short bằng `CacheWriter` thay cho `DefaultPreloadManager`

**Ngày**: 2026-10-11
**Trạng thái**: proposed (thay mục 1 của ADR-0024 về cơ chế tải trước; các mục còn lại của ADR-0024 giữ nguyên). Chờ người dùng xác nhận sau khi kiểm tay.
**Người quyết định**: LamBao (đề xuất bởi Claude khi làm Lát 8c)

## Bối cảnh
ADR-0024 chọn `DefaultPreloadManager` của Media3 để tải trước video kế tiếp và video trước (SV-13). Khi làm 8c thấy:
- `DefaultPreloadManager` buộc dựng player qua `DefaultPreloadManager.Builder.buildExoPlayer()` và đưa `MediaSource` đã tải trước vào player theo từng mục. Player của app đang được dựng chung cho màn Xem video và tab Short bởi `ExoPlayerFactory` (ADR-0025) với chuỗi nguồn dữ liệu, cache, bộ chọn decoder có FFmpeg dự phòng (ADR-0018) và xử lý Dolby Vision đã kiểm tay kỹ. Chuyển sang manager nghĩa là viết lại cách dựng này.
- Máy chỉ build thủ công bởi người dùng, nên chữ ký API của manager (chưa dùng ở đâu trong dự án) không kiểm chứng được trước khi giao.
- Thời gian tới khung hình đầu đo được ở 8b là khoảng 2,2 đến 2,6 giây (log 2026-10-11). Phần lớn là lấy link ký, mở kết nối và tải đoạn đầu; giải mã chỉ ~50ms. Tải sẵn byte và link là phần có lợi nhất; chuẩn bị track hay giải mã sẵn là phần nhỏ.

## Quyết định
1. Tải trước bằng `VideoPreloader` (`:core:media`) dùng `androidx.media3.datasource.cache.CacheWriter`: ghi vài giây đầu (ước `bitrate/8 × 4 giây`, chặn 512 KB đến 4 MB, không vượt kích thước tệp) của các video kế cận vào **cache video chung** qua đúng chuỗi nguồn dữ liệu (`StreamDataSource` → HTTP) và **đúng khóa cache** (`videoCacheKey`) mà player dùng. Khi player nạp video đó, phần đầu đã có trong cache.
2. Cửa sổ ±1 như ADR-0024, ưu tiên video kế tiếp. Chỉ bắt đầu **sau khi video đang xem đã vẽ khung hình đầu** để không giành băng thông với nó. Không tải trước khi không có mạng (SV-13): mất mạng thì dừng ngay các việc đang tải.
3. Link ký lấy qua `StreamUrlProvider` riêng của bộ tải trước. Khi app bị khóa PIN: dừng tải trước, xóa link ký của cả bộ tải trước lẫn player Short, `stop()` player và bỏ nguồn, giữ nguyên ExoPlayer và bộ giải mã (CH-03, bổ sung ADR-0024 mục 6); sau khi mở khóa nạp lại đúng video và vị trí.
4. Vẫn **một** ExoPlayer cho tab Short (ADR-0024 mục 1) và vẫn tính phần tải trước vào trần cache video (BN-01).

## Phương án đã cân nhắc
### Giữ `DefaultPreloadManager` (ADR-0024)
- Ưu: cách Media3 khuyến nghị cho video ngắn; chuẩn bị cả track và có thể giải mã sẵn tới mức chọn.
- Nhược: viết lại cách dựng player dùng chung; API chưa kiểm chứng được; rủi ro hồi quy ở màn Xem video vừa kiểm tay xong.
- Lý do không chọn bây giờ: đạt cùng mục tiêu tải byte và link với thay đổi nhỏ và độc lập. Xem lại nếu sau khi có tải trước mà thời gian tới khung hình đầu vẫn còn cao.
### Hai ExoPlayer (một giữ video kế tiếp ở trạng thái tạm dừng)
- Ưu: poster là khung hình đầu thật, vuốt là có hình tức thì.
- Nhược: hai decoder cùng lúc, tải CPU của FFmpeg; đã bị ADR-0024 loại cho pool 2 đến 3 player. Ghi là hướng 1 của "Kế hoạch poster" ở `tien-do-mvp1.md`, đo lại sau khi có tải trước.

## Hệ quả
### Tích cực
- Thay đổi gọn: một lớp mới ở `:core:media`, không đổi `ExoPlayerFactory` ngoài việc lộ `streamUrls`.
- Phần tải trước dùng lại được khi offline (đã ở cache video).
### Tiêu cực
- Không chuẩn bị track và không giải mã sẵn, nên vẫn còn thời gian mở kết nối tới nguồn đã có byte, khởi tạo decoder và vẽ khung hình đầu.
- Tốn thêm băng thông cho video tải trước mà người dùng có thể vuốt qua ngay.
### Rủi ro
- `CacheWriter.cache()` chặn luồng và không biết hủy coroutine; đã bù bằng coroutine canh gọi `cancel()`. Cần kiểm tay vuốt nhanh qua nhiều video.
- Tệp nhỏ hơn mức tải trước dựa vào `sizeBytes` của Graph để chặn độ dài; nếu `sizeBytes` sai thì `cache()` có thể ném lỗi (đã bắt, ghi log, không ảnh hưởng phát).
