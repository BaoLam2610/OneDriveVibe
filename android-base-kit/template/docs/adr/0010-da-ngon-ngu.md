# ADR-0010: Đa ngôn ngữ

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0007, `conventions/resources-i18n.md`

## Bối cảnh

App hỗ trợ <Tiếng Việt (mặc định) và English>, người dùng chọn được ngôn ngữ trong app khác với ngôn ngữ máy. Số, ngày, dung lượng phải theo ngôn ngữ đang dùng.

## Quyết định

- Chuỗi ở `res/values` (mặc định) và `res/values-xx` của từng module. `domain` và `data` không chứa chuỗi hiển thị.
- Chọn ngôn ngữ qua `AppCompatDelegate.setApplicationLocales` (sau interface `AppLocaleController` ở `domain.platform`); `MainActivity` là `AppCompatActivity`; `locales_config.xml`; `localeFilters`.
- `MainActivity` khai `locale|layoutDirection` trong `configChanges`: đổi ngôn ngữ **không tạo lại Activity** (không chớp, giữ vị trí cuộn).
- Hệ quả bắt buộc: đọc `Resources` bằng `LocalResources.current`, locale bằng `LocalLocale.current` (helper `pfxLocale()`), không `LocalContext.current.resources`, không `Locale.getDefault()`.

## Phương án đã cân nhắc

### Để Activity tạo lại khi đổi ngôn ngữ
- Ưu: không phải để ý cách đọc `Resources`.
- Nhược: màn hình chớp; trạng thái đọc bất đồng bộ (theme từ DataStore) có thể nháy về mặc định.
- Lý do không chọn: trải nghiệm kém; đã gặp ở dự án trước.

## Hệ quả

### Tích cực
- Đổi ngôn ngữ tức thì, mượt.

### Tiêu cực
- Phải tuân thủ luật đọc `Resources`/locale; sai thì chữ cũ nằm lại (khó thấy khi review).

### Rủi ro
- Code cũ hoặc thư viện đọc `Locale.getDefault()`: kiểm tay đổi ngôn ngữ ở mọi màn mỗi lát.

## Cách xác nhận

- Kiểm tay: đổi ngôn ngữ ở màn sâu, mọi chữ (kể cả dialog, sheet, định dạng số/ngày) đổi ngay, vị trí cuộn giữ.
- Lint `MissingTranslation` mức error.
