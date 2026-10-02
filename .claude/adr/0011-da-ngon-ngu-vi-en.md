# ADR-0011: Đa ngôn ngữ Tiếng Việt + English qua Android `res/`

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
App cần hiển thị bằng Tiếng Việt và English, sau này có thể thêm ngôn ngữ khác. Cách lưu chuỗi ở MVP2 phụ thuộc UI iOS dùng Compose Multiplatform hay SwiftUI (chưa quyết định).

## Quyết định
Chuỗi nằm ở tầng UI trong `res/values` (Tiếng Việt, mặc định) và `res/values-en`. Chọn ngôn ngữ trong app bằng `AppCompatDelegate.setApplicationLocales()` kèm `locales_config.xml`. `domain` và `data` **không chứa chuỗi hiển thị**: lỗi là kiểu có cấu trúc (`AppError` → `UiError`), UI đổi sang chuỗi. Ngày, dung lượng, thời lượng, số định dạng theo locale.

## Phương án đã cân nhắc
### Compose Multiplatform resources
- Ưu: dùng chung với iOS nếu UI iOS dùng CMP.
- Nhược: không dùng được từ Swift; ràng buộc vào quyết định chưa chốt.
- Lý do không chọn: chưa biết UI iOS dùng gì.
### moko-resources
- Ưu: dùng được cho cả CMP lẫn SwiftUI.
- Nhược: thêm plugin Gradle và một lớp trừu tượng.
- Lý do không chọn: chưa cần ở MVP1.
### Hardcode chuỗi trong code
- Nhược: không đổi ngôn ngữ được, khó dịch.
- Lý do không chọn: không đáp ứng yêu cầu.

## Hệ quả
### Tích cực
- Đơn giản nhất cho MVP1; thêm ngôn ngữ chỉ cần thêm bộ chuỗi.
- Vì domain không chứa chuỗi, đổi cơ chế ở MVP2 chỉ ảnh hưởng tầng UI.
### Tiêu cực
- Chuỗi chưa dùng chung được với iOS.
### Rủi ro
- Lỡ đưa chuỗi hiển thị vào `domain`/`data` thì phải gỡ khi sang MVP2. Giảm thiểu bằng quy tắc "lỗi là kiểu, không phải chuỗi".
