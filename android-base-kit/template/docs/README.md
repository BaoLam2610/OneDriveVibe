# Tài liệu {{TEN_DU_AN}}

## Thứ tự đọc cho thành viên mới

1. `../CLAUDE.md`: tóm tắt luật trong một trang.
2. [architecture.md](architecture.md): tầng, module, luồng dữ liệu.
3. [conventions/mvi.md](conventions/mvi.md) và [conventions/compose-ui.md](conventions/compose-ui.md): cách viết một màn.
4. [conventions/error-handling.md](conventions/error-handling.md) và [conventions/concurrency-time.md](conventions/concurrency-time.md): hai chủ đề dễ sai nhất.
5. [conventions/testing.md](conventions/testing.md): cách viết test ViewModel.
6. [process/vertical-slice.md](process/vertical-slice.md): một tính năng đi từ đặc tả tới merge thế nào.
7. Các file còn lại thì đọc khi đụng tới chủ đề đó.

## Cấu trúc

```
docs/
├── README.md               file này
├── architecture.md         tầng, module, luật phụ thuộc
├── tech-stack.md           thư viện, lý do chọn, thứ không dùng
├── debug-tools.md          thiết kế :tools:debug
├── conventions/            quy ước code (13 file, xem bảng dưới)
├── process/                quy trình làm việc và checklist
├── prompts/                prompt dùng với AI agent: thêm feature, review, viết ADR
├── templates/              MẪU TRỐNG, chép ra rồi điền, không sửa bản mẫu
├── adr/                    quyết định kiến trúc
├── spec/                   đặc tả nghiệp vụ (chép từ templates/dac-ta-nghiep-vu.md)
├── design/                 đặc tả giao diện (chép từ templates/thiet-ke-ui.md) + token
└── plan/                   kế hoạch và tiến độ (chép từ templates/ke-hoach.md, tien-do.md)
```

`spec/`, `design/`, `plan/` chưa có lúc khởi tạo; tạo khi có feature đầu tiên.

## Quy ước

| File | Nội dung |
|---|---|
| [kotlin.md](conventions/kotlin.md) | Phong cách Kotlin, hằng số, visibility, lỗi biên dịch hay gặp |
| [naming.md](conventions/naming.md) | Tên package, file, type, tiền tố UI, mã quy tắc |
| [mvi.md](conventions/mvi.md) | State, Intent, Effect, BaseMviViewModel, Screen/Content |
| [compose-ui.md](conventions/compose-ui.md) | Composable, token, trạng thái tải, hiệu năng, cử chỉ |
| [navigation.md](conventions/navigation.md) | Navigation 3, route, tab, process death |
| [di-koin.md](conventions/di-koin.md) | Module Koin, `singleOf`, `verify()`, ghi đè ở debug |
| [data-layer.md](conventions/data-layer.md) | Repository, Ktor, Room, DataStore, offline-first, xóa dữ liệu |
| [error-handling.md](conventions/error-handling.md) | `AppResult`, `AppError`, `UiError`, hủy coroutine |
| [concurrency-time.md](conventions/concurrency-time.md) | Coroutine, dispatcher, scope, single-flight, đồng hồ |
| [resources-i18n.md](conventions/resources-i18n.md) | Chuỗi, plurals, locale, đổi ngôn ngữ trong app |
| [security.md](conventions/security.md) | Bí mật, FLAG_SECURE, sao lưu, mã hóa |
| [logging.md](conventions/logging.md) | Kermit, cái gì được log, log tạm |
| [testing.md](conventions/testing.md) | Unit test ViewModel, `:core:testing`, test kiến trúc, `verify()` |

## Quy trình

| File | Nội dung |
|---|---|
| [vertical-slice.md](process/vertical-slice.md) | Từ đặc tả tới merge; sửa lỗi khó |
| [review-checklist.md](process/review-checklist.md) | Checklist review PR |
| [manual-test-checklist.md](process/manual-test-checklist.md) | Kiểm tay trên thiết bị |
| [lam-viec-voi-ai.md](process/lam-viec-voi-ai.md) | Làm việc với AI agent |

## Prompt cho AI agent

| File | Dùng khi |
|---|---|
| [add-feature.md](prompts/add-feature.md) | Làm một lát mới khi đặc tả và thiết kế đã có |
| [review.md](prompts/review.md) | Review chỉ đọc sau mỗi lát hoặc trước khi merge PR lớn |
| [write-adr.md](prompts/write-adr.md) | Có quyết định kiến trúc mới hoặc thay ADR cũ |

## Giữ tài liệu đúng

- Một giá trị chỉ có **một** nơi định nghĩa. Nơi khác thì trỏ tới, không chép lại.
- Đổi code làm một quy ước sai thì sửa quy ước trong cùng PR.
- ID (mã quy tắc, mã lát, số ADR) không bao giờ đổi hay dùng lại.
