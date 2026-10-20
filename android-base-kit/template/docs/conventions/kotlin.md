# Quy ước Kotlin

Định dạng do ktlint quyết (`.editorconfig` ở gốc). File này chỉ ghi những thứ ktlint và detekt không bắt được.

## 1. Chung

- `val` trước, `var` khi thật cần. Collection công khai là `List`/`Map`/`Set` (chỉ đọc); bên trong mới dùng `mutable*`.
- Không `!!`. Thiếu giá trị là trạng thái hợp lệ thì mô hình hóa bằng kiểu (`sealed`, nullable có xử lý); không thể xảy ra thì `error("lý do")` hoặc `checkNotNull(x) { "lý do" }`.
- `when` trên `sealed`/`enum` viết đủ nhánh, không `else` (để thêm nhánh mới thì trình biên dịch báo).
- Hàm public của module có kiểu trả về tường minh.
- Visibility: mặc định `internal` trong module; chỉ `public` thứ module khác cần. Kiểu lộ qua tham số của hàm public cũng phải public.
- Không dùng `object` làm nơi giữ state có thể thay đổi, trừ registry có chủ đích (ghi KDoc lý do) và công cụ debug.
- Extension function cho mapper (`fun UserDto.toDomain(): User`), đặt cạnh model của tầng nguồn (`data`).

## 2. Hằng số

- Mỗi module **một** file `XxxConstants.kt` chứa `internal object` chia nhóm theo mục đích:

  ```kotlin
  // core/network/.../NetworkConstants.kt
  internal object HttpConstants {
      /** Timeout kết nối. 15 giây: mạng di động yếu ở thử nghiệm mất 6 đến 9 giây. */
      const val CONNECT_TIMEOUT_MS = 15_000L
      const val MAX_RETRIES = 2
  }
  internal object AuthConstants { /* ... */ }
  ```

- **Không** rải `const val` theo từng class, không chép một hằng số ra hai nơi.
- **KDoc ghi lý do chọn giá trị**, nhất là hằng số hiệu năng (cỡ trang, timeout, trần bộ nhớ): số đo, máy đo, vì sao không lớn hơn.
- **Giá trị đã ghi xuống máy người dùng** (tên tệp, khóa DataStore, khóa Keystore, tên SharedPreferences, byte định dạng) ở nhóm riêng, KDoc ghi **"Không được đổi giá trị: dữ liệu cũ trên máy sẽ không đọc được."** Refactor chỉ được chuyển chỗ; kiểm bằng `git diff` so với HEAD.
- **Không** đặt trong file Constants:
  - luật nghiệp vụ: đặt ở đối tượng luật trong domain (`PinPolicy.MAX_ATTEMPTS`);
  - token giao diện: đặt ở `:core:designsystem` (`PfxSpacing`, `PfxMotion`);
  - hằng số bố cục chỉ có nghĩa ở một composable (số cột của một lưới, độ trễ ẩn thanh cuộn): đặt `private const val` cạnh composable đó.

## 3. Đặt tên hàm

- Hàm cập nhật state đặt **`applyX` / `updateX`**, không `setX`. `var x by mutableStateOf(...); private set` sinh setter JVM `setX(...)`, nên khai báo thêm `fun setX(...)` cùng kiểu tham số gây lỗi "Platform declaration clash".
- Hàm trong ViewModel không trùng tên tham số UseCase được tiêm (`connect` vs `connectUseCase`): đặt `onConnect()` hoặc đổi tên tham số.
- Hàm `suspend` không có hậu tố `Async`. Hàm trả `Flow` bắt đầu bằng `observe` (`observeCart()`); hàm lấy một lần là `get`/`load`.

## 4. Lớp và interface

- Interface có một hàm thì cân nhắc `fun interface` (cho phép lambda SAM). Với hàm `suspend`, kiểm lại chuyển đổi lambda khi build.
- Lớp chỉ có dữ liệu là `data class`; model UI chứa `List` mà cần ổn định cho Compose thì đánh dấu `@Immutable` (xem `compose-ui.md`).
- `sealed interface` cho tập kết quả đóng (`AppResult`, `UiError`, Intent, Effect).
- Constructor nhận phụ thuộc là interface, không nhận lambda cho giá trị người dùng có thể đổi lúc chạy (dùng interface `settings`) và không nhận `() -> Long` làm đồng hồ (xem `concurrency-time.md`).

## 5. Comment

- KDoc cho mọi thứ public của module và mọi chỗ "vì sao" không hiển nhiên.
- Comment nói **vì sao**, không kể lại code làm gì.
- Comment trỏ về nguồn: mã quy tắc (`// AUTH-04: khóa sau 5 lần sai`), ADR (`// ADR-0008`), issue.
- Bỏ hoặc đổi so với ADR/template: ghi comment nêu lý do và ADR liên quan.
- Không để code bị comment-out; xóa đi (git giữ lịch sử).

## 6. Lỗi biên dịch hay gặp khi không build được ngay

| Lỗi | Cách tránh |
|---|---|
| Platform declaration clash (`setX`) | Mục 3 |
| `suspend` gọi trong lambda không suspend (`await`, `delay` trong `onClick`) | Gọi qua `scope.launch { }` hoặc chuyển vào ViewModel |
| Thiếu import sau khi tách file | So danh sách khai báo và import với bản cũ |
| `internal` lộ qua tham số `public` | Nâng visibility của kiểu hoặc hạ visibility của hàm |
| Chữ ký API lệch phiên bản | Đọc mã nguồn đúng phiên bản trong `libs.versions.toml` |
| `@OptIn` cho API thử nghiệm (Media3 `UnstableApi`, Material3) | `@file:OptIn(...)` ở đầu file dùng; với Media3 dùng `androidx.annotation.OptIn` |
