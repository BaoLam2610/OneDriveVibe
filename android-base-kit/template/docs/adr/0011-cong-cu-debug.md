# ADR-0011: Công cụ debug trong app

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0005, `docs/debug-tools.md`

## Bối cảnh

Kiểm thử trên máy thật cần thấy ngay request/response, log, dữ liệu đã lưu, mà không cắm cáp hay mở Android Studio. Ở dự án trước, công cụ này được dựng ngay sau lát đầu và gần như mọi lỗi khó đều được chẩn đoán bằng nó.

## Quyết định

- Module `:tools:debug` (`debugImplementation`): nút bọ nổi (cả trên Dialog/BottomSheet), `DebugActivity` bốn tab (API, Log, Lưu trữ, Khác), trình duyệt bảng CSDL, design gallery.
- Cổng `DebugTools` ở `:app/src/debug` và `:app/src/release` cùng API; `src/main` chỉ gọi cổng, không import `:tools:debug`.
- Log API **đầy đủ, không che** mặc định (có công tắc che), chỉ trong RAM, qua `HttpTrafficRecorder`. Log local qua writer Kermit vào bộ đệm vòng.
- Bản release: không module, không recorder, `Logger.setLogWriters(emptyList())`.
- Mọi khác biệt hành vi debug/release đi qua `DebugTools` và ghi ở đây hoặc ADR mới.

## Phương án đã cân nhắc

### Chỉ dùng Logcat + Android Studio
- Lý do không chọn: người kiểm (QA, PO) không có môi trường dev; không xem được dữ liệu đã lưu dễ dàng.

### Thư viện có sẵn (Chucker, Flipper)
- Ưu: không tự viết phần log mạng.
- Nhược: Chucker gắn vào OkHttp interceptor (Ktor dùng OkHttp engine nên vẫn được nhưng phải cấu hình riêng), không có log Kermit, không có trình duyệt DataStore/Room của app, không có hook xóa dữ liệu/A-B; Flipper đã ngừng phát triển cho Android.
- Lý do không chọn: cần một màn gộp; có thể thêm Chucker song song nếu muốn (ghi ADR).

## Hệ quả

### Tích cực
- Báo lỗi kèm log đầy đủ chụp từ máy; A/B đo được trên máy thật.

### Tiêu cực
- Bản debug chứa secret trong log API: ai cầm máy bản debug cũng đọc được. Chỉ dùng tài khoản/secret dành cho phát triển trên bản debug; không chia sẻ ảnh chụp màn Debug ra ngoài.

### Rủi ro
- Lọt vào release: kiểm `releaseRuntimeClasspath` không có `:tools:debug` trước mỗi lần phát hành.
- Công cụ đọc CSDL làm hỏng CSDL: chỉ đọc qua Room (`debug-tools.md`).

## Cách xác nhận

- Checklist kiểm tay ở `debug-tools.md` mục 7.
