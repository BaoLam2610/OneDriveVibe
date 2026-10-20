# ADR-0008: Room, version và schema

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** `conventions/data-layer.md` mục 3, `tech-stack.md` mục 3

## Bối cảnh

Dự án trước ghi luật "giữ `version = 1` cho tới khi phát hành". Đổi cột mà không tăng version thì Room báo lỗi kiểm tra hash và crash ("Room cannot verify the data integrity"), phải viết ADR mới thay luật đó. Dự án trước còn gặp CSDL hỏng khi một công cụ mở tệp của Room bằng bản SQLite khác trong cùng tiến trình.

## Quyết định

- **Mọi thay đổi schema tăng `version`** và ghi một dòng vào "Lịch sử version" (KDoc của lớp database).
- `exportSchema = true`, thư mục `schemas/` commit vào git.
- Trước bản phát hành đầu: `fallbackToDestructiveMigration(dropAllTables = true)` được phép.
- Từ bản phát hành đầu: bắt buộc `Migration`/`@AutoMigration` kèm test `MigrationTestHelper`; bỏ fallback destructive.
- Driver: mặc định SQLite của hệ thống. Dùng `BundledSQLiteDriver` cần ADR riêng và khi đó không mở tệp CSDL bằng `android.database.sqlite`.

## Phương án đã cân nhắc

### Giữ `version = 1` trước khi phát hành
- Lý do không chọn: crash hash khi đổi schema; đã gặp.

### SQLDelight
- Lý do không chọn: Room đủ dùng, có Paging, đội quen.

## Hệ quả

### Tích cực
- Đổi schema luôn hiện rõ trong PR (schema JSON đổi).
- Người cài bản dev mới không phải xóa dữ liệu tay.

### Tiêu cực
- Trước phát hành, dữ liệu dev bị xóa mỗi lần tăng version (chấp nhận).

### Rủi ro
- Quên tăng version: checklist PR và review; CI có thể so schema JSON với bản ở `main`.

## Cách xác nhận

- Cài đè bản mới lên bản cũ (không gỡ app) sau mỗi thay đổi schema: app mở được.
- Từ bản phát hành đầu: test migration pass.
