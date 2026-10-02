# ADR-0007: Room KMP, offline-first

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Drive có thể rất lớn. App cần duyệt thư mục, dựng tab Thư viện theo ngày, tìm kiếm offline và mở nhanh (DB-05). Graph có giới hạn tốc độ (429/503).

## Quyết định
**Room KMP** (`BundledSQLiteDriver`) là nguồn dữ liệu duy nhất cho UI. Danh sách tệp được đồng bộ về Room bằng delta query (DB-01 → DB-05) trong luồng riêng; repository `observe*()` đọc từ Room. Ngoại lệ duy nhất: TM-07 (duyệt trực tiếp qua API khi đồng bộ lần đầu chưa xong). Schema giữ ở `version = 1` đến khi phát hành.

## Phương án đã cân nhắc
### Gọi API mỗi lần mở màn (không lưu cục bộ)
- Ưu: đơn giản, luôn mới.
- Nhược: chậm, không offline, không làm được Thư viện và tìm kiếm trên toàn drive, dễ bị throttle.
- Lý do không chọn: không đáp ứng đặc tả.
### SQLDelight
- Ưu: KMP từ gốc.
- Nhược: thêm một công nghệ khi Room KMP đã đủ và quen Android.
- Lý do không chọn: Room KMP đủ dùng.
### Repository gọi remote trước rồi ghi local (mẫu trong skill)
- Ưu: đơn giản.
- Nhược: UI phụ thuộc mạng.
- Lý do không chọn: trái DB-05.

## Hệ quả
### Tích cực
- Mở nhanh, dùng được khi offline, tìm kiếm không tốn API.
### Tiêu cực
- Quét lần đầu mất thời gian và dung lượng; phải xử lý `410`, ngắt giữa chừng (DB-03, DB-04).
### Rủi ro
- Dữ liệu cục bộ lệch với OneDrive. Giảm thiểu bằng đồng bộ khi mở app nếu đã quá 15 phút và kéo để làm mới (DS-04).
