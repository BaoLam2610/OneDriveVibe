# ADR-0002: Clean Architecture + MVI (State/Intent/Effect)

**Ngày**: 2026-10-02
**Trạng thái**: accepted (phần "chỉ tạo UseCase khi có logic thật" được ADR-0016 thay thế)
**Người quyết định**: LamBao

## Bối cảnh
App có nhiều màn với trạng thái phức tạp (đồng bộ nền, trình phát, khóa/mở khóa). Cần tách logic nghiệp vụ khỏi framework để dùng lại ở MVP2, và luồng dữ liệu một chiều để dễ theo dõi.

## Quyết định
Dùng Clean Architecture (`domain` / `data` / `presentation`) và MVI: mỗi màn có một data class `State`, nhận `Intent` qua `onIntent()`, phát `Effect` cho thao tác một lần (điều hướng, toast, mở player). Dùng một `BaseMviViewModel` nhỏ. Chỉ tạo UseCase khi có logic thật; repository pass-through thì ViewModel gọi thẳng.

## Phương án đã cân nhắc
### MVVM đơn giản (state + hàm công khai)
- Ưu: ít khuôn mẫu.
- Nhược: thao tác một lần dễ bị làm thành state, gây lặp lại khi xoay màn hình.
- Lý do không chọn: app có nhiều sự kiện một lần.
### Framework MVI (Orbit, MVIKotlin)
- Ưu: có sẵn cơ chế side effect.
- Nhược: thêm phụ thuộc cho thứ chỉ cần vài chục dòng.
- Lý do không chọn: quy mô dự án cá nhân. Vẫn có thể chuyển sang Orbit sau.

## Hệ quả
### Tích cực
- `domain` là Kotlin thuần, dùng lại nguyên vẹn ở MVP2.
- Reducer thuần, dễ đọc và gỡ lỗi.
### Tiêu cực
- Nhiều file hơn mỗi màn (Contract, ViewModel, Screen, Content).
### Rủi ro
- Tách UseCase quá đà. Giảm thiểu bằng quy tắc "chỉ khi có logic thật".
