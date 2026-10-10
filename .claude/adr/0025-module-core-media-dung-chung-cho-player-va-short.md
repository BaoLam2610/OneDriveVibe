# ADR-0025: Module `:core:media` dùng chung nền phát video cho Xem video và Short

**Ngày**: 2026-10-10
**Trạng thái**: accepted (bổ sung ADR-0010, ADR-0018, ADR-0024)
**Người quyết định**: LamBao

## Bối cảnh
Tab Short (Lát 8b, ADR-0024) cần cùng nền phát với màn Xem video: chuỗi nguồn dữ liệu `CacheDataSource` → `StreamDataSource` (link ký tự làm mới, VD-14) → HTTP, cache video chung có trần (ADR-0021), bộ chọn decoder có FFmpeg dự phòng (ADR-0018) và xử lý Dolby Vision profile 8. Toàn bộ phần này đang là `internal` trong `:feature:player`.

Quy tắc kiến trúc: `feature` chỉ phụ thuộc `domain`, không phụ thuộc feature khác (ADR-0010, ADR-0016). `:feature:shorts` không được kéo `:feature:player` vào. Nhân bản mã là không chấp nhận được vì hai bản sẽ lệch nhau (khóa cache khác nhau thì phần tải trước của Short không dùng lại được ở màn Xem video và ngược lại).

## Quyết định
1. Tạo module **`:core:media`** (convention `odv.kmp.library`, chỉ target Android ở MVP1) chứa phần nền phát **không dính giao diện**:
    - `VideoCache` (`SimpleCache`, `BudgetCacheEvictor`, `videoCacheKey`) vẫn là `ConnectionResetter` và `CacheStore` loại Video.
    - `StreamDataSource`, `StreamUrlProvider`, `streamUri` và các ngoại lệ của nó.
    - `VideoDecoders`, `DolbyVisionAsHevc` (decoder FFmpeg dự phòng, ADR-0018).
    - `ExoPlayerFactory` dựng `ExoPlayer` đã gắn đủ chuỗi nguồn dữ liệu, decoder và `LoadControl`; trả về `ExoPlayerHandle(player, decoders)`.
    - Hằng số liên quan và logger cùng thẻ `ODVPlayer` để tab Log của màn Debug vẫn lọc "Player" thấy toàn bộ đường đi (ADR-0012).
2. **Phụ thuộc:** `:core:media` → `:core:common`, `:core:domain` (cần `GetStreamUrlUseCase`, `CacheBudgetProvider`, `CacheStore`), Media3 `exoplayer`, NextLib. Không phụ thuộc `:core:data`, `:core:designsystem` hay feature nào.
3. **`:feature:player`** giữ phần riêng của màn Xem video: `VideoPlayerController` (gắn với `ODVPlayState` của designsystem), ViewModel, gesture, thanh điều khiển. `VideoPlayerFactory` của feature chỉ còn bọc `ExoPlayerFactory` rồi dựng controller.
4. **`:feature:shorts`** (Lát 8b) dùng `ExoPlayerFactory` và `VideoCache` từ `:core:media`, cộng `DefaultPreloadManager` (ADR-0024).
5. **Koin:** module `androidMediaModule` của `:core:media` đăng ký `VideoCache` (kèm `ConnectionResetter`, `CacheStore`) và `ExoPlayerFactory`; `MainApplication` ghép thêm module này. `androidPlayerModule` chỉ còn `VideoPlayerFactory` của feature.
6. Chuyển mã **không đổi hành vi**: giữ nguyên tên lớp, logic và chuỗi log; chỉ đổi package thành `com.lambao.odv.core.media` và nới `internal` thành `public` cho phần feature cần.

## Phương án đã cân nhắc
### `:feature:shorts` phụ thuộc `:feature:player`
- Ưu: ít file phải chuyển.
- Nhược: phá quy tắc feature không phụ thuộc feature, kéo cả UI và ViewModel của màn Xem video vào Short.
- Lý do không chọn: người dùng chọn tách ra module chung (2026-10-10).
### Nhân bản mã sang `:feature:shorts`
- Ưu: không đụng vào `:feature:player` đã kiểm tay.
- Nhược: hai bản lệch nhau, khóa cache khác nhau thì mất lợi ích cache chung (BN-01).
- Lý do không chọn: tốn kém bảo trì và dễ sai âm thầm.
### Đưa vào `:core:data`
- Ưu: đã là nơi giữ các kho cache khác.
- Nhược: `:core:data` hiện không phụ thuộc Media3; sẽ kéo ExoPlayer và NextLib (GPL-3.0) vào mọi module dùng data.
- Lý do không chọn: tách riêng để phạm vi phụ thuộc rõ ràng.

## Hệ quả
### Tích cực
- Hai feature video dùng chung một cache, một bộ decoder, một cơ chế làm mới link.
- Quy tắc phụ thuộc của kiến trúc được giữ.
### Tiêu cực
- Một module mới phải thêm vào `settings.gradle.kts`, build và Koin.
- Một lần chuyển file có nguy cơ sót import hoặc thay đổi khả năng nhìn thấy (`internal` → `public`), cần build và kiểm tay lại Xem video.
### Rủi ro
- Tên lớp trùng khi hai module cùng có `shortId()` hoặc logger: giữ hàm trong `:core:media` là `internal` cho chính nó, feature dùng bản riêng.
- Kiểm tay: phát video, tua, đổi tab/khóa PIN giữa chừng, tắt mạng, HEVC 10-bit trên máy MediaTek (FFmpeg), Ngắt kết nối xóa cache video.
