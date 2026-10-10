# OneDriveVibe

App Android chỉ đọc OneDrive for Business qua Microsoft Graph: xem video, ảnh, truyện PDF. MVP1 là Android; MVP2 thêm iOS bằng KMP. Tài liệu viết bằng tiếng Việt, trả lời người dùng bằng tiếng Việt. Tên cũ "ODV (OneDrive Viewer)" đã đổi thành OneDriveVibe; tên app/tài liệu là OneDriveVibe, còn tiền tố code UI là `ODV` (xem "Quy ước đặt tên"); package giữ `com.lambao.odv`.

## Tài liệu (đọc trước khi làm)

| File | Dùng khi |
|---|---|
| `.claude/docs/dac-ta-nghiep-vu.md` | Nghiệp vụ: cái gì xảy ra. Các mã KN, BM, KH, DH, TM, TV, DS, SV, VD, AN, PD, CD, CH, TK, DB, BN... |
| `.claude/docs/thiet-ke-ui.md` + `odv-tokens.json` | Giao diện: token, component, màn hình. Mục 1 là quy tắc bắt buộc khi dựng. Thanh điều hướng đáy và tab Short (Lát 8) đã có mô tả (mục 4.7, 4.8, 5.5) và **đã có artboard** (Foundations board 20, page Short SH1 đến SH9, Danh sách D1 đến D9, Cài đặt C1 đến C7 và O6); mục 11 là danh sách việc vẽ, còn lại 11.5 |
| `.claude/docs/onedrive-graph-api.md` | Endpoint Graph, token, delta, mã lỗi |
| `.claude/docs/onedrive-graph-responses.md` | Mẫu response thật (giá trị đã thay bằng mẫu) |
| `.claude/adr/` | 25 quyết định kiến trúc (mục lục ở `README.md`). Muốn đổi thì viết ADR mới, không sửa ADR cũ |
| `.claude/docs/ke-hoach-mvp1.md` | Kế hoạch triển khai MVP1 theo lát cắt dọc, thứ tự Lát hiện hành, thư viện đã chọn |
| `.claude/docs/ke-hoach-refactor-kien-truc.md` | Refactor nền móng R0 đến R7 và nợ kỹ thuật A1 đến A3: **đã xong** (2026-10-07). Đọc để biết lý do cấu trúc hiện tại |
| `.claude/docs/tien-do-mvp1.md` | Đang ở lát nào, bước nào. Đọc đầu mỗi phiên, cập nhật khi bắt đầu hoặc xong một bước |

**Thứ tự Lát hiện hành (chốt 2026-10-10):** 7 Cài đặt → 8 Thanh điều hướng đáy và tab Short → 9 PDF → 10 Xem tiếp / Đọc tiếp. Comment cũ trong code ghi số Lát khác thì đọc theo bảng ở `ke-hoach-mvp1.md`.

Khi mâu thuẫn: đặc tả nghiệp vụ quyết định hành vi, tài liệu UI quyết định hình thức. Không tự chế màu, cỡ chữ, khoảng cách hay câu chữ chưa có trong thiết kế; hỏi lại.

## Kiến trúc đã chốt

- **Clean Architecture + MVI** (ADR-0002): mỗi màn có `State`, `Intent` (`onIntent()`), `Effect` cho thao tác một lần. **UseCase bắt buộc giữa feature và data** (ADR-0016, thay quy tắc "chỉ khi có logic thật"): ViewModel không gọi thẳng repository.
- **Navigation 3** (ADR-0003): back stack do app sở hữu, `NavDisplay` + `entryProvider`. Không truyền nav controller xuống composable, dùng lambda.
- **Thanh điều hướng đáy** (ADR-0023, Lát 8): Màn chính có 4 tab Thư mục, Thư viện, Short, Cài đặt (Short chỉ hiện khi bật Video và đã đồng bộ xong lần đầu). Mỗi tab giữ trạng thái riêng suốt vòng đời Màn chính; màn xem, Khóa và màn con của Cài đặt đẩy lên back stack của app và che thanh đáy. Cài đặt là tab, không còn nút bánh răng trên AppBar.
- **Koin 4.x** (ADR-0004), không dùng Hilt.
- **Xác thực Client Credentials** (ADR-0005): 4 trường Tenant ID, Client ID, Client Secret, UPN; gọi qua `/users/{UPN}/drive`. Không MSAL, không OneDrive cá nhân.
- **Graph REST qua Ktor** (ADR-0006), kotlinx.serialization. Không dùng Graph SDK, Retrofit.
- **Offline-first** (ADR-0007): Room KMP (`BundledSQLiteDriver`) là nguồn dữ liệu duy nhất cho UI, đồng bộ bằng delta query. **Mỗi lần đổi schema (thêm/bỏ/đổi cột, bảng, index) phải tăng `version` ở `OdvDatabase`** và ghi vào "Lịch sử version" (ADR-0015); quên là crash "Room cannot verify the data integrity". Trước phát hành: tăng version là đủ (có `fallbackToDestructiveMigration`); từ bản phát hành đầu: bắt buộc viết Migration.
- **Bảo mật config** (ADR-0008, 0014): AES-GCM, khóa dẫn xuất từ PIN bằng Argon2id + Android Keystore. **Không lưu PIN, không lưu hash PIN.** Access token chỉ giữ trong bộ nhớ. Tự khóa khi rời app có độ trễ do người dùng chọn (ADR-0019).
- **Video** (Media3 ExoPlayer): decoder FFmpeg (NextLib, GPL-3.0) chỉ làm dự phòng khi máy không giải mã được, vd HEVC 10-bit (ADR-0018). Tab Short dùng một ExoPlayer và `DefaultPreloadManager` tải trước video ±1 qua cache video chung (ADR-0024). Phần nền phát dùng chung của Xem video và Short nằm ở `:core:media` (ADR-0025).
- **Bộ nhớ đệm** (ADR-0021): trần chung 1 đến 10 GB, chia theo loại (thumbnail, ảnh, video, PDF), đọc lúc chạy; mỗi kho là `CacheStore`.
- **Không viết test tự động** (ADR-0009): không unit test, UI test, coverage. Đừng thêm junit, kotlin-test, MockK, Turbine, Kover.
- **Đa ngôn ngữ VI + EN** (ADR-0011): chuỗi trong Android `res/values` (VI, mặc định) và `res/values-en`. `domain` và `data` không chứa chuỗi hiển thị; lỗi là kiểu có cấu trúc (`AppError` → `UiError`).
- **Công cụ debug** (ADR-0012, 0013, 0017, 0020): module `:tools:debug` chỉ có trong bản debug (nút bọ nổi luôn nằm trên cùng → màn Debug: log API có màn chi tiết + tìm kiếm + sao chép, log local, lưu trữ; FLAG_SECURE theo cài đặt Bảo vệ màn hình, ADR-0020; log giữ nguyên khi app khóa, ADR-0017). Log local dùng Kermit; log API đi qua `HttpTrafficRecorder`, hiển thị **đầy đủ không che** (có công tắc che), chỉ trong bộ nhớ, không dùng Ktor `Logging`. Release không ghi log nào và không có recorder.

## Cấu trúc module (ADR-0001, ADR-0010)

```
:androidApp          Android app (ADR-0010 gọi là :app; giữ tên của template); Màn chính có thanh điều hướng đáy (ADR-0023)
:build-logic         convention plugin odv.kmp.library (core), odv.kmp.feature (feature)
:core:common
:core:domain         chỉ phụ thuộc :core:common
:core:data           → common, domain, network, database, security
:core:network  :core:database  :core:security   → common
:core:designsystem   Compose, token và component dùng chung
:core:media          nền phát video dùng chung cho Xem video và Short: VideoCache, StreamDataSource, decoder, ExoPlayerFactory (ADR-0025) → common, domain
:feature:*           tạo khi bắt đầu làm feature đó (auth, browser, library, shorts, player, imageviewer, pdfviewer, settings)
:tools:debug         công cụ debug, chỉ bản debug (ADR-0012)
```

- Các module core dùng `kotlin("multiplatform")` nhưng **chỉ có target Android** ở MVP1. Đừng thêm target iOS.
- Không dùng `java.*` hay `android.*` trong `commonMain`; phần phụ thuộc nền tảng (player, PDF, lưu bí mật, tác vụ nền) đặt sau interface.
- `feature` → `domain`, không gọi thẳng `data`/`network`.
- Thư mục `iosApp/` còn từ template, chưa dùng ở MVP1.

## Quy ước đặt tên

- **Code phục vụ hiển thị UI dùng tiền tố `ODV`, viết gọn:** `ODVTheme`, `ODVButton`, `ODVText`, `ODVSpinner`, `ODVColors`, `ODVIcon`, `ODVLogo`. Không dùng `OneDriveVibe...` làm tiền tố.
- Áp dụng cho `:core:designsystem` và mọi `:feature:*` có composable/token. Tên file khớp tên type chính (`ODVButton` nằm trong `Button.kt` được, nhưng type vẫn có tiền tố).
- `OneDriveVibe` chỉ dùng cho **tên app** (app_name, tiêu đề tài liệu, `rootProject.name`), không dùng làm tiền tố code.
- Code không phải UI (domain, data, network...) không cần tiền tố.

## Quy tắc bắt buộc

- **Bí mật:** không ghi Client Secret, access token, header `Authorization` vào log (CH-06). Ktor Logging phải che bằng `sanitizeHeader`, không log body request lấy token.
- **Riêng tư:** màn Kết nối, Khóa, Thiết lập bảo mật, nhập PIN luôn đặt `FLAG_SECURE` và ẩn nội dung ở danh sách app gần đây; Cài đặt chỉ chặn khi bật "Bảo vệ màn hình" (ADR-0022). Từ Lát 8, Cài đặt là tab nên cờ được áp lại mỗi lần đổi tab (DH-08).
- **Chỉ đọc:** app không xóa, đổi tên hay upload tệp trên OneDrive. Quyền `Files.Read.All`.
- **UI:** màu lấy từ token, không viết hex trong màn hình. Chỉ dùng icon bộ OneDriveVibe (không Material Icons, không emoji), font Be Vietnam Pro và JetBrains Mono. Vùng chạm ≥ 48dp. Màn xem video/ảnh/PDF và tab Short luôn nền đen. Số, ngày, giờ, dung lượng theo ngôn ngữ đang dùng (CD-10, `odvLocale()`; bản tiếng Việt ra `12.480`, `4,9 MB`, `23/05/2026`), không cố định vi-VN.
- **Dữ liệu mẫu:** tài liệu trong `.claude/docs` có UPN, tenant và GUID thật của người dùng. Không đưa thêm giá trị thật vào code, log hay commit message.

## Cách làm việc

- **Thêm hoặc đổi giá trị theo yêu cầu của người dùng thì sửa luôn tài liệu liên quan:** `dac-ta-nghiep-vu.md` nếu ở đó có nêu (bảng Cài đặt mục 3.8, các quy tắc CD/VD/CH...), `thiet-ke-ui.md` nếu là giao diện, `tech-stack.md` nếu là quy ước kỹ thuật. Rà lại bằng grep các chỗ nhắc cùng giá trị để không còn câu mâu thuẫn.
- **Đổi thứ tự hay số Lát thì sửa cùng lúc** `ke-hoach-mvp1.md`, `tien-do-mvp1.md`, `tech-stack.md` và `ke-hoach-refactor-kien-truc.md` (các lần đổi trước từng để lại chỗ lệch).
- **Vẽ thiết kế làm ở topic riêng:** topic tài liệu chỉ sửa các file `.md`; việc vẽ trên Claude Design làm ở topic khác và theo đúng mục 11 của `thiet-ke-ui.md`, không tự chế giá trị.
- **Gradle và build do người dùng tự chạy thủ công.** Không chạy `./gradlew` hay lệnh build. Chỉ sửa file rồi báo lại.
- Kiểm thử là thủ công trên thiết bị (ADR-0009). Các phần dễ hỏng âm thầm cần dặn người dùng kiểm tra tay: làm mới token, khóa PIN và mã hóa config, tua video, PDF nhiều trang, giữ trạng thái khi đổi tab và khi hệ điều hành thu hồi tiến trình, tải trước video ở tab Short.
- Khi bỏ hoặc đổi thứ gì so với template/ADR, ghi comment trong code nói rõ lý do và ADR liên quan.
- Thư viện, phiên bản và quy ước nhỏ (tắt dynamic color, che header log...) ở `.claude/docs/tech-stack.md` (§9 là quy ước nhỏ, §3 là API tránh dùng trong `commonMain`).