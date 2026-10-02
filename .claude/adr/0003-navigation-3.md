# ADR-0003: Navigation 3

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Cần điều hướng giữa Kết nối, Khóa, Danh sách, các màn xem và Cài đặt. Luồng khóa/mở khóa phải điều khiển được back stack (vd. app bị khóa thì đưa về màn Khóa).

## Quyết định
Dùng **Navigation 3**: back stack là danh sách do app sở hữu, hiển thị bằng `NavDisplay` + `entryProvider`. Không truyền nav controller sâu vào composable, dùng lambda callback.

## Phương án đã cân nhắc
### Navigation Compose 2.x (`NavHost`, `composable<Route>`)
- Ưu: phổ biến, nhiều tài liệu.
- Nhược: back stack do thư viện giữ, khó điều khiển trực tiếp.
- Lý do không chọn: luồng khóa cần thao tác thẳng trên back stack.
### Decompose / Voyager
- Ưu: sẵn đa nền tảng.
- Nhược: thêm một mô hình điều hướng ngoài Jetpack.
- Lý do không chọn: Navigation 3 đã đủ cho MVP1.

## Hệ quả
### Tích cực
- Back stack là state thường, dễ lưu, khôi phục và kiểm tra.
### Tiêu cực
- Thư viện mới hơn, ít ví dụ; skill `compose-multiplatform-patterns` chỉ có ví dụ Navigation 2 nên không dùng được cho phần này.
### Rủi ro
- Hỗ trợ Compose Multiplatform của Navigation 3 cần kiểm tra khi vào MVP2. Nếu không đủ, ADR mới sẽ thay thế.
