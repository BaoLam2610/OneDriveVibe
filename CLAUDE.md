# OneDriveVibe

App Android chỉ đọc OneDrive for Business qua Microsoft Graph: xem video, ảnh, truyện PDF. MVP1 là Android; MVP2 thêm iOS bằng KMP. Tài liệu viết bằng tiếng Việt, trả lời người dùng bằng tiếng Việt. Tên cũ "ODV (OneDrive Viewer)" đã đổi thành OneDriveVibe; tên app/tài liệu là OneDriveVibe, còn tiền tố code UI là `ODV` (xem "Quy ước đặt tên"); package giữ `com.lambao.odv`.

## Tài liệu (đọc trước khi làm)

| File | Dùng khi |
|---|---|
| `.claude/docs/dac-ta-nghiep-vu.md` | Nghiệp vụ: cái gì xảy ra. Các mã KN, CH, DB, TM, DS, VD, PD, CD, TK... |
| `.claude/docs/thiet-ke-ui.md` + `odv-tokens.json` | Giao diện: token, component, màn hình. Mục 1 là quy tắc bắt buộc khi dựng |
| `.claude/docs/onedrive-graph-api.md` | Endpoint Graph, token, delta, mã lỗi |
| `.claude/docs/onedrive-graph-responses.md` | Mẫu response thật (giá trị đã thay bằng mẫu) |
| `.claude/adr/` | 13 quyết định kiến trúc. Muốn đổi thì viết ADR mới, không sửa ADR cũ |
| `.claude/docs/ke-hoach-mvp1.md` | Kế hoạch triển khai MVP1 theo lát cắt dọc, thư viện đã chọn |
| `.claude/docs/ke-hoach-refactor-kien-truc.md` | Đang refactor nền móng (R0 đến R6, 2026-10-06): làm đúng phase, không làm trước |
| `.claude/docs/tien-do-mvp1.md` | Đang ở lát nào, bước nào. Đọc đầu mỗi phiên, cập nhật khi bắt đầu hoặc xong một bước |

Khi mâu thuẫn: đặc tả nghiệp vụ quyết định hành vi, tài liệu UI quyết định hình thức. Không tự chế màu, cỡ chữ, khoảng cách hay câu chữ chưa có trong thiết kế; hỏi lại.

## Kiến trúc đã chốt

- **Clean Architecture + MVI** (ADR-0002): mỗi màn có `State`, `Intent` (`onIntent()`), `Effect` cho thao tác một lần. **UseCase bắt buộc giữa feature và data** (ADR-0016, thay quy tắc "chỉ khi có logic thật"): ViewModel không gọi thẳng repository.
- **Navigation 3** (ADR-0003): back stack do app sở hữu, `NavDisplay` + `entryProvider`. Không truyền nav controller xuống composable, dùng lambda.
- **Koin 4.x** (ADR-0004), không dùng Hilt.
- **Xác thực Client Credentials** (ADR-0005): 4 trường Tenant ID, Client ID, Client Secret, UPN; gọi qua `/users/{UPN}/drive`. Không MSAL, không OneDrive cá nhân.
- **Graph REST qua Ktor** (ADR-0006), kotlinx.serialization. Không dùng Graph SDK, Retrofit.
- **Offline-first** (ADR-0007): Room KMP (`BundledSQLiteDriver`) là nguồn dữ liệu duy nhất cho UI, đồng bộ bằng delta query. **Mỗi lần đổi schema (thêm/bỏ/đổi cột, bảng, index) phải tăng `version` ở `OdvDatabase`** và ghi vào "Lịch sử version" (ADR-0015); quên là crash "Room cannot verify the data integrity". Trước phát hành: tăng version là đủ (có `fallbackToDestructiveMigration`); từ bản phát hành đầu: bắt buộc viết Migration.
- **Bảo mật config** (ADR-0008): AES-GCM, khóa dẫn xuất từ PIN bằng Argon2id + Android Keystore. **Không lưu PIN, không lưu hash PIN.** Access token chỉ giữ trong bộ nhớ.
- **Không viết test tự động** (ADR-0009): không unit test, UI test, coverage. Đừng thêm junit, kotlin-test, MockK, Turbine, Kover.
- **Đa ngôn ngữ VI + EN** (ADR-0011): chuỗi trong Android `res/values` (VI, mặc định) và `res/values-en`. `domain` và `data` không chứa chuỗi hiển thị; lỗi là kiểu có cấu trúc (`AppError` → `UiError`).
- **Công cụ debug** (ADR-0012, 0013, 0020): module `:tools:debug` chỉ có trong bản debug (nút bọ nổi luôn nằm trên cùng → màn Debug: log API có màn chi tiết + tìm kiếm + sao chép, log local, lưu trữ; FLAG_SECURE theo cài đặt Bảo vệ màn hình, ADR-0020). Log local dùng Kermit; log API đi qua `HttpTrafficRecorder`, hiển thị **đầy đủ không che** (có công tắc che), chỉ trong bộ nhớ, không dùng Ktor `Logging`. Release không ghi log nào và không có recorder.

## Cấu trúc module (ADR-0001, ADR-0010)

```
:androidApp          Android app (ADR-0010 gọi là :app; giữ tên của template)
:build-logic         convention plugin odv.kmp.library
:core:common
:core:domain         chỉ phụ thuộc :core:common
:core:data           → common, domain, network, database, security
:core:network  :core:database  :core:security   → common
:core:designsystem   Compose, token và component dùng chung
:feature:*           tạo khi bắt đầu làm feature đó (auth, browser, library, player, imageviewer, pdfviewer, settings)
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
- **Riêng tư:** màn Kết nối, Khóa, Thiết lập bảo mật, nhập PIN luôn đặt `FLAG_SECURE` và ẩn nội dung ở danh sách app gần đây; màn Cài đặt chỉ chặn khi bật "Bảo vệ màn hình" (ADR-0022).
- **Chỉ đọc:** app không xóa, đổi tên hay upload tệp trên OneDrive. Quyền `Files.Read.All`.
- **UI:** màu lấy từ token, không viết hex trong màn hình. Chỉ dùng icon bộ OneDriveVibe (không Material Icons, không emoji), font Be Vietnam Pro và JetBrains Mono. Vùng chạm ≥ 48dp. Màn xem video/ảnh/PDF luôn nền đen. Số, ngày, giờ, dung lượng theo ngôn ngữ đang dùng (CD-10, `odvLocale()`; bản tiếng Việt ra `12.480`, `4,9 MB`, `23/05/2026`), không cố định vi-VN.
- **Dữ liệu mẫu:** tài liệu trong `.claude/docs` có UPN, tenant và GUID thật của người dùng. Không đưa thêm giá trị thật vào code, log hay commit message.

## Cách làm việc

- **Thêm hoặc đổi giá trị theo yêu cầu của người dùng thì sửa luôn tài liệu liên quan:** `dac-ta-nghiep-vu.md` nếu ở đó có nêu (bảng Cài đặt mục 3.8, các quy tắc CD/VD/CH...), `thiet-ke-ui.md` nếu là giao diện, `tech-stack.md` nếu là quy ước kỹ thuật. Rà lại bằng grep các chỗ nhắc cùng giá trị để không còn câu mâu thuẫn.
- **Gradle và build do người dùng tự chạy thủ công.** Không chạy `./gradlew` hay lệnh build. Chỉ sửa file rồi báo lại.
- Kiểm thử là thủ công trên thiết bị (ADR-0009). Các phần dễ hỏng âm thầm cần dặn người dùng kiểm tra tay: làm mới token, khóa PIN và mã hóa config, tua video, PDF nhiều trang.
- Khi bỏ hoặc đổi thứ gì so với template/ADR, ghi comment trong code nói rõ lý do và ADR liên quan.
- Thư viện, phiên bản và quy ước nhỏ (tắt dynamic color, che header log...) ở `.claude/docs/tech-stack.md` (§9 là quy ước nhỏ, §3 là API tránh dùng trong `commonMain`).
