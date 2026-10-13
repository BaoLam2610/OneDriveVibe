# Architecture Decision Records — OneDriveVibe

Mỗi ADR ghi **một quyết định kiến trúc**: bối cảnh, quyết định, phương án đã loại và hệ quả. Mục đích là sau này đọc lại biết **vì sao** chọn như vậy.

**Một thư mục dùng chung cho cả dự án** (Android và iOS ở MVP2 cùng một chuỗi số, không tách theo nền tảng). Quyết định riêng cho iOS sẽ là ADR tiếp theo trong cùng thư mục, ghi rõ nền tảng trong tiêu đề.

> Vị trí: `.claude/adr/` (giữ nguyên ở đây, không chuyển sang `docs/adr/`).

| ADR | Quyết định | Trạng thái | Ngày |
|---|---|---|---|
| [0001](0001-android-truoc-san-sang-kmp.md) | Android trước, sẵn sàng KMP: module dùng chung chỉ khai báo `androidTarget()` ở MVP1 | accepted | 2026-10-02 |
| [0002](0002-clean-architecture-mvi.md) | Clean Architecture + MVI (State/Intent/Effect); chỉ tạo UseCase khi có logic thật | accepted (phần UseCase thay bằng ADR-0016) | 2026-10-02 |
| [0003](0003-navigation-3.md) | Navigation 3 thay cho Navigation Compose 2.x | accepted | 2026-10-02 |
| [0004](0004-koin-thay-hilt.md) | Koin thay cho Hilt | accepted | 2026-10-02 |
| [0005](0005-xac-thuc-client-credentials.md) | Xác thực Client Credentials, chỉ OneDrive for Business, nhập 4 trường | accepted | 2026-10-02 |
| [0006](0006-graph-rest-qua-ktor.md) | Gọi Graph REST trực tiếp qua Ktor, không dùng Graph SDK hay MSAL | accepted | 2026-10-02 |
| [0007](0007-room-kmp-offline-first.md) | Room KMP, offline-first: Room là nguồn dữ liệu duy nhất cho UI, đồng bộ delta | accepted | 2026-10-02 |
| [0008](0008-bao-mat-config-pin-argon2id.md) | Bảo mật config: khóa dẫn xuất từ PIN (Argon2id) + Keystore, không lưu PIN hay hash | accepted | 2026-10-02 |
| [0009](0009-khong-viet-test-tu-dong.md) | Không viết test tự động (dự án cá nhân) | accepted | 2026-10-02 |
| [0010](0010-chia-module.md) | Chia module: tách nhỏ phần core, module feature tạo khi bắt đầu làm feature đó | accepted | 2026-10-02 |
| [0011](0011-da-ngon-ngu-vi-en.md) | Đa ngôn ngữ VI + EN qua Android `res/`, domain không chứa chuỗi hiển thị | accepted | 2026-10-02 |
| [0012](0012-cong-cu-debug-va-kermit.md) | Công cụ debug trong app (`:tools:debug`, chỉ bản debug), log bằng Kermit, retry bằng `HttpRequestRetry` | accepted | 2026-10-02 |
| [0013](0013-log-api-debug-day-du-khong-che.md) | Log API ở bản debug hiển thị đầy đủ (không che), che là tùy chọn; thay thế một phần ADR-0012 | accepted | 2026-10-02 |
| [0014](0014-phong-bi-pin-sinh-trac-hoc-tu-khoa.md) | Phong bì PIN hai lớp, bộ đếm sai bền, sinh trắc học bọc khóa dẫn xuất, tự khóa; bổ sung ADR-0008 | accepted | 2026-10-03 |
| [0015](0015-phien-ban-schema-room.md) | Tăng version Room mỗi lần đổi schema; thay phần "giữ `version = 1`" của ADR-0007 | accepted | 2026-10-04 |
| [0016](0016-usecase-bat-buoc-giua-feature-va-data.md) | UseCase bắt buộc giữa feature và data; thay phần "chỉ tạo UseCase khi có logic thật" của ADR-0002 | accepted | 2026-10-06 |
| [0017](0017-giu-log-debug-khi-khoa-app.md) | Giữ log debug khi app khóa; thay phần "xóa log API khi khóa" của ADR-0013/0014 | accepted | 2026-10-07 |
| [0018](0018-decoder-ffmpeg-du-phong-cho-video-10-bit.md) | Decoder FFmpeg (NextLib, GPL-3.0) làm renderer dự phòng cho video máy không giải mã được, vd HEVC 10-bit | accepted | 2026-10-07 |
| [0019](0019-tu-khoa-co-do-tre.md) | Tự khóa khi rời app có độ trễ do người dùng chọn (mặc định ban đầu 1 phút; đổi thành Ngay lập tức 2026-10-13 theo đặc tả mục 3.8, không viết ADR mới) | accepted | 2026-10-07 |
| [0020](0020-bo-ghi-de-flag-secure-rieng-cua-debug.md) | Bỏ ghi đè FLAG_SECURE riêng của công cụ debug, màn Debug theo cài đặt Bảo vệ màn hình | accepted | 2026-10-07 |
| [0021](0021-tran-bo-nho-dem-tuy-chinh-chia-theo-loai.md) | Trần bộ nhớ đệm tùy chỉnh 1 đến 10 GB, chia theo loại, đọc lúc chạy, mỗi kho là CacheStore | accepted | 2026-10-07 |
| [0022](0022-man-cai-dat-theo-bao-ve-man-hinh.md) | Màn Cài đặt chụp được khi tắt "Bảo vệ màn hình" (thay phần Cài đặt luôn chặn của CH-05) | accepted | 2026-10-08 |
| [0023](0023-thanh-dieu-huong-day-nhieu-ngan-xep.md) | Thanh điều hướng đáy (Thư mục, Thư viện, Short, Cài đặt), mỗi tab giữ trạng thái riêng; Cài đặt thành tab; bổ sung ADR-0003 | accepted | 2026-10-10 |
| [0024](0024-tai-truoc-video-tab-short.md) | Tab Short dùng một ExoPlayer và `DefaultPreloadManager` tải trước video ±1, chung cache video | accepted (mục 1 thay bằng ADR-0026) | 2026-10-10 |
| [0025](0025-module-core-media-dung-chung-cho-player-va-short.md) | Module `:core:media` dùng chung nền phát video (cache, nguồn dữ liệu, decoder, ExoPlayerFactory) cho Xem video và Short | accepted | 2026-10-10 |
| [0026](0026-tai-truoc-video-short-bang-cachewriter.md) | Tải trước video của tab Short bằng `CacheWriter` vào cache video chung thay cho `DefaultPreloadManager`; khóa app thì xóa link ký và dừng player (thay mục 1 ADR-0024) | accepted | 2026-10-11 |
| [0027](0027-poster-khung-hinh-dau-cho-tab-short.md) | Poster của tab Short là khung hình đầu thật của video kế cận (`MediaMetadataRetriever`, một lần một video, tối đa 4 ảnh trong RAM, xóa khi khóa), không dùng thumbnail OneDrive hay hai player; bổ sung ADR-0024/0026 | accepted | 2026-10-11 |
| [0028](0028-tai-truoc-video-ke-tiep-vao-ram-bang-defaultpreloadmanager.md) | Chuẩn bị sẵn video +1 của tab Short trong RAM bằng `DefaultPreloadManager` (stage `SPECIFIED_RANGE_LOADED`, không `setCache`), giữ `CacheWriter` cho cache đĩa ±1; bổ sung ADR-0026, giữ lại nếu số đo cải thiện (trung vị 301ms so với 963ms) | accepted | 2026-10-12 |
| [0029](0029-link-phat-dung-chung-mot-yeu-cau-cho-mot-id.md) | Link phát của tab Short dùng chung (`SharedStreamUrls`, cũng là `ConnectionResetter`) và chỉ một yêu cầu cho một id (single-flight, `epoch` chống ghi link sau khi khóa app); màn Xem video giữ provider riêng; thay mục 3 của ADR-0026 và phần provider riêng của ADR-0025 cho đường Short | accepted | 2026-10-12 |
| [0030](0030-nhieu-tai-khoan-chung-token.md) | Nhiều tài khoản cùng tenant: một token dùng chung, dữ liệu theo tài khoản (khóa `accountId` = drive id), phạm vi xem là một tài khoản hoặc Tất cả, đồng bộ mọi tài khoản lần lượt; bổ sung ADR-0005 và ADR-0007 | accepted | 2026-10-12 |
| [0031](0031-man-nhay-cam-theo-bao-ve-man-hinh-o-ban-debug.md) | Chỉ bản debug: các màn nhạy cảm (Kết nối, Khóa, nhập PIN, Thiết lập bảo mật, Cập nhật secret, Thêm tài khoản) theo công tắc "Bảo vệ màn hình" thay vì luôn chặn chụp (`ODVSecureWindowPolicy.sensitiveScreensFollowAppWide`); release giữ nguyên ADR-0022; bổ sung ADR-0022 và ADR-0020 | accepted | 2026-10-13 |

## Quy ước
- Đánh số tăng dần, không dùng lại số. Tên file: `NNNN-tieu-de-khong-dau.md`.
- Trạng thái: `proposed` → `accepted` → `deprecated` hoặc `superseded by ADR-NNNN`.
- Đổi quyết định thì **viết ADR mới** và đánh dấu ADR cũ là superseded, không sửa lịch sử.
- Chỉ ghi quyết định có ảnh hưởng kiến trúc; quy ước nhỏ (tắt dynamic color, che header log...) nằm ở `tech-stack.md` §9.
- Mẫu trống: [template.md](template.md).