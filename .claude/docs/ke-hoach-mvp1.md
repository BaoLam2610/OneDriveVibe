# Kế hoạch triển khai MVP1 (Android)

- **Trạng thái:** đã duyệt 2026-10-02
- **Tiến độ thực tế:** xem [tien-do-mvp1.md](tien-do-mvp1.md) (file này chỉ đổi khi đổi kế hoạch)
- **Điểm xuất phát:** Foundations (`:core:designsystem`) đã xong; các module `:core:*` khác đã tạo nhưng còn rỗng; chưa có `:feature:*`

## 1. Nguyên tắc

- **Làm theo lát cắt dọc, không theo thứ tự ADR.** ADR là các quyết định đã chốt, không phải lộ trình. Mỗi lát là một luồng chạy được từ màn hình xuống Graph, áp dụng những ADR nó cần.
- **Rủi ro cao làm trước:** mã hóa và token (lát 1–2), delta sync (lát 3), rồi mới tới các màn xem.
- **Kiểm tra bằng tay** (ADR-0009): mỗi lát kết thúc bằng danh sách kiểm tay trên thiết bị, không có test tự động.
- **Người dùng tự build, commit và push.** Cuối mỗi lát Claude chạy `/ecc:kotlin-review` (thêm review bảo mật cho lát 1–2) và gợi ý tên commit.
- Module `:feature:*` tạo khi bắt đầu lát dùng tới nó (ADR-0010).

ADR đã xong từ trước: 0001 và 0010 (cấu trúc module), 0009 (không làm gì).

## 2. Các lát

| Lát | Nội dung | ADR | Module chính |
|---|---|---|---|
| 0 | Bộ khung app, chưa có tính năng | 0002, 0003, 0004, 0011 | common, androidApp |
| 1 | Kết nối → Danh sách thư mục gốc (luồng xuyên suốt đầu tiên) | 0005, 0006, một phần 0008 | network, security, data, domain, feature:auth, feature:browser |
| D | Công cụ debug: nút bọ nổi, màn Debug (log API, log local, lưu trữ), Kermit, retry bằng `HttpRequestRetry` | 0012 | tools:debug, common, network |
| 2 | Mã PIN và màn Khóa | phần còn lại của 0008 | security, feature:auth |
| 3 | Đồng bộ delta và offline | 0007 | database, data, feature:browser |
| 4 | Thumbnail, bộ nhớ đệm, tab Thư viện | | data, feature:library |
| 5 | Xem ảnh | | feature:imageviewer |
| 6 | Xem video (6a: phát và điều khiển cơ bản; 6b: phần còn lại) | | feature:player |
| 7 | Xem PDF | | feature:pdfviewer |
| 8 | Xem tiếp / Đọc tiếp | | data, feature:browser, player, pdfviewer |
| 9 | Cài đặt đầy đủ | | feature:settings |

### Lát 0: Bộ khung app

- Khai báo thư viện: Koin, Ktor, kotlinx.serialization, Room KMP, Navigation 3, coroutines, DataStore.
- `:core:common`: `AppError`, `Result`, dispatcher.
- `BaseMviViewModel` (`State`, `Intent`, `Effect`).
- Khởi động Koin; `NavDisplay` với các route rỗng.
- Đa ngôn ngữ: `locales_config`, `res/values` (VI), `res/values-en` (EN), `AppCompatDelegate.setApplicationLocales()`.
- Tắt sao lưu tự động (CH-04).
- Tạo `.claude/docs/tech-stack.md` ghi lại các lựa chọn ở mục 3 và quy ước nhỏ (§9).
- **Kiểm tay:** app mở vào một màn trống; đổi ngôn ngữ được.

### Lát 1: Kết nối → Danh sách thư mục gốc

- `:core:network`: Ktor gọi Graph, lấy token Client Credentials, token chỉ trong bộ nhớ; TK-01, TK-02, TK-03; log che header (CH-06).
- `:core:security`: mã hóa config AES-GCM bằng khóa Keystore (nhánh bảo mật TẮT).
- `:feature:auth`: màn Kết nối (KN-01 → KN-13, bảng lỗi theo mã, `FLAG_SECURE`): kết nối xong lưu config ngay ở chế độ thiết bị rồi hiện hộp thoại hỏi thiết lập PIN (KN-13); "Để sau" vào Danh sách. Màn Thiết lập bảo mật chỉ là khung (Back về hộp thoại, BM-08); phần đặt PIN làm ở Lát 2.
- `:feature:browser`: tab Thư mục gọi thẳng API (TM-07), vào thư mục con, Breadcrumb, Back.
- Điều hướng khởi động theo luồng tổng thể (mục 2 đặc tả): chưa có config → Kết nối; có config → Danh sách.
- **Kiểm tay:** nhập 4 trường đúng/sai (từng mã lỗi); thấy cây thư mục thật; tắt rồi mở lại app vào thẳng Danh sách.

### Lát D: Công cụ debug (chen giữa, không đổi thứ tự các lát)

- Thêm sau Lát 1 theo yêu cầu: kiểm tay từ Lát 1 trở đi cần nhìn request/log ngay trên máy. ADR-0012.
- `:tools:debug` (chỉ bản debug): nút bọ nổi kéo thả → `DebugActivity` (không FLAG_SECURE) với 4 tab: Log API (bấm vào mở màn chi tiết: header, request body, response, JSON đẹp có +/-, sao chép, tìm kiếm có đếm), Log local (có tìm kiếm), Lưu trữ (SharedPreferences, DataStore, Room chỉ đọc), Khác (FLAG_SECURE toàn app: theo thiết kế/luôn bật/luôn tắt; công tắc che log API; mở Foundations gallery). Nút bọ luôn nằm trên cùng kể cả trên Dialog/BottomSheet. Log API đầy đủ không che (ADR-0013).
- Kermit trong `:core:common`; `HttpTrafficRecorder` trong `:core:network` (làm sạch trước khi ghi; không ghi body endpoint token).
- Thay hàm `withRetry` tự viết bằng plugin `HttpRequestRetry` của Ktor.
- Chỉ còn MỘT icon launcher: Foundations gallery bỏ LAUNCHER, mở từ tab "Khác" của màn Debug.
- **Kiểm tay:** nút bọ hiện trên mọi màn và kéo được; tab API thấy request Graph (không có Authorization/token); tab Log thấy log kết nối; tab Lưu trữ liệt kê prefs; bản release không có nút bọ.

### Lát 2: Mã PIN và màn Khóa

- Argon2id + Keystore, không lưu PIN hay hash (CH-01, CH-02, CH-07).
- Đặt PIN: nhập 2 lần, chặn PIN dễ đoán (BM-01 → BM-08); mã hóa lại config đã lưu ở chế độ thiết bị bằng khóa dẫn xuất từ PIN (BM-04, ADR-0008).
- Màn Khóa: KH-01 → KH-06 (chờ tăng gấp đôi, vẫn tính khi tắt app; Quên PIN; xóa dữ liệu sau 10 lần nếu bật).
- Tự khóa khi rời app; khóa thì xóa token và config đã giải mã khỏi bộ nhớ (CH-03).
- Sinh trắc học.
- **Kiểm tay:** sai PIN 5 lần, tắt app rồi mở lại; khóa khi app xuống nền; Quên PIN.

### Lát 3: Đồng bộ delta và offline

- `:core:database`: Room (`BundledSQLiteDriver`, `version = 1`), bảng item, trạng thái đồng bộ.
- Delta sync DB-01 → DB-05 (tiếp tục trang dở, xử lý 410).
- Tab Thư mục đọc từ Room; sắp xếp (TM-05), lưới/danh sách (TM-06), lọc loại tệp (TM-03, TM-04).
- Kéo để làm mới, tự đồng bộ khi mở nếu quá 15 phút (DS-04); thanh "Đang offline" (DS-05); gỡ tệp đã xóa (DS-06).
- Tìm kiếm (DS-03). TK-05, TK-06.
- Đăng ký `ConnectionResetter` xóa Room và dữ liệu đồng bộ khi ngắt kết nối (CD-05, KH-03, KH-06; xem `DisconnectUseCase`).
- **Kiểm tay:** drive lớn; tắt app giữa lúc quét; chế độ máy bay.

### Lát 4: Thumbnail, bộ nhớ đệm, tab Thư viện

- Cache thumbnail, giới hạn dung lượng, nhận diện theo `cTag` (BN-01 → BN-03).
- Đăng ký `ConnectionResetter` xóa cache khi ngắt kết nối.
- `:feature:library`: TV-01 → TV-06 (nhóm theo ngày, chip lọc, thanh cuộn nhanh, "Đang lập chỉ mục").

### Lát 5: Xem ảnh

- AN-01 → AN-07.

### Lát 6: Xem video

- 6a: phát stream, tự lấy link mới (VD-14), Play/Pause, tua, chạm đúp tua, tốc độ, Back, giữ màn sáng, lỗi codec và mất mạng.
- 6b: vuốt độ sáng/âm lượng, khung hình, xoay, khóa thao tác, trước/sau và chế độ phát, Phát lại, zoom, bảng thông tin.

### Lát 7: Xem PDF

- PD-01 → PD-08 (tải có tiến trình và tải tiếp phần dở, cuộn dọc hoặc lật ngang, nhớ trang).

### Lát 8: Xem tiếp / Đọc tiếp

- DS-02, VD-12, PD-05.
- Đăng ký `ConnectionResetter` xóa lịch sử xem khi ngắt kết nối.

### Lát 9: Cài đặt đầy đủ

- CD-01 → CD-10; cập nhật Client Secret; ngắt kết nối và xóa sạch; giới hạn cache; nhắc secret sắp hết hạn.
- Nối `SecurityRepositoryImpl.wipeAfterFailures` với cài đặt CD-08 (hiện tắt). Đăng ký `ConnectionResetter` xóa cài đặt. Nút Ngắt kết nối gọi `DisconnectUseCase`.

## 3. Thư viện chưa có trong ADR (đã duyệt)

| Việc | Chọn | Ghi chú |
|---|---|---|
| Argon2id | `argon2kt` | Native, nhanh. Phương án dự phòng: BouncyCastle (thuần Java, chậm hơn) |
| Lưu cài đặt | DataStore Preferences | |
| Tải ảnh, thumbnail | Coil 3 | |
| Phát video | Media3 ExoPlayer + `SimpleCache` | |
| PDF | `PdfRenderer` của Android | |
| Đồng bộ chạy nền | Coroutine trong app | Thêm WorkManager khi cần đồng bộ lúc app đã đóng |
| Log local | Kermit 2.2.0 | Bản debug đẩy vào màn Debug; release không writer (ADR-0012) |
| Retry mạng | Plugin `HttpRequestRetry` của Ktor | Thay hàm `withRetry` tự viết (ADR-0012) |

## 4. Bỏ so với quy trình `/orch-build-mvp`

- Bộ chấm điểm tự động (GAN harness): cần build và chạy app. Thay bằng danh sách kiểm tay.
- Commit tự động: người dùng tự commit; Claude chỉ gợi ý tên.
