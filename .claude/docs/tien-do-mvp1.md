# Tiến độ MVP1

Kế hoạch: [ke-hoach-mvp1.md](ke-hoach-mvp1.md). Cập nhật file này mỗi khi bắt đầu hoặc xong một bước.

Ký hiệu: `[ ]` chưa làm · `[~]` đang làm · `[x]` xong, chờ kiểm tay · `[v]` đã kiểm tay trên thiết bị

## Đang làm

- **Lát:** 6, Xem video: đã build, kiểm tay OK và review xong (2026-10-04), còn các mục review chưa sửa (H1, M1 đến M4) ghi ở Lát 6; kế tiếp là Lát 7 (PDF) hoặc sửa H1 trước; Lát 5 đã build và thử máy ổn, chờ kiểm tay (Lát 4 xong; Lát 0, 1, 2, 3, D code xong, còn chờ kiểm tay)
- **Bước:** Lát 5 đã viết xong toàn bộ code (2026-10-04), chưa build màn xem. Lát 4 đã build, review hai đợt và được người dùng kiểm tay thành công. Lưu ý khi đổi schema Room: **phải tăng `version` ở `OdvDatabase`** (ADR-0015, thay quy tắc "giữ `version = 1`" cũ vốn sai: đổi cột mà không tăng version thì Room báo lỗi hash và crash)
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
- [x] Review Kotlin + bảo mật Lát 3 (2026-10-03): không CRITICAL; 2 HIGH và 2 MEDIUM đã sửa; còn 1 MEDIUM chờ quyết định (Room không mã hóa, xem nhật ký)
- [ ] Kiểm tay

## Lát 4: Thumbnail, bộ nhớ đệm, tab Thư viện

- [x] Cache thumbnail (BN-01 → BN-03): Coil 3, `GraphThumbnailFetcher` (302 → URL đã ký, client không bearer), `DiskCache` 200 MB khóa id + `cTag` + cỡ, `ThumbnailCacheResetter` (`ConnectionResetter`). Tải tiếp phần dở (BN-03) chỉ cần cho ảnh gốc và PDF, làm ở Lát 5 và 7; giới hạn dung lượng nối với Cài đặt ở Lát 9
- [x] Tab Thư viện (TV-01 → TV-06): `:feature:library` (MVI), Paging 3 + `room3-paging`, cột `sortDate` + index, lưới 4 cột dựng từ số mục theo ngày, tiêu đề nhóm dính (lớp phủ), chip lọc, cuộn nhanh theo tháng, banner offline/lập chỉ mục (kèm lối tắt sang Thư mục)/lỗi
- Hoãn có chủ ý: TV-03 chỉ hiện loại đang bật trong Cài đặt (hiện đủ ba chip, làm ở Lát 9, CD-01); `ThumbnailCacheGeneration` chặn ghi cache sau khi xóa (review 2026-10-04). Còn lại, mức LOW: log API debug bị thumbnail làm tràn (`MAX_REQUESTS = 200`), nhãn "Hôm nay/Hôm qua" không đổi khi qua nửa đêm
- [x] Tabs Thư mục / Thư viện (`ODVHomeScreen` trong `:androidApp`), thumbnail thật ở tab Thư mục (DS-01)
- [x] Review Kotlin + bảo mật Lát 4 (2026-10-04, hai đợt): không CRITICAL; đã sửa thiếu import `crossfade`, đăng ký `PagingSourceDaoReturnTypeConverter`, `coil-singleton`, cửa sổ ghi cache sau khi ngắt kết nối (`ThumbnailCacheGeneration`). Còn LOW chưa sửa: Back ở tab Thư viện thoát app, thumbnail 404/offline bị gọi lại mỗi lần cuộn vào, `LibraryGrid` recompose khi cuộn, múi giờ và "Hôm nay/Hôm qua" chốt một lần, log API debug bị thumbnail làm tràn
- [v] Kiểm tay (2026-10-04, người dùng xác nhận thành công)

## Lát 5: Xem ảnh

Quyết định (2026-10-04): thông tin ảnh lấy theo yêu cầu qua Graph (`image`, `photo`, `fileSystemInfo`), không đổi schema Room; `DiskCache` riêng cho ảnh gốc; Telephoto + `coil-gif`.

- [x] 1. Catalog: `telephoto`, `coil-gif`; cập nhật `tech-stack.md`
- [x] 2. `:core:network`: `GraphApi.getItemInfo`, `GraphApi.downloadContent` (302 → URL ký, `Range`, đọc theo luồng) + `ImageFacetDto`, `PhotoFacetDto` thêm camera. Rủi ro build: `readAvailable`, `HttpTimeoutConfig.INFINITE_TIMEOUT_MS`
- [x] 3. Domain/data: `ViewerContext`, `ImageInfo`, `DriveRepository.observeViewerImages`/`getImageInfo`, `DriveDao.observeLibraryItems`, `toImageInfo`. Còn phải nối `ViewerContext` vào `onOpenFile` của Thư mục/Thư viện (bước 8)
- [x] 4. Ảnh gốc: đổi so với plan, không dùng Coil `Fetcher` mà dùng `OriginalImageRepository` (domain) + `FileOriginalImageRepository` (`:core:data` androidMain): thư mục `cacheDir/originals` riêng, tệp `.part` tải tiếp bằng `Range` (BN-03), chỉ đổi tên khi đủ byte, dọn bản cũ theo `cTag` (BN-02), LRU 1 GB tạm cố định (Lát 9 nối Cài đặt), `ConnectionResetter` xóa sạch + chặn ghi sau reset. Lý do: Telephoto cần tệp seek được để cắt vùng ảnh lớn
- [x] 5. `:feature:imageviewer` (MVI): `HorizontalPager` + Telephoto (`ZoomableAsyncImage`), thumbnail lớn không cắt (`ThumbnailSize.Viewer` = Graph `large`) mờ dần sang ảnh gốc, viên thuốc "Đang tải ảnh gốc", GIF qua Coil + `coil-gif`, ảnh hỏng → `ODVViewerError` (đọc kích thước bằng `BitmapFactory` trước, AN-07), khóa vuốt ngang khi đang zoom, chạm một lần ẩn/hiện thanh và system bar (`ODVViewerSystemBars`)
- [x] 6. Bảng thông tin: bottom sheet (dọc) / `ODVSidePanel` (ngang); dòng từ Room hiện ngay, kích thước ảnh + thiết bị chụp lấy từ Graph khi mở bảng (`getImageInfo`), đường dẫn từ `folderPathOf`
- [x] 7. Chuỗi VI/EN (toàn bộ đánh dấu [mới], thiết kế chưa có câu chữ; cần duyệt)
- [x] 8. `AppRoute.ImageViewer` + `ViewerContext` đi qua `onOpenFile(item, context)` của Thư mục (kể cả kết quả tìm) và Thư viện; video/PDF chưa mở (Lát 6, 7)
- [x] 9. FLAG_SECURE: màn xem chạy trong cùng cửa sổ nên `ODVSecureWindow` toàn cửa sổ (Android ≤ 12) và `setRecentsScreenshotEnabled(false)` đã phủ; bottom sheet kế thừa. Cần kiểm tay khi bật PIN
- Đã build, người dùng thử máy thấy ổn (2026-10-04); lỗi biên dịch `await` trong lambda đã sửa.
- Giới hạn đã biết: (a) mở ảnh khi quét lần đầu chưa xong (TM-07, danh sách lấy từ API) thì màn xem tự đóng vì chưa có trong Room; (b) % zoom ở viên thuốc quy ước xấp xỉ theo `maxZoomFactor = 4`; (c) GIF zoom theo khung chứ không theo cạnh ảnh; (d) chỉ tải ảnh của trang đang hiện, chưa tải trước ảnh kế
- [x] Review Lát 5 (2026-10-04): không CRITICAL/HIGH; đã sửa M1, M2 (xem nhật ký). Còn MEDIUM/LOW chưa sửa: ảnh gốc trong cache không mã hóa (cùng nhóm M3 của Lát 3, chờ quyết định ADR), Thư viện nạp cả danh sách ảnh vào bộ nhớ, khoảng cách 72/84dp tự đặt cần đối chiếu thiết kế, thumbnail vẫn tải khi ảnh gốc đã có sẵn, `.part` mồ côi khi `reset()` chen giữa lúc tải, tệp không có `Content-Length` không kiểm được độ đầy, `ImageViewerInfo.isLoadingDetails` chưa dùng, màn xem vẫn ẩn system bar khi màn Khóa chèn lên, chưa có nhãn TalkBack "Ảnh X trên Y"
- [ ] Kiểm tay

## Lát 6: Xem video

Quyết định (2026-10-04, sau review docs): Media3 ExoPlayer + `SimpleCache` khóa `itemId:cTag` (không phải URL vì `tempauth` đổi mỗi giờ), URL lấy qua `ResolvingDataSource` mỗi lần mở kết nối và tự làm mới khi 401/403 (VD-14); VD-17 thêm cột video vào Room (một lần xóa dữ liệu local); xoay màn hình bằng `configChanges` trên `MainActivity` (6b); player do composable sở hữu (nhả khi rời màn hoặc màn Khóa che, vị trí lưu ở ViewModel). Cải tiến đưa vào: thumbnail làm ảnh nền lúc tải, giữ lâu phát 2x, bong bóng thời gian khi kéo thanh tua, tạm dừng khi rút tai nghe/nhường audio focus, giữ 30 giây phía sau, lỗi riêng cho video đã xóa. Hoãn: nạp trước video kế tiếp, picture-in-picture.

- [x] 6a: phát và điều khiển cơ bản (code xong 2026-10-04, chờ build). `:feature:player` (MVI, `ODVPlayerScreen`), `VideoStreamRepository` + `GraphApi.getDownloadUrl`, `StreamDataSource`/`VideoCache`/`VideoPlayerFactory`/`VideoPlayerController`, route `AppRoute.VideoPlayer`, `DriveRepository.observeViewerImages` đổi thành `observeViewerItems(context, kind)` (ảnh và video dùng chung). Gồm: phát stream, link tự làm mới (VD-14), Play/Pause/Phát lại, thanh tua có buffer, chạm đúp tua cộng dồn, trước/sau (VD-10), tốc độ (VD-05), giữ màn sáng (VD-09), lỗi codec/đã xóa/mất mạng (VD-15, VD-16), thumbnail làm ảnh nền, giữ lâu phát 2x, bong bóng thời gian khi kéo tua, nhường audio focus và dừng khi rút tai nghe, giữ 30 giây phía sau
- [x] 6b: code xong 2026-10-04, chờ build. Vuốt độ sáng/âm lượng (VD-04, `DeviceLevels` + `playerSwipeGestures`, vùng chết 24 dp hai mép và 56 dp đáy, tắt khi zoom/khóa), khung hình Vừa khung / Cắt đầy / Kéo giãn (VD-06, nhãn viên thuốc, nhớ lại), nút xoay Dọc ↔ Ngang không theo cảm biến (VD-07, VD-19: `PlayerOrientationEffect`, vào màn luôn dọc, giữ hướng qua các video, rời màn về dọc), khóa thao tác giữ 1 giây để mở (VD-08, rung nhẹ, Back chỉ nhắc), chế độ phát 4 kiểu (VD-20) và tự phát tiếp với thẻ đếm ngược 5 giây + Hủy (VD-13), Lặp danh sách quay vòng cả nút Trước/Sau (VD-10), bỏ qua video lỗi và dừng khi cả danh sách lỗi (VD-15), zoom hai ngón 1x–4x + kéo + "Đặt lại zoom" (VD-18), bảng thông tin tệp từ Room dùng được offline, video tạm dừng lúc mở và phát tiếp khi đóng (VD-17), `PlayerPreferences` (domain) + `DataStorePlayerPreferences` (data, tệp DataStore `player`) lưu chế độ phát và khung hình
- Giới hạn 6b đã biết: (a) bảng thông tin chỉ có một dòng "Ngày tạo" thay cho "Ngày tải lên" và "Ngày tạo gốc" vì Room chỉ lưu một ngày tạo (tách ra cần thêm cột và tăng version, ADR-0015); (b) Cắt đầy / Kéo giãn ở hướng dọc cho khung lấp cả màn (thiết kế ghi khung 16:9 cho Vừa khung, chưa đối chiếu được với mockup V13); (c) zoom bị cắt trong khung video (ở hướng dọc Vừa khung là khung 16:9); (d) bước tua 10 giây và nối Cài đặt (Lát 9) vẫn là hằng số; (e) VD-12 "Xem tiếp từ mm:ss?" là Lát 8
- Việc thêm so với 6a để 6b đỡ phải đổi schema lần nữa: Room thêm 5 cột video (`videoWidth`, `videoHeight`, `videoFrameRate`, `videoBitRate`, `videoFourCc`) và `DriveItem.video`. Schema Room lên `version = 2` (ADR-0015): bản đã cài tự xóa DB cũ và quét lại, không cần xóa dữ liệu tay (nếu vẫn dính lỗi hash do đã chạy bản trước khi tăng version thì xóa dữ liệu app một lần)
- Chưa làm, ghi lại: dọn cache video khi tệp bị xóa qua đồng bộ (DS-06), nối giới hạn cache (2 GB) với Cài đặt (Lát 9), nạp trước video kế tiếp, picture-in-picture. Thanh tua: thiết kế ghi vùng chạm 24 dp nhưng CLAUDE.md bắt buộc ≥ 48 dp; đang theo thiết kế (token `ODVSize.seekTouch`), chờ quyết định
- [x] Review Lát 6 (2026-10-04, đọc code): không CRITICAL, 1 HIGH, 5 MEDIUM, vài LOW; APPROVE kèm góp ý, **chưa sửa**. HIGH: (H1) mở video khi quét lần đầu chưa xong thì màn tự đóng (danh sách rỗng) hoặc phát nhầm video đầu danh sách nếu Room mới có một phần, `PlayerViewModel.observeVideos` chỉ nên chọn video thay thế khi video đang xem biến mất giữa chừng, còn lúc đầu thì chờ `startItemId` xuất hiện. MEDIUM: (M1) `PlayerLayer` ~330 dòng/`ODVPlayerContent.kt` ~700 dòng cần tách lớp cử chỉ và trạng thái giao diện; (M2) `failedIds` không hồi phục trong phiên (lỗi tạm loại `Other` bị bỏ qua mãi), nên xóa khi có video READY hoặc mạng trở lại; (M3) giữ lâu 2x có thể bật nhầm khi vuốt dọc chậm, cần kiểm tra chưa vượt `touchSlop`; (M4) `StreamUrlProvider` `runBlocking` không có thời hạn (retry Graph tới 60 giây giữ luồng tải), nên `withTimeout` ~15 giây; (M5) thanh tua 24 dp so với quy tắc ≥ 48 dp; (M6) bảng thông tin thiếu hai ngày tách riêng. LOW: số dp tự đặt trong `ODVPlayerContent`, đơn vị bitrate là chuỗi cứng, icon viên thuốc 14 dp so với thiết kế 18 dp, log lỗi dựng player ghi `e.message`, log API debug còn `Location` (ADR-0013)
- [v] Kiểm tay (2026-10-04, người dùng xác nhận "tất cả kiểm thử đều OK" sau các vòng chỉnh 6a/6b)

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
| 2026-10-03 | Review Lát 3 (không CRITICAL), đã sửa: (H1) `SyncCoordinator` bắt mọi ngoại lệ ngoài hủy trong `launchSync` (Room/ổ đĩa đầy trước đây rơi ra scope không handler gây crash và kẹt "đang đồng bộ"), (H2) `BrowserViewModel.navigateTo` không đặt `isLoading` khi đích trùng thư mục đang mở (chạm kết quả tìm trùng thư mục hiện tại làm vòng quay treo mãi), (M1) `reset()` đặt cờ `resetting` và hủy mọi job con thay vì dựa vào biến `job` (đóng khe hở một lần đồng bộ mới chen vào giữa lúc dừng và xóa, làm Ngắt kết nối/Quên PIN chờ cả lần quét), (M2) tham số catch trong `DataStoreBrowserPreferences` đặt tên thay vì `_`. Chưa xử lý, cần quyết định: (M3) Room lưu tên tệp/cây thư mục không mã hóa kể cả khi bật PIN (ADR-0008 chỉ mã hóa config); nếu cần che thì phải viết ADR mới. Ghi nhận mức LOW: con của sổ tay OneNote vẫn được lưu (mồ côi), tùy chọn sắp xếp/dạng hiển thị chưa xóa khi ngắt kết nối (Lát 9), log API debug giữ cả body trang delta ~200 KB (nợ M4 của ADR-0013). Chưa build |
| 2026-10-03 | Công cụ debug, tab Lưu trữ: "Làm mới" chỉ đọc lại danh sách (giữ nguyên). Thêm "Xóa dữ liệu local" (có hộp thoại xác nhận): gọi `DisconnectUseCase` như Ngắt kết nối rồi khởi động lại app ở task mới về màn Kết nối; cài đặt debug được giữ. Hook `DebugHooks.clearLocalData` đặt trong `DebugTools.install` (bản debug) vì `:tools:debug` không phụ thuộc domain. Chưa build |
| 2026-10-04 | Lát 4: thumbnail và tab Thư viện. Catalog (Coil 3.6.3, Paging 3.5.1, `room3-paging`); `GraphApi.fetchThumbnail` (chấp nhận 302, URL đã ký tải bằng client không bearer, `createDownloadHttpClient`); `GraphThumbnailFetcher` + `ThumbnailKeyer` + `ThumbnailCacheResetter`, `ImageLoader` trong `androidDataModule`, `MainApplication` là `SingletonImageLoader.Factory`; Room thêm `sortDate` + index (TV-02) + `pagedLibrary`/`libraryDays`; `DriveRepository.libraryPages`/`libraryDays`, `LibraryFilter`, `LibraryDay`, `ThumbnailSource`, `UtcOffsetProvider`; `:feature:library` (lưới dựng từ số mục theo ngày, không dùng `insertSeparators`); `ODVRemoteImage`; `ODVHomeScreen` với Tabs; tab Thư mục dùng thumbnail thật. Chưa build, chưa review. Cần xóa dữ liệu app vì schema Room đổi |
| 2026-10-04 | Chỉnh Lát 4 sau thử máy: `DELTA_PAGE_SIZE` 500 (1000 gây crash khi gọi delta), thumbnail hạ xuống cỡ trung bình (ô 240 px, thẻ 360×270 px), cuộn nhanh Thư viện tự ẩn và bắt đầu dưới tiêu đề nhóm, tiêu đề nhóm ghi "Hôm nay/Hôm qua" và "N ảnh · M video" (`videoCount` trong `libraryDays`). Sửa lỗi build: đăng ký `PagingSourceDaoReturnTypeConverter` cho `DriveDao` (Room 3), thêm `coil-singleton` cho `SingletonImageLoader`. Công cụ debug (tab Khác, nhóm Thumbnail): chọn tỉ lệ chất lượng thumbnail 50 → 150% (`ThumbnailQuality`, khóa cache có cỡ thực) và "Xóa cache thumbnail" (`ThumbnailCache`). Chưa build |
| 2026-10-04 | Review Lát 4 (hai đợt, kotlin-reviewer): không CRITICAL; đã sửa mục MEDIUM ghi cache sau khi ngắt kết nối bằng `ThumbnailCacheGeneration`. Người dùng kiểm tay Lát 4 thành công; đóng Lát 4. Mục LOW còn lại ghi ở checklist Lát 4 |
| 2026-10-04 | Lát 5 (code, chưa build màn xem): Telephoto + `coil-gif` trong catalog; `GraphApi.getItemInfo`/`downloadContent` (302 → URL ký, `Range`); `ViewerContext`, `ImageInfo`, `observeViewerImages`/`getImageInfo`/`folderPathOf`; kho ảnh gốc riêng `FileOriginalImageRepository` (`.part` tải tiếp, LRU 1 GB, `ConnectionResetter`); `:feature:imageviewer`; route `ImageViewer`, `onOpenFile` mang ngữ cảnh. Người dùng build thành công phần catalog, bước 1–4, thumbnail không lỗi. Chưa review, chưa kiểm tay |
| 2026-10-04 | Review Lát 5 (không CRITICAL/HIGH), đã sửa: (M1) `ODVImageViewerContent` giữ vị trí theo ảnh Pager đang dừng (`anchorId`) thay vì `currentId` của ViewModel, nếu không sau khi tiến trình bị thu hồi Pager khôi phục đúng trang nhưng bị kéo về ảnh mở đầu; (M2) `showInfo` bắt lỗi Room để không làm sập app qua `viewModelScope`. Xác nhận FLAG_SECURE/ẩn recents (`MainActivity`) phủ màn xem. Mục còn lại ghi ở checklist Lát 5 |
| 2026-10-04 | Lát 6a (code, chưa build): review docs trước khi làm, chốt khóa cache theo `itemId:cTag` (không theo URL ký), link làm mới ở tầng DataSource (VD-14), Room thêm cột facet video (VD-17). Module `:feature:player` (MVI; ExoPlayer do composable sở hữu, nhả khi rời màn hoặc màn Khóa che), `StreamDataSource` + `VideoCache` (`SimpleCache` 2 GB, `ConnectionResetter`), `GraphApi.getDownloadUrl`, `VideoStreamRepository`, `AppRoute.VideoPlayer`, `observeViewerItems(context, kind)` thay `observeViewerImages`. Rủi ro build chính: package của `PlayerSurface`/`SURFACE_TYPE_TEXTURE_VIEW` (media3-ui-compose), `StandaloneDatabaseProvider`, phiên bản Media3 1.8.0. Cần xóa dữ liệu local vì schema Room đổi. Chưa review, chưa kiểm tay |
| 2026-10-04 | Lát 6a: người dùng báo "không phát được video". Thêm log trace (Kermit, thẻ duy nhất `Player`, lọc ở tab Log của màn Debug): `[Url]` lấy link/tuổi/lỗi, `[Source]` mở kết nối/HTTP/làm mới link, `[Cache]` mở/dọn/bỏ qua, `[Factory]`, `[Player]` trạng thái/khung hình đầu/lỗi (mã ExoPlayer + chuỗi nguyên nhân), `[UI]` dựng player thất bại (trước đây nuốt lỗi, hiện cùng thẻ "Không phát được"), `[VM]`; `GraphApi.getDownloadUrl` ghi khi phản hồi thiếu link. Không ghi link/`tempauth`/message ngoại lệ (CH-06). Mọi hằng số của module gom vào `PlayerConstants` (commonMain, có `TAG`), `StreamUrlProvider` đổi `describe()` cho `AppError`. Chưa tìm ra nguyên nhân, chờ log từ máy |
| 2026-10-04 | Lát 6a sửa lỗi "không phát được": log của người dùng cho thấy `[Url] lỗi lấy link … Unknown(null)` lặp mỗi ~250 ms rồi ExoPlayer báo `ERROR_CODE_IO_UNSPECIFIED`. Nguyên nhân: `GraphApi.getDownloadUrl` gọi `$select=id,@microsoft.graph.downloadUrl` nhưng phản hồi không có link hợp lệ. Đổi sang `GET /items/{id}/content` (302, đọc header `Location`, không tải byte nào), cùng cách ảnh gốc đã chạy ở Lát 5; bỏ trường `downloadUrl` thừa trong `DriveItemDto`. Chưa build, cần người dùng thử lại; nếu còn lỗi thì xem dòng `GraphApi` (HTTP status, có Location không) |
| 2026-10-04 | Lát 6a chỉnh cử chỉ theo phản hồi: (1) điều khiển hiện tức thì: bộ nhận cử chỉ tự viết (`PlayerGestures.kt`) báo chạm ngay lúc nhả tay thay vì chờ ngưỡng chạm đúp ~300 ms của `detectTapGestures`, fade 150 ms; (2) chạm đúp chỉ tính khi điều khiển đang ẩn, lần chạm đầu đã hiện điều khiển nên chạm đúp thật thì hoàn tác; điều khiển đã hiện thì chặn tua; (3) sóng lan từ điểm chạm đúp (`TapFeedback.kt`, tắt khi Giảm hiệu ứng); (4) chạm đúp vùng giữa (30% bề ngang khung, đúng cả dọc/ngang, `PlayerConstants.CENTER_ZONE_FRACTION`) đổi Phát/Tạm dừng kèm biểu tượng. Chưa build |
| 2026-10-04 | Lát 6a sửa lại cử chỉ theo phản hồi lần hai: (1) "gợn sóng" là vùng ripple nửa elip **trong khung video** chứ không phải vòng tròn toàn màn hình: `SeekRipple` thêm vòng sóng lan từ mép giữa khung ra mép ngoài, cắt theo hình ripple; chạm đúp vùng giữa có sóng tròn quanh biểu tượng Phát/Tạm dừng (`CenterFeedbackIcon`), cả hai nằm trong `VideoFrame`; bỏ `TapWave` toàn màn hình; (2) bỏ nháy điều khiển khi chạm đúp: lần chạm đầu lúc điều khiển ẩn không hiện ngay mà hẹn giờ `DOUBLE_TAP_WINDOW_MS = 200` ms (hủy nếu có lần hai), ẩn điều khiển vẫn tức thì, fade 120 ms. Chưa build |
| 2026-10-04 | Lát 6a chỉnh theo phản hồi lần ba: (1) điều khiển ẩn thì ẩn luôn system bar (`ODVPlayerSystemBars(hidden = !showControls)`, PlayerLayer báo lên qua `onBarsHiddenChange`); (2) sóng bớt đường: 3 vòng → 2, mảnh và nhạt hơn; (3) xoay màn hình không dừng video: `MainActivity` khai `configChanges` (orientation, screenSize, smallestScreenSize, screenLayout, keyboard, keyboardHidden; cố ý không có `locale`, `uiMode` để đổi ngôn ngữ/giao diện vẫn tạo lại Activity), và `SavePosition` mang thêm `playing` để dù Activity bị tạo lại vì lý do khác vẫn phát tiếp (xuống nền thì player đã dừng nên màn Khóa vẫn nằm chờ tạm dừng); (4) bảng tốc độ ở hướng ngang cho cuộn dọc (7 hàng cao hơn màn ngang nên 2x bị cắt). Việc `configChanges` thuộc 6b (VD-19) nay đã làm sớm. Chưa build |
| 2026-10-04 | Lát 6b (code, chưa build): người dùng đã thử 6a ("quá OK"). Domain `PlayMode`, `VideoFit`, `PlayerPreferences`; data `DataStorePlayerPreferences` + `createPreferencesDataStore(context, name)` (browser/player); `PlayerState` thêm `playMode`, `videoFit`, `failedIds`, `info`, `nextInQueue`, `nextPlayable`; intent `Advance`, `VideoFailed`, `CyclePlayMode`, `CycleVideoFit`, `ShowInfo`/`HideInfo`; UI: `SwipeGestures.kt`, `DeviceLevels.kt`, `PlayerOrientation.kt`, `PlayerZoom.kt`, `NextUpCard.kt`, `PlayerOptionLabels.kt`, `PlayerInfoContent.kt`, viết lại `ODVPlayerContent.kt` (khóa thao tác, zoom, fit, HUD, thẻ tiếp theo, bảng thông tin); hằng số mới trong `PlayerConstants`; chuỗi VI/EN. Rủi ro build: `transformable(canPan, lockRotationOnZoomChange)`, `detectVerticalDragGestures`, `ODVViewerHud`/`LockHoldButton` đã có sẵn. Chưa review, chưa kiểm tay |
| 2026-10-04 | Lát 6b sửa theo thử máy: (1) vuốt độ sáng/âm lượng nhạy hơn (`SWIPE_FULL_RANGE_RATIO` 1,2 → 2,2); (2) bug zoom khựng: vượt 1x thì `isZoomed` đổi và modifier vuốt bị gỡ khỏi chuỗi, Compose gán lại nút nhận cử chỉ zoom và hủy cử chỉ hai ngón đang chạy; nay `playerSwipeGestures` và `playerZoomable` luôn nằm trong chuỗi và kiểm tra cờ bên trong; (3) hết video vẫn chạm để ẩn điều khiển (bật cờ lúc hết video thay vì ép hiện, VD-21 vẫn không tự ẩn); (4) vuốt độ sáng/âm lượng ẩn điều khiển ngay và để ẩn sau khi chỉnh xong. **Chưa làm, đã review, chờ quyết định:** zoom vượt khung ở Vừa khung (xem bên dưới) |
| 2026-10-04 | Review zoom vượt khung (Vừa khung, hướng dọc): hiện zoom áp lên video bên trong khung 16:9 và bị `clipToBounds` của khung cắt, nên chỉ phóng trong dải 16:9. Muốn phóng vượt ra cả màn hình thì khả thi, độ phức tạp trung bình, cần: (a) bỏ cắt theo khung, cắt theo cả màn; (b) chặn kéo theo kích thước video thật đang hiển thị so với màn hình (nội dung nhỏ hơn màn thì giữ giữa, lớn hơn thì chặn ở mép) thay vì theo khung; (c) tính hộp video hiển thị cho từng chế độ Vừa khung/Cắt đầy/Kéo giãn. Vướng: 16:9 trên màn dọc cần ~3,8x mới phủ hết chiều cao nên mức tối đa 4x chỉ vừa đủ (nên tính mức tối đa theo tỉ lệ phủ màn, hoặc tăng lên 6x); zoom đang quanh tâm thay vì quanh điểm hai ngón; thanh điều khiển/gợn chạm đúp vẽ trên video không bị ảnh hưởng. Ghi để làm sau |
| 2026-10-04 | Lát 6b sửa theo thử máy lần hai: (1) vùng chết 56 dp ở đỉnh cho vuốt độ sáng/âm lượng (`SWIPE_TOP_DEAD_ZONE_DP`) để vuốt từ mép trên kéo system bar không bị bắt; (2) "Giữ để mở khóa" không còn bị tính là giữ lâu phát 2x: nút nằm đè lên khung và không nuốt lần chạm xuống, nên `playerGestures` thêm tham số `enabled` (tắt khi khóa, kiểm tra bên trong để không đổi cấu trúc chuỗi modifier); (3) gợi ý khóa ẩn/hiện như điều khiển video: chạm hiện, chạm nữa ẩn, tự ẩn sau 3 giây, Back thì hiện; (4) thời gian "1:17:50 / 1:26:02" luôn một dòng (`ODVViewerBottomBar` không xuống dòng, tự thu nhỏ chữ tới 70% khi thiếu chỗ); (5) "Đặt lại zoom" hướng ngang nằm trong hàng nút của thanh dưới (trước nút Tốc độ), hướng dọc giữ cách đáy 112 dp. Chưa build |
| 2026-10-03 | Crash SIGSEGV (`libsqliteJni.so`, `sqlite3_step` trong `DriveDao.applyPage`/`upsertItems`, lúc đồng bộ lần đầu rồi vào danh sách; không tái hiện đều). Không thấy lỗi ở code app (không có chỗ đóng/tạo lại DB). Thử nâng Room 2.8.5 → 3.0.3 (`androidx.room3`, sqlite 2.6.2 → 2.7.1, plugin `androidx.room3`) vì Room 2.x chỉ còn bảo trì; chưa có bằng chứng bản này sửa crash. Đổi import `androidx.room` → `androidx.room3` trong `:core:database`, schema giữ `version = 1`. Cần xác nhận khi build: chữ ký `fallbackToDestructiveMigration(dropAllTables = true)`, `schemaDirectory`, KSP 2.3.12 với room3-compiler. Chưa build |
