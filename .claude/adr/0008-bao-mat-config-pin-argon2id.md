# ADR-0008: Bảo mật config bằng khóa dẫn xuất từ PIN (Argon2id) và Keystore

**Ngày**: 2026-10-02 (cập nhật 2026-10-03: thứ tự lưu config và thiết lập PIN)
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Config gồm Client Secret nên rất nhạy cảm. Người dùng mở app bằng PIN 6 số, chỉ có 1 triệu tổ hợp, nên không thể dựa vào độ phức tạp của PIN để chống đoán.

Người dùng cũng muốn PIN là tùy chọn: sau khi kết nối xong, app không ép đi qua một màn thiết lập bảo mật nhiều bước.

## Quyết định
Config luôn được mã hóa (AES-GCM). Bảo mật bật: khóa dẫn xuất từ PIN bằng **Argon2id** kết hợp khóa phần cứng trong Android Keystore. Bảo mật tắt (chế độ thiết bị): chỉ khóa Keystore. **Không lưu PIN, không lưu hash PIN**; PIN đúng/sai là giải mã config thành công hay không (CH-01, CH-02, CH-07). Access token chỉ giữ trong bộ nhớ.

**Thứ tự sau khi kết nối thành công (KN-08, KN-13, BM-04):**
1. Lưu config ngay ở **chế độ thiết bị** (khóa Keystore).
2. Hiện hộp thoại hỏi thiết lập PIN, bắt buộc chọn: "Thiết lập mã PIN" hoặc "Để sau".
3. "Để sau": vào Danh sách, bảo mật tắt; bật lại ở Cài đặt (CD-02).
4. "Thiết lập mã PIN": sau khi đặt xong PIN thì **mã hóa lại** config bằng khóa dẫn xuất từ PIN + Keystore (ghi tệp tạm rồi thay như CD-09). Back hoặc đóng app giữa chừng thì config vẫn ở chế độ thiết bị.

Không có thanh tiến trình bước ở màn Kết nối và màn Thiết lập bảo mật.

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
### Chỉ lưu config sau khi hoàn tất bước bảo mật (thứ tự cũ)
- Ưu: không bao giờ có trạng thái config chưa có PIN.
- Nhược: đóng app giữa chừng thì phải nhập lại 4 trường; Back từ màn bảo mật phải giữ giá trị trong bộ nhớ.
- Lý do không chọn: bất tiện; chế độ thiết bị vốn đã được chấp nhận (BM-03).
### Bỏ hẳn PIN
- Nhược: mất lớp bảo vệ, Client Secret chỉ có Keystore che.
- Lý do không chọn: người dùng vẫn muốn có tùy chọn PIN.

## Hệ quả
### Tích cực
- Lấy được file config ra khỏi máy cũng không thử PIN được (khóa Keystore gắn thiết bị).
- Người dùng không muốn PIN vào thẳng Danh sách, ít thao tác.
- Đóng app giữa chừng sau khi kết nối không mất config.
### Tiêu cực
- Đổi PIN và thiết lập PIN lần đầu đều phải mã hóa lại config (CD-09, BM-04); quên PIN thì mất kết nối và dữ liệu (KH-03).
- Argon2id cần thư viện ngoài, tốn CPU/RAM trên máy yếu.
### Rủi ro
- Đếm số lần sai chỉ chặn được đoán PIN qua giao diện app. Giảm thiểu bằng tùy chọn xóa dữ liệu sau 10 lần sai (CD-08) và khóa Keystore.
- Trong khoảng giữa lúc kết nối xong và lúc đặt PIN (hoặc mãi mãi nếu chọn "Để sau"), ai mở được máy đang mở khóa đều xem được OneDrive. Giảm thiểu bằng cảnh báo BM-03 trong hộp thoại KN-13.
