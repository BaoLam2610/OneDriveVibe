# Bảo mật

Áp dụng cho mọi dự án. Dự án có yêu cầu riêng (PIN, sinh trắc học, mã hóa dữ liệu) thì ghi trong đặc tả và ADR.

## 1. Bí mật và log

- **Không bao giờ** log: token (access, refresh, id), mật khẩu, PIN, OTP, secret, header `Authorization`/`Cookie`, URL có chữ ký (`sig=`, `token=`), giá trị ô nhập, dữ liệu cá nhân, `e.message`/`e.toString()` của lỗi mạng.
- Log chỉ ghi kết quả, mã lỗi, tên lớp ngoại lệ, id rút gọn (vài ký tự cuối).
- Không cài plugin `Logging` của Ktor. Lưu lượng API đầy đủ chỉ có ở bản debug, chỉ trong RAM, qua `HttpTrafficRecorder`.
- Bản release gỡ mọi writer của Kermit (`DebugTools` bản release).
- `toString()` của lớp chứa bí mật được ghi đè để không in giá trị (`AppError.Unknown`, lớp credentials).

## 2. Lưu trữ

| Dữ liệu | Lưu ở |
|---|---|
| Access token | Chỉ trong RAM |
| Refresh token, thông tin đăng nhập | Mã hóa bằng khóa Android Keystore (AES-GCM, khóa không xuất được), hoặc Tink; tệp trong `filesDir` |
| Cài đặt không nhạy cảm | DataStore |
| Dữ liệu nghiệp vụ | Room (không mã hóa, trừ khi đặc tả yêu cầu, khi đó cần ADR) |

- **Không** dùng `EncryptedSharedPreferences` (deprecated).
- Không lưu PIN hay hash của PIN nếu dùng PIN để mở khóa: dẫn xuất khóa từ PIN (Argon2id) và để "giải mã được hay không" là phép kiểm PIN. Cần ADR riêng.
- Bộ đếm sai và thời gian chờ khóa nhập lưu bền (qua tắt/mở app) và không reset được bằng cách chỉnh giờ máy (đồng hồ có biết lần khởi động máy).
- Tên tệp bí mật, alias khóa Keystore: file Constants, "không được đổi giá trị".

## 3. Sao lưu

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="false"
    android:dataExtractionRules="@xml/data_extraction_rules" ...>
```

`data_extraction_rules.xml` loại trừ mọi miền cho cả `cloud-backup` và `device-transfer`: từ Android 12, `allowBackup` bị bỏ qua khi chuyển máy. Dự án muốn cho sao lưu thì viết ADR và liệt kê rõ miền được sao lưu.

## 4. Mạng

- Chỉ HTTPS. `network_security_config` không có `cleartextTrafficPermitted`; bản debug cho phép CA người dùng chỉ khi cần proxy gỡ lỗi (chỉ ở `src/debug`).
- Client mang bearer đặt `followRedirects = false`; redirect tới URL ký thì kiểm `https` rồi tải bằng client **không** bearer.
- Certificate pinning: chỉ khi đặc tả yêu cầu; có kế hoạch xoay vòng pin.

## 5. Chặn chụp màn hình (`FLAG_SECURE`)

- Màn nhạy cảm (đăng nhập, nhập PIN/OTP, hiện secret, thông tin thanh toán) đặt `FLAG_SECURE` và ẩn nội dung ở danh sách app gần đây (`setRecentsScreenshotEnabled(false)` trên Android 13+).
- `FLAG_SECURE` là của **từng Window**. Chính sách chặn là trạng thái **cấp tiến trình** (`PfxSecureWindowPolicy`):
  - mọi Activity đăng ký Window qua `ActivityLifecycleCallbacks` (`track(window)`), kể cả Activity của công cụ debug;
  - cờ của mỗi Window = "bảo vệ toàn app" (cài đặt người dùng, nếu có) **hoặc** có màn nhạy cảm đang yêu cầu (đếm theo Window);
  - Dialog và BottomSheet là Window riêng: component designsystem tự áp cờ theo chính sách.
- Chưa đọc xong cài đặt thì coi như **bật**.
- Hành vi khác ở bản debug (cho chụp để báo lỗi) đi qua `DebugTools` và có ADR.

## 6. Dữ liệu nhạy cảm trong bộ nhớ

- ViewModel giữ PIN, mật khẩu, OTP, secret đang nhập phải **quan sát trạng thái khóa/đăng xuất** và tự xóa (đưa về bước đầu). Nếu không, app tự khóa giữa lúc nhập thì giá trị còn trong RAM.
- Khóa app (nếu có) xóa token và dữ liệu đã giải mã khỏi bộ nhớ, dừng tác vụ đang dùng chúng, xóa URL ký đã cache.
- Không đặt bí mật vào `SavedStateHandle` hay `rememberSaveable` (được ghi xuống đĩa khi process death).
- Clipboard chứa bí mật gắn `EXTRA_IS_SENSITIVE`.

## 7. Phân phối

- `exported="false"` cho mọi Activity/Service/Receiver không cần mở từ ngoài.
- R8 bật cho release; giữ quy tắc keep cho kotlinx.serialization và lớp được gọi bằng reflection.
- Không có khóa API, secret hay URL nội bộ trong mã nguồn hoặc `BuildConfig` của bản release; lấy từ CI secret hoặc `local.properties` (không commit).
