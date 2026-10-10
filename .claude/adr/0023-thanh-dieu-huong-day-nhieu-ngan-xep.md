# ADR-0023: Thanh điều hướng đáy, mỗi tab một ngăn xếp riêng

**Ngày**: 2026-10-10
**Trạng thái**: accepted (bổ sung ADR-0003; thay cách mở Cài đặt từ nút bánh răng)
**Người quyết định**: LamBao

## Bối cảnh
Màn Danh sách đang có 2 tab Thư mục / Thư viện dạng segmented ở đầu màn, Cài đặt mở bằng nút bánh răng trên AppBar như một màn đẩy vào back stack. Lát 8 thêm tab **Short** (video ngắn vuốt dọc). Bốn mục (Thư mục, Thư viện, Short, Cài đặt) không còn vừa dạng segmented, và Short cần chiếm gần hết màn hình nên không hợp với Tabs ở đầu màn.

Người dùng yêu cầu: Cài đặt cũng nằm trên thanh đáy; mỗi tab giữ trạng thái riêng; chuyển tab rồi Back về phải đúng trạng thái và vị trí (DH-02, DH-03).

## Quyết định
1. **Màn chính** có thanh điều hướng đáy với tối đa 4 mục: Thư mục, Thư viện, Short, Cài đặt (DH-01). Mục Short chỉ hiện khi loại Video bật **và** đồng bộ lần đầu đã xong; giá trị này suy ra từ `Flow` của cài đặt loại tệp và trạng thái đồng bộ, không lưu riêng.
2. **Hai tầng điều hướng:**
    - Tầng app (back stack Navigation 3 hiện có, ADR-0003): Splash, Kết nối, Khóa, Thiết lập bảo mật, **Màn chính**, các màn xem (video, ảnh, PDF) và các màn con của Cài đặt (đổi PIN, nhập PIN, cập nhật Client Secret). Màn nào đẩy lên tầng này thì che Màn chính, nên thanh đáy tự ẩn (DH-05) mà không cần cờ ẩn/hiện riêng.
    - Tầng tab (bên trong Màn chính): mỗi tab có **trạng thái riêng tồn tại suốt vòng đời Màn chính**: ViewModel theo từng tab (không bị hủy khi đổi tab) và trạng thái giao diện (vị trí cuộn) giữ bằng `SaveableStateHolder` theo khóa tab. Điều hướng trong tab Thư mục (thư mục cha/con) vẫn là state của tab đó, không đẩy lên tầng app.
3. **Back** (DH-03): xử lý trong tab trước (Thư mục lên một cấp, thoát tìm kiếm); ở gốc tab khác Thư mục thì chuyển về Thư mục; ở gốc Thư mục thì để hệ thống thoát app.
4. **Cài đặt chuyển thành tab**: bỏ route Cài đặt ở tầng app và nút bánh răng trên AppBar. Chính sách chặn chụp của ADR-0022 giữ nguyên ý nghĩa nhưng áp theo **tab đang hiện** (DH-08): mỗi lần đổi tab thì áp lại `FLAG_SECURE` theo cài đặt "Bảo vệ màn hình".
5. **Tab mở khi khởi động** (DH-06): chỉ lưu tab gần nhất nếu là Thư mục hoặc Thư viện (DataStore, thay khóa "tab gần nhất" hiện có). Khôi phục sau khi hệ điều hành thu hồi tiến trình thì dùng saved state, kể cả khi đang ở Short (ở trạng thái tạm dừng).
6. **Ngắt kết nối** (CD-05) bỏ cả Màn chính khỏi tầng app, nên trạng thái mọi tab mất theo.

## Phương án đã cân nhắc
### Giữ Tabs ở đầu màn, thêm Short thành tab thứ 3
- Ưu: ít thay đổi.
- Nhược: Short cần toàn màn; Cài đặt vẫn phải mở bằng nút bánh răng; segmented 3 đến 4 mục chật trên màn 360dp.
- Lý do không chọn: không đáp ứng yêu cầu đưa Cài đặt xuống thanh đáy.
### Một back stack chung, đổi tab thì thay nội dung stack
- Ưu: đơn giản nhất với Navigation 3.
- Nhược: đổi tab làm mất trạng thái tab cũ.
- Lý do không chọn: trái DH-02.
### Mỗi tab một back stack Navigation 3 riêng (nhiều `NavDisplay`)
- Ưu: đúng mẫu "multiple back stacks" chính thống.
- Nhược: các màn xem và màn con Cài đặt cần che thanh đáy, nên vẫn phải đẩy lên tầng app; trong tab chỉ còn đúng một màn, back stack riêng không mang lại gì.
- Lý do không chọn: thêm độ phức tạp mà không có lợi ích. Nếu sau này một tab cần nhiều màn có thanh đáy (vd. màn chi tiết trong tab) thì xem lại.

## Hệ quả
### Tích cực
- Back ở Thư viện không còn thoát app (LOW tồn tại từ Lát 4 được giải quyết).
- Thêm tab sau này (vd. Kệ truyện) chỉ là thêm một mục.
### Tiêu cực
- ViewModel của mọi tab đã mở sống cùng Màn chính, tốn bộ nhớ hơn một chút; trình phát của Short phải tạm dừng khi rời tab (SV-11) và chỉ giải phóng khi Màn chính bị bỏ.
- Màn Cài đặt mất mũi tên quay lại; các chỗ code đang điều hướng tới route Cài đặt (banner CD-06, nút "Cập nhật") phải đổi thành chuyển tab rồi mở màn con.
### Rủi ro
- Khôi phục sau khi hệ điều hành thu hồi tiến trình ở nhiều tab cùng lúc: cần kiểm tay riêng (bật "Don't keep activities").
- `FLAG_SECURE` theo tab: đổi tab nhanh có thể để lọt một khung hình không chặn. Rủi ro thấp vì chỉ liên quan tab Cài đặt khi tắt "Bảo vệ màn hình" (đằng nào cũng chụp được).