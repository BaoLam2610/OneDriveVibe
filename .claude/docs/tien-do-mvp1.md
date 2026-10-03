# Tiến độ MVP1

Kế hoạch: [ke-hoach-mvp1.md](ke-hoach-mvp1.md). Cập nhật file này mỗi khi bắt đầu hoặc xong một bước.

Ký hiệu: `[ ]` chưa làm · `[~]` đang làm · `[x]` xong, chờ kiểm tay · `[v]` đã kiểm tay trên thiết bị

## Đang làm

- **Lát:** 3, Đồng bộ delta và offline (Lát 0, 1, 2, D code xong, còn chờ kiểm tay)
- **Bước:** Lát 3 xong code cả 3a (Room, delta sync, `SyncCoordinator`, `ConnectionResetter`, `NetworkMonitor`; đã build được) và 3b (tab Thư mục đọc từ Room, sắp xếp, lưới/danh sách, tìm kiếm, banner offline/lập chỉ mục/lỗi, kéo làm mới, DataStore nhớ lựa chọn). 3b chưa build, chưa review. Còn: build, review Lát 3, kiểm tay
- **Ghi chú:** Lát 1 chia 1a (network, security, domain, data), 1b (màn Kết nối, Thiết lập bảo mật), 1c (tab Thư mục, điều hướng khởi động). Kết nối xong lưu config ngay (chế độ thiết bị) rồi hiện hộp thoại K6; màn Thiết lập bảo mật đã có luồng đặt PIN (Lát 2, bước 8). Chuỗi đánh dấu [mới] trong strings.xml cần duyệt.

## Đã xong trước kế hoạch

- [v] Foundations: `:core:designsystem` (token, theme, icon, logo, component board 05, 08–18), gallery debug
- [x] `ODVSystemBars` (icon system bar theo theme app), `ODVScaffold` (AppBar cố định)

## Lát 0: Bộ khung app

- [x] Khai báo thư viện trong `libs.versions.toml` và build file
- [x] `:core:common`: `AppError`, `Result`, dispatcher
- [x] `BaseMviViewModel`
- [x] Khởi động Koin
- [x] Navigation 3: `NavDisplay`, route rỗng
- [x] Đa ngôn ngữ VI/EN
- [x] Tắt sao lưu tự động (CH-04)
- [x] `.claude/docs/tech-stack.md`
- [x] Review (`/ecc:kotlin-review`)
- [ ] Kiểm tay

## Lát 1: Kết nối → Danh sách thư mục gốc

- [x] `:core:network`: Ktor, token Client Credentials, TK-01 → TK-03, che log
- [x] `:core:security`: mã hóa config bằng Keystore
- [x] `:core:domain` / `:core:data`: repository config và drive
- [x] `:feature:auth`: màn Kết nối
- [x] `:feature:auth`: màn Thiết lập bảo mật (nhánh tắt)
- [x] Đổi luồng theo docs 2026-10-03: lưu config ngay sau kết nối, hộp thoại K6 (KN-13), bỏ StepBar/công tắc/sheet K4, không đọc quota (KN-12)
- [x] Review Kotlin đợt đổi luồng (không CRITICAL; đã sửa guard bấm đôi, khôi phục sau process death, thứ tự import)
- [x] Người dùng: bật lại FLAG_SECURE ở màn Kết nối (KN-10)
- [x] `:feature:browser`: tab Thư mục qua API (TM-07)
- [x] Điều hướng khởi động
- [x] Review Kotlin + bảo mật
- [ ] Kiểm tay

## Lát D: Công cụ debug (chen sau Lát 1, ADR-0012)

- [x] ADR-0012, Kermit trong `:core:common`
- [x] `HttpTrafficRecorder` + làm sạch trong `:core:network`; thay `withRetry` bằng `HttpRequestRetry`
- [x] `:tools:debug`: bộ đệm log/API, `DebugActivity` 4 tab (API, Log, Lưu trữ, Khác), nút bọ nổi
- [x] `DebugTools` bản debug/release; một app duy nhất, gallery mở từ tab "Khác"
- [x] Mở rộng: nút bọ trên cùng, màn chi tiết log API, tìm kiếm API/Log, FLAG_SECURE toàn app, log API không che (ADR-0013)
- [x] Review Kotlin + bảo mật (đợt mở rộng): không CRITICAL/HIGH; đã sửa M1, M2, M3, M5; M4, M6 ghi vào ADR-0013 (nợ bản debug)
- [x] Review Kotlin + bảo mật
- [ ] Kiểm tay

## Lát 2: Mã PIN và màn Khóa

- [x] 1. ADR-0014, thư viện (`argon2kt`, `biometric`, `lifecycle-process`) trong catalog, cập nhật `tech-stack.md`
- [x] 2. `AppError.AppLocked`
- [x] 3. `:core:security`: phong bì PIN (`PinEnvelopeCodec`), Argon2id (`PinKeyDeriver`), `BootAwareClock`, `SecretStore.wipeAll()`
- [x] 4. `:core:domain`: `SecurityRepository`, `UnlockResult`, `LockState`, `PinPolicy` (BM-06), `ConnectionResetter`, `DisconnectUseCase`
- [x] 5. `:core:data`: `ConfigVault`, `LockoutStore` (KH-02), `SecurityRepositoryImpl`, Koin
- [x] 6. `:androidApp`: tự khóa (`AppLockController` + `ProcessLifecycleOwner`), cổng khóa trong `ODVNavDisplay` (không dựng `NavDisplay` ở khởi động nguội cho tới khi `Lock` nằm trên cùng); `SplashViewModel` chờ `lockState == Unlocked` rồi mới điều hướng, `DebugTools.onAppLocked()`. Nút bọ debug hiện cả khi khóa (xem ADR-0013)
- [x] 7. `:feature:auth`: màn Khóa (KH-01 → KH-06), Quên PIN 2 bước, đồng hồ khóa tạm (viết ngay trong `ODVLockContent`, chưa tách `ODVCooldownTimer`); sinh trắc học chưa có
- [x] 8. `:feature:auth`: đặt PIN B1 → B5, B8 (BM-01 → BM-08); B6 sinh trắc học làm ở bước 9. `ConnectViewModel` không đổi nhưng `init` của nó (tự vào Danh sách khi `load()` thành công) là chỗ đỡ cho luồng PIN: đừng bỏ
- [x] 9. Sinh trắc học (bọc khóa dẫn xuất): `BiometricAuthenticator` (domain) + `AndroidBiometricAuthenticator`/`CurrentActivityHolder` (app), `SecurityRepository.enableBiometric`/`unlockWithBiometric`, B6 trong màn đặt PIN, phím sinh trắc học và hộp thoại tự hiện ở màn Khóa (L2)
- [x] 10. Ẩn nội dung ở danh sách app gần đây khi bảo mật BẬT: `SecurityRepository.isProtected`; Android 13+ `setRecentsScreenshotEnabled(false)` trong `MainActivity`, bản thấp hơn `FLAG_SECURE` toàn cửa sổ (`ODVSecureWindow` trong `ODVApp`)
- [x] 11. Chuỗi VI/EN (feature/auth 70 key, androidApp khớp VI/EN), checklist sinh trắc học trong `thiet-ke-ui.md`, ADR-0014 và tech-stack
- [x] Review Kotlin + bảo mật (bước 6 đến 9, đã sửa theo review)
- [ ] Kiểm tay

## Lát 3: Đồng bộ delta và offline

- [x] 3a. Room: schema, DAO (`:core:database`: `DriveItemEntity`, `SyncStateEntity`, `DriveDao`, `OdvDatabase`, `databaseModule`)
- [x] 3a. Delta sync (DB-01 → DB-05): `GraphApi.deltaPage`, `SyncEngine`, `SyncCoordinator` (single-flight, scope riêng, cũng là `ConnectionResetter` xóa Room), `DriveRepository.observeChildren`/`search`, `SyncRepository`, `NetworkMonitor` (Android). Chưa build; rủi ro build chính là Room KMP + KSP với convention plugin
- [x] 3b. Tab Thư mục đọc từ Room (TM-07 gọi API khi quét lần đầu chưa xong), sắp xếp (TM-02, TM-05, bottom sheet D5), lưới/danh sách (TM-06), lọc loại tệp (TM-03, tạm coi cả ba loại bật). `BrowserPreferences` (DataStore) nhớ sắp xếp và dạng hiển thị
- [x] 3b. Kéo làm mới và tự đồng bộ khi mở (DS-04), banner offline (D6, DS-05) / đang lập chỉ mục (D7, TV-06) / đồng bộ lỗi, mục xóa trên OneDrive được gỡ qua delta (DS-06)
- [x] 3b. Tìm kiếm (D4, DS-03): thanh tìm tự lấy tiêu điểm, debounce 250 ms, không phân biệt dấu, hiện đường dẫn cha, tô từ khớp, chạm thư mục kết quả thì mở đúng thư mục
- [x] TK-05, TK-06: dùng chung `HttpRequestRetry` của Lát D cho cả delta (Retry-After tối đa 60 giây)
- Hoãn có chủ ý: TM-04 (ẩn thư mục không có tệp phù hợp) và bật/tắt loại tệp (Lát 9, CD-01); dấu `cloud-off` trên thẻ tệp chưa cache và Snackbar "Cần kết nối mạng để mở tệp này" (Lát 4–7, khi có cache và màn xem)
- [ ] Review
- [ ] Kiểm tay

## Lát 4: Thumbnail, bộ nhớ đệm, tab Thư viện

- [ ] Cache (BN-01 → BN-03)
- [ ] Tab Thư viện (TV-01 → TV-06)
- [ ] Review
- [ ] Kiểm tay

## Lát 5: Xem ảnh

- [ ] AN-01 → AN-07
- [ ] Review
- [ ] Kiểm tay

## Lát 6: Xem video

- [ ] 6a: phát và điều khiển cơ bản
- [ ] 6b: phần còn lại
- [ ] Review
- [ ] Kiểm tay

## Lát 7: Xem PDF

- [ ] PD-01 → PD-08
- [ ] Review
- [ ] Kiểm tay

## Lát 8: Xem tiếp / Đọc tiếp

- [ ] DS-02, VD-12, PD-05
- [ ] Review
- [ ] Kiểm tay

## Lát 9: Cài đặt đầy đủ

- [ ] CD-01 → CD-10
- [ ] Review
- [ ] Kiểm tay

## Nhật ký

| Ngày | Việc |
|---|---|
| 2026-10-02 | Duyệt kế hoạch MVP1 |
| 2026-10-02 | Lát 0: dựng khung app (catalog thư viện, common, MVI, Koin, Navigation 3, VI/EN, tắt backup, tech-stack.md); đã review, chờ kiểm tay |
| 2026-10-02 | Lát 1: dựng network (token single-flight, retry), security (Keystore AES-GCM), domain/data, feature:auth, feature:browser, điều hướng khởi động; chờ review và build |
| 2026-10-02 | Lát D (chen sau Lát 1): công cụ debug `:tools:debug` + Kermit + HttpRequestRetry, sửa Run mở nhầm gallery; ADR-0012 |
| 2026-10-02 | Sửa lỗi: thêm `ApiService` làm lớp cơ sở cho `GraphApi`; gộp gallery vào Debug (một app), bỏ FLAG_SECURE màn Debug, nút X đóng Activity; header Kết nối/Bảo mật cố định |
| 2026-10-02 | Lát D mở rộng: nút bọ luôn trên cùng (kể cả Dialog/BottomSheet), màn chi tiết log API (JSON đẹp, +/-, sao chép, tìm kiếm có đếm), tìm kiếm Log local, tùy chọn FLAG_SECURE toàn app, log API đầy đủ không che; ADR-0013 |
| 2026-10-03 | Đổi luồng Kết nối/Thiết lập bảo mật theo docs mới: lưu config ngay (chế độ thiết bị), hộp thoại K6 bắt buộc chọn, bỏ `PendingConnection`/StepBar/công tắc/sheet K4, `verifyConnection` không trả dung lượng; chờ review và kiểm tay |
| 2026-10-03 | Review Kotlin đợt đổi luồng: sửa guard bấm đôi "Thiết lập mã PIN", `ConnectViewModel` vào Danh sách nếu config đã lưu và giải mã được (process death khi K6 hiện, KN-13), thứ tự import. Đối chiếu kế hoạch: xong code Lát 0, 1, D; chưa có `:feature:library/player/...`; Lát 2 chưa bắt đầu |
| 2026-10-03 | Review Lát D mở rộng (không CRITICAL/HIGH): che URL ở danh sách API khi bật che (M1), cắt và bắt lỗi khi sao chép body lớn (M2), dựng section/đếm kết quả/pretty-print ngoài luồng chính + debounce tìm kiếm 250 ms (M3), regex che `sig`/`tempauth` không nuốt dấu `"` (M5). Chưa làm, ghi ở ADR-0013: trần bộ nhớ tổng của log API (M4), nút bọ mở được khi app khóa và log không xóa khi khóa (M6, làm cùng Lát 2) |
| 2026-10-03 | Lát 2 bước 1 → 5: ADR-0014 (phong bì PIN hai lớp, bộ đếm sai bền, sinh trắc bọc khóa, tự khóa), `argon2kt` + `biometric` + `lifecycle-process` trong catalog, `AppError.AppLocked`, `PinEnvelopeCodec`/`PinKeyDeriver`/`BootAwareClock`/`SecretStore.wipeAll()`, `SecurityRepository` + `PinPolicy` + `DisconnectUseCase`, `ConfigVault`/`LockoutStore`/`SecurityRepositoryImpl`. Chưa build; chờ người dùng duyệt trước khi làm bước 6 → 11 |
| 2026-10-03 | Lát 2 bước 6 → 8: sửa theo review (epoch chống race `lock()`/`adopt()`, `LockoutStore` có mutex, wipe không bị hủy, `Argon2Kt` lazy, `initialize()` thử lại khi đọc lỗi), tự khóa khi cả app xuống nền, cổng khóa + màn Khóa (`LockViewModel`, `ODVLockScreen`), Quên PIN, đặt PIN (`SecuritySetupViewModel`, B1 → B5, B8), `DebugTools.onAppLocked()` xóa log API và đóng màn Debug khi khóa, ẩn nút bọ khi khóa. Chưa build; còn sinh trắc học và ẩn recents |
| 2026-10-03 | Review Lát 2 bước 6 → 8, đã sửa: cổng khóa không kẹt màn trắng khi bấm Home lúc đang giải mã (`onUnlocked` chỉ pop khi thật sự Unlocked, `LockViewModel` hủy `completion` khi bị khóa lại, cổng đẩy lại `Lock` theo đỉnh back stack), giữ `NavDisplay` trong composition và phủ nền khi khóa (không mất `rememberSaveable`), `UnlockResult.Interrupted`, xóa token khi `adopt` hoàn tác, `AppLockController` quan sát `lockState`, thêm `koin-compose` vào catalog |
| 2026-10-03 | Sửa lỗi màn hình đen sau Splash: lớp phủ nền của cổng khóa trong `ODVNavDisplay` bị mất điều kiện `if (covered)` (do lệnh căn lề tự động của chính mình), nên luôn vẽ đè lên mọi màn. Đã khôi phục `if (covered)` |
| 2026-10-03 | Sửa lỗi khởi động nguội khi bật PIN (Splash nhấp nháy; nhập đúng PIN lại vào màn Kết nối): `ODVNavDisplay` không dựng `NavDisplay` ở lần đầu cho tới khi `Lock` nằm trên cùng (Splash/Kết nối không còn chạy dưới lớp khóa), `SplashViewModel` chờ `lockState == Unlocked` rồi mới quyết định điều hướng |
| 2026-10-03 | Xử lý code review e8027e03: `lockState` thu bằng `collectAsState` (không gắn lifecycle) ở `ODVNavDisplay` và `ODVApp` để không lộ khung hình cũ khi mở lại sau lúc bị khóa; `showDebug` chỉ khi `Unlocked`; bước 10 (`isProtected` + `setRecentsScreenshotEnabled`/`FLAG_SECURE`); xóa `ODVPlaceholderScreen`, `ConfigRepository.clear()` và tham số `TokenProvider` thừa; sửa dấu cách catalog; cập nhật ghi chú tiến độ. Chưa xử lý: `.claude/settings.json` trong commit (chờ người dùng quyết) |
| 2026-10-03 | Công cụ debug: nút bọ luôn hiện ở bản debug (kể cả màn Khóa, lúc `Unknown` và trên Dialog), bản release vẫn ẩn; tab API thêm "Sao chép cURL" (thanh trên màn chi tiết và icon cuối mỗi dòng, theo công tắc che); "Sao chép" và "Mở hết/Thu gọn" đổi thành icon gọn (`DebugIconButton` + 4 vector `ic_debug_*` trong `:tools:debug`, ngoài bộ icon sản phẩm). Cập nhật rủi ro ADR-0013. Chưa build |
| 2026-10-03 | Lát 2 bước 9 và 11: sinh trắc học (`BiometricAuthenticator`, `AndroidBiometricAuthenticator` + `CurrentActivityHolder`, khóa Keystore `odv_bio_key` bọc khóa dẫn xuất ở `bio_wrap`), B6 trong màn đặt PIN, phím sinh trắc học và tự hiện hộp thoại ở màn Khóa, từ chối khi khóa tạm, đổi PIN/tắt bảo vệ xóa phần bọc; chuỗi VI/EN, checklist trong `thiet-ke-ui.md`, ADR-0014. Bước 10 đã xong từ trước. Chưa build, chưa review |
| 2026-10-03 | Review sinh trắc học và đã sửa: thiếu import `BiometricOutcome` (lỗi biên dịch), `CurrentActivityHolder.awaitResumed` không để Activity bị hủy làm treo ViewModel, kiểm tra RESUMED ngay trước `BiometricPrompt.authenticate`, `isBusy`/`isEnrollingBiometric` trả về `false` trong `finally`, khóa hỏng (mọi `GeneralSecurityException`) thì dọn và về PIN, `cleanup()` không ném ngoại lệ, kiểm tra khóa tạm lại trong mutex, bật sinh trắc học cần xác nhận, B6 không kẹt khi lỗi. Chưa build |
| 2026-10-03 | Lát 3a: Room KMP (`:core:database`, 2 bảng, DAO với transaction mỗi trang delta), `GraphApi.deltaPage` + DTO delta (`deleted`, `root`, `parentReference`, ngày, `cTag`, `photo`), `SyncEngine` (tiếp tục trang dở DB-04, `410` quét lại DB-03, dọn mục cũ cùng transaction), `SyncCoordinator` (single-flight, quá 15 phút DS-04, reset khi ngắt kết nối), `DriveRepository.observeChildren`/`search` (sắp xếp TM-02/TM-05, tìm không phân biệt dấu DS-03), `NetworkMonitor` + quyền `ACCESS_NETWORK_STATE`. `SyncRepository` tách riêng khỏi `DriveRepository` (blueprint gộp, tách cho gọn). Chưa build, chưa review; 3b (giao diện) chưa làm |
| 2026-10-03 | Lát 3a build được. Lát 3b: `BrowserViewModel` kết hợp Room/API (TM-07), sắp xếp, tìm kiếm, banner, `BrowserPreferences` (DataStore trong `:core:data`, `androidDataModule`), `FolderRef` trong `SearchResult`, `sortedFor`, giao diện lưới/danh sách + `PullToRefreshBox` + bottom sheet sắp xếp + thanh tìm (`ODVSearchBar` thêm `autoFocus`), chuỗi VI/EN (mục [mới] cần duyệt). Chưa build, chưa review |
| 2026-10-03 | Review kiến trúc đồng bộ theo tài liệu người dùng gửi (Delta → Room → UI): khung đã đúng hướng. Tối ưu: `$select` cho delta (`DELTA_SELECT`; cần xóa dữ liệu local để quét lại với tập trường mới), `SyncEngine` tự chờ và chạy tiếp từ trang dở khi lỗi tạm thời (`Retry-After` hoặc backoff 2→32 giây, tối đa 5 lần không tiến triển), `observeChildren` có `conflate` + `distinctUntilChanged`. Chốt Paging 3 cho tab Thư viện (Lát 4, ghi ở kế hoạch và tech-stack). WorkManager chưa dùng, ghi lý do (xung đột khóa PIN, CH-03/ADR-0008). Chưa build |
| 2026-10-03 | Delta `$top=1000` (`DELTA_PAGE_SIZE`, tách khỏi `CHILDREN_PAGE_SIZE` giữ 200): người dùng đo drive ~3.200 mục, 16 trang (~8 giây) còn 4 trang (~4,5 giây), Graph không cắt về 200. Chưa build |
| 2026-10-03 | Công cụ debug, tab Lưu trữ: "Làm mới" chỉ đọc lại danh sách (giữ nguyên). Thêm "Xóa dữ liệu local" (có hộp thoại xác nhận): gọi `DisconnectUseCase` như Ngắt kết nối rồi khởi động lại app ở task mới về màn Kết nối; cài đặt debug được giữ. Hook `DebugHooks.clearLocalData` đặt trong `DebugTools.install` (bản debug) vì `:tools:debug` không phụ thuộc domain. Chưa build |
