# ADR-0006: Gọi Microsoft Graph REST trực tiếp qua Ktor

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
App chỉ dùng một nhóm endpoint của Graph (drive, children, delta, thumbnails, download URL) và lấy token bằng Client Credentials. Phần network cần dùng chung được cho KMP.

## Quyết định
Gọi REST trực tiếp bằng **Ktor Client 3.x** (OkHttp engine trên Android, Darwin trên iOS), JSON bằng kotlinx.serialization. Tự viết DTO, plugin auth, retry và phân trang `@odata.nextLink`.

## Phương án đã cân nhắc
### Microsoft Graph SDK for Java/Kotlin
- Ưu: sẵn model và phân trang.
- Nhược: chỉ JVM, không dùng được cho iOS; nặng so với số endpoint cần dùng.
- Lý do không chọn: không KMP.
### MSAL
- Ưu: lo token và cache.
- Nhược: hướng tới đăng nhập tương tác; app-only chỉ cần một request lấy token.
- Lý do không chọn: ADR-0005.
### Retrofit + OkHttp
- Ưu: quen thuộc.
- Nhược: chỉ Android.
- Lý do không chọn: không KMP.

## Hệ quả
### Tích cực
- Một client dùng chung cho Android và iOS.
- Kiểm soát hoàn toàn header, log và retry.
### Tiêu cực
- Phải tự viết và tự bảo trì DTO, auth, throttling.
### Rủi ro
- Log mặc định của Ktor in cả header `Authorization`. Phải che bằng `sanitizeHeader` và không log body request lấy token (CH-06).
