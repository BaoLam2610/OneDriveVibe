# ADR-0021: Trần bộ nhớ đệm tùy chỉnh, chia theo loại, đọc lúc chạy

**Ngày**: 2026-10-07
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Bốn cache (thumbnail Coil, ảnh gốc, video Media3, PDF ở Lát 8) có trần cố định trong code (200 MB, 1 GB, 2 GB). Đặc tả Cài đặt (CD, CD-07) cho người dùng đặt giới hạn và dọn ngay khi giảm. Người dùng chốt (2026-10-07): giới hạn chung **tùy chỉnh từ 1 đến 10 GB** và **tỉ lệ chia cho từng loại** (mỗi mốc 0 đến 100%, tổng luôn 100%, chỉnh một mốc thì phần còn lại tự tính lại). Mỗi cache do một thư viện khác nhau quản lý và không có LRU chung.

## Quyết định
1. Trần của một loại = giới hạn chung × tỉ lệ loại đó (`CacheBudget.bytesFor`). Mặc định 2 GB, chia 10% / 30% / 45% / 15% (thumbnail / ảnh / video / PDF). Luật tổng 100 và tự cân bằng nằm ở domain (`CacheShares.withShare`).
2. Mỗi kho cài `CacheStore` (`kind`, `usedBytes`, `clear`, `trimTo`) và đăng ký vào Koin; `GetCacheUsageUseCase`, `ClearCacheUseCase`, `SetCacheLimitUseCase`, `SetCacheSharesUseCase` gom bằng `getAll()` như `DisconnectUseCase`. Thêm kho (PDF ở Lát 8) không phải sửa chỗ khác.
3. Các kho đọc trần **lúc cần** qua `CacheBudgetProvider` (bản cài `CacheBudgetSource` giữ `StateFlow` của cài đặt), không tiêm hằng số: ảnh gốc qua `ResumableFileStore(maxBytes: () -> Long)`, video qua `BudgetCacheEvictor` (LRU như bản Media3 nhưng trần đọc lúc cần).
4. Đổi giới hạn hoặc tỉ lệ thì **dọn ngay** từng kho về trần mới của nó (CD-07). Thumbnail là ngoại lệ: Coil `DiskCache` chỉ nhận trần lúc tạo và không dọn chọn lọc, nên vượt trần thì xóa hết thumbnail, và trần mới có hiệu lực từ lần mở app sau.
5. Cài đặt lưu ở DataStore `settings` (`cache_limit_gb`, `cache_shares`), giá trị lạ về mặc định, bị xóa khi Ngắt kết nối (CD-05).

## Phương án đã cân nhắc
- Bốn mốc 1/2/5/10 GB và không chia theo loại (đặc tả ban đầu): đơn giản hơn nhưng bị người dùng bác.
- Một LRU chung cho mọi cache: không làm được vì Coil và Media3 tự quản lý việc dọn.
- Dựng lại `ImageLoader` khi đổi trần thumbnail: đụng vào mọi nơi giữ tham chiếu loader, rủi ro lớn so với lợi ích. Chọn "có hiệu lực từ lần mở sau".

## Hệ quả
- Tích cực: một luật rõ ràng, thêm kho mới dễ, người dùng điều khiển được dung lượng.
- Tiêu cực: mặc định cache ảnh giảm từ 1 GB xuống 600 MB và video từ 2 GB xuống 900 MB (45% của 2 GB). Thanh trượt và bảng tỉ lệ đã có thiết kế từ 2026-10-08 (board "Slider và tỉ lệ chia" trong ODV Foundations; `ODVSlider`, `ODVSliderRow`, `ODVSplitBar`).
- Rủi ro: `BudgetCacheEvictor` là bản viết lại của lớp Media3, theo dõi bản cập nhật Media3; `trimTo` của video gọi trong `synchronized(cache)`.
