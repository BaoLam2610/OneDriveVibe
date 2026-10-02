# ADR-0005: Xác thực Client Credentials, chỉ hỗ trợ OneDrive for Business

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
App là công cụ cá nhân, chỉ đọc OneDrive của một tài khoản trong tenant do chính người dùng quản lý. Cần cách kết nối không phụ thuộc trình duyệt và không cần đăng nhập lại định kỳ.

## Quyết định
Dùng Client Credentials (app-only). Người dùng nhập 4 trường: Tenant ID, Client ID, Client Secret, UPN. Mọi lời gọi Graph đi qua `/users/{UPN}/drive`. Không hỗ trợ OneDrive cá nhân trong MVP1.

## Phương án đã cân nhắc
### Authorization Code + PKCE (delegated)
- Ưu: hỗ trợ cả OneDrive cá nhân, không cần secret.
- Nhược: cần mở trình duyệt, xử lý redirect và refresh token.
- Lý do không chọn: vượt nhu cầu cá nhân; secret do chủ tenant tự quản lý được.
### Device Code flow
- Ưu: không cần redirect URI.
- Nhược: phải đăng nhập trên thiết bị khác, refresh token vẫn có thể hết hạn.
- Lý do không chọn: trải nghiệm kém hơn với app dùng hằng ngày.

## Hệ quả
### Tích cực
- Không dùng MSAL, auth viết bằng Ktor nên dùng chung được cho KMP.
- Không có luồng đăng nhập tương tác, màn Kết nối đơn giản.
### Tiêu cực
- Quyền `Files.Read.All` ở mức application đọc được mọi drive trong tenant.
- Client Secret hết hạn (tối đa 24 tháng), người dùng phải cập nhật tay.
### Rủi ro
- Lộ secret thì lộ toàn bộ tenant. Giảm thiểu bằng mã hóa config (ADR-0008) và không ghi log secret (CH-06).
- Sau này cần OneDrive cá nhân: thêm delegated flow làm một auth provider riêng sau interface; ADR mới sẽ thay thế ADR này.
