# ADR-0012: Công cụ debug trong app (module `:tools:debug`) và log bằng Kermit

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Kiểm thử là thủ công trên thiết bị (ADR-0009), nên cần nhìn được request Graph, log của app và dữ liệu local ngay trên máy mà không phải nối Logcat hay adb. Đồng thời CH-06 cấm ghi Client Secret, access token, header `Authorization` vào log, và Ktor `Logging` mặc định in cả những thứ đó. Cần một chỗ ghi log có kiểm soát, và chắc chắn không lọt vào bản release.

## Quyết định
1. **Module `:tools:debug`** (Android), gắn vào `:androidApp` bằng `debugImplementation`. Có `DebugActivity` (FLAG_SECURE) với 3 tab: **API** (request đã làm sạch), **Log** (log local), **Lưu trữ** (SharedPreferences, DataStore, SQLite/Room chỉ đọc, tên tệp kho bí mật). Mở từ nút nổi hình con bọ cánh cứng, kéo thả được, đặt chồng lên mọi màn.
2. **Cổng vào `DebugTools`** có hai bản cùng API: `androidApp/src/debug` (nối `:tools:debug`) và `androidApp/src/release` (rỗng). Code ở `src/main` chỉ biết `DebugTools`.
3. **Log local dùng Kermit** (`:core:common` xuất `api`). Bản debug cài `LogWriter` đẩy vào bộ đệm của màn Debug và Logcat; bản release gỡ mọi writer nên không ghi gì.
4. **Log API qua `HttpTrafficRecorder`** (interface trong `:core:network`), không qua plugin Ktor `Logging`. Chỉ bản debug có bản cài. `:core:network` tự làm sạch trước khi gọi recorder: bỏ `Authorization`, **không ghi body của endpoint token**, che `downloadUrl`/`tempauth`/`access_token`, cắt body ở 16 KB.
5. **Retry cấu hình bằng plugin `HttpRequestRetry` của Ktor** trong `createHttpClient` (lỗi mạng, timeout, `429`, `5xx`; tối đa 2 lần; `Retry-After` giới hạn 60 giây), thay cho hàm `withRetry` tự viết. `401` vẫn do `GraphApi` xử lý (lấy token mới, thử lại một lần, TK-02).

Chuỗi trong `:tools:debug` để trực tiếp (như gallery): đây là công cụ phát triển, không phải giao diện sản phẩm nên không theo ADR-0011. Màn Debug không theo MVI vì chỉ đọc bộ đệm/đĩa.

## Phương án đã cân nhắc
### Ktor `Logging` plugin + `sanitizeHeader`
- Ưu: sẵn có.
- Nhược: không kiểm soát được body request token (chứa `client_secret`); dễ bật nhầm ở release; khó đưa lên giao diện.
- Lý do không chọn: CH-06.
### Thư viện ngoài (Chucker, Flipper)
- Ưu: đầy đủ tính năng.
- Nhược: Chucker lưu body đầy đủ (kể cả token); không dùng được trong `commonMain`; phải tin plugin về chuyện che dữ liệu.
- Lý do không chọn: cần kiểm soát hoàn toàn việc làm sạch.
### Đặt code debug trong `androidApp/src/debug`
- Ưu: ít file build.
- Nhược: `androidApp` phình ra, khó gỡ.
- Lý do không chọn: module riêng tách bạch hơn.

## Hệ quả
### Tích cực
- Gỡ lỗi trên máy thật không cần Logcat; request lỗi nhìn thấy ngay mã Graph/AADSTS.
- Release không chứa module debug, không có writer log, không có recorder: CH-06 giữ nguyên.
### Tiêu cực
- Hai bản `DebugTools` phải giữ cùng API.
- Recorder chỉ thấy kết quả cuối của mỗi request (sau retry), không thấy từng lần thử.
### Rủi ro
- Quên làm sạch khi thêm endpoint mới (vd. tải tệp có `tempauth`). Giảm thiểu: `includeBody` mặc định là quyết định rõ ràng ở nơi gọi, và `sanitizeUrl`/`sanitizeBody` che các khóa đã biết; endpoint mới phải được xem lại trong review bảo mật.
- Màn Debug đọc được UPN và đường dẫn tệp: chỉ có ở bản debug và đặt FLAG_SECURE.
