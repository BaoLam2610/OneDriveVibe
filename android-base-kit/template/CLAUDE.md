# {{TEN_DU_AN}}

App Android viết bằng Kotlin và Jetpack Compose. Package gốc `{{PACKAGE}}`. Tài liệu viết bằng tiếng Việt; trả lời thành viên bằng tiếng Việt.

> File này là luật cho AI agent (Claude Code) và cũng là tóm tắt nhanh cho người. Chi tiết nằm ở `docs/`. Khi file này mâu thuẫn với một ADR thì ADR thắng; khi đặc tả nghiệp vụ mâu thuẫn với tài liệu UI thì đặc tả quyết định **hành vi**, tài liệu UI quyết định **hình thức**.

## Đọc trước khi làm

| File | Dùng khi |
|---|---|
| `docs/README.md` | Mục lục, thứ tự đọc |
| `docs/architecture.md` | Tầng, module, luật phụ thuộc |
| `docs/tech-stack.md` | Thư viện được dùng và không được dùng |
| `docs/conventions/*.md` | Quy ước code theo chủ đề |
| `docs/adr/` | Quyết định kiến trúc. Muốn đổi thì viết ADR mới, **không sửa ADR cũ** |
| `docs/spec/` | Đặc tả nghiệp vụ, mỗi quy tắc có mã `XX-NN` |
| `docs/design/` | Đặc tả giao diện: token, component, màn hình |
| `docs/plan/` | Kế hoạch theo lát, file tiến độ (đọc đầu mỗi phiên) |
| `docs/process/lam-viec-voi-ai.md` | Cách AI làm việc trong repo này |
| `docs/prompts/` | Prompt chuẩn: thêm feature, review, viết ADR |

## Kiến trúc đã chốt

- **Clean Architecture + MVI** (ADR-0001): mỗi màn có `State`, `Intent` (`onIntent()`), `Effect`. ViewModel kế thừa `BaseMviViewModel`.
- **UseCase bắt buộc giữa feature và data** (ADR-0006): ViewModel không import `domain.repository`. Feature được dùng thẳng interface ở `domain.platform` và `domain.settings`.
- **Module** (ADR-0002): `:app`, `:core:common`, `:core:domain` (Kotlin/JVM thuần), `:core:data`, `:core:network`, `:core:database`, `:core:designsystem`, `:core:ui`, `:core:testing`, `:feature:*`, `:tools:debug`, `build-logic`. Feature không phụ thuộc feature khác.
- **Navigation 3** (ADR-0003): back stack do app sở hữu. Màn nhận lambda, không nhận nav controller hay back stack.
- **Koin 4** (ADR-0004), không Hilt. Mỗi module có `xxxModule`; `KoinVerifyTest` phải pass.
- **Ktor Client** (ADR-0005), OkHttp engine, kotlinx.serialization. Không Retrofit. **Không cài plugin `Logging`.**
- **Lỗi có cấu trúc** (ADR-0007): `AppResult<T>` / `AppError` ở domain và data; UI đổi `AppError` → `UiError` → chuỗi. Không hiện, không lưu `e.message`.
- **Room** (ADR-0008): **mỗi lần đổi schema phải tăng `version`**, export schema, ghi "Lịch sử version". Từ bản phát hành đầu bắt buộc có Migration.
- **Unit test cho ViewModel** (ADR-0009): mỗi ViewModel mới có test; dùng `:core:testing` (fake, `MainDispatcherRule`, Turbine).
- **Đa ngôn ngữ** (ADR-0010): chuỗi chỉ ở `res/values*`; `domain` và `data` không chứa chuỗi hiển thị.
- **Công cụ debug** (ADR-0011): `:tools:debug` chỉ có ở bản debug, nối qua cổng `DebugTools` (hai bản debug/release cùng API). Bản release không ghi log nào.

## Quy tắc bắt buộc

- **Bí mật:** không ghi token, mật khẩu, header `Authorization`, giá trị ô nhập, `e.message` vào log. Log chỉ ghi mã lỗi và tên lớp ngoại lệ.
- **UI:** màu, chữ, khoảng cách, bo góc lấy từ token của `:core:designsystem`; không viết hex hay số dp tự đặt trong màn hình. Vùng chạm ≥ 48dp. Không tự chế câu chữ chưa có trong thiết kế; chuỗi mới đánh dấu `[mới]` trong PR và chờ duyệt.
- **Tiền tố UI:** composable và type phục vụ hiển thị có tiền tố `Pfx` (`PfxButton`, `PfxTheme`). Code không phải UI không có tiền tố.
- **Số, ngày, dung lượng** định dạng theo ngôn ngữ đang dùng của app (`LocalLocale`), không cố định một locale.
- **Coroutine:** không `runCatching` hay `catch (e: Exception)` trần trong code suspend mà không ném lại `CancellationException`. Không `GlobalScope`, không `runBlocking` trong code app. Tiêm `DispatcherProvider` và `Clock`.
- **Giá trị đã lưu trên máy** (tên tệp, khóa DataStore, khóa Keystore, tên bảng) nằm trong file Constants có ghi chú "không được đổi giá trị".
- **Bỏ hoặc đổi so với ADR/template** thì ghi comment trong code nêu lý do và ADR liên quan.

## Cách làm việc (cho AI)

- **Không ghi đè file có sẵn bằng Write.** Chỉ dùng Edit; Read lại file ngay trước khi sửa nếu nó đã được sửa trong phiên hoặc hệ thống báo "changed on disk". Người trong team có thể sửa tay bất cứ lúc nào.
- **Đổi một giá trị theo yêu cầu thì sửa luôn tài liệu liên quan** (đặc tả, thiết kế, tech-stack) và `grep` mọi chỗ nhắc cùng giá trị để không còn câu mâu thuẫn.
- **Đầu phiên đọc file tiến độ** (`docs/plan/tien-do.md`). Bắt đầu hoặc xong một bước thì cập nhật mục "Đang làm" và thêm một dòng ngắn (≤ 3 câu) vào Nhật ký.
- **Build và test:** {{CHON_MOT: "AI được chạy `./gradlew assembleDebug testDebugUnitTest` để kiểm" | "AI không chạy Gradle; người tự build, AI liệt kê 'rủi ro build' khi bàn giao"}}.
- **API ít phổ biến:** đọc mã nguồn hoặc tài liệu đúng phiên bản trong `gradle/libs.versions.toml` trước khi dùng, không đoán.
- **Sửa hàng loạt bằng script:** sau khi chạy, so danh sách khai báo cấp cao và `git diff` với HEAD.
- **Hàm cập nhật state đặt tên `applyX`/`updateX`**, không `setX` (trùng setter sinh ra từ `var x ... private set`).
- **Không commit, không push** trừ khi được yêu cầu rõ. Cuối mỗi phần việc gợi ý một tên commit kiểu Conventional Commits (`feat(auth): add login screen`).
- Kiểm tra `docs/process/review-checklist.md` trước khi báo xong.
