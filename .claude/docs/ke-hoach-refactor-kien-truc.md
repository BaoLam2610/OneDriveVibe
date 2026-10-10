# Kế hoạch refactor kiến trúc (nền móng common)

- **Trạng thái:** đã duyệt 2026-10-06
- **Tiến độ:** xem mục "Đang làm" của [tien-do-mvp1.md](tien-do-mvp1.md)
- **Quyết định đi kèm:** [ADR-0016](../adr/0016-usecase-bat-buoc-giua-feature-va-data.md)
- **Phạm vi:** refactor thuần, **không đổi hành vi, không đổi schema Room** (không phải tăng `version`). Mục tiêu: dựng nền chung vững cho Lát 7 đến 9 và MVP2, không viết code theo kiểu "cho xong".
- **Thứ tự đã chốt:** build và kiểm tay các bản sửa Lát 6 → R0 đến R5 (mỗi phase một commit) → Lát 7 kèm R6.

## 1. Quy tắc làm việc

- Mỗi phase build được độc lập. Sau phase: người dùng tự build và kiểm tay, Claude chạy `/ecc:kotlin-review`, rồi commit riêng. Claude không chạy build (CLAUDE.local.md).
- Không có test tự động (ADR-0009): lưới an toàn là một mốc đang chạy đúng trước khi bắt đầu và các commit nhỏ.
- Đổi hoặc bỏ thứ gì so với ADR/template thì ghi comment nêu lý do và ADR liên quan.

## 2. Các quyết định đã chốt

| # | Quyết định | Chốt |
|---|---|---|
| 1 | UseCase giữa feature và data | **Strict**: feature chỉ thấy UseCase hoặc interface domain không phải repository (ADR-0016, thay phần UseCase của ADR-0002) |
| 2 | Hằng số | Mỗi module một file `XxxConstants.kt` chứa `internal object` chia nhóm (R3) |
| 3 | `DriveRepository` | Tách theo 4 việc: kết nối, duyệt thư mục, thư viện, màn xem |
| 4 | Thứ tự | Như trên |

## 3. Các phase

### R0. Quyết định và tài liệu (S)
- [x] ADR-0016: UseCase bắt buộc giữa feature và data; đánh dấu phần UseCase của ADR-0002 là bị thay thế
- [x] `tech-stack.md` §9: quy ước Constants, quy ước thời gian (`Clock`/`TimeSource`, không `() -> Long`), quy ước UseCase strict
- [x] `tech-stack.md` §4: sửa dòng "UseCase chỉ tạo khi có logic thật"; §9 bảng "Ghi đè so với skill"
- [x] Sửa tài liệu lệch code: delta 1000 → 500 ở `tech-stack.md` §8, tick checklist `SecretStore` (comment "version = 1" ở build file làm ở R1)
- [x] README ADR và `CLAUDE.md` (mục "Kiến trúc đã chốt", bảng tài liệu) cập nhật tham chiếu

### R1. Build và DI (S)
- [x] Convention plugin `odv.kmp.feature` (`androidResources` và dependency chung); 5 feature dùng lại, chỉ còn phần riêng (namespace, Paging, Coil, Media3...). Cố ý không áp dụng hai plugin Compose trong convention (cần thêm artifact vào build-logic, khó kiểm khi không build được): mỗi feature vẫn khai báo hai alias Compose
- [x] `:core:data` xuất `coreModules` (androidMain) để `:androidApp` bỏ phụ thuộc `:core:network` và `:core:database`. **Giữ `:core:security`**: `AndroidBiometricAuthenticator` dùng `SecretStore` (phụ thuộc thật, không chỉ để ghép Koin). Kế hoạch gốc ghi "bỏ 3 dependency", đã sửa thành bỏ 2
- [x] Sửa comment lệch "giữ version = 1" ở `core/database/build.gradle.kts` (ADR-0015)
- Kiểm tay: sync Gradle, build debug; mở từng màn (thiếu binding Koin chỉ lộ lúc chạy); nút bọ debug còn hiện và chất lượng thumbnail chỉnh được ở màn Debug (kiểm thứ tự module: debug đứng cuối để ghi đè); `assembleRelease` không kéo `:tools:debug`.

### R2. `:core:common` (S)
- [x] Helper `AppResult`: `flatMap`, `fold`, `getOrElse` (giữ nguyên cách xử lý `CancellationException`, không `try/catch`). Không thêm `mapFailure`/`isSuccess` vì chưa có chỗ dùng. Áp dụng ở `ConfigVault.load` và `SecurityRepositoryImpl.enableProtection`; các chỗ còn lại chuyển khi phase tương ứng viết lại file (danh sách ở R3, R4)
- [x] Thời gian: `single<Clock> { Clock.System }` trong `dataModule`, `SyncEngine` và `SyncCoordinator` nhận `clock: Clock` thay `() -> Long`. Dời `Clock` sang `domainModule` ở R5 nếu UseCase cần
- [x] Ngoại lệ có chủ ý, không đổi: `StreamDataSource` (`SystemClock.elapsedRealtime`, đếm cả lúc máy ngủ) và các `System.currentTimeMillis()` ở code Android/UI/debug
- Kiểm tay: chỉnh giờ máy tiến 16 phút rồi mở app thì tự đồng bộ (DS-04); chỉnh lùi cũng tự đồng bộ; trong 15 phút sau lần đồng bộ thì không; kéo làm mới vẫn chạy; đặt PIN B1 đến B5 vẫn thành công.

### R3. `:core:network` (M, rủi ro cao nhất)
Ranh giới R3/R4 đã chỉnh (2026-10-06): kế hoạch gốc cho R3 bỏ tham số `credentials` khỏi `GraphApi`, nhưng 6 chỗ ở `:core:data` đang truyền nó nên data sẽ hỏng build tới R4. R3 chỉ làm sạch bên trong `:core:network` và giữ chữ ký công khai của `GraphApi`; R4 làm `GraphCredentialsSource` và bỏ tham số cùng lúc với 6 chỗ lặp load config.
- [x] `BearerTokenSource` (`token()`, `invalidate()`) ở `network.auth`; `ApiService.authorizedGet(tokens, acceptRedirect, configure)` nhận nguồn token theo từng lần gọi, không còn biết `GraphCredentials`. `GraphApi` có cầu nối `GraphCredentials.bearer()`. Thuật toán 401 giữ nguyên (bỏ đúng token bị từ chối, thử lại đúng một lần)
- [x] `ApiService` cũng không còn biết dạng lỗi của Graph: thêm hook `protected abstract suspend fun mapError(response)`; `toGraphError()` chuyển về `network.graph`, `toTokenError()` về `network.auth` (mở rộng so với kế hoạch gốc vì `ApiService` đang gọi thẳng `toGraphError`)
- [x] Chia package: `network.http` (`ApiService`, `HttpClientFactory`, `RequestErrors`, `Redirects`, `NetworkFailure`), `network.traffic` (tách riêng khỏi `http` vì là API công khai cho `:tools:debug`), `network.auth` (`TokenProvider`, `BearerTokenSource`, `TokenDto`, `TokenErrors`, **`GraphCredentials`**), `network.graph` (`GraphApi`, `GraphErrors`, `dto`). `GraphCredentials` đặt ở `auth` thay vì `graph` để `auth` không phụ thuộc ngược `graph`. `NetworkModule` ở gốc nên `coreModules` không đổi
- [x] `NetworkConstants.kt` (gốc `network`): `GraphConstants`, `HttpConstants` (timeout lặp ở hai client nay một chỗ), `AuthConstants`; hàm `installTimeouts()` và `isTransientFailure()` thay cho đoạn chép lặp ở hai client factory
- [x] Helper `isRedirect()` và `httpsLocationOrNull()` (`http/Redirects.kt`) thay đoạn "302 → `Location` phải là https" lặp 3 lần trong `GraphApi`
- [x] `ApiService` và `GraphApi` dùng `getOrElse` thay `when ... return`
- [x] `TokenProvider` tính hạn token bằng `Clock` giờ thực (tham số mặc định `Clock.System`, không qua Koin); đo thời lượng request vẫn dùng `TimeSource.Monotonic`
- Kiểm tay: build debug (cả `:tools:debug` vì import đổi); kết nối với secret đúng và sai (KN-07); token hết hạn → 401 → chỉ **một** lần gọi lấy token giữa hai lần gọi Graph (tab API của màn Debug; để app ngoài nền hơn 1 giờ hoặc chỉnh giờ máy tiến 65 phút); khóa app giữa lúc đồng bộ; thumbnail (302), xem ảnh gốc (`Range`, tải tiếp), phát video (VD-14); màn Debug: danh sách API, chi tiết, công tắc che, "Sao chép cURL".

### R4. `:core:data`, `:core:security`, `:core:database` (tách R4a và R4b, chốt 2026-10-06)
Tách làm hai phase để kiểm tay riêng: R4a chạm mọi đường gọi Graph, R4b chạm dữ liệu lưu trên đĩa và PIN. Hiệu chỉnh khi khám phá: mục "get() theo vị trí" của bản rà soát đầu **không phải lỗi** (Koin phân giải `get()` theo kiểu, không theo thứ tự; chỉ nguy hiểm khi hai tham số cùng kiểu mà khác qualifier, và dự án đã dùng `named()` đúng cho client `download`). Chỉ còn là việc dọn gọn bằng `singleOf(::X)`, đưa vào R4b.

#### R4a. `GraphCredentialsSource` và bỏ chỗ lặp load config
- [x] `network.graph.GraphCredentialsSource` (`fun interface`, `suspend fun current(): AppResult<GraphCredentials>`); `GraphApi` nhận nó ở constructor (tham số thứ 5) và **bỏ tham số `credentials`** khỏi `listChildren`, `deltaPage`, `fetchThumbnail`, `getItemInfo`, `downloadContent`, `getDownloadUrl`. Ngoại lệ duy nhất: `getDrive(credentials)` vì kết nối thử KN-07 dùng config chưa lưu
- [x] `data.config.ConfigCredentialsSource` (`configs.load().map { it.toCredentials() }`): nơi duy nhất đổi lỗi đọc config (kể cả `AppLocked`) thành kết quả của lời gọi Graph; bind ở `dataModule`; `NetworkModule` thêm `get()` thứ 5 (phụ thuộc runtime network → data qua Koin, không có import)
- [x] Bỏ 6 chỗ lặp load config: `DriveRepositoryImpl` (×2, bỏ `configs` khỏi constructor), `VideoStreamRepositoryImpl` (còn 1 phụ thuộc), `SyncEngine` (bỏ `configs`; mỗi trang vẫn lấy lại credentials nên vẫn dừng ngay khi khóa, CH-03), `GraphThumbnailFetcher` và factory, `FileOriginalImageRepository` (lỗi `AppLocked` nay đến từ kết quả `downloadContent`, đi nhánh `Failure` sẵn có: phát `Failed(AppLocked)`, không xóa `.part`)
- Khác biệt hành vi đã biết, vô hại: thumbnail thử lại cỡ `medium` khi `400` sẽ load config thêm một lần; `FileOriginalImageRepository` có thể tạo thư mục cache trước khi biết app đang khóa
- Kiểm tay: kết nối (secret đúng, sai); thư mục, tìm kiếm, delta; **khóa app giữa lúc đồng bộ, mở khóa, đồng bộ tiếp**; thumbnail, ảnh gốc (kể cả tải tiếp), video; Ngắt kết nối, Quên PIN.

#### R4b. Lưu trữ, bảo mật PIN, hằng số
- [x] `PreferencesDataSource` (commonMain): `observe`, `observeEnum(key, default, entries)`, `edit` an toàn (nuốt `IOException`), `Preferences.getEnum`; DataStore tạo bằng `createPreferencesDataStore(path, dispatchers)` (`createWithPath`) ở commonMain; phần Android chỉ đưa đường dẫn từ `preferencesDataStoreFile` nên **tên tệp `browser`/`player` giữ nguyên**. `DataStoreBrowserPreferences` và `DataStorePlayerPreferences` còn khoảng 30 và 25 dòng (gồm KDoc); khóa lưu trong `PreferenceKeys`
- [x] `SecuritySettings` (domain, `suspend fun isWipeOnTooManyFailuresEnabled(): Boolean`) thay `wipeAfterFailures: () -> Int?`; ngưỡng `PinPolicy.WIPE_AFTER_FAILURES = 10`; bản tạm `DefaultSecuritySettings` trả `false` (đúng hành vi cũ); `SecurityRepositoryImpl` dùng công tắc và ngưỡng ở hai chỗ (`attemptsBeforeWipe`, `onWrongPin`)
- [x] `LockoutPolicy` (domain, `model/LockoutPolicy.kt`): `penaltyMs` và hằng số (5 lần miễn phí, 30 giây, trần 1 giờ, thêm tên `MAX_DOUBLINGS = 7` cho số 7 trước đây là số trần trong hàm); công thức giữ nguyên
- [x] Constants: `DataConstants.kt` (`SyncConstants`, `DriveConstants`, `ThumbnailConstants`, `OriginalImageConstants`, `StorageNames`, `PreferenceKeys`), `SecurityConstants.kt` (androidMain: `CryptoConstants` dùng chung cho tệp bí mật và phong bì PIN, bỏ chép lặp `TRANSFORMATION`/`TAG_BITS`/`IV_BYTES`; `KeystoreConstants`, `EnvelopeFormat`, `Argon2Constants`), `DatabaseConstants.kt`. Giá trị lưu trên đĩa hoặc nằm trong định dạng dữ liệu chỉ chuyển chỗ; đã so 47 hằng số trước/sau với `git HEAD` bằng script, không lệch. `SyncStateEntity.SINGLE_ROW_ID` giữ trong entity Room. Ghi chú ghép nối đã bỏ ở A3: `SecretNames.LOCKOUT` (`:core:security`) dùng chung
- [x] `Dispatchers`: `DatabaseModule` lấy `DispatcherProvider.io`; DataStore lấy `dispatchers.io` qua `createPreferencesDataStore`. Ngoại lệ có chủ ý, giữ nguyên (đã ghi ở `tech-stack.md` §9): `AppLockController` (cần `Dispatchers.Main.immediate`) và `ImagePage` (composable Android)
- [x] Koin: `dataModule` dùng `singleOf(::X) bind Y::class` cho mọi binding đủ tham số; `AndroidDataModule` dùng `singleOf` cho `ThumbnailCacheResetter`, tham số có tên cho `FileOriginalImageRepository`; `DisconnectUseCase` giữ lambda vì cần `getAll()`
- [x] Không còn `when ... return` nào cho `AppResult` trong repo (đã chuyển hết sang `getOrElse` ở R2, R3, R4a, R4b)
- Kiểm tay: build; đổi sắp xếp, dạng hiển thị, chế độ phát rồi **cài đè bản mới (không gỡ)** và kiểm còn nguyên; nhập sai PIN 5 lần phải chờ 30 giây, sai tiếp thì gấp đôi (thử 6 và 7 lần: 30 giây rồi 60 giây); đặt PIN, mở khóa bằng PIN và sinh trắc học trên dữ liệu **đã tạo từ bản cũ** (không gỡ app, không xóa dữ liệu: kiểm định dạng phong bì và tên tệp bí mật không đổi); tắt app giữa lúc bị khóa nhập rồi mở lại vẫn còn thời gian chờ; Ngắt kết nối vẫn xóa Room, thumbnail, ảnh gốc, video như trước (**không** kiểm `odv.db`/`exoplayer_internal.db`: đó là nợ kỹ thuật riêng).

### R5. `:core:domain` và các `:feature:*` (L; chia R5a đến R5d, chốt 2026-10-06)
Quyết định đã chốt: chia 4 phase; `BrowserPreferences`, `PlayerPreferences`, `SecuritySettings` là interface **không phải repository** nên feature được dùng trực tiếp (chuyển sang gói `settings`); app shell (`MainActivity`, `ODVNavDisplay`, `AppLockController`, `MainApplication`, `SplashViewModel`) cũng đi qua UseCase; liên kết Koin của UseCase đặt ở **`:core:data`** (`useCaseModule`, thêm vào `coreModules`) để domain không phụ thuộc Koin (đổi tên so với `domainModule` của kế hoạch gốc). Mỗi phase dời code **nguyên văn** từ ViewModel sang UseCase, không viết lại thuật toán.

Đính chính so với kế hoạch gốc và ADR-0016:
- Quy tắc "feature chỉ thấy UseCase" **không ép được bằng Gradle** (feature phụ thuộc `:core:domain` chứa luôn interface repository); chỉ giữ bằng quy ước và review. Thêm lệnh `grep` vào checklist kiểm (xem R5d).
- **Không** chuyển "có mạng lại thì `syncIfStale`" vào `SyncCoordinator`: singleton đó chạy từ lúc mở app nên sẽ đồng bộ cả khi đang ở màn Khóa (vô ích, config chưa giải mã) và đổi hành vi hiện tại (chỉ khi màn Thư mục/Thư viện mở). Giữ hành vi bằng `ObserveOnlineAndSyncUseCase` (tác dụng phụ trong lúc được thu thập).
- Số UseCase ước tính khoảng 27 (đa số mỏng), nhiều hơn "khoảng 10" của ADR-0016 vì áp dụng strict cho cả app shell.

#### R5a. Cấu trúc domain (không đổi hành vi)
- [x] Tách gói `repository/`: `platform` (`BiometricAuthenticator`, `NetworkMonitor`, `UtcOffsetProvider`), `hook` (`ConnectionResetter`, `ThumbnailCache`, `ThumbnailQuality`), `settings` (`BrowserPreferences`, `PlayerPreferences`, `SecuritySettings`); `OriginalImageRef` và `OriginalImageState` sang `model/OriginalImage.kt`; `repository` giữ `ConfigRepository`, `SecurityRepository`, `SyncRepository`, `VideoStreamRepository`, `OriginalImageRepository`
- [x] Tách `DriveRepository` thành `ConnectionRepository` (`verifyConnection`), `FolderRepository` (`listChildren`, `observeChildren`, `search`, `folderPathOf`, `SEARCH_LIMIT`), `LibraryRepository` (`libraryPages`, `libraryDays`), `ViewerRepository` (`observeViewerItems`, `getImageInfo`); `DriveRepositoryImpl` tách thành `ConnectionRepositoryImpl`, `FolderRepositoryImpl`, `LibraryRepositoryImpl`, `ViewerRepositoryImpl` (code dời nguyên văn; `ViewerRepositoryImpl` dùng `FolderRepository.observeChildren` cho ngữ cảnh Thư mục thay vì gọi hàm cùng lớp); `dataModule` bind bốn lớp bằng `singleOf`
- [x] ViewModel hiện có chỉ đổi kiểu constructor và tên biến (`ConnectViewModel` → `ConnectionRepository`, `BrowserViewModel` → `FolderRepository`, `LibraryViewModel` → `LibraryRepository`, `ImageViewerViewModel` và `PlayerViewModel` → `ViewerRepository` + `FolderRepository`); `ImageViewerModule` và `PlayerModule` đổi tham số có tên; chưa dùng UseCase
- [x] Đổi import hàng loạt bằng script, kiểm lại bằng script Python (không còn file thiếu import, 0 lỗi) và `grep` (không còn tham chiếu `DriveRepository` hay đường dẫn gói cũ)
- Kiểm tay: chạy trọn luồng Splash, Kết nối, PIN, Thư mục (lần đầu và từ Room), tìm kiếm (kể cả bấm kết quả mở đúng thư mục), Thư viện, ảnh (bảng thông tin có đường dẫn thư mục), video (bảng thông tin), Quên PIN; Debug: "Xóa cache thumbnail" và "Xóa dữ liệu local" vẫn chạy.

#### R5b. Khởi động và bảo mật (rủi ro cao: khóa, PIN, vòng đời)
- [x] Model mới (`domain/model`): `SavedConnection`, `StartDestination` (`StartupModels.kt`), `LockStatus`, `ProtectionSetupResult` (cuối `SecurityModels.kt`)
- [x] 13 UseCase: `usecase/connection` (`CheckSavedConnectionUseCase` dùng chung Connect và Splash, `ResolveStartDestinationUseCase`, `ConnectUseCase`), `usecase/security` (`ObserveLockStateUseCase`, `ObserveProtectionUseCase`, `InitializeSecurityUseCase`, `LockAppUseCase` không suspend, `UnlockWithPinUseCase` và `UnlockWithBiometricUseCase` đều chạy `Wiped → DisconnectUseCase` (KH-06), `GetLockStatusUseCase` gộp ba giá trị, `GetLockoutRemainingUseCase` cho nhịp mỗi giây, `EnableProtectionUseCase` gồm "có sinh trắc thì hỏi" (BM-02), `EnableBiometricUseCase`)
- [x] `UseCaseModule.kt` ở `:core:data` (`factoryOf`), thêm vào `coreModules`
- [x] Sửa `ConnectViewModel` (đổi tên hàm `connect()` thành `onConnect()` vì trùng tên tham số UseCase), `LockViewModel` (6 phụ thuộc UseCase; ngưỡng cảnh báo `WARN_WHEN_ATTEMPTS_LEFT` và nhịp `TICK_MS` ở lại ViewModel, dời sang Constants ở R5d), `SecuritySetupViewModel` (đổi tên hàm `enableBiometric()` thành `startBiometricEnrollment()` vì trùng tên tham số), `SplashViewModel` (thân chỉ còn gọi `ResolveStartDestinationUseCase`); app shell: `AppLockController`, `MainApplication`, `MainActivity`, `ODVNavDisplay`
- [x] Sau R5b, `:feature:auth` và `:androidApp` không còn import `SecurityRepository` hay `ConfigRepository` (kiểm bằng `grep`); `AndroidBiometricAuthenticator` chỉ dùng `SecretStore` và `BiometricAuthenticator`
- Điểm bất biến đã giữ: `LockAppUseCase` không suspend; `ObserveLockStateUseCase` trả đúng `StateFlow` của repository (không bọc lại) vì cổng khóa đọc `.value` đồng bộ và thu bằng `collectAsState`; Splash vẫn chờ `Unlocked` trước khi quyết định; `Wiped → Disconnect` vẫn chạy trong coroutine của ViewModel (`DisconnectUseCase` có `NonCancellable`); PIN vẫn do ViewModel sở hữu và tự xóa.
- Hạn chế kiểm thử: nhánh `Wiped` (KH-06) **không kiểm tay được** vì tùy chọn "Xóa dữ liệu khi nhập sai quá nhiều" chưa có UI (`DefaultSecuritySettings` luôn tắt, tới Lát 9); chỉ kiểm bằng đọc code.
- Kiểm tay: khởi động nguội (chưa có config → Kết nối; có config chế độ thiết bị → Thư mục; bật PIN → màn Khóa rồi Thư mục); kết nối secret đúng và sai; K6; đặt PIN B1 đến B6 (PIN yếu, nhập lại sai, hỏi sinh trắc học, "Để sau"); khôi phục sau khi hệ điều hành thu hồi tiến trình khi K6 hoặc màn Thiết lập đang mở (vào thẳng Thư mục); bật PIN rồi bấm Home → mở lại thấy màn Khóa, Back không vào nội dung, ảnh ở danh sách app gần đây bị ẩn; sai PIN (lần 5 chờ 30 giây, thoát app giữa lúc chờ rồi mở lại vẫn còn đồng hồ, sinh trắc học không né được); sinh trắc học tự hiện, hủy thì về PIN; bấm Home ngay sau khi nhập đủ PIN (không được vào nội dung); Quên PIN 2 bước; nút bọ debug vẫn hiện ở màn Khóa; "Xóa dữ liệu local" vẫn chạy.

#### R5c. Đồng bộ, thư mục, thư viện
- [x] Model mới: `SyncStatus` (cuối `SyncState.kt`; thay `BrowserSync` và `LibrarySync` trùng nhau ở hai contract, hai kiểu đó đã xóa) và `FolderContent` (`Loading`, `Loaded`, `Failed`; thay `Content` riêng tư của `BrowserViewModel`)
- [x] 8 UseCase: `usecase/sync` (`ObserveSyncStatusUseCase` gồm quy tắc bỏ qua `AppLocked`, `SyncIfStaleUseCase`, `RefreshSyncUseCase`, `ObserveOnlineAndSyncUseCase` giữ hành vi "có mạng thì `syncIfStale` khi đang được thu thập"), `usecase/folder` (`ObserveFolderContentUseCase` gồm chọn Room hay API theo TM-07, sắp xếp và lọc TM-03; `SearchDriveUseCase`), `usecase/library` (`ObserveLibraryPagesUseCase`, `ObserveLibraryDaysUseCase`); đăng ký ở `useCaseModule`
- [x] `BrowserViewModel` (7 phụ thuộc: 6 UseCase và `BrowserPreferences`) và `LibraryViewModel` (6 UseCase và `UtcOffsetProvider`) không còn thấy `FolderRepository`, `LibraryRepository`, `SyncRepository`, `NetworkMonitor`. Vòng `combine(folderId, synced, retryTick) → flatMapLatest` ở lại ViewModel (là logic trình bày); log "hiển thị N mục" không còn in số mục chưa lọc
- Dời sang R5d (chỉ có màn xem dùng): `ObserveOnlineUseCase` (Player), `GetFolderPathUseCase`
- Lưu ý: lần chạy lại đồng bộ sau khi mở khóa **đã nằm trong `SyncCoordinator`** (sửa 2026-10-07), `ObserveOnlineAndSyncUseCase` không làm lại việc đó. Mục DS-04 ở chế độ thiết bị (process vào foreground thì `syncIfStale()`) **chưa làm**, chờ quyết định; nếu làm thì bằng một UseCase gọi từ app shell, không đặt vào ViewModel của từng tab.
- Kiểm tay: Thư mục khi quét lần đầu chưa xong (TM-07: danh sách lấy từ API, banner "Đang lập chỉ mục"), chuyển sang đọc Room khi quét xong, kéo làm mới (DS-04), banner offline (bật chế độ máy bay), có mạng lại thì tự đồng bộ nếu quá hạn, tìm kiếm (kể cả bấm kết quả mở đúng thư mục), đổi sắp xếp và dạng hiển thị rồi mở lại app (còn nhớ), "Thử lại" khi lỗi tải; Thư viện: cuộn nhanh theo tháng, chip lọc, tiêu đề nhóm "N ảnh · M video", banner đang lập chỉ mục kèm lối tắt sang Thư mục, đổi tab qua lại không làm lệch banner.

#### R5d. Màn xem
- [x] 5 UseCase (`usecase/viewer`: `ObserveViewerItemsUseCase`, `GetImageInfoUseCase`, `OpenOriginalImageUseCase`, `GetStreamUrlUseCase`; `usecase/folder`: `GetFolderPathUseCase`), đăng ký ở `useCaseModule`. **Bỏ `ObserveOnlineUseCase`** khỏi kế hoạch: `PlayerViewModel` chỉ cần trạng thái mạng và `NetworkMonitor` là interface nền tảng feature được dùng trực tiếp (ADR-0016, mục Đính chính), bọc lại chỉ thêm một lớp một dòng
- [x] `ImageViewerViewModel` (4 UseCase), `PlayerViewModel` (2 UseCase + `NetworkMonitor` + `PlayerPreferences`), `StreamUrlProvider` và `VideoPlayerFactory` (`GetStreamUrlUseCase` thay `VideoStreamRepository`); `ImageViewerModule` và `PlayerModule` đổi tham số có tên; `AndroidPlayerModule` không đổi (vị trí `get()` theo kiểu)
- [x] Constants: `DomainConstants` (`SEARCH_LIMIT` chuyển từ companion của `FolderRepository`, `MS_PER_DAY` gộp từ hai bản chép ở `Library.kt` và `LibraryLayout.kt`); `AuthConstants.kt` (`LockConstants`: ngưỡng cảnh báo KH-06, nhịp đồng hồ chờ), `BrowserConstants` (debounce tìm kiếm, `ENABLED_KINDS`), `LibraryConstants` (giãn cách cập nhật số mục theo ngày), `ImageViewerConstants` (`MAX_ZOOM`, `ZOOMED_THRESHOLD`), `PlayerConstants` thêm ba hằng số độ sáng của `DeviceLevels`. **Ngoại lệ có chủ ý:** hằng số bố cục giao diện (số cột, khoảng cách, độ cao tiêu đề, độ trễ ẩn thanh cuộn, khóa item của lưới) ở lại cạnh composable dùng nó; `PinPolicy` và `LockoutPolicy` giữ hằng số của luật ngay tại đối tượng luật
- [x] **Kiểm quy tắc ADR-0016 đạt:** `grep -rn "^import com.lambao.odv.core.domain.repository" feature androidApp/src` không còn kết quả. Phần còn lại feature/app dùng trực tiếp chỉ là interface được phép: `platform` (`NetworkMonitor`, `UtcOffsetProvider`, `BiometricAuthenticator`), `settings` (`BrowserPreferences`, `PlayerPreferences`), `hook` (`ConnectionResetter` ở `VideoCache`, `ThumbnailCache`/`ThumbnailQuality` ở công cụ debug)
- Kiểm tay: mở ảnh từ Thư mục và từ Thư viện, vuốt ảnh trước/sau, ảnh gốc (tải tiếp khi ngắt giữa chừng), bảng thông tin ảnh (đường dẫn thư mục, kích thước, thiết bị chụp), zoom (viên thuốc %, ngưỡng); phát video, chuyển video, đổi chế độ phát và khung hình rồi mở lại (còn nhớ), bảng thông tin video (đường dẫn thư mục), video lỗi/đã xóa, hết hạn link giữa chừng (VD-14, chờ trên 45 phút hoặc đổi giờ máy), vuốt độ sáng và âm lượng (VD-04), xoay màn hình; Thư viện: bấm ngày hôm nay/hôm qua (MS_PER_DAY dùng chung); tìm kiếm Thư mục (SEARCH_LIMIT dùng chung); sai PIN (cảnh báo còn ≤ 2 lần không bật được nên chỉ kiểm bằng đọc code).

### Giai đoạn tiếp theo trước Lát 7 (chốt 2026-10-07)
Thứ tự: A1, A2, A3 (nợ kỹ thuật, commit nhỏ độc lập) → R6 (`ResumableFileStore`) → R7 (tách `ODVPlayerContent`) → Lát 7 (PDF). **Đổi thứ tự 2026-10-07:** R6 trước R7 vì hai phase không đụng chung file (R6: domain, data, imageviewer; R7: player) và Lát 7a dựng `FilePdfRepository` trên `ResumableFileStore`, nên làm R6 sát Lát 7. Mỗi bước build, kiểm tay, review, commit riêng.

#### A. Nợ kỹ thuật trước Lát 7
- [x] **A1. Xóa sạch dữ liệu khi Ngắt kết nối và Quên PIN (CD-05, KH-03, KH-06).** `VideoCache.reset()`: trong `mutex.withLock` thì `release()` cache và đặt `cache = null`, xóa đệ quy `cacheDir/video`, gọi `context.deleteDatabase(StandaloneDatabaseProvider.DATABASE_NAME)` (xóa cả `-journal`/`-wal`/`-shm`); một bước lỗi vẫn chạy các bước còn lại, log chỉ ghi tên loại ngoại lệ (CH-06). Cần quyết định riêng cho `odv.db` (xóa hẳn tệp cần đóng Room trước) và DataStore (`browser`/`player` chưa xóa; CD-05 yêu cầu xóa cả cài đặt). Chi tiết ở mục "Nợ kỹ thuật cần sửa riêng" của `tien-do-mvp1.md`
- [x] **A2. DS-04 ở chế độ không PIN:** process vào foreground thì `syncIfStale()`. Làm bằng một UseCase gọi từ app shell (kiểu `AppLockController`, quan sát `ProcessLifecycleOwner` ON_START), không đặt vào ViewModel của từng tab. Lần chạy lại sau mở khóa đã nằm trong `SyncCoordinator`, không làm lại
- [x] **A3. Bỏ ràng buộc chỉ bằng comment** giữa `KeystoreConstants.LAST_WIPED_FILE` (`lock_state.bin`) ở `:core:security` và `StorageNames.LOCKOUT` (`lock_state`) ở `:core:data`: `:core:security` lộ một hằng số tên bí mật dùng chung

#### R7. Tách `ODVPlayerContent.kt` (1003 dòng, mục M1 review Lát 6)
- [x] Chỉ đụng giao diện video, không đổi hành vi (xong 2026-10-07, chờ build và kiểm tay). `ODVPlayerContent.kt` 1026 dòng tách thành: `ODVPlayerContent.kt` (127, điểm vào, tạo/nhả player), `PlayerLayer.kt` (340, giữ **toàn bộ state** và chuỗi modifier cử chỉ), `PlayerOverlays.kt` (270: `PlayerControlsOverlay`, `PlayerFeedbackOverlays`, `PlayerNextUpCard`, `PlayerSheets`, `FailureOverlay`, `SwipeHud`, `LabelPill`), `PlayerControls.kt` (194: `CenterControls`, `BottomControls`, `fraction`, `spokenDuration`), `VideoFrame.kt` (147: `VideoFrame`, `coverAspect`, `SeekFeedback`), `LockedLayer.kt` (95), `PlayerEffects.kt` (66: `PlayerPlaybackEffects` gồm nạp video, nhịp tiến độ, dừng khi xuống nền, giữ màn sáng, tạm dừng khi xem thông tin); `TapMemo`/`SwipeSession` chuyển sang `PlayerGestures.kt`. Quy tắc: composable tách ra không giữ state (nhận giá trị và lambda), thứ tự xếp lớp trong `Box` giữ nguyên (lớp khóa cuối cùng). Khác biệt vô hại: `durationMs` đọc trong `PlayerControlsOverlay` thay vì `PlayerLayer`; hai thẻ "Tiếp theo" dùng chung `PlayerNextUpCard` (trước đây chép lặp modifier). Kiểm tay: cử chỉ (chạm, chạm đúp tua cộng dồn, giữ 2x, vuốt độ sáng/âm lượng), zoom và Đặt lại zoom (dọc và ngang), khóa thao tác, thanh tua, chế độ phát và khung hình, xoay màn hình, thẻ tiếp theo, thẻ lỗi và bỏ qua video lỗi, bảng Tốc độ và Thông tin (dọc và ngang, Back)

#### R6. `ResumableFileStore` dùng chung (ảnh gốc và PDF)
- [x] `ResumableFileStore` (`:core:data` androidMain, gói `file`): nhận `RangeDownloader` (fun interface `suspend operator invoke(offset, onStart, onBytes)`) nên không biết Graph; thân thuật toán chuyển nguyên văn từ `FileOriginalImageRepository` (thư mục, trần dung lượng, `.part` tải tiếp bằng `Range`, 416 thử lại một lần, 404/410 xóa phần dở, đổi tên chỉ khi đủ byte, khóa chia theo băm, dọn bản cũ theo `cTag`, LRU theo `lastModified`, `generation` chặn ghi sau `reset()`); mỗi kho có thư mục và trần dung lượng riêng; tham số `logTag` giữ tag log `OriginalImage`
- [x] `FileOriginalImageRepository` còn khoảng 15 dòng (`ConnectionResetter by store`, chỉ bọc `api.downloadContent`); `AndroidDataModule` tạo store ngay trong `single` của repository (không thêm binding riêng để `reset()` không chạy hai lần). Hằng số: `ResumableFileConstants` (dùng chung) tách khỏi `OriginalImageConstants` (chỉ còn `CACHE_MAX_BYTES`), giá trị giữ nguyên
- [x] Đổi `OriginalImageRef`/`OriginalImageState` thành `CachedFileRef`/`CachedFileState` (`model/CachedFile.kt`, PDF dùng chung ba trạng thái Đang tải, Sẵn sàng, Lỗi); đổi cơ học ở domain (repository, UseCase) và 3 file `:feature:imageviewer`. `OriginalImageRepository` và `OpenOriginalImageUseCase` giữ tên vì là việc riêng của ảnh. Khác biệt duy nhất nhìn thấy được: câu log "Không dọn được cache tệp" thay vì "...cache ảnh gốc"
- Kiểm tay: ảnh gốc tải tiếp khi ngắt giữa chừng, mở lại ảnh đã cache không cần mạng, đổi nội dung (cTag) thì dọn bản cũ, Ngắt kết nối xóa sạch.

## 4. Rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Không có test, hỏng âm thầm | Cao | Mốc chạy đúng trước khi bắt đầu, commit nhỏ, kiểm tay theo từng phase |
| R3 đụng luồng token/401 | Cao | Giữ nguyên thuật toán, chỉ đổi chữ ký; kiểm tay đủ danh sách trên |
| R4 đổi đường dẫn DataStore làm mất lựa chọn đã lưu | Trung bình | Giữ đúng tên tệp, kiểm tay sau cập nhật |
| R5 thiếu binding Koin chỉ lộ lúc chạy | Trung bình | Chạy qua từng màn ở bản debug sau phase |
| Tách UseCase quá đà | Thấp | UseCase mỏng, mỗi cái một việc có tên nghiệp vụ |
