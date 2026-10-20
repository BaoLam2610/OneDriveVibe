# ADR-0002: Cấu trúc đa module

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0001, ADR-0006, `architecture.md`

## Bối cảnh

Cần ép luật tầng bằng công cụ (không chỉ bằng quy ước), build tăng dần nhanh, và nhiều người làm song song ở các feature khác nhau mà ít đụng nhau.

## Quyết định

- `:app`; `:core:common`, `:core:domain` (cả hai **Kotlin/JVM**), `:core:data`, `:core:network`, `:core:database`, `:core:designsystem`, `:core:ui`, `:core:testing`; `:feature:<ten>` (tạo khi bắt đầu feature); `:tools:debug` (chỉ bản debug); `build-logic` (convention plugin).
- Convention plugin: `pfx.android.application`, `pfx.android.library`, `pfx.android.compose`, `pfx.android.feature` (library + compose + phụ thuộc chung của feature + `:core:testing` cho test), `pfx.jvm.library`, `pfx.quality` (ktlint + detekt).
- Feature không phụ thuộc feature; phần dùng chung lên `:core:<ten>`.

## Phương án đã cân nhắc

### Một module duy nhất, chia package
- Ưu: đơn giản, build cấu hình ít.
- Nhược: không ép được luật phụ thuộc; build chậm dần.
- Lý do không chọn: luật tầng chỉ còn là quy ước, dễ vỡ khi đông người.

### Tách module theo tầng của từng feature (`:feature:cart:domain`, `:feature:cart:data`, `:feature:cart:ui`)
- Ưu: cô lập tối đa.
- Nhược: số module nhân ba, cấu hình nặng cho app cỡ vừa.
- Lý do không chọn: quá mức cần thiết; tách khi một feature thật sự lớn (viết ADR mới).

## Hệ quả

### Tích cực
- Domain là Kotlin/JVM nên Gradle chặn `android.*` trong domain.
- Feature không thấy `:core:data`/`:core:network` vì convention plugin không thêm chúng.

### Tiêu cực
- Feature vẫn thấy `domain.repository` (cùng module domain): luật "chỉ qua UseCase" cần test kiến trúc (ADR-0006).
- Thêm module tốn công cấu hình ban đầu (giảm nhờ convention plugin).

### Rủi ro
- Convention plugin quá "thông minh" khó hiểu: mỗi plugin chỉ làm một việc, logic riêng của module để trong `build.gradle.kts` của module đó.

## Cách xác nhận

- `./gradlew :core:domain:dependencies` không có artifact Android.
- Test kiến trúc pass (feature không import feature, không import data/network/database).
