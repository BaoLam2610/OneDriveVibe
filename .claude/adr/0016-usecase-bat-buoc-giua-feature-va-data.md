# ADR-0016: UseCase bắt buộc giữa feature và data

**Ngày**: 2026-10-06
**Trạng thái**: accepted (thay phần "chỉ tạo UseCase khi có logic thật" của ADR-0002)
**Người quyết định**: LamBao

## Bối cảnh
ADR-0002 cho ViewModel gọi thẳng repository khi chỉ là pass-through. Rà soát 2026-10-06 cho thấy cách này làm nghiệp vụ trôi vào ViewModel: kiểm tra "config còn và giải mã được" lặp ở `SplashViewModel` và `ConnectViewModel`; ánh xạ `SyncState` kèm quy tắc bỏ qua `AppLocked` và "có mạng lại thì đồng bộ" chép y hệt ở `BrowserViewModel` và `LibraryViewModel`; chọn đọc Room hay gọi API (TM-07) và dựng danh sách chuyển trước/sau nằm trong ViewModel. Ranh giới "có logic thật hay không" cũng thay đổi theo từng lát nên mỗi lần phải đánh giá lại. MVP2 có thể dùng SwiftUI, khi đó UseCase là bề mặt mà iOS gọi tới.

## Quyết định
`:feature:*` chỉ phụ thuộc UseCase và các interface domain không phải repository (`NetworkMonitor`, `UtcOffsetProvider`, `BiometricAuthenticator`...), không gọi trực tiếp repository. Mỗi UseCase làm một việc có tên nghiệp vụ, gọi bằng `operator fun invoke`; UseCase mỏng chỉ ủy quyền cho repository vẫn được phép. UseCase được khai báo trong `domainModule` (Koin). Phần còn lại của ADR-0002 (MVI, State/Intent/Effect, `BaseMviViewModel`) giữ nguyên.

## Phương án đã cân nhắc
### Giữ ADR-0002, chỉ thêm UseCase cho phần có logic
- Ưu: ít file hơn.
- Nhược: ranh giới mờ, mỗi lát lại tranh luận; logic dễ rò vào ViewModel như đã xảy ra.
- Lý do không chọn: mục tiêu của dự án là nền móng chắc, không tối ưu cho số file.

### Chia thêm tầng "presentation mapper" cho từng màn
- Ưu: ViewModel càng mỏng.
- Nhược: thêm tầng khi chưa có nhu cầu.
- Lý do không chọn: UseCase đã đủ.

## Đính chính (2026-10-06)
Bản đầu của mục Hệ quả ghi "quy tắc phụ thuộc kiểm được bằng Gradle": **không đúng**. Feature phụ thuộc `:core:domain`, nơi chứa luôn interface repository, nên Gradle không che được chúng; quy tắc chỉ giữ bằng quy ước và review (kèm lệnh `grep` ở kế hoạch R5d). Ngoài ra, interface không phải repository mà feature được dùng trực tiếp gồm `NetworkMonitor`, `UtcOffsetProvider`, `BiometricAuthenticator`, `BrowserPreferences`, `PlayerPreferences`, `SecuritySettings`. Số UseCase ước tính khoảng 27 chứ không phải 10, vì áp dụng cho cả app shell. Liên kết Koin của UseCase đặt ở `:core:data`.

## Hệ quả
### Tích cực
- Nghiệp vụ nằm một chỗ, hết chép lặp giữa ViewModel; ViewModel chỉ lo State/Intent/Effect.
- MVP2 dùng lại UseCase như API cho iOS.
### Tiêu cực
- Thêm khoảng 27 UseCase mỏng (xem Đính chính) và một lớp ủy quyền.
### Rủi ro
- Tách quá đà thành UseCase "một dòng gọi một dòng". Giảm thiểu: tên theo nghiệp vụ (`ObserveFolderContentUseCase`), không theo thao tác kỹ thuật; gộp khi hai UseCase luôn đi cùng nhau.
