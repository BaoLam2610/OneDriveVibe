# ADR-0013: Log API ở bản debug hiển thị đầy đủ, che là tùy chọn khi hiển thị

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao
**Thay thế một phần**: ADR-0012, quyết định số 4 ("làm sạch ở nguồn, không ghi body endpoint token")

## Bối cảnh
ADR-0012 làm sạch lưu lượng API ngay ở `:core:network` trước khi đưa cho màn Debug. Khi kiểm tay, cách này không đủ để gỡ lỗi: không thấy header thật, không thấy body request lấy token, response bị che nên không so được với `onedrive-graph-responses.md`. Cần màn chi tiết cho từng request (header, request body, response, JSON đẹp, sao chép, tìm kiếm).

## Quyết định
1. `HttpTrafficRecorder` nhận bản ghi **đầy đủ, chưa che**: mọi header (kể cả `Authorization`), body request (kể cả `client_secret` của endpoint token), response body (kể cả `access_token`, `downloadUrl`), tối đa 1.000.000 ký tự mỗi body.
2. Màn Debug hiển thị đầy đủ theo mặc định. Có công tắc "Che dữ liệu nhạy cảm" (tab Khác, lưu trong SharedPreferences `odv_debug`); bật thì `HttpTrafficEntry.masked()` che `Authorization`, `client_secret`, token, `downloadUrl`, `tempauth`, `sig` khi hiển thị, tìm kiếm và sao chép.
3. Nút "Sao chép" đánh dấu clip là nhạy cảm (`IS_SENSITIVE`) để Android 13+ không hiện bản xem trước.
4. Các ràng buộc giữ nguyên để CH-06 vẫn đúng với **bản phát hành**: recorder chỉ do `:tools:debug` cài (module chỉ có trong bản debug); dữ liệu chỉ nằm trong bộ nhớ của tiến trình, không ghi đĩa, không vào Logcat, không vào Kermit; bản release không có recorder nên `:core:network` không dựng bản ghi nào.
5. Bổ sung cùng đợt: tùy chọn FLAG_SECURE toàn app (Theo thiết kế / Luôn bật / Luôn tắt) trong tab Khác; nút bọ nổi vẽ thêm một cửa sổ riêng trên Dialog và BottomSheet qua `LocalODVTopOverlay`.

## Phương án đã cân nhắc
### Giữ làm sạch ở nguồn (ADR-0012)
- Ưu: không bao giờ có bí mật trong bộ nhớ ngoài đường dẫn token.
- Nhược: không gỡ lỗi được những lỗi liên quan header, body request, định dạng response.
- Lý do không chọn: ảnh hưởng trực tiếp khả năng kiểm tay (ADR-0009).
### Ghi đầy đủ nhưng luôn che khi hiển thị
- Nhược: không xem được giá trị thật khi cần đối chiếu.
- Lý do không chọn: người phát triển yêu cầu xem đầy đủ; công tắc che đã cover trường hợp cần che.

## Hệ quả
### Tích cực
- Gỡ lỗi mạng đầy đủ: so sánh response với tài liệu, kiểm tra header, sao chép request/response.
### Tiêu cực
- Trong bản debug, Client Secret và access token có mặt trong bộ nhớ của `ApiTrafficStore` và trên màn hình Debug.
### Rủi ro
- Chụp màn hình, quay màn hình hoặc dán nội dung đã sao chép ra ngoài sẽ lộ bí mật thật. Giảm thiểu: chỉ bản debug; có công tắc che; clip đánh dấu nhạy cảm; `ApiTrafficStore` có nút xóa và giới hạn 200 request.
- Nút bọ và `DebugActivity` mở được ở mọi màn, kể cả màn Khóa và nhập PIN, không qua PIN. Khi khóa app hoặc ngắt kết nối, `ApiTrafficStore` không bị xóa (CH-03 chỉ áp cho token trong `TokenProvider`), nên token và Client Secret vẫn xem được trong màn Debug. Chấp nhận có chủ đích cho bản debug (bổ sung 2026-10-03 sau review Lát D). Nếu cần chặt hơn: gọi `ApiTrafficStore.clear()` khi khóa app (làm cùng Lát 2).
- Bộ đệm log chưa có trần tổng: 200 request, mỗi body tối đa 1.000.000 ký tự, và `bodyAsText()` đọc cả body trước khi cắt. Nếu sau này dùng `authorizedGet` cho nội dung lớn hoặc nhị phân (tải tệp, thumbnail) thì bản debug có thể tốn nhiều bộ nhớ; khi đó chỉ ghi body có `Content-Type` là text/JSON và giới hạn tổng ký tự. `SectionCache` cũng chỉ được dọn khi tìm kiếm.
- Dùng bản debug với tenant/secret thật của môi trường sản xuất: nên tạo secret riêng cho việc phát triển và thu hồi sau khi dùng.
