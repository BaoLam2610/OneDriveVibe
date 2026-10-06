# ADR-0018: Decoder FFmpeg dự phòng cho video máy không giải mã được (HEVC 10-bit)

**Ngày**: 2026-10-07
**Trạng thái**: accepted (2026-10-07: video HEVC 10-bit đã phát được trên Xiaomi bằng FFmpeg, giật được chấp nhận, xem "Giới hạn đã chấp nhận")
**Người quyết định**: LamBao

## Bối cảnh
Video HEVC Main10 (10-bit, gồm video HDR quay bằng iPhone) không phát được trên Redmi Note 13 Pro 4G (Helio G99 Ultra, Android 15) mặc dù Samsung phát được. Log 2026-10-06 (tag `Player`, dòng `[Decoder]`) cho thấy máy chỉ có hai decoder `video/hevc`: `c2.mtk.hevc.decoder` khai báo profile `Main` và `c2.android.hevc.decoder` khai báo `Main/Still`; không cái nào có `Main10`. Cả hai đều chết khi nhận video. Không phải lỗi cấu hình: đã thử đổi định dạng Dolby Vision thành HEVC thường (video không phải Dolby Vision vẫn lỗi), chặn decoder chết rồi thử bộ khác, và loại trừ `TextureView` (decoder tự khai báo không nhận profile). Đây là giới hạn phần cứng, nên muốn phát phải giải mã bằng phần mềm trong app. Chi tiết chẩn đoán ở `tien-do-mvp1.md`, mục "Bug CRITICAL: video Dolby Vision trên MediaTek".

## Quyết định
Thêm thư viện NextLib (`io.github.anilbeesetti:nextlib-media3ext`, FFmpeg cho Media3) và dựng ExoPlayer bằng `NextRenderersFactory` ở chế độ extension `ON` (không phải `PREFER`). Ở chế độ `ON`, renderer FFmpeg đứng **sau** renderer của máy: ExoPlayer chọn renderer báo hỗ trợ tốt nhất cho định dạng, nên FFmpeg chỉ chạy khi decoder của máy không nhận định dạng (`EXCEEDS_CAPABILITIES`). Máy có decoder nhận được thì dùng phần cứng như cũ, không phải kiểm tra theo tên máy hay dòng chip.

## Giới hạn đã chấp nhận (2026-10-07, người dùng chọn)
Trên Redmi Note 13 Pro 4G, với Media3 1.11.1 và NextLib `1.11.1-0.16.0`, video 1080x1920 HEVC 10-bit phát đúng hình, màu và hướng nhưng rớt khung nhiều (log `[Decoder] rớt 50 khung` mỗi 0,9 đến 1,9 giây). Đây là đường dự phòng cho máy yếu: **phát được nhưng giảm tốc độ khung**, người dùng chấp nhận. Mã NextLib: số luồng giải mã mặc định `availableProcessors()`, đổi YUV sang RGBA bằng một luồng `sws_scale` trên CPU rồi vẽ qua `ANativeWindow`; nghi đây là nút cổ chai (chưa đo). Màu cũng bị nhạt với video HLG (BT.2020) trên màn hình SDR (người dùng xác nhận): đoạn đổi màu `sws_scale` của NextLib không thấy dùng ma trận BT.2020 hay ánh xạ tông màu HLG sang SDR. Sửa đúng cách phải đụng phần native (fork NextLib) hoặc thêm bước GPU, nên người dùng chọn **không sửa** ở thời điểm này (trade-off lớn so với lợi ích). Cách xấp xỉ rẻ nếu cần sau này: tăng độ bão hòa bằng bộ lọc màu trên khung video, chỉ khi dùng đường FFmpeg và video HDR; chưa kiểm chứng API và phải chỉnh bằng mắt. Các hướng đã loại ở thời điểm này nhưng còn mở nếu cần: tự dựng `FfmpegVideoRenderer` với số luồng khác, đổi màu bằng GPU (`VideoDecoderGLSurfaceView`, phải làm lại zoom), fork NextLib để giảm độ phân giải đầu ra, hoặc libVLC.

## Phương án đã cân nhắc
### Phương án 1: Chỉ báo lỗi rõ ràng, không thêm decoder
- Ưu: không thêm thư viện, không đụng giấy phép, APK không nặng thêm.
- Nhược: người dùng vẫn không xem được video 10-bit trên máy này (mà video iPhone HDR là video phổ biến).
- Lý do không chọn: người dùng muốn xem được video.

### Phương án 2: libVLC hoặc mpv thay ExoPlayer
- Ưu: phát được hầu hết định dạng.
- Nhược: phải viết lại toàn bộ màn xem (cache theo `itemId:cTag`, link tự làm mới VD-14, cử chỉ, zoom). libVLC (LGPL-2) làm APK nặng thêm hàng chục MB; mpv/IJKPlayer chưa kiểm.
- Lý do không chọn: chi phí viết lại quá lớn so với việc chỉ cần thêm một renderer dự phòng.

### Phương án 3: Tự build module FFmpeg của Media3
- Ưu: không phụ thuộc bên thứ ba.
- Nhược: Media3 chỉ có decoder FFmpeg cho âm thanh; video phải tự viết và build NDK/FFmpeg thủ công.
- Lý do không chọn: công sức lớn, không có người dùng nào khác cần.

### Phương án 4: Chuyển mã phía máy chủ
- Lý do không chọn: Graph không có API chuyển mã video để phát trực tiếp.

## Hệ quả
### Tích cực
- Phát được video HEVC 10-bit (và H.264, VP8, VP9, AV1 nếu máy thiếu decoder) trên máy yếu mà không đổi gì ở máy mạnh.
- Không thêm màn hình hay luồng mới; lỗi vẫn đi qua thẻ lỗi và đếm ngược VD-15.

### Tiêu cực
- **Giấy phép GPL-3.0** của NextLib: bản phát hành của app phải tuân GPL-3.0 (công bố mã nguồn cho người nhận bản dựng). Dự án hiện là app cá nhân không phát hành; nếu sau này phát hành công khai thì phải xem lại (xem Rủi ro).
- APK nặng thêm (FFmpeg dựng sẵn cho các ABI); chưa đo, cần đo sau khi build và cân nhắc `abiFilters` chỉ `arm64-v8a`.
- Giải mã phần mềm HEVC 10-bit 1080p tốn pin và CPU, có thể rớt khung ở máy yếu. Hiện chưa có giới hạn độ phân giải.
- Video `video/dolby-vision` (profile 8) không được FFmpeg nhận vì mime lạ; nếu thiếu decoder 10-bit thì vẫn báo lỗi. Cần ánh xạ mime Dolby Vision sang HEVC nếu muốn xử lý.

### Rủi ro
- Bản `1.8.0-0.9.0` đầu tiên bị giật khi phát HEVC 10-bit trên Helio G99 (người dùng báo 2026-10-07). Đã nâng Media3 lên 1.11.1 và NextLib lên `1.11.1-0.16.0` (0.15.0: sửa màu surface và "avoid copied output frames"; 0.16.0: áp dụng xoay video) để thử giảm giật; chưa đo. Đường FFmpeg vẫn đổi YUV sang RGBA bằng CPU (`sws_scale`), nên có thể vẫn giật; phương án tiếp là đổi bằng GPU (`VideoDecoderGLSurfaceView`, phải làm lại zoom) hoặc chấp nhận.
- API NextLib (`NextRenderersFactory`) chưa kiểm chứng với Media3 1.8.0 qua build.
- FFmpeg trong cùng tiến trình: lỗi native (crash) làm sập app thay vì ném ngoại lệ; theo dõi khi thử máy.
- Nếu app phát hành công khai, thay bằng thư viện giấy phép LGPL hoặc cho người dùng tự bật tùy chọn.
