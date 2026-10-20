# Log

Dùng **Kermit**. Không `android.util.Log`, không `println`.

## 1. Cách ghi

```kotlin
private val log = Logger.withTag("Cart")

log.d { "Mở giỏ, ${items.size} mục" }      // lambda: chuỗi chỉ dựng khi mức log được bật
log.w { "Tải giỏ lỗi: $error" }            // AppError có toString an toàn
log.e(throwable) { "Ghi CSDL lỗi" }        // throwable chỉ vào writer debug
```

- Tag = tên chủ đề ngắn, ổn định (`Cart`, `Sync`, `Auth`), không phải tên lớp dài. Một chủ đề phức tạp có thể dùng tiền tố trong message (`[Url]`, `[Cache]`) để lọc ở màn Debug.
- Mức: `v` chi tiết vòng lặp; `d` luồng chính; `i` mốc quan trọng (khởi động, đăng nhập xong); `w` lỗi tự phục hồi được; `e` lỗi người dùng thấy hoặc dữ liệu hỏng.
- Koin logger mức `ERROR`.

## 2. Được và không được ghi

| Được | Không được |
|---|---|
| Kết quả (thành công/thất bại), mã lỗi, status HTTP | Token, mật khẩu, PIN, OTP, secret |
| Tên lớp ngoại lệ (`e::class.simpleName`) | `e.message`, `e.toString()` của lỗi mạng/IO |
| Số lượng, thời lượng (ms), kích thước | Giá trị ô nhập, nội dung tin nhắn, dữ liệu cá nhân |
| Id rút gọn (vài ký tự cuối) | URL có chữ ký, tên tệp của người dùng, đường dẫn đầy đủ |

Xem thêm `security.md` mục 1.

## 3. Debug và release

- **Bản debug:** `DebugTools.install` đặt writer Logcat + writer đẩy vào màn Debug, mức `Verbose`.
- **Bản release:** `Logger.setLogWriters(emptyList())`. Không có log nào, kể cả Logcat.
- Gửi lỗi lên dịch vụ crash (Crashlytics, Sentry): quyết bằng ADR; chỉ gửi loại lỗi và stacktrace đã được làm sạch.

## 4. Log chẩn đoán cho lỗi khó

Lỗi phụ thuộc thiết bị hoặc khó tái hiện: thêm log **trước** khi sửa (xem `process/vertical-slice.md` mục "Sửa lỗi khó").

- Log chẩn đoán lâu dài (decoder được chọn, thời gian tới khung hình đầu, số lần thử lại) giữ lại nếu có ích; ghi ở mức `d` hoặc `v`.
- **Log truy vết tạm** dùng một tiền tố duy nhất tìm được bằng `grep` (ví dụ `TRACE-CART`), ghi vào file tiến độ "đã thêm log tạm ở X, Y, Z", và **gỡ trong commit riêng** sau khi chốt nguyên nhân: `grep -rn "TRACE-CART" .` không còn kết quả.
- Log có mốc thời gian tương đối (`+123ms từ lúc mở`) giúp đọc chuỗi sự kiện trên máy.
