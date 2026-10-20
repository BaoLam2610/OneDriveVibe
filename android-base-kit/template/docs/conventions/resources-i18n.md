# Chuỗi, ngôn ngữ, định dạng

Quyết định: ADR-0010. Ngôn ngữ mặc định ở `res/values`, ngôn ngữ thêm ở `res/values-xx`.

## 1. Chuỗi

- Mọi chữ hiển thị nằm trong `strings.xml` của **module dùng nó** (`feature/cart/src/main/res/values/strings.xml`). Chuỗi dùng chung (`common_retry`, `common_cancel`) ở `:core:ui`.
- Mỗi chuỗi có đủ bản ở mọi thư mục ngôn ngữ. Android Lint `MissingTranslation` để mức error.
- `domain` và `data` **không** chứa chuỗi hiển thị. Lỗi là `AppError` → `UiError` → chuỗi (xem `error-handling.md`).
- Tham số dùng `%1$s`, `%2$d`; số nhiều dùng `<plurals>` + `pluralStringResource`. **Không** ghép chuỗi bằng `+`.
- Câu chữ lấy nguyên văn từ thiết kế. Chuỗi chưa có trong thiết kế thì thêm comment `<!-- [mới] chờ duyệt -->` trong `strings.xml` và liệt kê trong PR.
- Chuỗi của công cụ debug viết thẳng trong code (chỉ dùng khi phát triển).

## 2. Đọc chuỗi trong Compose

- Trong composable: `stringResource`, `pluralStringResource`.
- Cần `getString` trong lambda (callback, mô tả nội dung trong `LazyColumn`): lấy `val resources = LocalResources.current` **bên ngoài** lambda.
- **Không dùng `LocalContext.current.resources`.** Khi đổi ngôn ngữ mà Activity không bị tạo lại (mục 4), `LocalResources` tự cập nhật còn `LocalContext.current.resources` thì không, nên chữ cũ nằm lại.
- **Không cache** chuỗi hay `Resources` trong ViewModel, `object`, `companion`, hay `remember` không có khóa là cấu hình.

## 3. Định dạng số, ngày, giờ, dung lượng

- Theo **ngôn ngữ đang dùng của app**, không cố định một locale. Bản tiếng Việt ra `12.480`, `4,9 MB`, `23/05/2026`; bản English ra `12,480`, `4.9 MB`, `5/23/2026`.
- Locale trong Compose: `LocalLocale.current.platformLocale`, bọc trong helper `pfxLocale()` ở `:core:ui`. **Không dùng `Locale.getDefault()`** (giá trị cấp tiến trình, không bảo đảm khớp ngôn ngữ app, không kích hoạt dựng lại) và không tự đọc `LocalConfiguration.locales`.
- Hàm không phải composable nhận locale qua tham số (`formatFileSize(bytes, locale)`). Formatter tạo trong `remember(locale) { ... }`.
- Logic ngày giờ ở domain bằng `kotlinx-datetime`; **định dạng** ở tầng UI. Không hardcode `dd/MM/yyyy`.
- Số đổi liên tục hoặc cần thẳng cột (đồng hồ, bộ đếm) dùng font đơn cách của thiết kế.

## 4. Đổi ngôn ngữ trong app

- Màn Cài đặt chọn "Theo hệ thống / Tiếng Việt / English" qua interface `AppLocaleController` (`domain.platform`); bản Android gọi `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"))`, "Theo hệ thống" là `LocaleListCompat.getEmptyLocaleList()`.
- `MainActivity` là `AppCompatActivity`. Khai báo `AppLocalesMetadataHolderService` với `autoStoreLocales = true` để Android 12 trở xuống nhớ lựa chọn.
- `res/xml/locales_config.xml` liệt kê ngôn ngữ hỗ trợ; `android:localeConfig` trong manifest.
- `androidResources { localeFilters += listOf("vi", "en") }` để chuỗi của thư viện không lẫn ngôn ngữ khác.
- **Tránh chớp màn hình:** thêm `locale|layoutDirection` vào `android:configChanges` của `MainActivity` (ghi comment lý do). Activity không bị tạo lại; Compose dựng lại phần đọc `LocalResources`/`LocalLocale`. Đây là lý do mục 2 và 3 bắt buộc.
- Máy dùng ngôn ngữ không hỗ trợ thì app hiện ngôn ngữ mặc định (`res/values`).
- Đăng xuất có đưa ngôn ngữ về "Theo hệ thống" không: quyết trong đặc tả.

## 5. Thêm một ngôn ngữ

1. Thêm `res/values-xx/strings.xml` ở **mọi** module có chuỗi.
2. Thêm `xx` vào `locales_config.xml` và `localeFilters`.
3. Thêm lựa chọn trong Cài đặt.
4. Kiểm: đổi ngôn ngữ khi đang ở màn sâu, mọi chữ đổi ngay (kể cả dialog, sheet, tiêu đề nhóm ngày), vị trí cuộn giữ nguyên.
