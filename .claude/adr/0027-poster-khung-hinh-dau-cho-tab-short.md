# ADR-0027: Poster là khung hình đầu thật của video kế cận cho tab Short

**Ngày**: 2026-10-11
**Trạng thái**: accepted (bổ sung ADR-0024 và ADR-0026; thay "thiết kế hai player" từng được đề xuất ở `tien-do-mvp1.md`)
**Người quyết định**: LamBao (đề xuất bởi Claude khi làm Lát 8c, người dùng chốt sau khi kiểm tay)

## Bối cảnh
Sau khi có tải trước byte video (ADR-0026), khi vuốt sang video mới vẫn còn 100 đến 200ms nền đen kèm spinner trước khi khung hình đầu hiện (kiểm tay 2026-10-11). Hai hướng đã thử hoặc cân nhắc:
- **Thumbnail của OneDrive làm poster:** đã dùng ở 8b nhưng bỏ, vì thumbnail không phải khung hình đầu nên hình nháy từ ảnh này sang ảnh khác khi khung thật hiện ra.
- **Hai ExoPlayer (một giữ sẵn video kế tiếp):** nghiên cứu (`nghien-cuu-kien-truc-player-short.md`) cho thấy Google/Meta dùng một player; chỉ ByteDance pre-render bằng player thứ hai. Giới hạn số phiên giải mã phần cứng tùy máy, FFmpeg HEVC 10-bit tốn CPU, và ADR-0024 đã loại pool nhiều player.

## Quyết định
1. Poster là **chính khung hình đầu** của video, lấy bằng `MediaMetadataRetriever` qua link ký (`FirstFrameProvider` ở `:core:media`), không phải thumbnail.
2. Chỉ lấy cho video kế cận (±1) cùng lúc với tải trước byte; **mỗi lần một video** (`Semaphore(1)`), để bộ giải mã chỉ được mượn ngắn rồi trả, không giữ phiên giải mã thứ hai suốt lúc xem.
3. Giữ **trong RAM**, tối đa 4 ảnh (`POSTER_MAX_COUNT`), rộng tối đa 540px; không ghi đĩa. Xóa hết poster và link ký khi app bị khóa (CH-03).
4. Giao diện (`ShortPage`) vẽ poster cùng khung và cùng cách Cắt đầy/Vừa khung với bề mặt phát, chỉ cho tới khi khung thật được vẽ, nên hai ảnh trùng nhau, không nháy. Spinner vẫn hiện sau 400ms nếu video chưa lên hình.
5. Không lấy được poster thì rơi về cách cũ (nền đen, spinner trễ). Video quá lớn (trên khoảng 4K) bị bỏ qua, thiếu bộ nhớ thì bỏ, API 27+ giải mã thẳng ra cỡ nhỏ.

## Phương án đã cân nhắc
### Hai ExoPlayer
- Ưu: khung hình đầu thật và có sẵn.
- Nhược: hai decoder cùng lúc, rủi ro theo máy (MediaTek, HEVC 10-bit), thêm bộ nhớ, phải sửa ADR-0024 mục 1.
### Thumbnail của OneDrive
- Ưu: không tốn giải mã.
- Nhược: không khớp khung hình đầu nên nháy; đã loại sau kiểm tay.

## Hệ quả
### Tích cực
- Vuốt có hình ngay, không đen, không nháy; vẫn một ExoPlayer; thay đổi gọn, độc lập.
### Tiêu cực
- Mỗi poster tốn một lần mở kết nối và giải mã một khung; video đầu tiên khi mở tab chưa có poster.
- Máy không giải mã được định dạng (vd. HEVC 10-bit trên một số chip) sẽ không có poster.
### Rủi ro
- `MediaMetadataRetriever` chặn luồng và không hủy được, `setDataSource` qua mạng không có timeout riêng. Bù bằng việc chạy phần chặn trong một coroutine riêng và chỉ `await` nó với `withTimeoutOrNull` 10 giây (không bọc `withContext` bằng timeout vì `withContext` bị hủy vẫn phải chờ khối chặn kết thúc): hết hạn thì bỏ poster video đó và hàng đợi chạy tiếp ngay. `release()` được gọi trên luồng I/O chứ không trên luồng gọi; hành vi của nó khi luồng khác còn kẹt trong `setDataSource` do nền tảng quyết định, nên có thể còn một luồng I/O treo đến khi hệ điều hành tự ngắt kết nối (không ảnh hưởng hàng đợi).
- Chưa kiểm trên máy Xiaomi HEVC 10-bit.
