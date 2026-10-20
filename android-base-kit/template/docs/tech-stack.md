# Tech stack

- **Nguồn sự thật về phiên bản:** `gradle/libs.versions.toml`. Bảng dưới chỉ để biết dùng gì và vì sao; **không** ghi phiên bản ở đây (tránh hai nơi lệch nhau).
- Quyết định lớn có ADR; quy ước nhỏ ở `conventions/`.

## 1. Thư viện

| Tầng | Thư viện | Artifact chính | Vai trò |
|---|---|---|---|
| Ngôn ngữ | Kotlin | | |
| Build | AGP, Version Catalog, convention plugin `build-logic` | | |
| UI | Jetpack Compose (BOM), Material 3 | `compose-bom`, `material3`, `ui-tooling-preview` | Giao diện. **Tắt dynamic color**, không dùng Material Icons |
| ViewModel | AndroidX Lifecycle | `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | State MVI, `collectAsStateWithLifecycle` |
| Điều hướng | Navigation 3 | `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3` | Back stack do app sở hữu (ADR-0003) |
| DI | Koin (BOM) | `koin-android`, `koin-androidx-compose`, `koin-test` | ADR-0004 |
| Async | Kotlin Coroutines + Flow | `kotlinx-coroutines-android`, `kotlinx-coroutines-test` | |
| Serialization | kotlinx.serialization | `kotlinx-serialization-json` | JSON, route Navigation 3 |
| Mạng | Ktor Client | `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-auth` | ADR-0005. **Không** `ktor-client-logging` |
| CSDL | Room + KSP | `room-runtime`, `room-ktx` (hoặc nhóm `room3`), `room-compiler`, `room-paging` | ADR-0008. Driver: xem mục 3 |
| Cài đặt | DataStore Preferences | `datastore-preferences` | Cài đặt không nhạy cảm |
| Ảnh | Coil 3 | `coil-compose`, `coil-network-ktor3` | Tải và cache ảnh |
| Phân trang | Paging 3 | `paging-runtime`, `paging-compose` | Danh sách không giới hạn kích thước |
| Ngày giờ | kotlinx-datetime | | Logic ngày giờ ở domain |
| Log | Kermit | `kermit` | ADR-0011; release không có writer |
| Đa ngôn ngữ | AppCompat | `appcompat` | `AppCompatDelegate.setApplicationLocales` (ADR-0010) |
| Mã hóa | Android Keystore; Tink nếu cần mã hóa tệp/DataStore | `tink-android` | Chỉ khi có dữ liệu nhạy cảm; cần ADR |
| Rò bộ nhớ | LeakCanary | `leakcanary-android` (debugImplementation) | Phát hiện rò Activity/ViewModel |
| Hiệu năng | Baseline Profiles, R8 | `profileinstaller`, module `:baselineprofile` | Trước khi phát hành |

### Test (ADR-0009)

| Thư viện | Dùng cho |
|---|---|
| JUnit 4 | Runner cho unit test JVM (`src/test`) |
| `kotlinx-coroutines-test` | `runTest`, `StandardTestDispatcher`, `advanceUntilIdle` |
| Turbine | Kiểm `Flow`, `StateFlow`, kênh Effect |
| Fake tự viết (ưu tiên) | Thay repository, UseCase, interface `platform` |
| MockK | Chỉ khi fake quá tốn công (lớp của bên thứ ba). Không mock lớp của chính mình nếu viết fake được |
| `koin-test` | `verify()` cho mọi module Koin |
| Konsist | Test kiến trúc (luật phụ thuộc, tên, vị trí) |
| Room `MigrationTestHelper` | Test migration (androidTest), bắt buộc từ bản phát hành đầu |

### Chất lượng code

| Công cụ | Ghi chú |
|---|---|
| ktlint (plugin `org.jlleitschuh.gradle.ktlint` hoặc Spotless) | Định dạng. `.editorconfig` ở gốc repo |
| detekt + compose-rules (`io.nlopez.compose.rules:detekt`) | Phân tích tĩnh, luật riêng cho Compose |
| Android Lint | `warningsAsErrors` cho nhóm lỗi quan trọng; baseline cho lỗi cũ |
| CI | build, ktlint, detekt, lint, unit test cho mọi PR |

## 2. Không dùng

| Không dùng | Lý do | Thay bằng |
|---|---|---|
| Hilt / Dagger | Đã chọn Koin (ADR-0004) | Koin + `verify()` |
| Retrofit | Đã chọn Ktor (ADR-0005) | Ktor Client |
| Ktor plugin `Logging` | In header `Authorization` và body có token | `HttpTrafficRecorder` chỉ ở bản debug |
| Navigation Compose 2.x (`NavHost`) | Đã chọn Navigation 3 (ADR-0003) | `NavDisplay` |
| `EncryptedSharedPreferences` (security-crypto) | Đã deprecated | Keystore hoặc Tink |
| Accompanist System UI Controller | Đã deprecated | `enableEdgeToEdge()` + `WindowInsetsControllerCompat` |
| Material Icons | Dùng bộ icon riêng của thiết kế | `PfxIcon` |
| Dynamic color | Màu lấy từ token thiết kế | `PfxTheme` |
| `android.util.Log` | Không tắt được ở release theo một chỗ | Kermit |
| `GlobalScope`, `runBlocking` (code app) | Rò, chặn luồng | scope có chủ, `withTimeout` |
| Framework MVI nặng (Orbit, MVIKotlin) | `BaseMviViewModel` khoảng 40 dòng là đủ | `BaseMviViewModel` |

## 3. Ghi chú kỹ thuật

- **Room driver.** App Android thuần nên dùng driver SQLite của hệ thống (mặc định của Room trên Android, hoặc `AndroidSQLiteDriver` nếu bản Room đang dùng bắt buộc chọn driver). Chỉ dùng `BundledSQLiteDriver` khi cần tính năng SQLite mới hơn bản của hệ điều hành, và khi đó **không bao giờ** mở tệp CSDL bằng `android.database.sqlite` trong cùng tiến trình (hai bản SQLite cùng mở một tệp làm hỏng CSDL; đã gặp ở dự án trước). Kiểm lại API của đúng bản Room lúc khởi tạo.
- **Room schema.** `room { schemaDirectory("$projectDir/schemas") }`, commit thư mục `schemas/` vào git để review được thay đổi schema.
- **Ktor.** Một `HttpClient` cho API (có bearer, `followRedirects = false`) và, nếu cần tải URL đã ký, một client riêng không bearer. `HttpRequestRetry` cho lỗi mạng, timeout, `429`, `5xx` (tôn trọng `Retry-After`, có trần). `401` xử lý ở tầng auth: làm mới token và thử lại **một** lần. Không retry `400`/`403`/`404`.
- **Compose.** Tắt dynamic color. `enableEdgeToEdge()` ở `MainActivity`. Chữ: `includeFontPadding = false` và `LineHeightStyle(Center, Trim.None)` trong token chữ.
- **minSdk {{MIN_SDK}}.** Nếu dưới 26 mà dùng `java.time` thì bật core library desugaring, hoặc dùng `kotlinx-datetime` ở logic.
- **Phiên bản đi cặp.** Một số thư viện phải khớp nhau (Kotlin với KSP và Compose compiler; Media3 với extension bên thứ ba). Ghi cặp phiên bản trong comment của `libs.versions.toml`.
