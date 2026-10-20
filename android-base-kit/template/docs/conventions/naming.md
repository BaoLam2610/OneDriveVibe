# Đặt tên

## 1. Package và module

| Module | Package gốc |
|---|---|
| `:app` | `{{PACKAGE}}` |
| `:core:<ten>` | `{{PACKAGE}}.core.<ten>` |
| `:feature:<ten>` | `{{PACKAGE}}.feature.<ten>` |
| `:tools:debug` | `{{PACKAGE}}.tools.debug` |

- Tên module là danh từ ngắn, chữ thường, không gạch ngang nếu tránh được (`imageviewer`, không `image-viewer`).
- `namespace` trong `build.gradle.kts` trùng package gốc của module.
- Package con theo vai trò: `domain/model`, `domain/repository`, `domain/platform`, `domain/settings`, `domain/hook`, `domain/usecase/<chu-de>`; `data/<chu-de>`; `network/http`, `network/auth`, `network/<api>`.

## 2. Tiền tố UI

- **Code phục vụ hiển thị** (composable public, token, theme, type chỉ dùng cho UI) có tiền tố **`Pfx`**: `PfxTheme`, `PfxColors`, `PfxButton`, `PfxIcon`, `PfxCartScreen`, `PfxCartContent`.
- **Code không phải UI** không có tiền tố: `CartViewModel`, `CartState`, `AppRoute`, `MainApplication`, `CartRepository`.
- Tên app đầy đủ (`{{TEN_DU_AN}}`) chỉ dùng cho `app_name`, tiêu đề tài liệu, `rootProject.name`; không dùng làm tiền tố code.
- Tên file trùng type chính. File chứa một nhóm component nhỏ được đặt tên theo nhóm (`Buttons.kt` chứa `PfxPrimaryButton`, `PfxTextButton`), nhưng type bên trong vẫn có tiền tố.

## 3. Tên type theo vai trò

| Vai trò | Mẫu | Ví dụ |
|---|---|---|
| Màn hình (nối ViewModel) | `Pfx<Ten>Screen` | `PfxCartScreen` |
| Nội dung stateless | `Pfx<Ten>Content` | `PfxCartContent` |
| ViewModel | `<Ten>ViewModel` | `CartViewModel` |
| State, Intent, Effect | `<Ten>State`, `<Ten>Intent`, `<Ten>Effect` (trong `<Ten>Contract.kt`) | `CartIntent.RemoveClicked` |
| UseCase | `<DongTu><DoiTuong>UseCase` theo nghiệp vụ | `ObserveCartUseCase`, `PlaceOrderUseCase` |
| Repository | `<ChuDe>Repository` / `<ChuDe>RepositoryImpl` | `CartRepository` |
| Interface nền tảng | danh từ chỉ khả năng | `NetworkMonitor`, `BiometricAuthenticator` |
| Cài đặt | `<ChuDe>Settings` | `PlaybackSettings` |
| API service | `<Ten>Api` | `OrderApi` |
| DTO | `<Ten>Dto` | `OrderDto` |
| Entity Room | `<Ten>Entity` | `OrderEntity` |
| DAO | `<Ten>Dao` | `OrderDao` |
| Route | `AppRoute.<Ten>` | `AppRoute.ProductDetail(id)` |
| Module Koin | `<ten>Module` (val chữ thường đầu) | `cartModule`, `dataModule` |
| Hằng số | `<Module>Constants.kt` chứa `internal object <Nhom>Constants` | `NetworkConstants.kt` / `HttpConstants` |
| Fake test | `Fake<Interface>` | `FakeCartRepository` |

- Intent đặt theo **việc người dùng làm** (`RemoveClicked`, `QueryChanged`), không theo việc ViewModel sẽ làm (`DeleteItemFromDb`).
- Effect đặt theo **việc UI phải làm** (`OpenProduct`, `ShowRemovedSnackbar`).

## 4. Chuỗi resource

- Khóa: `<module>_<man>_<vai-tro>`, chữ thường, gạch dưới: `cart_empty_title`, `cart_remove_confirm`, `common_retry`.
- Chuỗi dùng chung (Thử lại, Hủy, OK) ở `:core:ui` với tiền tố `common_`.
- Chuỗi lỗi: `error_<loai>` (`error_network`, `error_timeout`, `error_server`).

## 5. Mã quy tắc nghiệp vụ và mã thiết kế

- Mỗi quy tắc trong đặc tả có mã `<NHOM>-<NN>`: hai đến bốn chữ cái in hoa chỉ nhóm màn/chủ đề + số hai chữ số (`AUTH-01`, `CART-07`). Mã **không bao giờ đổi** và không dùng lại sau khi xóa (ghi "đã bỏ").
- Artboard thiết kế có mã ngắn theo màn (`C1`, `C2` cho Giỏ hàng) để tài liệu UI và PR trỏ tới.
- Comment, mục kiểm tay, mô tả PR ghi mã: `// CART-07: xóa mục cuối thì hiện trạng thái rỗng`.

## 6. Nhánh, commit, lát

- Nhánh và commit: xem `CONTRIBUTING.md`.
- Lát (slice) trong kế hoạch có ID ổn định; thứ tự làm nằm ở một bảng duy nhất trong `docs/plan/ke-hoach.md`. Lát chen ngang lấy ID mới, **không đánh số lại** các lát khác.
