# ADR-0014: Phong bì PIN hai lớp, bộ đếm sai bền, sinh trắc học bọc khóa dẫn xuất, tự khóa

**Ngày**: 2026-10-03
**Trạng thái**: accepted
**Người quyết định**: LamBao
**Bổ sung cho**: ADR-0008 (không thay thế; mọi quyết định của ADR-0008 vẫn giữ nguyên)

## Bối cảnh
ADR-0008 chốt "khóa dẫn xuất từ PIN (Argon2id) kết hợp Keystore" nhưng chưa nói cách kết hợp, định dạng tệp, nơi lưu số lần sai, cách mở khóa bằng sinh trắc học khi không được lưu PIN, và khi nào tự khóa. Lát 2 cần các chi tiết này.

## Quyết định
1. **Hai lớp mã hóa chồng nhau.** Khóa Keystore không xuất ra được nên không trộn byte với khóa Argon2id. Lớp ngoài là `SecretStore` hiện có (AES-GCM, khóa Keystore). Lớp trong, chỉ có ở chế độ PIN, là **phong bì PIN** nằm trong plaintext của lớp ngoài. Mất khóa Keystore hay sai PIN đều làm giải mã hỏng, đúng nghĩa "PIN kết hợp Keystore".
2. **Nhận biết chế độ bằng byte đầu của plaintext lớp ngoài.** `{` (0x7B) là JSON config ở chế độ thiết bị (tệp Lát 1 vẫn đọc được); `0xA1` là phong bì PIN. Không có tệp meta riêng nên không thể lệch với tệp config.
3. **Định dạng phong bì**: `[0xA1][ver=1][kdf=1 Argon2id][m KiB u32][t u8][p u8][salt 16][IV 12][ciphertext + tag GCM]`. AAD là toàn bộ phần header (đến hết salt) cộng tên bí mật. Tham số Argon2id lưu trong header để sau này nâng cấp mà vẫn đọc được tệp cũ. Tham số ban đầu: m = 32 MiB, t = 3, p = 1, khóa ra 32 byte; đo trên máy yếu, quá 700 ms thì hạ t xuống 2.
4. **Bộ đếm sai** lưu trong `SecretStore` (tên `lock_state`, mã hóa bằng khóa Keystore, không dùng PIN): số lần sai liên tiếp, mốc lần thử cuối theo `elapsedRealtime`, số lần khởi động máy (`Settings.Global.BOOT_COUNT`). **Ghi trước khi giải mã**, xóa khi đúng, nên tắt app giữa chừng cũng tính là sai. Từ lần sai thứ 5: chờ 30 giây, mỗi lần sai tiếp theo gấp đôi, trần 1 giờ. Phát hiện máy đã khởi động lại thì tính lại thời gian chờ từ đầu (nghiêm hơn một chút còn hơn bị né bằng cách khởi động lại).
5. **Sinh trắc học bọc khóa dẫn xuất** (làm ở bước sau của Lát 2): khóa Argon2id (không phải PIN, không phải hash PIN) được bọc bằng khóa Keystore `odv_bio_key` yêu cầu `BIOMETRIC_STRONG` cho từng lần dùng (`setUserAuthenticationRequired`, `setInvalidatedByBiometricEnrollment`). Thêm hoặc đổi vân tay thì khóa mất hiệu lực, app tự tắt cờ sinh trắc và rơi về PIN. Không cho `DEVICE_CREDENTIAL` làm phương án dự phòng.
6. **Trạng thái khóa nằm trong bộ nhớ** (`Unknown`, `Unlocked`, `Locked`). Khởi động nguội ở chế độ PIN luôn là `Locked` nên khôi phục sau process death không bỏ qua được màn Khóa. Khóa thì xóa khóa phiên, config đã giải mã và token (CH-03).
7. **Tự khóa** ngay khi app xuống nền (`ProcessLifecycleOwner` ON_STOP, trễ sẵn khoảng 700 ms nên xoay màn hình hay chuyển sang `DebugActivity` không khóa nhầm). Màn Khóa đè lên back stack hiện tại; mở khóa xong bỏ màn Khóa và thấy lại màn đang xem.
8. **Ẩn nội dung ở danh sách app gần đây** khi bảo mật BẬT: Android 13 trở lên dùng `setRecentsScreenshotEnabled(false)`, bản thấp hơn dùng `FLAG_SECURE` toàn cửa sổ.
9. **Bản debug**: khi khóa app, xóa log API của màn Debug và đóng `DebugActivity` (xử lý rủi ro ghi ở ADR-0013).
10. **Ngắt kết nối** đi qua `DisconnectUseCase`: các `ConnectionResetter` do từng lát đăng ký (Room, cache, cài đặt) chạy trước, rồi xóa config, khóa Keystore, bộ đếm sai và token.

## Phương án đã cân nhắc
### Trộn khóa Argon2id với khóa Keystore (HKDF)
- Lý do không chọn: khóa Keystore không xuất ra được.
### Lưu số lần sai bằng DataStore
- Nhược: thêm phụ thuộc, không mã hóa; người dùng root sửa tay được.
- Lý do không chọn: `SecretStore` đã có, mã hóa bằng Keystore và ghi nguyên tử.
### Lưu hash PIN để kiểm tra nhanh
- Lý do không chọn: ADR-0008.
### Sinh trắc học mở khóa bằng cách lưu PIN
- Lý do không chọn: vi phạm CH-02.

## Hệ quả
### Tích cực
- Tệp config Lát 1 (chế độ thiết bị) tiếp tục dùng được; chuyển chế độ chỉ là một lần ghi nguyên tử qua tệp tạm.
- Số lần sai không né được bằng cách tắt app hay chỉnh đồng hồ.
### Tiêu cực
- Trên máy có khóa Keystore bọc sinh trắc học, tồn tại một khóa dẫn xuất nằm trên máy, chỉ mở được bằng sinh trắc học đã đăng ký. Lệch nhẹ khỏi tinh thần "không lưu gì suy ra được PIN" của ADR-0008, chấp nhận được vì khóa này không cho phép suy ngược PIN và Keystore bảo vệ nó.
- Argon2id tốn khoảng 32 MiB RAM mỗi lần mở khóa.
### Rủi ro
- Kết nối lại (KN-08) luôn ghi config ở chế độ thiết bị và xóa bộ đếm sai, nên nếu người dùng tới được màn Kết nối khi đang có PIN (chỉ sau lỗi đọc tạm thời lúc khởi động) thì PIN cũ biến mất. Chủ ý: người dùng tự nhập đủ 4 trường và hộp thoại KN-13 hỏi đặt PIN lại (code review e8027e03, L3).
- Reboot rồi mở lại làm thời gian chờ tính lại từ đầu (người dùng có thể thấy phạt dài hơn thực tế).
- Thư viện `argon2kt` là native: kiểm tra ABI và kích thước trang bộ nhớ 16 KB trên thiết bị thật; dự phòng BouncyCastle (chậm hơn).
