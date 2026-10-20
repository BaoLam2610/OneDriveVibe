# Xử lý lỗi

Quyết định: ADR-0007. Domain và data trả **lỗi có cấu trúc**; chỉ tầng UI đổi lỗi sang chữ.

```
Exception (Ktor, Room, IO)  ──map ở data/network──►  AppError  ──map ở ViewModel──►  UiError  ──map ở Composable──►  String
```

## 1. `AppResult` và `AppError` (`:core:common`)

```kotlin
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
// map, flatMap, fold, onSuccess, onFailure, getOrNull, getOrElse (inline)

sealed interface AppError {
    data object Network : AppError                       // không có mạng / không tới được máy chủ
    data object Timeout : AppError
    data class Http(val status: Int, val code: String? = null, val retryAfterSeconds: Long? = null) : AppError
    data object Unauthorized : AppError                  // phiên hết hạn, cần đăng nhập lại
    data object Storage : AppError                       // đọc/ghi máy lỗi (đĩa đầy, tệp hỏng)
    data class Unknown(val cause: Throwable? = null) : AppError {
        // Chỉ in tên loại: nội dung ngoại lệ có thể chứa URL, token, dữ liệu người dùng.
        override fun toString(): String = "AppError.Unknown"
    }
}
```

- Tên `AppResult` để không trùng `kotlin.Result` (chỉ mang `Throwable`).
- **Thêm loại lỗi khi nghiệp vụ cần phân biệt** (ví dụ `AppError.Validation(field)`, `AppError.AppLocked`). Đừng nhét mọi thứ vào `Http` rồi để UI đoán theo `status`.
- `AppError` **không** mang chuỗi hiển thị và **không** mang message của máy chủ để hiển thị.
- Repository trả `AppResult<T>` cho thao tác một lần, `Flow<T>` (hoặc `Flow<AppResult<T>>` khi luồng có thể lỗi) cho dữ liệu quan sát.

## 2. Thoát sớm gọn

```kotlin
suspend fun placeOrder(cartId: String): AppResult<Order> {
    val cart = cartRepo.get(cartId).getOrElse { return AppResult.Failure(it) }
    val payment = paymentRepo.authorize(cart.total).getOrElse { return AppResult.Failure(it) }
    return orderRepo.create(cart, payment)
}
// Hoặc ghép chuỗi: cartRepo.get(id).flatMap { paymentRepo.authorize(it.total) }.flatMap { ... }
```

Không viết `when (val r = ...) { is Failure -> return r; is Success -> r.value }` bốn dòng cho mỗi bước.

## 3. Bắt ngoại lệ ở biên

Chỉ bắt ngoại lệ ở **biên** (network, database, IO) và đổi ngay sang `AppError`. Mọi tầng phía trên chỉ thấy `AppResult`.

```kotlin
/** Như runCatching nhưng KHÔNG nuốt hủy coroutine. Đặt ở :core:common. */
suspend inline fun <T> catching(mapError: (Throwable) -> AppError = ::toAppError, block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(mapError(e))
    }
```

- **Không dùng `runCatching`** trong code suspend: nó bắt cả `CancellationException`, làm coroutine bị hủy vẫn chạy tiếp.
- `catch (e: Exception)` trần thì phải có nhánh `catch (e: CancellationException) { throw e }` đứng trước.
- Không bắt `Throwable` (nuốt cả `OutOfMemoryError`), trừ chỗ có chủ đích như giải mã ảnh lớn (bắt `OutOfMemoryError` riêng, ghi comment).
- Lỗi mạng của Ktor: `IOException`/`UnresolvedAddressException` → `Network`; `HttpRequestTimeoutException`/`SocketTimeoutException` → `Timeout`; response không thành công → `Http(status, code)` với `code` parse từ body lỗi của API (nếu có), `Retry-After` từ header.
- Coroutine chạy ở scope không có handler (app scope, coordinator) phải bắt mọi ngoại lệ không phải hủy, nếu không sẽ crash app và kẹt trạng thái "đang chạy".

## 4. `UiError` (`:core:ui`)

```kotlin
sealed interface UiError {
    data object NoConnection : UiError
    data object Timeout : UiError
    data object SessionExpired : UiError
    data object Server : UiError
    data object Unknown : UiError
    // Thêm loại theo màn khi đặc tả có thông báo riêng (vd. AUTH-09: sai mật khẩu).
}

fun AppError.toUiError(): UiError = when (this) {
    AppError.Network -> UiError.NoConnection
    AppError.Timeout -> UiError.Timeout
    AppError.Unauthorized -> UiError.SessionExpired
    is AppError.Http -> if (status >= 500) UiError.Server else UiError.Unknown
    AppError.Storage, is AppError.Unknown -> UiError.Unknown
}

@Composable
fun UiError.message(): String = when (this) {
    UiError.NoConnection -> stringResource(R.string.error_network)
    /* ... */
}
```

- State lưu `UiError?`, **không** lưu `String`, **không** lưu `e.message`. Lưu chuỗi đã dịch trong State thì đổi ngôn ngữ xong chữ vẫn là ngôn ngữ cũ.
- Màn có bảng lỗi riêng trong đặc tả (ví dụ màn đăng nhập) thì map theo **mã lỗi** (`Http.code`), không theo message.
- Lỗi tạm mà màn khác đang xử lý (ví dụ app đang khóa, phiên đang làm mới) thì không hiện như lỗi thật; ghi rõ trong ViewModel.

## 5. Log lỗi

- Log **tên loại lỗi** và mã: `log.w { "Tải giỏ hàng lỗi: $error" }` (nhờ `toString()` an toàn ở trên), hoặc `e::class.simpleName`.
- **Không bao giờ** log `e.message`, `e.toString()`, stacktrace của lỗi mạng ở bản release (có thể chứa URL ký, token, dữ liệu người dùng). Ở bản debug, stacktrace vào màn Debug.
- Xem thêm `logging.md`.
