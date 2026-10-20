# ADR-0005: Ktor Client cho mạng

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** `conventions/data-layer.md` mục 2, ADR-0011

## Bối cảnh

App gọi REST API qua HTTPS, cần retry có kiểm soát, làm mới token khi `401`, timeout, và một nơi duy nhất ghi lưu lượng cho công cụ debug mà không lọt token vào log.

## Quyết định

Ktor Client với OkHttp engine, `ContentNegotiation` + kotlinx.serialization, `HttpTimeout`, `HttpRequestRetry` (lỗi mạng, timeout, `429`, `5xx`; không `4xx` khác), `Auth` bearer (làm mới token single-flight, thử lại một lần). `expectSuccess = false`, `followRedirects = false`. **Không cài plugin `Logging`.** Lưu lượng cho màn Debug qua `HttpTrafficRecorder` (interface ở `:core:network`, chỉ bản debug cài).

## Phương án đã cân nhắc

### Retrofit + OkHttp
- Ưu: phổ biến nhất trên Android, interface khai báo gọn.
- Nhược: hai thư viện (Retrofit + converter); retry và auth tự viết bằng interceptor.
- Lý do không chọn: đội chọn Ktor; plugin có sẵn cho retry, timeout, auth; dự án trước đã chạy ổn.

## Hệ quả

### Tích cực
- Retry, timeout, auth cấu hình ở một chỗ (`createApiClient`).
- Lỗi đổi sang `AppError` ở một helper (`safeGet`/`safePost`).

### Tiêu cực
- Không có interface khai báo kiểu Retrofit; mỗi endpoint là một hàm viết tay (ngắn).

### Rủi ro
- Plugin `Logging` in header `Authorization`: cấm, kiểm trong review.
- Theo redirect sẽ gửi bearer sang máy chủ khác: `followRedirects = false`, tải URL ký bằng client không bearer.

## Cách xác nhận

- Tab API của màn Debug thấy đủ request; bản release không có recorder.
- Token hết hạn → `401` → đúng một lần làm mới token giữa hai request (xem ở tab API).
