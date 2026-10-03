# Architecture Decision Records — OneDriveVibe

Mỗi ADR ghi **một quyết định kiến trúc**: bối cảnh, quyết định, phương án đã loại và hệ quả. Mục đích là sau này đọc lại biết **vì sao** chọn như vậy.

**Một thư mục dùng chung cho cả dự án** (Android và iOS ở MVP2 cùng một chuỗi số, không tách theo nền tảng). Quyết định riêng cho iOS sẽ là ADR tiếp theo trong cùng thư mục, ghi rõ nền tảng trong tiêu đề.

> Vị trí: `.claude/adr/` (giữ nguyên ở đây, không chuyển sang `docs/adr/`).

| ADR | Quyết định | Trạng thái | Ngày |
|---|---|---|---|
| [0001](0001-android-truoc-san-sang-kmp.md) | Android trước, sẵn sàng KMP: module dùng chung chỉ khai báo `androidTarget()` ở MVP1 | accepted | 2026-10-02 |
| [0002](0002-clean-architecture-mvi.md) | Clean Architecture + MVI (State/Intent/Effect); chỉ tạo UseCase khi có logic thật | accepted | 2026-10-02 |
| [0003](0003-navigation-3.md) | Navigation 3 thay cho Navigation Compose 2.x | accepted | 2026-10-02 |
| [0004](0004-koin-thay-hilt.md) | Koin thay cho Hilt | accepted | 2026-10-02 |
| [0005](0005-xac-thuc-client-credentials.md) | Xác thực Client Credentials, chỉ OneDrive for Business, nhập 4 trường | accepted | 2026-10-02 |
| [0006](0006-graph-rest-qua-ktor.md) | Gọi Graph REST trực tiếp qua Ktor, không dùng Graph SDK hay MSAL | accepted | 2026-10-02 |
| [0007](0007-room-kmp-offline-first.md) | Room KMP, offline-first: Room là nguồn dữ liệu duy nhất cho UI, đồng bộ delta | accepted | 2026-10-02 |
| [0008](0008-bao-mat-config-pin-argon2id.md) | Bảo mật config: khóa dẫn xuất từ PIN (Argon2id) + Keystore, không lưu PIN hay hash | accepted | 2026-10-02 |
| [0009](0009-khong-viet-test-tu-dong.md) | Không viết test tự động (dự án cá nhân) | accepted | 2026-10-02 |
| [0010](0010-chia-module.md) | Chia module: tách nhỏ phần core, module feature tạo khi bắt đầu làm feature đó | accepted | 2026-10-02 |
| [0011](0011-da-ngon-ngu-vi-en.md) | Đa ngôn ngữ VI + EN qua Android `res/`, domain không chứa chuỗi hiển thị | accepted | 2026-10-02 |
| [0012](0012-cong-cu-debug-va-kermit.md) | Công cụ debug trong app (`:tools:debug`, chỉ bản debug), log bằng Kermit, retry bằng `HttpRequestRetry` | accepted | 2026-10-02 |
| [0013](0013-log-api-debug-day-du-khong-che.md) | Log API ở bản debug hiển thị đầy đủ (không che), che là tùy chọn; thay thế một phần ADR-0012 | accepted | 2026-10-02 |
| [0014](0014-phong-bi-pin-sinh-trac-hoc-tu-khoa.md) | Phong bì PIN hai lớp, bộ đếm sai bền, sinh trắc học bọc khóa dẫn xuất, tự khóa; bổ sung ADR-0008 | accepted | 2026-10-03 |

## Quy ước
- Đánh số tăng dần, không dùng lại số. Tên file: `NNNN-tieu-de-khong-dau.md`.
- Trạng thái: `proposed` → `accepted` → `deprecated` hoặc `superseded by ADR-NNNN`.
- Đổi quyết định thì **viết ADR mới** và đánh dấu ADR cũ là superseded, không sửa lịch sử.
- Chỉ ghi quyết định có ảnh hưởng kiến trúc; quy ước nhỏ (tắt dynamic color, che header log...) nằm ở `tech-stack.md` §9.
- Mẫu trống: [template.md](template.md).
