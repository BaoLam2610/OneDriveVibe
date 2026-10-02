# ADR-0009: Không viết test tự động

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Dự án cá nhân, một người làm, không có yêu cầu nghiệm thu hay CI bắt buộc.

## Quyết định
Không viết unit test, UI test và không đo coverage. Không có module `:core:testing`, Kover, Turbine, MockK/Mokkery, Compose UI Test.

## Phương án đã cân nhắc
### Test cho phần lõi (UseCase, reducer, auth, mã hóa)
- Ưu: bắt lỗi hồi quy ở phần khó thấy.
- Nhược: tốn thời gian viết và bảo trì.
- Lý do không chọn: ưu tiên tốc độ cho dự án cá nhân.

## Hệ quả
### Tích cực
- Ít khuôn mẫu, ra tính năng nhanh hơn.
### Tiêu cực
- Dễ hồi quy khi sửa phần dùng chung; phải kiểm tra thủ công trên thiết bị.
### Rủi ro
- Các phần dễ hỏng âm thầm: làm mới token, khóa PIN và mã hóa config, tua video, PDF nhiều trang. Phải kiểm tra tay mỗi lần đụng tới.
- Kiến trúc vẫn giữ reducer và UseCase thuần nên thêm test sau này không cần đổi cấu trúc. Nếu thêm: `kotlin.test` + fakes/Mokkery cho code dùng chung (MockK/Mockito chỉ chạy JVM). ADR mới sẽ thay thế ADR này.
