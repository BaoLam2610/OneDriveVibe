# ADR-0028: Chuẩn bị sẵn video kế tiếp của tab Short trong RAM bằng `DefaultPreloadManager`, bổ sung cho `CacheWriter`

**Ngày**: 2026-10-12
**Trạng thái**: accepted 2026-10-12 (bổ sung ADR-0026, không thay thế). Người dùng chốt sau khi thử máy: A/B bằng công tắc debug cho thấy trung vị thời gian tới khung hình đầu giảm khoảng 69% (xem "Kết quả đo"); log khóa PIN và vuốt ngược đạt. Còn tồn: kiểm HEVC 10-bit qua FFmpeg trên Xiaomi (chưa có máy), không chặn.
**Người quyết định**: LamBao (đề xuất bởi Claude sau khi so sánh `CacheWriter` và `DefaultPreloadManager`)

## Bối cảnh
ADR-0026 tải trước byte đầu của video ±1 vào cache đĩa bằng `CacheWriter`. Player vẫn phải mở nguồn, đọc phần mô tả tệp (`moov`), dựng extractor, chọn track và đệm sau khi vuốt sang; khoảng 100 đến 200 ms đen còn lại đang được poster (ADR-0027) che. ADR-0026 đã ghi "xem lại nếu thời gian tới khung hình đầu vẫn còn cao".

Kiểm chứng trong sources Media3 1.11.1 (`~/.gradle/caches`), không dựa vào trí nhớ:
- `DefaultPreloadManager` có các stage `SOURCE_PREPARED`, `TRACKS_SELECTED`, `SPECIFIED_RANGE_LOADED` (mẫu đã tách sẵn trong RAM) và `SPECIFIED_RANGE_CACHED` (ghi cache đĩa qua `PreCacheHelper`).
- **Bẫy:** ở chế độ `SPECIFIED_RANGE_CACHED`, khi gỡ mục (`remove`), `reset()` hoặc `release()`, manager gọi `PreCacheHelper.release(removeCachedContent = true)` → `ProgressiveDownloader.remove()` → `cache.removeResource(cacheKey)`: **xóa cả video khỏi cache**, không chỉ đoạn đã tải trước. Cache video là cache chung với màn Xem video (VD-11, BN-03, phát offline), nên không được bật chế độ này. `Builder.setCache` cũng bắt buộc khi dùng stage đó.
- `PreloadMediaSource.releasePreloadMediaSource()` chỉ dọn phần tải trước; việc `releaseSourceInternal()` bị chặn khi nguồn đang được player dùng. `clear()` chỉ giải phóng period đang tải trước, không đụng period đang phát.
- Manager buộc player dựng qua `Builder.buildExoPlayer(...)`, hàm này ghi đè `MediaSourceFactory`, `RenderersFactory`, `LoadControl`, `TrackSelector`, `BandwidthMeter` và luồng phát.

## Quyết định
1. **Giữ nguyên `VideoPreloader` (ADR-0026)** cho video ±1: cache đĩa, phát lại được khi offline, tính vào trần cache video (BN-01).
2. **Thêm `NextVideoPreload`** (`:core:media`) bọc một `DefaultPreloadManager` chỉ cho **video +1**, ở stage `SPECIFIED_RANGE_LOADED` với `NEXT_PRELOAD_MS` = 3 giây đầu. Video −1 không vào RAM.
3. **Không gọi `Builder.setCache` và không dùng stage `SPECIFIED_RANGE_CACHED`.** Phần đĩa chỉ đi qua `CacheDataSource` trong chuỗi nguồn dữ liệu như hiện nay. Gỡ mục hay `reset()` chỉ bỏ dữ liệu trong RAM.
4. **Player của tab Short dựng bằng `ExoPlayerFactory.createWithPreload()`**, lấy nguồn, renderer, load control từ cùng `PlayerParts` với `create()`; màn Xem video vẫn dùng `create()` và không đổi hành vi. Manager và player dùng chung một `StreamUrlProvider`. `MediaItem` của hai bên dựng qua một hàm duy nhất `streamMediaItem` vì manager tra nguồn theo `MediaItem.equals`.
5. **Vòng đời:**
   - Cửa sổ tối đa hai mục (video đang xem và video +1), cập nhật sau khi video đang xem đã vẽ khung hình đầu, như ADR-0026 mục 2.
   - Mất mạng: trả `NOT_PRELOADED` cho mọi mục (SV-13).
   - App bị khóa hoặc `onCleared`: `reset()`/`release()` manager **trước** khi `release()` player (CH-03).
   - Nguồn đã qua tay player không dùng lại; vuốt ngược hoặc xáo lại thì dựng mục mới.
   - Chỉ dùng nguồn tải trước khi video bắt đầu từ đầu (vị trí 0). Lỗi bộ giải mã hoặc thử lại thì mở lại bằng `MediaItem` thường, vì nguồn tải trước đã chọn track theo bộ giải mã lúc đó.
6. **Đo trước và sau.** Log có `[Short] đã vẽ khung hình đầu … sau Xms kể từ lúc nạp (nguồn=ram|thường)`, `[Preload+1] xong …` và, khi player lấy nguồn, `[Preload+1] player dùng nguồn tải trước id=… sẵn=true|false chuẩnBị=Xms` (sẵn = manager đã báo hoàn tất; chuẩnBị = thời gian từ lúc thêm mục, tách "chuẩn bị chưa kịp" khỏi "kịp nhưng vẫn chậm"). Bản debug có công tắc ở màn Debug › Khác › Short (hook `NextVideoPreloadSwitch` ở domain, mặc định luôn bật ở `:core:media`, bản debug ghi đè qua Koin như `ThumbnailQuality`) để tắt nguồn tải trước và so A/B trong cùng phiên; có hiệu lực từ lần vuốt kế tiếp, bản phát hành không có công tắc.

## Điều kiện giữ lại
Giữ nếu trung vị thời gian tới khung hình đầu của video có `nguồn=ram` thấp hơn rõ (đề xuất ≥ 30%) so với `nguồn=thường` trong cùng phiên thử, không có hồi quy ở khóa PIN giữa lúc phát, vuốt nhanh, xáo lại và video HEVC 10-bit qua FFmpeg. Nếu không đạt thì gỡ thay đổi, ghi ADR này là `rejected` kèm số đo. Nếu poster đã che hết khoảng đen và số đo `nguồn=thường` đã dưới khoảng 300 ms thì không cần giữ.

## Kết quả đo (2026-10-12, máy MediaTek, công tắc debug bật/tắt trong hai phiên)
- **Trung vị** thời gian tới khung hình đầu (không tính video đầu lúc khởi động nguội): **301 ms** với `nguồn=ram` (16 mẫu, p90 khoảng 544 ms) so với **963 ms** với `nguồn=thường` (13 mẫu, p90 khoảng 1610 ms). Hai phiên dùng danh sách xáo khác nhau và cache của phiên tắt đã nóng hơn, nên mức giảm thật có thể lớn hơn mức này.
- Một lần video hiện tại lỗi (MPEG-4 2176x2880, không có bộ giải mã) khiến video kế tiếp mở bằng nguồn thường mất 2114 ms; đã sửa bằng cách tải trước cả khi video hiện tại lỗi (`ShortsContent`, `currentFailed`).
- Chỉ khoảng 1/4 lần `sẵn=true` (7/26 qua ba log; phần còn lại manager chưa tải đủ 3 giây lúc vuốt) mà vẫn đạt khoảng 300 ms cho cả hai nhóm: lợi ích chính đến từ nguồn đã mở, track đã chọn và mẫu đầu đã có, không cần tải đủ `NEXT_PRELOAD_MS`. Cần khoảng 2 giây chuẩn bị trước khi vuốt; dưới 2 giây thì 540 đến 700 ms.
- Khóa PIN: `[Preload] dừng`, `[Preload+1] xóa 2 mục` và cả ba nơi giữ link ký (`VideoPreloader`, player/manager, poster) đều xóa link; sau mở khóa video nạp lại `nguồn=thường` đúng vị trí rồi cửa sổ tải trước dựng lại. Vuốt ngược: mục kế tiếp từng được phát bị dựng lại (`cửa sổ 1 mục`) và lần vuốt sau `nguồn=ram` 266 ms.
- Video 4K H.264 vẫn đi qua FFmpeg phần mềm (ADR-0018) và rớt khung; không do ADR này.

## Phương án đã cân nhắc
### Thay hẳn `CacheWriter` bằng `DefaultPreloadManager` (cả ±1, có `setCache`)
- Nhược: gỡ mục xóa cả video khỏi cache chung; mất khả năng phát offline phần đầu video đã tải; phải viết lại phần khóa app/mất mạng cho cả hai hướng.
- Không chọn.
### Giữ nguyên ADR-0026, không làm gì thêm
- Ưu: không thêm độ phức tạp; poster đã che khoảng đen.
- Nhược: không biết có thể nhanh hơn bao nhiêu. Phương án này vẫn là kết cục hợp lệ nếu số đo không đạt.
### Hai ExoPlayer (ADR-0026 đã cân nhắc)
- Giữ nguyên lý do loại: hai decoder cùng lúc, tải CPU của FFmpeg.

## Hệ quả
### Tích cực
- Bỏ được bước mở nguồn, đọc `moov` và chọn track khi vuốt sang video +1, mẫu đầu đã sẵn cho decoder.
- Cache đĩa dùng chung không bị ảnh hưởng.
### Tiêu cực
- Hai đường dựng player (`create`, `createWithPreload`); `ExoPlayerFactory` dài hơn.
- Tốn RAM cho khoảng 3 giây mẫu của một video.
- Thêm một tầng trạng thái (`consumed`, `failed`, `ranks`) cần giữ đúng khi xáo lại, vuốt ngược, mất mạng.
### Rủi ro
- Chưa kiểm chứng trên máy Xiaomi/HEVC 10-bit qua FFmpeg; `LoadControl` dùng chung có thể làm việc tải trước +1 chờ khi player đang đệm.
- Link ký trong nguồn đã chuẩn bị có thể hết hạn nếu nằm chờ lâu; `StreamDataSource` tự lấy link mới khi gặp 401/403 lúc mở lại kết nối (VD-14).
- Build chưa chạy: chữ ký Java của `MediaSourceFactorySupplier`, `TargetPreloadStatusControl` viết theo sources 1.11.1, cần người dùng build để xác nhận.
