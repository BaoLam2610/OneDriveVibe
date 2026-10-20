# ADR-0006: UseCase bắt buộc giữa feature và data

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0001, ADR-0002, `architecture.md` mục 2 và 3

## Bối cảnh

Ở dự án trước, luật ban đầu là "chỉ tạo UseCase khi có logic thật". Bốn ngày sau đổi thành bắt buộc vì ViewModel bắt đầu chứa luật nghiệp vụ và gọi repository lung tung; việc đổi tốn bốn phase refactor với khoảng 27 UseCase. Chốt từ đầu rẻ hơn nhiều.

## Quyết định

- ViewModel và app shell **chỉ** phụ thuộc UseCase (`domain.usecase`) và interface **không phải repository** của domain: `domain.platform` (khả năng nền tảng), `domain.settings` (cài đặt đọc lúc chạy), `domain.model`.
- Không import `domain.repository` trong `feature/**` và `app/src/main/**`.
- UseCase tên theo nghiệp vụ, một `operator fun invoke`. UseCase mỏng chỉ ủy quyền repository vẫn được phép.
- Kiểm bằng **test kiến trúc** (Konsist) trong CI.

## Phương án đã cân nhắc

### UseCase chỉ khi có logic
- Ưu: ít lớp một dòng.
- Nhược: ranh giới mờ, mỗi người hiểu "logic thật" một kiểu; luật nghiệp vụ trôi vào ViewModel.
- Lý do không chọn: đã thử ở dự án trước và phải đổi.

### Bọc cả `platform` và `settings` bằng UseCase
- Lý do không chọn: thêm lớp một dòng vô ích; các interface này không chứa luật nghiệp vụ.

## Hệ quả

### Tích cực
- Luật nghiệp vụ có một chỗ ở; ViewModel chỉ còn logic trình bày.
- Test ViewModel dùng UseCase thật + repository giả.

### Tiêu cực
- Nhiều UseCase một dòng.

### Rủi ro
- Gradle không ép được (feature phải thấy module domain): phụ thuộc vào test kiến trúc.

## Cách xác nhận

- Test kiến trúc pass; bản tối thiểu: `grep -rn "^import {{PACKAGE}}.core.domain.repository" feature app/src/main` không ra kết quả.
