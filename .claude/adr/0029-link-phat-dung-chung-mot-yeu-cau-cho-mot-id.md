# ADR-0029: Link phát của tab Short dùng chung và chỉ một yêu cầu cho một id

**Ngày**: 2026-10-12
**Trạng thái**: accepted 2026-10-12 (thay mục 3 của ADR-0026 về `StreamUrlProvider` riêng của bộ tải trước, và phần "mỗi player có provider riêng" của ADR-0025, chỉ cho đường tab Short; màn Xem video giữ nguyên). Người dùng chốt sau khi thử máy: log cho thấy mục 1 đến 3 của "Cách xác nhận" đạt (mỗi video một lần lấy link, khóa PIN hai lần chỉ một dòng xóa và không lấy link sau đó, mất mạng và có mạng lại không mất link). **Còn tồn, chưa kiểm:** mục 4 (Ngắt kết nối rồi kết nối lại) và mục 5 (màn Xem video không đổi).
**Người quyết định**: LamBao (đề xuất bởi Claude sau khi đọc log vuốt tab Short)

## Bối cảnh
Link tải của OneDrive là URL đã ký sẵn, sống khoảng 1 giờ; mỗi lần lấy là **một lần gọi Graph** (`GraphApi.getDownloadUrl`, 302 → `Location`), đo được 220 đến 370 ms (cold start 1,3 s). Log vuốt tab Short (2026-10-12) cho thấy mỗi video kế tiếp gọi Graph **2 đến 3 lần** cùng một mili-giây:
- Có ba `StreamUrlProvider` độc lập, mỗi cái có bản đồ link riêng: `VideoPreloader.urls` (`CacheWriter`), `streamUrls` của `ExoPlayerFactory` (player và manager tải trước dùng chung, ADR-0028), `FirstFrameProvider.urls` (poster). Cùng video +1 bị cả ba cần cùng lúc nên mỗi bên tự gọi.
- Ngay trong một provider, `urlFor` không có cơ chế một yêu cầu cho một id: hai luồng cùng hỏi một id lúc chưa có link thì cùng gọi Graph (log: hai luồng của manager lấy link cùng một id cách nhau dưới 10 ms).

Hệ quả: tốn request vào Graph vốn có giới hạn tốc độ (`429` + `Retry-After`, tài liệu `onedrive-graph-api.md`), tranh băng thông với video đang xem, và các request chen nhau làm chậm lẫn nhau (có lần 745 và 909 ms). Chưa thấy `429` hay thẻ lỗi do việc này; đây là giảm rủi ro khi vuốt nhanh hoặc hạn mức thấp.

Một khe hở CH-03 cũ cũng liên quan (review 2026-10-11): việc lấy link đang chạy lúc khóa app vẫn có thể ghi lại một link vào bộ nhớ sau khi đã xóa.

## Quyết định
1. **Single-flight trong `StreamUrlProvider`:** bản đồ `inflight: id → Deferred` chạy trong scope riêng của provider. Người đến sau cùng id chờ chung kết quả. Người chờ hết hạn (`URL_FETCH_TIMEOUT_MS`) hoặc bị ngắt chỉ bỏ chờ, không hủy việc của người khác; việc lấy link tự có hạn riêng để không treo mãi.
2. **`epoch` chống khe hở CH-03:** `clear()` tăng `epoch`, xóa link, hủy mọi việc đang chạy. Việc lấy link chỉ ghi link nếu `epoch` vẫn như lúc bắt đầu (ghi và xóa cùng một khóa). Người đang chờ một việc bị hủy nhận `StreamUrlException(AppError.AppLocked)`.
3. **`SharedStreamUrls`** (`:core:media`, singleton Koin) bọc một `StreamUrlProvider` dùng chung cho **tab Short** gồm `VideoPreloader`, player cùng manager (`ExoPlayerFactory.createWithPreload`) và `FirstFrameProvider`. **Màn Xem video giữ provider riêng cho mỗi player** (`ExoPlayerFactory.create`), link chết theo màn xem.
4. **Vòng đời:** khóa app và `onCleared` của `ShortPlayerHolder`: dừng mọi người dùng rồi `sharedUrls.clear()` **một lần**. Mất mạng chỉ dừng việc tải trước (`VideoPreloader.cancelAll()` không còn xóa link), vì player đang phát vẫn cần link.
5. **`SharedStreamUrls` là `ConnectionResetter`:** Ngắt kết nối (CD-05) xóa link, không để link ký sống thêm tối đa 45 phút trong singleton (khoảng hở có sẵn của `VideoPreloader` và `FirstFrameProvider` trước đây).
6. **VD-14 giữ nguyên:** `StreamDataSource.openWith(refresh = true)` không đổi. Yêu cầu làm mới cũng tham gia việc đang chạy của cùng id vì việc đó luôn gọi Graph nên cho ra link mới.

## Phương án đã cân nhắc
### Một provider dùng chung cho cả màn Xem video
- Ưu: ít loại hơn. Nhược: link của màn Xem video sống lâu hơn màn (tới khi khóa app hoặc hết hạn), đổi vòng đời đã kiểm tay kỹ. Không chọn.
### Chỉ single-flight trong từng provider, giữ ba provider
- Ưu: thay đổi nhỏ. Nhược: vẫn 2 đến 3 lần gọi mỗi video (một lần mỗi provider). Không giải quyết vấn đề chính.
### Không làm gì
- Hợp lệ vì chưa thấy `429`; nhưng rủi ro tăng khi vuốt nhanh và khe hở CH-03 cũ còn đó.

## Hệ quả
### Tích cực
- Mỗi video chỉ một lần gọi Graph để lấy link (kỳ vọng: một dòng `[Url] lấy link` mỗi video).
- Một chỗ duy nhất xóa link khi khóa app, và không còn khe hở ghi link sau khi xóa.
- Ngắt kết nối xóa luôn link ký đang giữ.
### Tiêu cực
- `StreamUrlProvider` phức tạp hơn (scope, `inflight`, `epoch`); constructor thêm `DispatcherProvider`.
- Hai kiểu sống của provider (riêng cho màn Xem video, chung cho Short).
### Rủi ro
- Quên dừng một người dùng trước khi `clear()` thì việc lấy link của họ bị hủy giữa chừng và họ nhận `AppLocked`; chấp nhận được lúc khóa app.
- Chưa build; chữ ký `Deferred`/`computeIfAbsent`/`invokeOnCompletion` viết theo kotlinx.coroutines đang dùng trong dự án.
- VD-14 ít khi chạy thật (TTL 45 phút nhỏ hơn thời hạn 1 giờ của link), nên nhánh làm mới khó kiểm tay.

## Cách xác nhận
1. Vuốt 20 video tab Short: mỗi video kế tiếp chỉ có **một** dòng `[Url] lấy link id=…`, có thể kèm dòng `[Url] gộp yêu cầu`; thời gian tới khung hình đầu không tệ hơn trung vị 301 ms (ADR-0028).
2. Khóa PIN giữa lúc phát Short: **một** dòng `[Url] xóa N link đã giữ, hủy M việc`; sau đó không có `lấy link` nào cho tới khi mở khóa; mở khóa nạp lại bình thường.
3. Bật chế độ máy bay giữa lúc phát: video đang phát không mất link; tắt chế độ máy bay thì tải trước chạy lại.
4. Ngắt kết nối từ Cài đặt, kết nối lại, vào tab Short: không dùng link cũ.
5. Màn Xem video mở từ Thư mục: phát bình thường, đóng màn thì link của nó không còn liên quan tab Short.
