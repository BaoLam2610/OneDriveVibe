# ADR-0007: Mô hình lỗi có cấu trúc

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0010, `conventions/error-handling.md`

## Bối cảnh

Lỗi cần hiện theo ngôn ngữ đang dùng, chọn thông báo theo mã lỗi của đặc tả, không lộ dữ liệu nhạy cảm (URL ký, token) qua message của ngoại lệ, và không làm coroutine bị hủy vẫn chạy tiếp.

## Quyết định

- `:core:common`: `AppResult<T>` (`Success`/`Failure(AppError)`) và `sealed interface AppError` (`Network`, `Timeout`, `Http(status, code, retryAfter)`, `Unauthorized`, `Storage`, `Unknown(cause)`; thêm loại khi nghiệp vụ cần). `AppError` không mang chuỗi hiển thị; `Unknown.toString()` chỉ in tên loại.
- Ngoại lệ chỉ bị bắt ở biên (network, database, IO) bằng helper `catching` không nuốt `CancellationException`. Cấm `runCatching` trong code suspend.
- `:core:ui`: `UiError` + `AppError.toUiError()` + `UiError.message()` (composable, lấy chuỗi từ resource). State lưu `UiError?`, không lưu `String`.

## Phương án đã cân nhắc

### `kotlin.Result` + `Throwable`
- Nhược: chỉ mang `Throwable`; UI phải `is`-check loại ngoại lệ; `runCatching` nuốt hủy coroutine.
- Lý do không chọn: lỗi không có cấu trúc, dễ lộ message.

### Ném ngoại lệ tới ViewModel rồi `try/catch`
- Lý do không chọn: mỗi ViewModel tự đổi lỗi, không nhất quán; dễ quên.

## Hệ quả

### Tích cực
- Đổi ngôn ngữ thì thông báo lỗi đổi theo (State không giữ chuỗi đã dịch).
- Một chỗ đổi lỗi mạng → `AppError`.

### Tiêu cực
- Phải map hai lần (`AppError` → `UiError` → chuỗi).

### Rủi ro
- Nhồi mọi lỗi vào `Http` rồi UI đoán theo status: thêm loại `AppError` khi đặc tả phân biệt.

## Cách xác nhận

- Review: không còn `e.message` trong State, log hay UI; không `runCatching` trong code suspend (detekt rule hoặc grep trong CI).
