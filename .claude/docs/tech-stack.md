# Tech stack OneDriveVibe

> Ứng dụng cá nhân, **chỉ đọc**, đọc OneDrive for Business qua Microsoft Graph để xem **video, ảnh, truyện PDF**.
> Lộ trình: **MVP1 Android** → **MVP2 KMP (thêm iOS)**. Nguyên tắc: **sẵn sàng KMP ngay từ MVP1**, code dùng chung không phụ thuộc API Android/Java.
> **Tài khoản MVP1 (đã chốt):** chỉ OneDrive for Business, xác thực Client Credentials (app-only), nhập 4 trường `tenant_id`, `client_id`, `client_secret`, `UPN`. Không hỗ trợ OneDrive cá nhân, không MSAL. Chi tiết §5.1.

- **Cập nhật:** 2026-10-02, Lát 0. Gộp từ bản tech stack ban đầu (trước khi có repo) với thực tế trong code.
- **Nguồn sự thật về phiên bản:** `gradle/libs.versions.toml`. Bảng §1 chỉ để tra nhanh; đổi phiên bản thì sửa cả hai.
- **Quyết định kiến trúc:** `.claude/adr/`. **Kế hoạch và thứ tự làm:** `ke-hoach-mvp1.md`. Khi mâu thuẫn với file này thì ADR và kế hoạch thắng.

> ⚠️ **Không viết test** (ADR-0009): không unit test, UI test, coverage; không `:core:testing`, Kover, Turbine, MockK/Mokkery, Compose UI Test. Kiểm tay trên thiết bị, nhất là làm mới token, khóa PIN và mã hóa config, tua video, PDF nhiều trang.

---

## 1. Stack chốt

Cột **Trạng thái**: *đang dùng* = đã gắn vào module; *catalog* = đã khai báo trong `libs.versions.toml`, chưa gắn; *Lát n* = khai báo khi làm lát đó.

| Tầng | Công nghệ | Phiên bản | Vai trò | Phạm vi KMP | Trạng thái |
|---|---|---|---|---|---|
| Ngôn ngữ | Kotlin | 2.4.20 | | Common | đang dùng |
| Build | AGP, Version Catalog, convention plugin (`build-logic`) | AGP 9.1.1 | Quản lý Gradle | | đang dùng |
| SDK | min 24, target/compile 37, JVM 11 | | | | đang dùng |
| UI | Compose Multiplatform (chỉ Android ở MVP1), Material3 | CMP 1.12.1, M3 1.12.0-alpha03 | Giao diện | Android (MVP2: CMP hay SwiftUI, §6) | đang dùng |
| Kiến trúc | Clean Architecture + MVI | | State / Intent / Effect (ADR-0002) | Domain/Data: Common | đang dùng |
| ViewModel | `org.jetbrains.androidx.lifecycle` (KMP) | 2.11.0 | Giữ state MVI | Common | đang dùng (`:core:common`) |
| Điều hướng | Navigation 3 (`androidx.navigation3`) + `lifecycle-viewmodel-navigation3` | 1.2.0 / 2.11.0 | Back stack do app sở hữu (ADR-0003) | Android (kiểm tra CMP khi vào MVP2) | đang dùng (`:androidApp`) |
| DI | Koin (BOM) | 4.2.2 | ADR-0004 | Common | đang dùng (`:androidApp`) |
| Async | Coroutines + Flow | 1.11.0 | | Common | đang dùng (`:core:common`, api) |
| Serialization | kotlinx.serialization | 1.11.0 | JSON, route Navigation 3 | Common | đang dùng (`:androidApp`) |
| Mạng | Ktor Client (OkHttp trên Android, Darwin trên iOS) | 3.6.0 | Graph, token endpoint (ADR-0006) | Common + engine theo nền tảng | catalog → Lát 1 |
| CSDL | Room KMP + `BundledSQLiteDriver`, KSP | 2.8.5 / sqlite 2.6.2 / KSP 2.3.12 | Metadata, cache, chỉ mục Thư viện (ADR-0007) | Common | catalog → Lát 3 |
| Cài đặt | DataStore Preferences (KMP) | 1.2.1 | Cài đặt không nhạy cảm | Common | catalog |
| Lưu bí mật | Android Keystore (AES-GCM, khóa không xuất được) + Argon2id (`argon2kt`, dự phòng BouncyCastle) | | Mã hóa config 4 trường; token chỉ trong bộ nhớ (ADR-0008) | `expect/actual`, iOS: Keychain | Lát 1 (Keystore), Lát 2 (Argon2id) |
| Ảnh | Coil 3 (`coil-network-ktor3`) | | Tải và cache ảnh, thumbnail | Common | Lát 4 |
| Zoom ảnh | Telephoto (zoomable) hoặc tự viết transform gesture | | Zoom/pan ảnh, trang PDF | Kiểm tra KMP khi vào MVP2 | Lát 5 |
| Video | Media3 ExoPlayer + `SimpleCache` | | Phát stream từ `downloadUrl` | Android, iOS: AVPlayer | Lát 6 |
| PDF | `android.graphics.pdf.PdfRenderer` | | Render trang thành bitmap | Android, iOS: PDFKit | Lát 7 |
| Phân trang | Paging 3 (`paging-common` KMP) + `room-paging` | | Danh sách lớn | Common | **Chốt cho Lát 4** (tab Thư viện): đọc Room qua `PagingSource`, sắp xếp bằng SQL, index ghép theo ngày. Tab Thư mục vẫn dùng `Flow<List>` (một thư mục), chuyển sang Paging nếu gặp thư mục rất lớn |
| Chạy nền | Coroutine trong app (`SyncCoordinator`, scope riêng) | | Delta sync, tải | Android, iOS: BGTaskScheduler | Lát 3. **Chưa dùng WorkManager**: chế độ PIN xóa config và token khỏi bộ nhớ khi khóa (CH-03, ADR-0008) nên nền không gọi được Graph; đặc tả chỉ cần đồng bộ khi mở app và kéo làm mới (DS-04). Muốn đồng bộ lúc app đóng (chỉ chế độ thiết bị) thì viết ADR mới |
| Ngày giờ | kotlinx-datetime | | Thư viện theo ngày, ngày chụp | Common | khi dùng tới (Lát 3–4) |
| File I/O | kotlinx-io (hoặc Okio) | | Cache file, thay `java.io.File` trong code chung | Common | khi dùng tới (Lát 4, 7) |
| Log | Kermit | 2.2.0 | Log local; bản debug thêm LogWriter đẩy vào màn Debug, bản release gỡ hết writer (ADR-0012) | Common | đang dùng (`:core:common`, api) |
| Đa ngôn ngữ | Android `res/` (`values` = VI mặc định, `values-en`) + AppCompat `setApplicationLocales` + `locales_config.xml` | AppCompat 1.8.0 | VI + EN (ADR-0011), §5.5 | Android (MVP2: §5.5) | đang dùng (`:androidApp`) |
| Phân tích tĩnh | Ktlint (qua Spotless) + Detekt + compose-rules | | Chất lượng code | | chưa làm (§7) |
| Hiệu năng | R8 + Baseline Profiles | | Bản release | Android | trước khi phát hành |
| Backend | Microsoft Graph REST qua Ktor | | `/users/{UPN}/drive` (MVP1) | | Lát 1 |
| Xác thực | Client Credentials (Ktor gọi token endpoint, không MSAL) | | ADR-0005 | Common | Lát 1 |
| Test | ⛔ bỏ qua (ADR-0009) | | | | |

Plugin `kotlinSerialization`, `ksp`, `room` đã khai báo `apply false` ở `build.gradle.kts` gốc; module cần thì `alias(...)`.

**Không dùng:** Hilt, Retrofit, Graph SDK (Java), MSAL, Navigation Compose 2.x, `EncryptedSharedPreferences` (security-crypto đã deprecated), thư viện test.

---

## 2. Thay đổi so với đề xuất ban đầu

| Mục | Đề xuất ban đầu | Chốt | Lý do |
|---|---|---|---|
| Log | Napier / Kermit | **Kermit** | Chọn một; Kermit được maintain tích cực, API gọn |
| Test | JUnit + Turbine + MockK/Mockito | **Bỏ toàn bộ** (ADR-0009) | Dự án cá nhân. Nếu sau này thêm: `kotlin.test` + fake/Mokkery cho code chung (MockK/Mockito chỉ chạy JVM) |
| HTTP | Ktor 3.x | Ktor 3.6.0, pin trong catalog | Graph Java SDK không dùng được cho KMP |
| Room | Room | **Room KMP** từ đầu | Không phải chuyển lại ở MVP2 |
| Xác thực | (chưa chốt) | **Client Credentials, chỉ OneDrive for Business** | ADR-0005 |
| Chạy nền | WorkManager cho delta sync | **Coroutine trong app**, WorkManager khi cần | Kế hoạch MVP1 mục 3 (đã duyệt sau bản tech stack đầu) |
| Bổ sung | | PDF renderer, zoom ảnh, kotlinx-datetime, kotlinx-io, compose-rules, Baseline Profiles | Bản đầu thiếu so với phạm vi (truyện PDF, zoom, Thư viện theo ngày) |

---

## 3. Cấu trúc module (Clean Architecture, sẵn sàng KMP)

```
:androidApp                   Android app: Compose, Navigation 3, khởi động Koin (ADR-0010 gọi là :app; giữ tên template)
:build-logic                  convention plugin

:core:common                  [KMP] AppResult/AppError, DispatcherProvider, BaseMviViewModel, Kermit (api)
:core:domain                  [KMP] entity, interface repository, use case. Kotlin thuần
:core:data                    [KMP] repository impl, mapper, DataStore
:core:network                 [KMP] Ktor client (HttpRequestRetry), ApiService (lớp cơ sở: token, 401, ghi lưu lượng) và GraphApi, DTO Graph, HttpTrafficRecorder
:core:database                [KMP] Room database, DAO, schema
:core:security                [KMP] expect/actual: SecretStore (Keystore | Keychain), dẫn xuất khóa từ PIN
:core:designsystem            [KMP, Compose] token, theme, icon, component dùng chung

:feature:auth                 Kết nối (4 trường), Thiết lập bảo mật, Khóa
:feature:browser              tab Thư mục, tìm kiếm
:feature:library              tab Thư viện theo ngày
:feature:player               xem video
:feature:imageviewer          xem ảnh, zoom
:feature:pdfviewer            truyện PDF
:feature:settings             cài đặt

:tools:debug                  [Android, chỉ bản debug] nút bọ nổi (luôn trên cùng, kể cả trên Dialog/BottomSheet), DebugActivity: log API (có màn chi tiết, tìm kiếm, sao chép), log local, lưu trữ, tùy chọn FLAG_SECURE toàn app, công cụ khác (ADR-0012, 0013)
```

- Module core dùng `kotlin("multiplatform")` nhưng **chỉ target Android** ở MVP1 (ADR-0001). MVP2 chỉ cần thêm `iosArm64()` + `iosSimulatorArm64()` (`iosX64` đã lỗi thời) và viết `actual`.
- Module `:feature:*` tạo khi bắt đầu lát dùng tới nó (ADR-0010).
- **Convention plugin:** hiện chỉ có `odv.kmp.library` (module core). Dự kiến thêm `odv.kmp.feature` ở Lát 1 (Compose, designsystem, lifecycle-viewmodel-compose, Koin compose). Plugin `quality` (ktlint + detekt) chưa làm.

### Quy tắc phụ thuộc
- `domain` chỉ phụ thuộc `common`; không có `android.*`, `java.*`, Ktor, Room.
- `feature` → `domain` + `designsystem` (+ `common`); **không** gọi thẳng `data`/`network`.
- `data` → `domain`, `network`, `database`, `security`.
- Ép bằng cấu trúc module và scope Gradle. Ví dụ: lifecycle-viewmodel gắn vào `:core:common` bằng `implementation` để `:core:domain` không thấy `ViewModel`.

### Code dùng chung: tránh dùng
Các API sau chỉ có trên JVM/Android; dùng trong `commonMain` thì chỉ lỗi khi thêm target iOS (ADR-0001):

| Tránh | Thay bằng |
|---|---|
| `java.io.File` | kotlinx-io / Okio |
| `java.time.*` | kotlinx-datetime |
| `java.util.UUID` | `kotlin.uuid.Uuid` |
| `java.util.Locale`, `String.format` | định dạng ở tầng UI (Android) |
| `android.util.Log` | Kermit |
| `Uri`, `Context`, `SharedPreferences` | `expect/actual` hoặc interface; DataStore KMP |
| `Build.VERSION`, `LocalContext` | chỉ trong `androidMain` |
| `System.currentTimeMillis()`, `Thread`, `synchronized` | `kotlin.time.Clock`, `Mutex`, atomicfu |
| `Dispatchers.IO` gọi thẳng | tiêm `DispatcherProvider` |
| Chuỗi hiển thị trong `domain`/`data` | `AppError` có cấu trúc (ADR-0011) |

Ngoại lệ: thư viện androidx đã là KMP (lifecycle, Room, DataStore, sqlite-bundled) dùng được trong `commonMain`.

---

## 4. Quy ước MVI

`BaseMviViewModel<S, I, E>` ở `:core:common` (`com.lambao.odv.core.common.mvi`):

```kotlin
abstract class BaseMviViewModel<S : Any, I : Any, E : Any>(initialState: S) : ViewModel() {
    val state: StateFlow<S>
    val effects: Flow<E>              // Channel(UNLIMITED).receiveAsFlow(): mỗi effect nhận đúng một lần
    protected val currentState: S
    abstract fun onIntent(intent: I)
    protected fun setState(reduce: S.() -> S)
    protected fun sendEffect(effect: E)
}
```

- Không dùng framework MVI nặng (Orbit MVI là phương án dự phòng, có hỗ trợ KMP).
- **State hay Effect** (Android khuyến nghị đưa sự kiện UI vào state, vì Channel không bảo đảm effect được xử lý; ADR-0002 vẫn giữ kênh Effect):

  | Loại | Đặt ở | Ví dụ |
  |---|---|---|
  | Không được mất, hoặc phải còn sau khi xoay màn hình / đổi ngôn ngữ | **State** + Intent báo "đã xử lý" để ViewModel xóa | Dialog lỗi KN-09, sheet "Đã kết nối" KN-08, cảnh báo KH-06 |
  | Mất cũng không hại | **Effect** | Chuyển màn, mở player, snackbar nhẹ |

- Channel là `UNLIMITED` + `trySend` (giữ thứ tự, không treo). Không đổi capacity khác mà giữ `trySend`: kênh đầy thì effect bị bỏ mà không báo.
- Chỉ thu `effects` ở **một** nơi (`XxxScreen`), trong `repeatOnLifecycle(STARTED)` trên `Dispatchers.Main.immediate`.
- Reducer thuần, dễ đọc và gỡ lỗi.
- UseCase chỉ tạo khi có logic thật; không tạo UseCase chỉ gọi lại repository.
- Mỗi màn: `XxxContract.kt` (State, Intent, Effect), `XxxViewModel.kt`, `XxxScreen.kt` (nối ViewModel), `XxxContent.kt` (stateless, preview được).
- Module feature tự thêm `lifecycle-viewmodel-compose`, vì `:core:common` không lộ lifecycle (§3).

### Lỗi và kết quả (`:core:common`)
- `AppError`: `Network`, `Timeout`, `Http(status, code, retryAfterSeconds)`, `Unknown(cause)`. Không mang chuỗi hiển thị; thêm loại mới khi nghiệp vụ cần phân biệt.
- `AppResult<T>`: `Success` / `Failure(AppError)` kèm `map`, `onSuccess`, `onFailure`, `getOrNull`. Tên tránh trùng `kotlin.Result`.
- Không dùng `runCatching` trần: nó nuốt `CancellationException`. Nếu bắt lỗi rộng thì ném lại `CancellationException`.
- `AppError` → `UiError` → chuỗi ở tầng UI; làm cùng màn đầu tiên cần đến (Lát 1).

### Điều hướng (ADR-0003)
- Route: `AppRoute` (sealed, `@Serializable`, `NavKey`) trong `:androidApp/navigation`.
- `ODVNavDisplay`: `rememberNavBackStack(AppRoute.Splash)`, decorator `rememberSaveableStateHolderNavEntryDecorator` + `rememberViewModelStoreNavEntryDecorator` (ViewModel theo từng màn).
- Màn nhận lambda (`onConnected`, `onOpenFile`), không nhận back stack.

### DI (ADR-0004)
- `startKoin` ở `MainApplication`, logger mức `ERROR`. Mỗi module core/feature có `xxxModule` riêng, ghép ở `MainApplication`.
- `viewModelOf` / `koinViewModel()`. Thiếu binding chỉ lộ khi chạy: sau mỗi lần thêm module Koin, chạy qua màn đó ở bản debug.

---

## 5. Lưu ý kỹ thuật với Microsoft Graph

### 5.1 Xác thực (đã chốt, ADR-0005)
Người dùng nhập đúng **4 trường** ở màn Kết nối:

| Trường | Ghi chú |
|---|---|
| `tenant_id` | GUID hoặc tên miền tenant |
| `client_id` | Application (client) ID của app đăng ký trên Entra |
| `client_secret` | Giá trị cột **Value**, không phải Secret ID |
| `UPN` | Email tài khoản OneDrive cần đọc, vd. `user@contoso.com` |

- App trên Entra cần quyền **Application** `Files.Read.All` + **Grant admin consent**.
- Gọi `/users/{UPN}/drive/...` (app-only không có `/me`).
- Token lấy qua Ktor tới `https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token`, `scope=https://graph.microsoft.com/.default`. Token chỉ trong bộ nhớ, làm mới trước khi hết hạn (~1 giờ); nhiều request cùng cần token thì gộp thành một lần lấy (TK-03).
- `client_secret` có hạn (tối đa 24 tháng): báo rõ khi hết hạn (AADSTS7000222), cho cập nhật secret.
- `403` → thiếu quyền hoặc chưa admin consent; `404` → sai UPN hoặc OneDrive chưa được khởi tạo.
- **Ngoài phạm vi MVP1:** OneDrive cá nhân và đăng nhập tương tác. Nếu cần, thêm luồng delegated (Authorization Code + PKCE hoặc Device Code) như một auth provider riêng sau interface.
- Màn Kết nối, kiểm tra định dạng và bảng lỗi: `dac-ta-nghiep-vu.md` §3.1.

### 5.2 Mạng
- Retry cấu hình bằng plugin `HttpRequestRetry` của Ktor trong `createHttpClient` (không còn hàm tự viết): lỗi mạng, timeout, `429`, `5xx`, tối đa 2 lần, chờ đúng `Retry-After` (giới hạn 60 giây). `401` do `GraphApi` xử lý: lấy token mới và thử lại **một** lần (chỉ bỏ đúng token bị từ chối); không retry `400`/`403`/`404` (`onedrive-graph-api.md` §9).
- Offline-first (ADR-0007): UI đọc Room; `sync()` là luồng riêng. Ngoại lệ duy nhất là TM-07 (Lát 1 gọi thẳng API khi chưa có Room).
- Delta query (`/drive/root/delta`), lưu `deltaLink` và trang đang quét dở trong Room (DB-04); `410 resyncRequired` thì quét lại từ đầu.
- Phân trang theo `@odata.nextLink`. Paging 3 chỉ cân nhắc cho danh sách đọc từ Room.
- Thư viện: lập chỉ mục từ facet `photo.takenDateTime` / `video` vào Room, truy vấn theo ngày.

### 5.3 Media
- Video: `@microsoft.graph.downloadUrl` (đã ký sẵn, ngắn hạn) đưa thẳng vào ExoPlayer, tua bằng range request. URL hết hạn khi xem lâu hoặc xem tiếp thì lấy lại metadata để có URL mới (VD-14), **không** lấy lại access token.
- Ảnh: endpoint `thumbnails` cho lưới, ảnh gốc khi mở màn xem; Coil dùng chung Ktor client.
- PDF: `PdfRenderer` cần file seekable nên tải về cache trước (tải tiếp phần dở), render theo trang, giới hạn bộ nhớ bitmap.

### 5.4 Bảo mật
> Nguồn chuẩn: `dac-ta-nghiep-vu.md` §4.1 (CH-01 → CH-07) và ADR-0008.

- **Config (4 trường):** AES-GCM, lưu ciphertext vào file. Bảo mật **bật**: khóa dẫn xuất từ PIN (Argon2id) kết hợp khóa phần cứng Keystore. Bảo mật **tắt**: khóa phần cứng Keystore.
- **Không lưu PIN, không lưu hash PIN** (CH-02). PIN đúng hay sai = giải mã config được hay không. Số lần sai và thời gian chờ (KH-02) lưu riêng, bền qua tắt/mở app.
- **Access token chỉ trong bộ nhớ** (CH-03); khóa app thì xóa token và config đã giải mã khỏi bộ nhớ.
- Thư viện Argon2id đặt sau interface (`PinKeyDeriver`) để thay được. Bản Android dùng `argon2kt` (`com.lambdapioneer.argon2kt`, khai báo `androidMain` của `:core:security`); phiên bản trong `libs.versions.toml` cần kiểm tra lại khi cập nhật (ABI, trang bộ nhớ 16 KB). Chi tiết phong bì PIN, bộ đếm sai, sinh trắc học, tự khóa: ADR-0014.
- `androidx.biometric` và `androidx.lifecycle:lifecycle-process` đã khai báo trong catalog và đã gắn vào `:androidApp` (tự khóa ở bước 6, sinh trắc học `BiometricPrompt` ở bước 9).
- `FLAG_SECURE` cho màn Kết nối, Khóa, nhập PIN, Cài đặt; ẩn nội dung ở danh sách app gần đây.
- **CH-04 (đã làm ở Lát 0):** `allowBackup="false"`, `fullBackupContent="false"`, `dataExtractionRules` loại trừ mọi miền cho cả `cloud-backup` và `device-transfer` (Android 12+ bỏ qua `allowBackup` khi chuyển máy).
- **CH-06:** không cài Ktor `Logging`. Log API cho màn Debug đi qua `HttpTrafficRecorder` (chỉ bản debug, chỉ trong bộ nhớ). Từ ADR-0013 bản ghi là **đầy đủ, chưa che** (kể cả `Authorization`, `client_secret`, token, `downloadUrl`); màn Debug hiện đầy đủ mặc định và có công tắc che (`HttpTrafficEntry.masked()`). Bản release không có recorder. `followRedirects = false` để bearer không bị gửi sang máy chủ khác.

### 5.5 Đa ngôn ngữ (ADR-0011)
- **VI (mặc định) + EN.** Thêm ngôn ngữ: thêm `res/values-xx`, một dòng trong `locales_config.xml` và trong `localeFilters`.
- **Đã làm ở Lát 0:**
  - Chuỗi trong `androidApp/src/main/res/values` và `values-en`. Module feature có `res/` riêng theo cùng quy tắc.
  - `locales_config.xml` (vi, en); `androidResources.localeFilters = vi, en` để chuỗi thư viện không lẫn ngôn ngữ khác (máy dùng ngôn ngữ ngoài VI/EN thì app hiện VI, CD-10).
  - `MainActivity` là `AppCompatActivity`; service `AppLocalesMetadataHolderService` (autoStoreLocales) lưu lựa chọn trên Android ≤ 12.
- **Chọn ngôn ngữ (Lát 9, CD-10):** "Theo hệ thống / Tiếng Việt / English" qua `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"|"en"))`; "Theo hệ thống" là `LocaleListCompat.getEmptyLocaleList()`. Màn Cài đặt gọi qua interface, không gọi từ `domain`.
- Chuỗi chỉ ở tầng UI; lỗi là `AppError` → `UiError`, bảng lỗi KN-09 chọn theo mã lỗi, không dùng `e.message`.
- Định dạng theo locale cho ngày, dung lượng, thời lượng, số; không hardcode `dd/MM/yyyy`. Logic dùng kotlinx-datetime, định dạng ở tầng UI.
- Dùng `plurals` và `%1$s` trong `strings.xml`, không ghép chuỗi bằng `+`.
- **MVP2:** UI iOS dùng SwiftUI thì chuỗi ở `Localizable.xcstrings`; dùng CMP thì chuyển sang CMP resources hoặc moko-resources. Chỉ ảnh hưởng tầng UI.

---

## 6. Lộ trình MVP2 (KMP): điểm cần quyết định

| Câu hỏi | Lựa chọn | Ảnh hưởng |
|---|---|---|
| UI iOS | **(A)** Compose Multiplatform dùng chung UI / **(B)** SwiftUI, chỉ chia sẻ domain + data | A: Navigation 3, Coil, thư viện zoom phải hỗ trợ CMP. B: expose ViewModel/state sang Swift (SKIE hoặc KMP-NativeCoroutines) |
| Phần theo nền tảng | Player, PDF, chạy nền, lưu bí mật | Interface trong `domain`/`core` ngay từ MVP1: `VideoPlayer`, `PdfDocumentRenderer`, `SecretStore`, `BackgroundSyncScheduler` |

Việc của MVP2: thêm target iOS → viết `actual` (Darwin engine, Keychain, AVPlayer, PDFKit, BGTaskScheduler, `DispatcherProvider`) → UI iOS.

---

## 7. Checklist khởi tạo dự án

Tiến độ theo lát xem `tien-do-mvp1.md`; mục này chỉ theo dõi phần hạ tầng.

- [x] Chốt OneDrive for Business, Client Credentials, 4 trường (§5.1)
- [ ] Tạo app registration trên Entra: quyền Application `Files.Read.All` + Grant admin consent, client secret (người dùng tự làm)
- [x] `build-logic` với `odv.kmp.library`
- [ ] Convention plugin `odv.kmp.feature` (Lát 1)
- [ ] Plugin `quality` (ktlint qua Spotless + detekt + compose-rules): chưa có lát nào nhận; người dùng tự chạy, Claude không chạy (CLAUDE.local.md)
- [x] `libs.versions.toml` pin phiên bản các thư viện Lát 0 khai báo (§1)
- [x] Skeleton module theo §3
- [x] Ktor client + token + retry (`HttpRequestRetry`) + Kermit + log API đã làm sạch (Lát 1, Lát D)
- [x] Room KMP `BundledSQLiteDriver`, export schema (Lát 3a, chờ build và kiểm tay)
- [ ] `SecretStore` (Keystore) (Lát 1) + dẫn xuất khóa từ PIN Argon2id (Lát 2)
- [ ] CI (nếu dùng): build, ktlint, detekt, không có bước test
- [ ] Baseline Profile cho danh sách tệp và player (trước khi phát hành)

---

## 8. Phạm vi foundation

Bản tech stack đầu định nghĩa foundation 8 hạng mục. Kế hoạch MVP1 (đã duyệt) chuyển phần network, database, security sang các lát dọc; bảng dưới ghi trạng thái thật.

| # | Hạng mục | Trạng thái |
|---|---|---|
| 1 | Khởi tạo, `libs.versions.toml`, `build-logic` | Xong (`odv.kmp.library`); `odv.kmp.feature` ở Lát 1; `quality` chưa làm |
| 2 | Skeleton module | Xong |
| 3 | `:core:common`: `AppResult` / `AppError`, dispatcher, Kermit | Xong (Kermit ở Lát D) |
| 4 | `:core:network`: Ktor, token Client Credentials (gộp lấy token, TK-03), retry bằng `HttpRequestRetry`, không log `Authorization` | Xong ở Lát 1 + Lát D |
| 5 | `:core:database`: Room KMP, `version = 1`, 2 bảng lõi | Lát 3 |
| 6 | `:core:security`: `SecretStore` (Keystore), Argon2id | Lát 1 (Keystore), Lát 2 (Argon2id) |
| 7 | `:core:designsystem`: theme từ `odv-tokens.json`, không dynamic color | Xong (Foundations) |
| 8 | `:androidApp`: Koin, `NavDisplay`, edge-to-edge, `locales_config` | Xong ở Lát 0 |

### Bảng Room lõi (Lát 3)
Đồng bộ delta (DB-01 → DB-05) là xương sống: Thư mục, Thư viện, tìm kiếm và "Xem tiếp" đều đọc từ đây. Cột chi tiết chốt khi làm, dựa trên `onedrive-graph-api.md` và `onedrive-graph-responses.md`.

| Bảng | Mục đích |
|---|---|
| `drive_item` | Tệp/thư mục: id, parentId, name, `nameKey` (chữ thường, bỏ dấu), size, `mediaKind`, childCount, durationMs, `modifiedAt`, cTag, `takenAt`, `createdAt`, `scanId`. Ngày lưu epoch mili giây |
| `sync_state` | Một dòng: `rootId`, `deltaLink`, `pendingNextLink` (trang đang quét dở, DB-04), `scanId`, `scannedCount`, `initialSyncDone`, `lastSyncedAt` |

Quy ước đồng bộ (Lát 3a): mỗi trang delta ghi nguyên tử cùng `pendingNextLink`; quét đầy đủ mới (chưa có `deltaLink`) tăng `scanId`, trang cuối dọn mục có `scanId` khác trong cùng transaction với `deltaLink` mới (DB-03, DS-06). Tìm kiếm dùng `instr(nameKey, :key)`, không dùng `LIKE` (quét toàn bảng, ổn tới khoảng 100k mục; lớn hơn thì cân nhắc FTS). Delta lấy 1000 mục mỗi trang (`DELTA_PAGE_SIZE`; đo thực tế 4 trang thay vì 16, các trang nối đuôi nhau nên số trang quyết định thời gian quét). Delta dùng `$select` đúng các trường đang lưu (`DELTA_SELECT` trong `GraphApi`), chỉ gắn ở request đầu vì `nextLink`/`deltaLink` mang sẵn. Lỗi tạm thời (mạng, `429`, `5xx`) sau khi `HttpRequestRetry` đã thử: `SyncEngine` chờ `Retry-After` hoặc 2→32 giây rồi chạy tiếp từ trang dở, tối đa 5 lần liên tiếp không tiến triển. `observeChildren` có `conflate` + `distinctUntilChanged` để mỗi trang delta không làm thư mục đang mở sắp xếp và vẽ lại vô ích. Schema đổi lúc dev: `fallbackToDestructiveMigration` ở bản Android.

Thêm theo feature: `playback_progress`, `reading_progress`, `cache_entry`. Trước khi phát hành schema giữ `version = 1`; bản debug được xóa và tạo lại DB, chưa cần migration.

---

## 9. Quy ước

Tổng hợp từ rà soát skill `android-clean-architecture` và `compose-multiplatform-patterns`, cộng các quy ước nhỏ của dự án. Phần **ghi đè** là chỗ quyết định của dự án thắng ví dụ trong skill.

### Ghi đè so với skill
| Skill nói | Dự án chốt |
|---|---|
| Navigation Compose 2.8 (`NavHost`, `composable<Route>`) | **Navigation 3**, back stack do app sở hữu; dùng lambda, không truyền nav controller |
| Repository gọi remote trước rồi ghi local | **Offline-first**: `observe*()` đọc Room, `sync()` là luồng riêng (DB-05). Ngoại lệ: TM-07 |
| Mỗi thao tác một UseCase | Chỉ khi có logic thật (sắp xếp/lọc theo loại tệp, nhóm ngày TV-02, danh sách phát VD-10, khóa PIN) |
| Convention plugin khai báo sẵn iOS + `commonTest` | MVP1 chỉ target Android, không test |
| SQLDelight, Hilt | Room KMP, Koin |
| `onEvent`, lỗi lưu `state.error: String?` | `onIntent()` + kênh Effect; lỗi trong state là `UiError` có cấu trúc, **không lưu `e.message`** |

### Điểm không an toàn trong ví dụ của skill (không chép nguyên)
1. Ktor `Logging { level = HEADERS }` in cả `Authorization` → vi phạm CH-06. Dự án không cài plugin này; log API đi qua `HttpTrafficRecorder` (ADR-0012).
2. `runCatching` trong repository bắt cả `CancellationException` → ném lại hoặc dùng `AppResult`.
3. Theme `dynamicColor = true` → tắt (màu từ `odv-tokens.json`); `Build.VERSION`/`LocalContext` không đặt trong `commonMain`.
4. `rememberSystemUiController` (Accompanist) đã deprecated → `enableEdgeToEdge()` + `WindowInsetsControllerCompat` (VD-07).

### Giữ nguyên từ skill
- `domain` Kotlin thuần, không lộ Entity/DTO ra UI; mapper là extension function cạnh model tầng data.
- Mỗi màn một data class state, expose `StateFlow`, UI dùng `collectAsStateWithLifecycle()`; tách `Screen` (có ViewModel) và `Content` (stateless).
- Koin `viewModelOf` / `koinViewModel()`; convention plugin trong `build-logic`.
- Hiệu năng: `key` cho lazy list, `derivedStateOf`, `remember`; `@Immutable` cho UI model chứa `List`.

### Quy ước nhỏ
- **Màu:** không dynamic color; màu chỉ từ `ODVTheme.colors`, không viết hex trong màn hình. Theme XML `Theme.OneDriveVibe` (AppCompat) không đặt màu.
- **Tên:** code UI có tiền tố `ODV` (`ODVSplashScreen`, `ODVNavDisplay`); code không phải UI thì không (`AppRoute`, `MainApplication`). `OneDriveVibe` chỉ là tên app.
- **Log:** dùng `Logger.withTag("Tên")` của Kermit (log local; không bao giờ đưa bí mật vào Kermit, kể cả khi log API đầy đủ ở màn Debug); Koin mức `ERROR`; không bao giờ log Client Secret, access token, PIN, config đã giải mã, giá trị ô nhập (CH-06). Chỉ log kết quả và mã lỗi. Bản release không có writer nào (ADR-0012).
- **Dispatcher:** tiêm `DispatcherProvider`, không gọi `Dispatchers.IO` trong code dùng chung.
- **Bỏ hoặc đổi so với template/ADR:** ghi comment trong code nêu lý do và ADR liên quan.

---

## 10. Architecture Decision Records

ADR nằm ở **`.claude/adr/`** (có `README.md` làm mục lục và `template.md`). Một thư mục dùng chung cho cả Android và iOS. Muốn đổi quyết định thì viết ADR mới, không sửa ADR cũ.

| ADR | Quyết định | Trạng thái |
|---|---|---|
| 0001 | Android trước, sẵn sàng KMP: module dùng chung chỉ target Android ở MVP1 | accepted |
| 0002 | Clean Architecture + MVI (State/Intent/Effect); chỉ tạo UseCase khi có logic thật | accepted |
| 0003 | Navigation 3 thay cho Navigation Compose 2.x | accepted |
| 0004 | Koin thay cho Hilt | accepted |
| 0005 | Xác thực Client Credentials, chỉ OneDrive for Business, nhập 4 trường | accepted |
| 0006 | Gọi Graph REST trực tiếp qua Ktor, không Graph SDK hay MSAL | accepted |
| 0007 | Room KMP, offline-first: Room là nguồn dữ liệu duy nhất cho UI, đồng bộ delta | accepted |
| 0008 | Bảo mật config: khóa dẫn xuất từ PIN (Argon2id) + Keystore, không lưu PIN hay hash | accepted |
| 0009 | Không viết test tự động | accepted |
| 0010 | Chia module: tách nhỏ core, module feature tạo khi bắt đầu làm feature đó | accepted |
| 0011 | Đa ngôn ngữ VI + EN qua Android `res/`, domain không chứa chuỗi hiển thị | accepted |
| 0012 | Công cụ debug trong app (`:tools:debug`, chỉ bản debug), log bằng Kermit, retry bằng `HttpRequestRetry` | accepted |
| 0013 | Log API bản debug hiển thị đầy đủ (không che), che là tùy chọn | accepted |
| 0014 | Phong bì PIN hai lớp, bộ đếm sai bền, sinh trắc học bọc khóa dẫn xuất, tự khóa (bổ sung ADR-0008) | accepted |
