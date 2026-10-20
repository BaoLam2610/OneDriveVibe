# Checklist kiểm tay trên thiết bị

Unit test không bắt được những gì phụ thuộc vòng đời Android, thiết bị và mạng thật. Mỗi lát chạy các mục **áp dụng** dưới đây, cộng danh sách riêng của lát trong kế hoạch. Mục không kiểm được thì ghi lý do vào file tiến độ.

Chuẩn bị: một máy tầm trung (chip khác hãng với máy dev nếu có), Developer options bật, bản debug có màn Debug.

## Vòng đời

- [ ] **Don't keep activities** bật: vào màn sâu của lát, ra nền, vào lại → đúng màn, đúng dữ liệu, đúng vị trí cuộn, ô nhập còn nội dung.
- [ ] Ra nền rồi vào lại sau vài phút (không bật tùy chọn trên): dữ liệu tự làm mới nếu đặc tả yêu cầu.
- [ ] Kill tiến trình từ Android Studio khi đang ở màn của lát, mở lại.
- [ ] Bấm Back ở mọi màn, mọi tab; Back dự đoán (Android 14+).
- [ ] Bấm Home ngay sau một thao tác đang chạy (gửi form, tải): mở lại không kẹt "đang tải", không chạy hai lần.
- [ ] Nếu app có khóa/đăng xuất tự động: khóa giữa lúc tác vụ đang chạy, mở khóa → tác vụ chạy tiếp hoặc báo đúng.

## Mạng

- [ ] Chế độ máy bay khi mở màn: banner/trạng thái offline đúng, không crash.
- [ ] Tắt mạng giữa lúc tải; bật lại → tự chạy tiếp hoặc nút Thử lại chạy được.
- [ ] Mạng chậm (giới hạn băng thông của emulator hoặc proxy): trạng thái tải hiện, không nháy.
- [ ] Tab API của màn Debug: không có request thừa, không request lặp vô tận, không request sau khi đăng xuất.

## Cấu hình

- [ ] Đổi ngôn ngữ trong app khi đang ở màn của lát: mọi chữ đổi ngay (kể cả dialog, sheet), không chớp, vị trí cuộn giữ.
- [ ] Đổi giao diện Sáng/Tối trong app và theo hệ thống; khởi động nguội ở cả hai: không nháy màu.
- [ ] Cỡ chữ hệ thống lớn nhất: không cắt chữ quan trọng, không đè nút.
- [ ] Xoay màn hình (nếu app không khóa hướng): dữ liệu và trạng thái giữ.
- [ ] Bàn phím mở: nút chính không bị che, không bị đẩy đè lên ô nhập.

## Giao diện

- [ ] So với artboard (cạnh nhau trên màn hình), đặc biệt khoảng cách, cỡ chữ, màu ở theme tối.
- [ ] Đổi bộ lọc/tab/tài khoản nhiều lần nhanh: không khựng, không nháy vòng quay.
- [ ] Trạng thái rỗng, lỗi, đang tải đều đúng thiết kế.
- [ ] TalkBack: đọc được các nút chính, thứ tự đọc hợp lý.
- [ ] Chạm nhanh hai lần vào nút chính: chỉ chạy một lần.

## Dữ liệu và bảo mật

- [ ] Đăng xuất / xóa dữ liệu: tab Lưu trữ của màn Debug không còn tệp dữ liệu nào ngoài cài đặt debug; đăng nhập lại không thấy dữ liệu cũ.
- [ ] Nâng version CSDL: cài đè bản mới lên bản cũ (không gỡ) → app mở được (đã phát hành thì dữ liệu còn nguyên).
- [ ] Cài đặt đã chọn còn nguyên sau khi cài đè bản mới.
- [ ] Màn nhạy cảm không chụp được màn hình; danh sách app gần đây ẩn nội dung.
- [ ] Tab API/Log: không có bí mật trong log local (log API đầy đủ ở bản debug là có chủ đích).

## Bản release (trước mỗi lần phát hành)

- [ ] `assembleRelease` + cài lên máy: chạy được với R8 (serialization, reflection).
- [ ] Không có nút bọ, Logcat không có log của app.
- [ ] Kích thước APK/AAB không tăng bất thường.
