# Kiến trúc

Clean Architecture ba tầng (UI, Domain, Data) với MVI ở tầng UI. Lý do và phương án đã loại: ADR-0001, ADR-0002, ADR-0006.

## 1. Sơ đồ module

```
                         :app
          ┌────────────────┼─────────────────────┐
          ▼                ▼                     ▼
     :feature:*      :core:data (coreModules)   :tools:debug (debugImplementation)
          │                │
          │          ┌─────┴──────┐
          │          ▼            ▼
          │   :core:network  :core:database
          │
          ├──► :core:domain ──► :core:common
          ├──► :core:ui ──────► :core:designsystem
          └──► :core:designsystem

:core:testing  ← testImplementation của mọi module có test
build-logic    ← convention plugin, không phải module runtime
```

| Module | Loại Gradle | Chứa | Phụ thuộc |
|---|---|---|---|
| `:app` | Android application | `MainActivity`, `MainApplication` (startKoin), `NavDisplay` + route, app shell (khóa, foreground), cổng `DebugTools` (src/debug, src/release) | mọi feature, `:core:data`, `:core:ui`, `:core:designsystem` |
| `:core:common` | **Kotlin/JVM** | `AppResult`, `AppError`, `catching`, `DispatcherProvider`, extension thuần Kotlin; lộ Kermit và coroutines bằng `api` | Kermit, coroutines |
| `:core:domain` | **Kotlin/JVM** | `model`, `repository`, `platform`, `settings`, `hook`, `usecase` | `:core:common` |
| `:core:data` | Android library | Repository impl, mapper, DataStore, `DataResetter` registry, `dataModule`, `useCaseModule`, `coreModules` | `domain`, `common`, `network`, `database` |
| `:core:network` | Android library | `HttpClient` factory, API service, DTO, token, `HttpTrafficRecorder` (interface) | `common` |
| `:core:database` | Android library | Room database, entity, DAO, schema JSON | `common` |
| `:core:designsystem` | Android library + Compose | Token (màu, chữ, khoảng cách, bo góc, chuyển động), `PfxTheme`, icon, component dùng chung | không phụ thuộc core khác |
| `:core:ui` | Android library + Compose | `BaseMviViewModel`, `UiError` + map sang chuỗi, `CollectEffects`, định dạng theo locale | `common`, `designsystem` |
| `:core:testing` | Android library | Fake dùng chung, `MainDispatcherRule`, `TestDispatcherProvider`, `FixedClock`, helper test MVI | `common`, `domain`, `ui` |
| `:feature:<ten>` | Android library + Compose | Màn hình, ViewModel, Contract, `xxxModule` của feature | `domain`, `ui`, `designsystem`, `common` |
| `:tools:debug` | Android library + Compose | Nút bọ, màn Debug, bộ ghi log/API, trình duyệt dữ liệu | `common`, `network`, `database`, `designsystem` |

- `:core:common` và `:core:domain` là module **Kotlin/JVM** (`kotlin("jvm")`), nên Gradle không cho dùng `android.*` trong domain. Đây là điểm khác so với dự án KMP trước (ở đó chỉ ép bằng quy ước).
- Module feature tạo khi bắt đầu feature đó, không tạo sẵn.
- Hai feature cần cùng một thứ (player, camera, bộ chọn ngày phức tạp) thì tách thành `:core:<ten>`. **Feature không bao giờ phụ thuộc feature.**

## 2. Luật phụ thuộc

| Từ | Được thấy | Không được thấy |
|---|---|---|
| `feature` | `domain.usecase`, `domain.model`, `domain.platform`, `domain.settings`, `core:ui`, `core:designsystem` | `domain.repository`, `core:data`, `core:network`, `core:database`, feature khác |
| `domain` | `core:common`, Kotlin stdlib, coroutines, `kotlinx-datetime` | Android, Ktor, Room, Koin, Compose |
| `data` | `domain`, `network`, `database`, Android | `feature`, Compose |
| `network`, `database` | `common` | `domain` (DTO và entity không biết model domain; mapper nằm ở `data`) |
| `designsystem` | Compose | mọi module nghiệp vụ |

Cách ép:

1. **Gradle:** feature không khai báo `:core:data`, `:core:network`, `:core:database` (convention plugin `pfx.android.feature` không thêm chúng).
2. **Kiểu module:** domain là Kotlin/JVM.
3. **Test kiến trúc (Konsist)** ở `:app/src/test`: chặn import `{{PACKAGE}}.core.domain.repository` trong `feature/**` và `app/**`; chặn `android.` trong domain; chặn feature import feature. Xem `conventions/testing.md`.
4. **Review:** `docs/process/review-checklist.md`.

## 3. Domain

```
core/domain/src/main/kotlin/{{PACKAGE}}/core/domain/
├── model/        entity và luật nghiệp vụ (data class, sealed, policy object)
├── repository/   interface dữ liệu nghiệp vụ. CHỈ data cài, CHỈ usecase gọi
├── platform/     interface thứ nền tảng cung cấp: NetworkMonitor, BiometricAuthenticator, ForegroundSignal...
├── settings/     interface cài đặt người dùng đọc lúc chạy: ThemeSettings, PlaybackSettings...
├── hook/         điểm móc cho tầng khác cài: DataResetter, CacheStore...
└── usecase/      <chu-de>/XxxUseCase.kt
```

- **`repository`**: feature không gọi thẳng. Một repository phục vụ một nhóm việc; quá khoảng 8 hàm hoặc phục vụ nhiều màn không liên quan thì tách.
- **`platform`, `settings`**: feature được dùng trực tiếp. Bọc chúng bằng UseCase một dòng chỉ thêm một lớp vô ích.
- **`usecase`**: tên theo nghiệp vụ (`ConnectAccountUseCase`, không phải `CallLoginApiUseCase`). Một `operator fun invoke`. UseCase mỏng chỉ ủy quyền cho repository vẫn được phép.
- **Luật nghiệp vụ có hằng số** (chính sách khóa, giới hạn) đặt ở đối tượng luật trong `model` (`PinPolicy`, `LockoutPolicy`), không đặt trong file Constants của module khác.

## 4. Luồng dữ liệu

```
UI (Composable)
  │  Intent                ▲ State (StateFlow), Effect (Flow, một lần)
  ▼                        │
ViewModel (BaseMviViewModel<S, I, E>)
  │  gọi UseCase, nhận AppResult / Flow
  ▼
UseCase (domain)
  │
  ▼
Repository interface (domain) ◄── cài bởi ── RepositoryImpl (data)
                                              │            │
                                         ApiService    Dao / DataStore
                                         (network)     (database)
```

- **Đọc:** UI thu `state` bằng `collectAsStateWithLifecycle()`.
- **Ghi:** UI gửi Intent; ViewModel gọi UseCase trong `viewModelScope`; kết quả đổi State hoặc gửi Effect.
- **Lỗi:** data trả `AppResult.Failure(AppError)`; ViewModel đổi sang `UiError` trong State; UI đổi `UiError` sang chuỗi. Xem `conventions/error-handling.md`.
- **Offline-first (nếu dự án chọn, cần ADR riêng):** UI chỉ đọc CSDL; đồng bộ là luồng riêng do một coordinator ở app scope sở hữu. Xem `conventions/data-layer.md`.

## 5. Cấu trúc một feature

```
feature/cart/src/main/kotlin/{{PACKAGE}}/feature/cart/
├── CartContract.kt        CartState, CartIntent, CartEffect
├── CartViewModel.kt
├── CartScreen.kt          PfxCartScreen: nối ViewModel, thu Effect, truyền lambda điều hướng
├── CartContent.kt         PfxCartContent: stateless, có @Preview
├── components/            composable con của màn (không giữ state)
├── CartConstants.kt       hằng số không phải bố cục
└── CartModule.kt          val cartModule = module { viewModelOf(::CartViewModel) }
feature/cart/src/main/res/values/strings.xml       (VI hoặc ngôn ngữ mặc định)
feature/cart/src/main/res/values-en/strings.xml
feature/cart/src/test/kotlin/.../CartViewModelTest.kt
```

## 6. Đặt cái gì ở đâu

| Cần thêm | Đặt ở |
|---|---|
| Màn hình mới của tính năng có sẵn | `:feature:<ten>` |
| Component dùng ở ≥ 2 feature | `:core:designsystem` (nếu chỉ là hình) hoặc `:core:ui` (nếu cần ViewModel/locale) |
| Gọi API mới | `:core:network` (DTO + hàm API) → `:core:data` (mapper + repo impl) → `:core:domain` (interface + model + UseCase) |
| Bảng Room mới | `:core:database` (entity, DAO, tăng `version`) → `:core:data` |
| Cài đặt người dùng mới | `domain.settings` (interface) → `:core:data` (DataStore impl) |
| Thứ phụ thuộc nền tảng (cảm biến, quyền, sinh trắc học) | `domain.platform` (interface) → `:app` hoặc `:core:data` (impl Android) |
| Hành vi chỉ có ở bản debug | `:tools:debug` + cổng `DebugTools` ở `:app/src/debug` |
| Hằng số | file `XxxConstants.kt` của module dùng nó |
