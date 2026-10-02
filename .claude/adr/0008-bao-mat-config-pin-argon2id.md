# ADR-0008: Bảo mật config bằng khóa dẫn xuất từ PIN (Argon2id) và Keystore

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Config gồm Client Secret nên rất nhạy cảm. Người dùng mở app bằng PIN 6 số, chỉ có 1 triệu tổ hợp, nên không thể dựa vào độ phức tạp của PIN để chống đoán.

## Quyết định
Config luôn được mã hóa (AES-GCM). Bảo mật bật: khóa dẫn xuất từ PIN bằng **Argon2id** kết hợp khóa phần cứng trong Android Keystore. Bảo mật tắt: chỉ khóa Keystore. **Không lưu PIN, không lưu hash PIN**; PIN đúng/sai là giải mã config thành công hay không (CH-01, CH-02, CH-07). Access token chỉ giữ trong bộ nhớ.

## Phương án đã cân nhắc
### Lưu hash PIN (PBKDF2) để kiểm tra
- Ưu: kiểm tra PIN nhanh, không cần giải mã.
- Nhược: hash bị trích ra thì thử 1 triệu tổ hợp offline rất nhanh.
- Lý do không chọn: yếu với PIN ngắn.
### EncryptedSharedPreferences
- Ưu: sẵn có.
- Nhược: thư viện `security-crypto` đã deprecated.
- Lý do không chọn: không còn được phát triển.
### Lưu config không mã hóa trong DataStore
- Nhược: Client Secret ở dạng rõ trên đĩa.
- Lý do không chọn: không chấp nhận được.

## Hệ quả
### Tích cực
- Lấy được file config ra khỏi máy cũng không thử PIN được (khóa Keystore gắn thiết bị).
### Tiêu cực
- Đổi PIN phải mã hóa lại config (CD-09); quên PIN thì mất kết nối và dữ liệu (KH-03).
- Argon2id cần thư viện ngoài, tốn CPU/RAM trên máy yếu.
### Rủi ro
- Đếm số lần sai chỉ chặn được đoán PIN qua giao diện app. Giảm thiểu bằng tùy chọn xóa dữ liệu sau 10 lần sai (CD-08) và khóa Keystore.
