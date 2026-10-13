# Kế hoạch triển khai MVP1 (Android)

- **Trạng thái:** đã duyệt 2026-10-02; đổi thứ tự Lát 2026-10-07 (Cài đặt lên trước PDF) và 2026-10-10 (thêm Lát 8 Short)
- **Tiến độ thực tế:** xem [tien-do-mvp1.md](tien-do-mvp1.md) (file này chỉ đổi khi đổi kế hoạch)
- **Điểm xuất phát:** Foundations (`:core:designsystem`) đã xong; các module `:core:*` khác đã tạo nhưng còn rỗng; chưa có `:feature:*`

## 1. Nguyên tắc

- **Làm theo lát cắt dọc, không theo thứ tự ADR.** ADR là các quyết định đã chốt, không phải lộ trình. Mỗi lát là một luồng chạy được từ màn hình xuống Graph, áp dụng những ADR nó cần.
- **Rủi ro cao làm trước:** mã hóa và token (lát 1–2), delta sync (lát 3), rồi mới tới các màn xem.
- **Kiểm tra bằng tay** (ADR-0009): mỗi lát kết thúc bằng danh sách kiểm tay trên thiết bị, không có test tự động.
- **Người dùng tự build, commit và push.** Cuối mỗi lát Claude chạy `/ecc:kotlin-review` (thêm review bảo mật cho lát 1–2) và gợi ý tên commit.
- Module `:feature:*` tạo khi bắt đầu lát dùng tới nó (ADR-0010).
- **Tài liệu và thiết kế tách topic (chốt 2026-10-10):** topic tài liệu chỉ sửa các file `.md`; vẽ trên Claude Design làm ở topic riêng theo `thiet-ke-ui.md` mục 11.

ADR đã xong từ trước: 0001 và 0010 (cấu trúc module), 0009 (không làm gì).

**Lịch sử đánh số:** ban đầu Lát 7 là PDF, Lát 8 Xem tiếp, Lát 9 Cài đặt. Ngày 2026-10-07 Cài đặt lên Lát 7 (PDF thành 8, Xem tiếp thành 9). Ngày 2026-10-10 thêm **Lát 8 Short** kèm thanh điều hướng đáy, nên **PDF thành Lát 9** và **Xem tiếp thành Lát 10**. Comment trong code hoặc tài liệu cũ ghi số Lát khác thì đọc theo bảng dưới.

## 2. Các lát

| Lát | Nội dung | ADR | Module chính |
|---|---|---|---|
| 0 | Bộ khung app, chưa có tính năng | 0002, 0003, 0004, 0011 | common, androidApp |
| 1 | Kết nối → Danh sách thư mục gốc (luồng xuyên suốt đầu tiên) | 0005, 0006, một phần 0008 | network, security, data, domain, feature:auth, feature:browser |
| D | Công cụ debug: nút bọ nổi, màn Debug (log API, log local, lưu trữ), Kermit, retry bằng `HttpRequestRetry` | 0012 | tools:debug, common, network |
| 2 | Mã PIN và màn Khóa | phần còn lại của 0008 | security, feature:auth |
| 3 | Đồng bộ delta và offline | 0007 | database, data, feature:browser |
| 4 | Thumbnail, bộ nhớ đệm, tab Thư viện | | data, feature:library |
| 5 | Xem ảnh | | feature:imageviewer |
| 6 | Xem video (6a: phát và điều khiển cơ bản; 6b: phần còn lại) | | feature:player |
| 7 | Cài đặt đầy đủ | 0019, 0021, 0022 | feature:settings |
| 8 | Thanh điều hướng đáy và tab Short | 0023, 0024, 0025, 0026, 0027 | androidApp, feature:shorts, feature:settings, data |
| T | Nhiều tài khoản: thêm, chọn, xóa, xem gộp Tất cả; đồng bộ mọi tài khoản (chen sau Lát 8, 2026-10-12) | 0030 (bổ sung 0005, 0007) | domain, security, network, database, data, designsystem, feature:auth, browser, library, shorts, settings, androidApp |
| 9 | Xem PDF | | feature:pdfviewer |
| 10 | Xem tiếp / Đọc tiếp | | data, feature:browser, player, pdfviewer |

### Lát 0: Bộ khung app

- Khai báo thư viện: Koin, Ktor, kotlinx.serialization, Room KMP, Navigation 3, coroutines, DataStore.
- `:core:common`: `AppError`, `Result`, dispatcher.
- `BaseMviViewModel` (`State`, `Intent`, `Effect`).
- Khởi động Koin; `NavDisplay` với các route rỗng.
- Đa ngôn ngữ: `locales_config`, `res/values` (VI), `res/values-en` (EN), `AppCompatDelegate.setApplicationLocales()`.
- Tắt sao lưu tự động (CH-04).
- Tạo `.claude/docs/tech-stack.md` ghi lại các lựa chọn ở mục 3 và quy ước nhỏ (§9).
- **Kiểm tay:** app mở vào một màn trống; đổi ngôn ngữ được.

### Lát 1: Kết nối → Danh sách thư mục gốc

- `:core:network`: Ktor gọi Graph, lấy token Client Credentials, token chỉ trong bộ nhớ; TK-01, TK-02, TK-03; log che header (CH-06).
- `:core:security`: mã hóa config AES-GCM bằng khóa Keystore (nhánh bảo mật TẮT).
- `:feature:auth`: màn Kết nối (KN-01 → KN-13, bảng lỗi theo mã, `FLAG_SECURE`): kết nối xong lưu config ngay ở chế độ thiết bị rồi hiện hộp thoại hỏi thiết lập PIN (KN-13); "Để sau" vào Danh sách. Màn Thiết lập bảo mật chỉ là khung (Back về hộp thoại, BM-08); phần đặt PIN làm ở Lát 2.
- `:feature:browser`: tab Thư mục gọi thẳng API (TM-07), vào thư mục con, Breadcrumb, Back.
- Điều hướng khởi động theo luồng tổng thể (mục 2 đặc tả): chưa có config → Kết nối; có config → Danh sách.
- **Kiểm tay:** nhập 4 trường đúng/sai (từng mã lỗi); thấy cây thư mục thật; tắt rồi mở lại app vào thẳng Danh sách.

### Lát D: Công cụ debug (chen giữa, không đổi thứ tự các lát)

- Thêm sau Lát 1 theo yêu cầu: kiểm tay từ Lát 1 trở đi cần nhìn request/log ngay trên máy. ADR-0012.
- `:tools:debug` (chỉ bản debug): nút bọ nổi kéo thả → `DebugActivity` (không FLAG_SECURE) với 4 tab: Log API (bấm vào mở màn chi tiết: header, request body, response, JSON đẹp có +/-, sao chép, tìm kiếm có đếm), Log local (có tìm kiếm), Lưu trữ (SharedPreferences, DataStore, Room chỉ đọc), Khác (FLAG_SECURE toàn app: theo thiết kế/luôn bật/luôn tắt; công tắc che log API; mở Foundations gallery). Nút bọ luôn nằm trên cùng kể cả trên Dialog/BottomSheet. Log API đầy đủ không che (ADR-0013).
- Kermit trong `:core:common`; `HttpTrafficRecorder` trong `:core:network` (làm sạch trước khi ghi; không ghi body endpoint token).
- Thay hàm `withRetry` tự viết bằng plugin `HttpRequestRetry` của Ktor.
- Chỉ còn MỘT icon launcher: Foundations gallery bỏ LAUNCHER, mở từ tab "Khác" của màn Debug.
- **Kiểm tay:** nút bọ hiện trên mọi màn và kéo được; tab API thấy request Graph (không có Authorization/token); tab Log thấy log kết nối; tab Lưu trữ liệt kê prefs; bản release không có nút bọ.

### Lát 2: Mã PIN và màn Khóa

- Argon2id + Keystore, không lưu PIN hay hash (CH-01, CH-02, CH-07).
- Đặt PIN: nhập 2 lần, chặn PIN dễ đoán (BM-01 → BM-08); mã hóa lại config đã lưu ở chế độ thiết bị bằng khóa dẫn xuất từ PIN (BM-04, ADR-0008).
- Màn Khóa: KH-01 → KH-06 (chờ tăng gấp đôi, vẫn tính khi tắt app; Quên PIN; xóa dữ liệu sau 10 lần nếu bật).
- Tự khóa khi rời app; khóa thì xóa token và config đã giải mã khỏi bộ nhớ (CH-03).
- Sinh trắc học.
- **Kiểm tay:** sai PIN 5 lần, tắt app rồi mở lại; khóa khi app xuống nền; Quên PIN.

### Lát 3: Đồng bộ delta và offline

- `:core:database`: Room (`BundledSQLiteDriver`, `version = 1`), bảng item, trạng thái đồng bộ.
- Delta sync DB-01 → DB-05 (tiếp tục trang dở, xử lý 410).
- Tab Thư mục đọc từ Room; sắp xếp (TM-05), lưới/danh sách (TM-06), lọc loại tệp (TM-03, TM-04).
- Kéo để làm mới, tự đồng bộ khi mở nếu quá 15 phút (DS-04); thanh "Đang offline" (DS-05); gỡ tệp đã xóa (DS-06).
- Tìm kiếm (DS-03). TK-05, TK-06.
- Đăng ký `ConnectionResetter` xóa Room và dữ liệu đồng bộ khi ngắt kết nối (CD-05, KH-03, KH-06; xem `DisconnectUseCase`).
- **Kiểm tay:** drive lớn; tắt app giữa lúc quét; chế độ máy bay.

### Lát 4: Thumbnail, bộ nhớ đệm, tab Thư viện

- Cache thumbnail, giới hạn dung lượng, nhận diện theo `cTag` (BN-01 → BN-03).
- Đăng ký `ConnectionResetter` xóa cache khi ngắt kết nối.
- `:feature:library`: TV-01 → TV-06 (nhóm theo ngày, chip lọc, thanh cuộn nhanh, "Đang lập chỉ mục").
- Thư viện đọc Room qua **Paging 3** (`room-paging`), không nạp cả danh sách vào bộ nhớ: sắp xếp theo ngày (TV-02) bằng SQL, index ghép (`mediaKind`, ngày). Bổ sung 2026-10-03 sau review kiến trúc đồng bộ.

### Lát 5: Xem ảnh

- AN-01 → AN-07.

### Lát 6: Xem video

- 6a: phát stream, tự lấy link mới (VD-14), Play/Pause, tua, chạm đúp tua, tốc độ, Back, giữ màn sáng, lỗi codec và mất mạng.
- 6b: vuốt độ sáng/âm lượng, khung hình, xoay, khóa thao tác, trước/sau và chế độ phát, Phát lại, zoom, bảng thông tin.
- Sửa chen ngang 2026-10-11 (không đổi thứ tự Lát): zoom (VD-18) tràn cả màn hình ở Vừa khung hướng dọc và zoom theo điểm giữa hai ngón; chi tiết ở `tien-do-mvp1.md`.

### Lát 7: Cài đặt đầy đủ (đưa lên trước PDF, 2026-10-07)

- CD-01 → CD-12; cập nhật Client Secret; ngắt kết nối và xóa sạch; giới hạn cache và tỉ lệ chia theo loại (ADR-0021); nhắc secret sắp hết hạn; tự khóa có độ trễ (ADR-0019); Bảo vệ màn hình (ADR-0022).
- Nối `SecurityRepositoryImpl.wipeAfterFailures` với cài đặt CD-08. Đăng ký `ConnectionResetter` xóa cài đặt. Nút Ngắt kết nối gọi `DisconnectUseCase`.
- Chia 7a đến 7e; chi tiết ở `tien-do-mvp1.md`.

### Lát 8: Thanh điều hướng đáy và tab Short (thêm 2026-10-10)

Làm sau khi review code Lát 7 xong. Đặc tả: mục 3.4 (DH-01 → DH-08) và 3.4.4 (SV-01 → SV-16), CD-13. Thiết kế giao diện: `thiet-ke-ui.md` mục 4.7 (thanh điều hướng đáy), 4.8 (Short), 5.2, 5.4, 5.5 **đã cập nhật mô tả (2026-10-10)**; **artboard đã vẽ xong (2026-10-10)** và `thiet-ke-ui.md`, `odv-tokens.json` đã đồng bộ. **Đã duyệt kế hoạch triển khai (2026-10-10):** 8a gộp một lần commit; player dùng chung được **tách ra khỏi `:feature:player`** (cần ADR-0025 trước 8b) để `:feature:shorts` không phụ thuộc feature khác; O6 giữ thanh trượt, nút Áp dụng đối chiếu artboard khi làm 8b.

- **8a. Thanh điều hướng đáy** (ADR-0023): Màn chính với 4 mục Thư mục, Thư viện, Short, Cài đặt (DH-01); mỗi tab giữ trạng thái riêng, kể cả sau khi khóa và khi hệ điều hành thu hồi tiến trình (DH-02); Back theo DH-03; chạm lại tab (DH-04); thanh ẩn ở màn xem, màn khóa và màn con Cài đặt (DH-05); tab mở khi khởi động (DH-06); chấm nhắc secret (DH-07); `FLAG_SECURE` theo tab (DH-08). Cài đặt chuyển thành tab, bỏ nút bánh răng trên AppBar; các chỗ đang điều hướng tới route Cài đặt (banner CD-06) đổi thành chuyển tab. Bỏ `ODVHomeScreen` dạng Tabs. Thêm icon `short` và 4 token `nav-*` vào `:core:designsystem` khi thiết kế được duyệt.
- **8b. Tab Short cơ bản** (`:feature:shorts` mới): danh sách ID video theo thời lượng tối đa lấy từ Room (SV-01; xem có cần index theo `mediaKind`, thời lượng không, nếu đổi schema thì tăng version theo ADR-0015); xáo bằng seed lưu trong saved state (SV-02); kéo để xáo lại ở video đầu (SV-03); vuốt dọc chuyển video (SV-04); tự phát có tiếng và tự lặp (SV-05); chạm tạm dừng (SV-06); thanh tiến độ (SV-07); khung hình theo kích thước sau giải mã (SV-08); tên tệp mờ (SV-09); nền tối (SV-10); danh sách rỗng (SV-15). Cài đặt › Video thêm "Thời lượng tối đa của Short" (CD-13).
- **8c. Tải trước và vòng đời** (ADR-0024; cơ chế tải trước đổi sang `CacheWriter` ở ADR-0026, `accepted` 2026-10-11; poster khung hình đầu ở ADR-0027): cửa sổ ±1 qua cache video chung (SV-13); tạm dừng/phát tiếp khi đổi tab, xuống nền, khóa (SV-11); không lưu vị trí xem (SV-12); lỗi link, codec, mạng (SV-14); dữ liệu thay đổi (SV-16).
- **Kiểm tay:** chuyển qua lại 4 tab ở giữa thư mục sâu và giữa danh sách Thư viện đã cuộn xa rồi kiểm vị trí; Back từ từng tab; bật "Don't keep activities" rồi mở lại; tab Short ẩn khi đang lập chỉ mục và khi tắt Video; vuốt nhanh 10 video liên tiếp (có mạng, mất mạng giữa chừng); video HEVC 10-bit qua decoder FFmpeg; video dọc quay bằng điện thoại (cờ xoay), video 3:4, video ngang; khóa app khi đang phát Short; đổi thời lượng tối đa rồi quay lại tab Short; chụp màn hình ở tab Cài đặt khi bật và tắt "Bảo vệ màn hình". Checklist giao diện ở `thiet-ke-ui.md` mục 10.

### Lát T: Nhiều tài khoản (chen sau Lát 8, trước phần kiểm còn lại của Lát 9; thêm 2026-10-12)

Người dùng yêu cầu làm ngay vì quan trọng; Lát 9 đã code xong 9a đến 9c nên chỉ lùi phần kiểm phụ và review. **Không đánh số lại** (đặt tên T = Tài khoản, theo tiền lệ Lát D; không dùng 8b vì đã là bước con của Lát 8; nhờ vậy các tài liệu và comment ghi Lát 9, 10 vẫn đúng). Đặc tả: `dac-ta-nghiep-vu.md` mục 3.9 (TA-01 → TA-12) và các quy tắc đã sửa theo. ADR-0030. Thiết kế: `thiet-ke-ui.md` mục 4.9, 5.2 (D10), 5.4 (AC1 → AC9), 5.5 (SH10), **đã vẽ xong (2026-10-12)**. **Chốt 2026-10-12:** đồng bộ **mọi tài khoản**, lần lượt, tài khoản đang xem chạy trước; phạm vi xem là một tài khoản hoặc Tất cả; không giới hạn số tài khoản; thêm/xóa không hỏi PIN.

- **T1. Domain và security:** tách `ConnectionConfig` thành phần dùng chung + danh sách `Account` (UPN, tên hiển thị, email, drive id); đổi định dạng `ConfigVault` (chưa phát hành nên không cần migration); use case thêm, xóa, chọn phạm vi, theo dõi danh sách và phạm vi.
- **T2. Network:** UPN truyền theo từng lời gọi, một token dùng chung; kiểm tra kết nối lấy thêm `owner` (TA-02).
- **T3. Database và data:** thêm `accountId` (= drive id) vào `drive_item` (khóa kép), `sync_state` (một dòng mỗi tài khoản), `reading_progress`; **tăng `version` ở `OdvDatabase`** (ADR-0015) và ghi lịch sử; mọi truy vấn đọc nhận danh sách `accountId` của phạm vi; khóa cache và link phát chia sẻ (ADR-0029) thêm tài khoản (BN-02).
- **T4. Đồng bộ nhiều tài khoản:** hàng đợi lần lượt, tài khoản đang xem lên đầu, lỗi một tài khoản không chặn các tài khoản sau (TA-10); DB-01 → DB-04 theo từng tài khoản.
- **T5. UI dùng chung và màn:** `:core:designsystem` (AccountAvatar, AccountRow, icon `users`, `user-plus`, `trash`), avatar trên AppBar, sheet Chọn tài khoản, màn Thêm tài khoản, nhóm "Tài khoản và kết nối" ở Cài đặt, D10, dòng thư mục có tên tài khoản ở Short (SH10), chuỗi VI/EN.
- **T6. Đổi phạm vi và dọn dẹp:** TA-06 (dừng Short, bỏ link đã ký, bỏ trạng thái tab, về gốc Thư mục), ngắt kết nối/quên PIN xóa mọi tài khoản (TA-12), review Kotlin.
- **Kiểm tay:** thêm tài khoản khi bảo mật PIN bật và tắt; đổi phạm vi giữa thư mục sâu, Thư viện đã cuộn, Short đang phát; bật "Don't keep activities" rồi mở lại; đồng bộ lần lượt ba tài khoản (tắt mạng giữa chừng, tài khoản lỗi `403`/`404`); xóa tài khoản đang xem và không đang xem; Thư viện gộp không lẫn tệp tài khoản đã xóa; ngắt kết nối xóa hết.

### Lát 9: Xem PDF

- PD-01 → PD-08 (tải có tiến trình và tải tiếp phần dở, cuộn dọc hoặc lật ngang, nhớ trang). **PD-05 làm ở lát này** (đổi 2026-10-07): bảng `reading_progress`. Chia 9a, 9b, 9c; chi tiết ở `tien-do-mvp1.md`.

### Lát 10: Xem tiếp / Đọc tiếp

- DS-02, VD-12 (PD-05 đã chuyển sang Lát 9).
- Đăng ký `ConnectionResetter` xóa lịch sử xem khi ngắt kết nối.

### Mục bổ sung sau Lát 10: Tìm kiếm trong Thư viện (TV-07, duyệt 2026-10-13)

Không phải lát mới; làm sau Lát 10, trước khi chuyển sang phần kiểm còn lại. Kế hoạch đã duyệt (Q1 đến Q4 đều chọn phương án A):

- **Phạm vi:** chỉ ảnh và video của Thư viện (loại tệp bật ở Cài đặt, chip lọc đang chọn, phạm vi tài khoản đang xem); kết quả là **lưới phẳng** 4 cột dùng lại ô của Thư viện; không giới hạn số kết quả (phân trang); màn xem chỉ vuốt trong kết quả khớp.
- **Không đổi schema Room** (dùng cột `nameKey` và `instr` như DS-03) nên không tăng `version` ở `OdvDatabase`.
- **Các bước:** (1) `:core:database`/`:core:domain`/`:core:data`: `DriveDao.pagedLibrarySearch` + `observeLibrarySearchItems`, `LibraryRepository.librarySearchPages`, `SearchLibraryPagesUseCase`, `ViewerContext.Library(filter, query)`, nhánh tìm trong `ViewerRepositoryImpl`; (2) `:feature:library`: `LibrarySearchState`, intent `OpenSearch/CloseSearch/QueryChanged`, `searchPages` (debounce 250ms, `cachedIn`), đóng tìm khi đổi phạm vi tài khoản; (3) giao diện: nút Tìm kiếm, `ODVSearchBar`, `SearchBody` (lưới phẳng, trạng thái trống, spinner), `LibraryMediaCell` dùng chung, Back đóng tìm trước; (4) `:androidApp`: `libraryQuery` ở route `ImageViewer`/`VideoPlayer` (mặc định null, tương thích route cũ); (5) chuỗi VI/EN [mới], đặc tả TV-07, thiết kế mục 5.2 và mục 11 dòng 20.
- **Chưa có artboard** (xem `thiet-ke-ui.md` mục 11 dòng 20).

## 3. Thư viện chưa có trong ADR (đã duyệt)

| Việc | Chọn | Ghi chú |
|---|---|---|
| Argon2id | `argon2kt` | Native, nhanh. Phương án dự phòng: BouncyCastle (thuần Java, chậm hơn) |
| Lưu cài đặt | DataStore Preferences | |
| Tải ảnh, thumbnail | Coil 3 | |
| Phát video | Media3 ExoPlayer + `SimpleCache` | Tab Short tải trước ±1 bằng `CacheWriter` vào cache video chung (ADR-0026, thay `DefaultPreloadManager` của ADR-0024) |
| PDF | `PdfRenderer` của Android | |
| Đồng bộ chạy nền | Coroutine trong app | Thêm WorkManager khi cần đồng bộ lúc app đã đóng |
| Log local | Kermit 2.2.0 | Bản debug đẩy vào màn Debug; release không writer (ADR-0012) |
| Retry mạng | Plugin `HttpRequestRetry` của Ktor | Thay hàm `withRetry` tự viết (ADR-0012) |

## 4. Bỏ so với quy trình `/orch-build-mvp`

- Bộ chấm điểm tự động (GAN harness): cần build và chạy app. Thay bằng danh sách kiểm tay.
- Commit tự động: người dùng tự commit; Claude chỉ gợi ý tên.