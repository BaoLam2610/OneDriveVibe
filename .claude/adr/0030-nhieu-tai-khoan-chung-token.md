# ADR-0030: Nhiều tài khoản cùng tenant, một token, dữ liệu theo tài khoản

**Ngày**: 2026-10-12
**Trạng thái**: accepted 2026-10-12 (bổ sung ADR-0005 và ADR-0007; không thay thế). Người dùng chốt phạm vi, phương án gộp dữ liệu và cách đồng bộ ở phiên 2026-10-12.
**Người quyết định**: LamBao

## Bối cảnh
Xác thực là Client Credentials (ADR-0005): token app-only đọc được drive của **mọi người dùng trong tenant** đã cấp `Files.Read.All`, app chỉ đổi `{UPN}` trong `/users/{UPN}/drive`. Người dùng cần đưa nhiều tài khoản cùng tenant vào app (cùng Tenant ID, Client ID, Client Secret, chỉ khác UPN), chọn tài khoản đang xem, xem gộp tất cả. Hiện `ConnectionConfig` cố định 4 trường, `drive_item.id` là khóa đơn, `sync_state` một dòng duy nhất, `reading_progress.itemId` khóa đơn, nên mọi thứ gắn với đúng một UPN.

## Quyết định
1. **Config tách hai phần:** phần dùng chung (Tenant ID, Client ID, Client Secret) và danh sách tài khoản (UPN, tên hiển thị, email, drive id). Một access token dùng cho mọi tài khoản.
2. **Dữ liệu theo tài khoản** (đồng bộ, `deltaLink`, tiến độ quét lần đầu, vị trí xem, trang đọc, dải Xem tiếp); cài đặt, token và trần bộ nhớ đệm dùng chung. Khóa tài khoản trong CSDL là **drive id** (`accountId`), không dùng UPN, để đổi UPN không làm mất dữ liệu và để khóa cache không lộ email.
3. **Phạm vi xem** là một tài khoản hoặc "Tất cả tài khoản" (chỉ hiện khi có từ 2 tài khoản). Các truy vấn đọc nhận **danh sách `accountId`** của phạm vi; phạm vi một tài khoản là danh sách một phần tử.
4. **Đồng bộ mọi tài khoản, lần lượt**, tài khoản đang xem chạy trước; một tài khoản lỗi (`403`/`404`) không chặn các tài khoản sau.
5. Tên hiển thị và email lấy từ `owner.user` của `GET /users/{upn}/drive?$select=id,driveType,owner`, không xin thêm quyền. **Đã kiểm chứng với token app-only (2026-10-12):** response có tên hiển thị và email; vẫn giữ dự phòng khi thiếu: dùng phần trước `@` của UPN (đặc tả TA-02).
6. Thêm hoặc xóa tài khoản không hỏi PIN (app đã mở khóa) và mã hóa lại config bằng khóa phiên đang giữ, ghi tệp tạm rồi thay (CD-09).

## Phương án đã cân nhắc
### Phương án 1: Xóa dữ liệu khi đổi tài khoản
- Ưu: schema không đổi.
- Nhược: mỗi lần đổi phải quét lại cả drive; không thể xem gộp.
- Lý do không chọn: người dùng muốn gộp dữ liệu giữa các tài khoản.

### Phương án 2: Mỗi tài khoản một tệp CSDL
- Ưu: cách ly tuyệt đối, xóa tài khoản là xóa tệp.
- Nhược: Thư viện, Short, tìm kiếm gộp phải truy vấn nhiều CSDL rồi trộn ở bộ nhớ (sắp xếp theo ngày, phân trang, xáo trộn); hai kết nối Room cùng lúc.
- Lý do không chọn: truy vấn gộp là yêu cầu chính; cột `accountId` trong một CSDL cho `WHERE accountId IN (...)` với index.

### Phương án 3: Đồng bộ chỉ tài khoản đang xem
- Ưu: ít pin và băng thông.
- Nhược: ở phạm vi gộp, tài khoản chưa từng chọn riêng sẽ trống hoặc cũ.
- Lý do không chọn: người dùng chốt đồng bộ mọi tài khoản.

## Hệ quả
### Tích cực
- Dùng chung một token và một bộ cài đặt; thêm tài khoản chỉ cần UPN.
- Xem gộp bằng một truy vấn có index.
### Tiêu cực
- Đổi schema Room (tăng `version`, ADR-0015) và định dạng config đã mã hóa; chưa phát hành nên không cần Migration.
- Mọi truy vấn phải lọc theo `accountId`; sót một chỗ sẽ hiện nhầm dữ liệu tài khoản khác.
- Khóa cache video, ảnh, PDF và link phát chia sẻ (ADR-0029) phải có thêm tài khoản: hai tài khoản có thể có tệp trùng mã. **Cách làm:** id của mục trong CSDL và mọi nơi dùng nó là chuỗi mờ `<drive id>|<id Graph>` (`ItemKey` ở domain), nên khóa cache và link phát tự có tài khoản mà không phải đổi từng nơi; lời gọi Graph đổi lại sang UPN và id thô qua `AccountDirectory` ở data.
- Xóa tài khoản dọn dữ liệu theo hook `AccountDataCleaner` (Room, tiến độ đọc, ảnh gốc, PDF, video). Thumbnail nằm trong `DiskCache` của Coil không liệt kê được khóa, nên mỗi lần ghi thumbnail mới thì ghi khóa vào chỉ mục theo tài khoản (`ThumbnailKeyIndex`, tệp văn bản trong `cacheDir`) và xóa tài khoản thì gỡ từng khóa bằng `remove(key)`. Thumbnail đã tải trước khi có chỉ mục nằm lại tới khi bị dọn theo LRU.
### Rủi ro
- Hai tài khoản trùng drive id (cùng một người nhập hai UPN khác nhau): chặn khi thêm bằng drive id trả về.
- Đồng bộ nhiều tài khoản lần lượt kéo dài lần quét đầu; chấp nhận, hiển thị tiến độ theo từng tài khoản (AC9).
- Tên hiển thị không có trong response app-only: đã có phương án dự phòng (TA-02).
