# Architecture Decision Records

Mỗi ADR ghi **một** quyết định kiến trúc: bối cảnh, quyết định, phương án đã loại, hệ quả, cách xác nhận. Mục đích là sau này đọc lại biết **vì sao** chọn như vậy.

| ADR | Quyết định | Trạng thái | Ngày |
|---|---|---|---|
| [0001](0001-clean-architecture-mvi.md) | Clean Architecture + MVI (State, Intent, Effect) với `BaseMviViewModel` tự viết | accepted | {{NGAY}} |
| [0002](0002-cau-truc-module.md) | Đa module: `:app`, `:core:*`, `:feature:*`, `:tools:debug`, `build-logic`; domain là Kotlin/JVM | accepted | {{NGAY}} |
| [0003](0003-navigation-3.md) | Navigation 3, back stack do app sở hữu | accepted | {{NGAY}} |
| [0004](0004-koin.md) | Koin 4 cho DI, kiểm binding bằng `verify()` trong unit test | accepted | {{NGAY}} |
| [0005](0005-ktor-client.md) | Ktor Client (OkHttp engine) + kotlinx.serialization, không plugin Logging | accepted | {{NGAY}} |
| [0006](0006-usecase-bat-buoc.md) | UseCase bắt buộc giữa feature và data, kiểm bằng test kiến trúc | accepted | {{NGAY}} |
| [0007](0007-mo-hinh-loi.md) | Lỗi có cấu trúc: `AppResult` / `AppError` → `UiError` → chuỗi | accepted | {{NGAY}} |
| [0008](0008-room-version-schema.md) | Room: tăng version mỗi lần đổi schema, export schema, Migration từ bản phát hành đầu | accepted | {{NGAY}} |
| [0009](0009-unit-test-viewmodel.md) | Unit test bắt buộc cho ViewModel, module `:core:testing` | accepted | {{NGAY}} |
| [0010](0010-da-ngon-ngu.md) | Đa ngôn ngữ qua `res/values*`, đổi ngôn ngữ trong app không tạo lại Activity | accepted | {{NGAY}} |
| [0011](0011-cong-cu-debug.md) | Công cụ debug trong app (`:tools:debug`), chỉ có ở bản debug | accepted | {{NGAY}} |

## Quy ước

- Đánh số tăng dần, không dùng lại số. Tên file: `NNNN-tieu-de-khong-dau.md`.
- Trạng thái: `proposed` → `accepted` → `deprecated` hoặc `superseded by ADR-NNNN`. Thêm `rejected` cho đề xuất đã thử và bỏ (giữ lại để biết vì sao không làm).
- **Đổi quyết định thì viết ADR mới** và đánh dấu ADR cũ là superseded (ghi rõ phần nào bị thay). Không sửa nội dung ADR đã `accepted`, trừ sửa lỗi chính tả và thêm dòng "Bị thay bởi".
- Quyết định có rủi ro kỹ thuật: để `proposed`, làm spike hoặc đo, ghi kết quả vào mục "Cách xác nhận", rồi mới `accepted`.
- Chỉ ghi quyết định có ảnh hưởng kiến trúc. Quy ước nhỏ nằm ở `docs/conventions/`.
- Code đi khác ADR thì comment nêu lý do và số ADR.
- Mẫu trống: [template.md](template.md). Prompt hỗ trợ: [../prompts/write-adr.md](../prompts/write-adr.md).
