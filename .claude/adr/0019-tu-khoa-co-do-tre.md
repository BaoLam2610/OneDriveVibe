# ADR-0019: Tự khóa khi rời app có độ trễ do người dùng chọn

**Ngày**: 2026-10-07
**Trạng thái**: accepted (thay phần "khóa ngay, không có thời gian ân hạn" của ADR-0014)
**Người quyết định**: LamBao

## Bối cảnh
ADR-0014 khóa app ngay khi cả process xuống nền. Đặc tả (mục 3.8, Cài đặt › Bảo mật) có mục "Tự khóa khi rời app: Ngay lập tức / 1 / 5 / 15 phút, mặc định 1 phút". Chuyển sang app khác để lấy mã rồi quay lại là việc thường gặp, khóa ngay làm người dùng phải nhập PIN liên tục.

## Quyết định
1. `AppLockController` đếm từ lúc `ProcessLifecycleOwner` báo `ON_STOP` (cả app không còn màn hiển thị): quay lại trước mốc thì không khóa, quá mốc thì khóa. Các mốc: Ngay lập tức, 10 giây, 30 giây, 1 phút (mặc định), 5 phút, 15 phút (10 và 30 giây thêm theo yêu cầu 2026-10-07); "Ngay lập tức" khóa trong `onStop` như trước.
2. Mốc tính bằng `SystemClock.elapsedRealtime()` (đếm cả lúc máy ngủ) và kiểm tra lại ở `onStart`, để Doze trì hoãn bộ hẹn giờ cũng không làm app sống quá hạn.
3. Giá trị đọc từ `SecuritySettings.autoLockDelay` (DataStore `settings`), giữ trong bộ nhớ để `onStop` quyết định đồng bộ. Chưa đọc xong thì coi như "Ngay lập tức" (phía an toàn).
4. Process bị hệ thống thu hồi trong lúc chờ thì lần mở sau là khởi động nguội và luôn vào màn Khóa.

## Đánh đổi
Trong thời gian chờ (tối đa 15 phút), config đã giải mã và access token còn nằm trong bộ nhớ của process (trái với "xóa ngay khi khóa", CH-03, nhưng CH-03 là khi **khóa**; lúc này app chưa khóa). Ảnh ở danh sách app gần đây vẫn bị ẩn từ lúc bật PIN (CH-05, ADR-0014) nên nội dung không lộ qua đó. Người cần an toàn tối đa chọn "Ngay lập tức".

## Phương án đã cân nhắc
- Giữ khóa ngay, không có cài đặt: bỏ qua mục đặc tả, bất tiện khi dùng thường xuyên.
- Chỉ kiểm tra thời gian ở `onStart`, không hẹn giờ: token và config nằm trong bộ nhớ lâu không giới hạn nếu người dùng không quay lại. Chọn hẹn giờ cộng kiểm tra lại.

## Hệ quả
- Tích cực: khớp đặc tả, đỡ nhập PIN lặp lại.
- Tiêu cực: cửa sổ vài phút không bị khóa, cần nói rõ ở mô tả cài đặt.
- Rủi ro: hẹn giờ trong coroutine của process có thể bị hoãn lúc Doze, đã giảm nhờ kiểm tra lại ở `onStart`.
