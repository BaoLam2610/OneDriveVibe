# ADR-0015: Tăng version Room mỗi lần đổi schema

**Ngày**: 2026-10-04
**Trạng thái**: accepted (thay phần "giữ `version = 1` đến khi phát hành" của ADR-0007)
**Người quyết định**: LamBao

## Bối cảnh
ADR-0007 giữ schema Room ở `version = 1` đến khi phát hành, kèm giả định rằng bản đã cài tự tạo lại DB khi đổi cột. Giả định đó sai: Room kiểm tra bằng **mã băm schema**, không phải số version. Đổi cột mà version vẫn là 1 thì app crash lúc mở DB với `IllegalStateException: Room cannot verify the data integrity`, và `fallbackToDestructiveMigration` không chạy vì nó chỉ kích hoạt khi version thay đổi. Lỗi này đã xảy ra ở Lát 6 khi thêm 5 cột facet video.

## Quyết định
1. **Mỗi lần đổi schema (thêm, bỏ, đổi cột, bảng, index) phải tăng `version`** trong `OdvDatabase`, và ghi một dòng vào "Lịch sử version" trong KDoc của lớp đó.
2. **Trước khi phát hành:** tăng version là đủ. Bản Android giữ `fallbackToDestructiveMigration(dropAllTables = true)`, nên DB cũ bị xóa và đồng bộ lại từ OneDrive (dữ liệu chỉ là bản sao, DB-01).
3. **Từ lần phát hành đầu tiên:** không dùng destructive nữa; mỗi lần tăng version phải kèm `Migration` (hoặc `AutoMigration`), và bỏ `fallbackToDestructiveMigration`.
4. Schema xuất ra `core/database/schemas/` (`exportSchema = true`) được commit để đối chiếu và viết migration sau này.

## Phương án đã cân nhắc
### Giữ `version = 1` và bắt người dùng xóa dữ liệu app mỗi lần đổi
- Ưu: không phải nhớ tăng số.
- Nhược: dễ quên và crash khi mở app (đã xảy ra); không áp dụng được cho người dùng thật.
- Lý do không chọn: lỗi đã gặp, và đây là cách làm ngược với cách Room hoạt động.
### Viết Migration cho mọi thay đổi ngay từ bây giờ
- Ưu: sẵn sàng cho phát hành, giữ được dữ liệu.
- Nhược: tốn công cho dữ liệu chỉ là bản sao, quét lại mất vài giây.
- Lý do không chọn trước phát hành: không đáng; sẽ làm từ bản phát hành đầu tiên.

## Hệ quả
### Tích cực
- Đổi schema lúc dev không crash; có quy tắc rõ để nhớ.
### Tiêu cực
- Mỗi lần tăng version lúc dev, người dùng chờ quét lại toàn bộ drive một lần.
### Rủi ro
- Quên tăng version vẫn crash. Giảm thiểu bằng quy tắc trong `CLAUDE.md` và nhắc ở mục Cách làm việc; nhớ kiểm tra khi sửa `Entities.kt`.
