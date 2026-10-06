# ADR-0017: Giữ log debug khi app khóa

**Ngày**: 2026-10-07
**Trạng thái**: accepted (thay phần "xóa log API khi khóa" của ADR-0013 và ADR-0014)
**Người quyết định**: LamBao

## Bối cảnh
ADR-0013/0014 xóa log API và đóng màn Debug mỗi khi app khóa bằng PIN (CH-03), vì log API hiển thị đầy đủ token và Client Secret. Khi dùng thực tế, app xuống nền rồi mở lại là mất log, không xem lại được lỗi vừa xảy ra.

## Quyết định
1. Khi app khóa: **chỉ đóng màn Debug**, không xóa log API, không xóa log local. Không có công tắc.
2. Log chỉ nằm trong bộ nhớ nên vẫn mất khi process chết. Bộ đệm log local tăng từ 500 lên 2000 dòng.
3. Chỉ áp dụng bản debug. Bản release không có recorder và không ghi log (ADR-0012).

## Đánh đổi
Nút bọ vẫn hiện ở màn Khóa (ADR-0013), nên trong bản debug ai cầm máy đều xem được log API chứa secret mà không cần PIN. Chấp nhận vì bản debug chỉ dùng với secret riêng cho phát triển.

## Phương án đã cân nhắc
- Thêm công tắc "xóa log khi khóa": người dùng chọn bỏ hẳn cho gọn.
