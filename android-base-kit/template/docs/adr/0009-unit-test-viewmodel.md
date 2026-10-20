# ADR-0009: Unit test bắt buộc cho ViewModel

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0001, ADR-0004, ADR-0006, `conventions/testing.md`

## Bối cảnh

Dự án trước không viết test tự động. Tám phase refactor và mọi lát đều dựa vào kiểm tay; nhiều lỗi hồi quy chỉ lộ khi người dùng thử trên máy, và mỗi phase phải kèm danh sách kiểm tay dài. Dự án công ty có nhiều người, cần lưới an toàn tự động cho phần logic có thể test trên JVM.

## Quyết định

- **Bắt buộc:** unit test (JVM, JUnit 4) cho mọi ViewModel mới hoặc đổi hành vi; `KoinVerifyTest`; test kiến trúc (Konsist).
- **Nên có:** test UseCase có logic thật, mapper phức tạp, policy domain.
- **Từ bản phát hành đầu:** test migration Room.
- Module `:core:testing`: `MainDispatcherRule`, `TestDispatcherProvider`, `FixedClock`, helper `testMvi` (Turbine), fake dùng chung. Ưu tiên fake; MockK chỉ cho lớp bên thứ ba.
- Chưa làm UI test Compose; thêm bằng ADR mới khi cần.
- Không đặt chỉ tiêu % coverage.

## Phương án đã cân nhắc

### Không test (như dự án trước)
- Lý do không chọn: chi phí kiểm tay và lỗi hồi quy cao khi đông người.

### Test toàn diện (UI test, screenshot test, coverage ≥ 80%)
- Lý do không chọn: tốn công ở giai đoạn giao diện còn đổi nhiều; ViewModel là nơi logic tập trung nên đáng test nhất.

## Hệ quả

### Tích cực
- Refactor ViewModel và UseCase có lưới an toàn.
- MVI làm test đơn giản: gửi Intent, kiểm State/Effect.

### Tiêu cực
- Thêm thời gian viết test và duy trì fake.

### Rủi ro
- Test kiểm chi tiết cài đặt thay vì hành vi thì vỡ khi refactor: test State/Effect và lời gọi vào fake, không test biến private.

## Cách xác nhận

- CI chạy `testDebugUnitTest` cho mọi PR.
- Review: ViewModel mới không có test thì không merge.
