# ADR-0020: Bỏ ghi đè FLAG_SECURE riêng của công cụ debug

**Ngày**: 2026-10-07
**Trạng thái**: accepted (thay phần "tùy chọn FLAG_SECURE toàn app: Theo thiết kế / Luôn bật / Luôn tắt" của ADR-0013; mục Đánh đổi về màn Cài đặt luôn chặn chụp bị ADR-0022 thay)
**Người quyết định**: LamBao

## Bối cảnh
ADR-0013 thêm ở tab Khác của màn Debug một tùy chọn FLAG_SECURE toàn app với ba chế độ. Lát 7c thêm mục "Bảo vệ màn hình" ở Cài đặt (CD-12) cũng điều khiển FLAG_SECURE toàn app. Hai nguồn cùng điều khiển một cờ gây xung đột: ghi đè của Debug thắng cài đặt người dùng mà không ai biết, `DebugActivity` (cửa sổ riêng) không theo cài đặt trong khi log API ở đó chứa secret, và nhãn "Theo thiết kế" thành sai nghĩa.

## Quyết định
1. **Bỏ** ba chế độ ghi đè của Debug (`ODVSecureMode`, `DebugSettings.secureMode`, khóa lưu `secure_mode`). Màn Debug theo đúng cài đặt "Bảo vệ màn hình" của người dùng như mọi màn khác. Tab Khác **vẫn có công tắc Bật/Tắt**, nhưng là công tắc của **chính cài đặt thật** (`SecuritySettings.screenProtection`, qua `DebugHooks.screenProtection/setScreenProtection`), nên đổi ở đâu thì Cài đặt › Bảo mật và màn Debug cùng đổi; Debug không giữ trạng thái riêng.
2. Cờ của mỗi cửa sổ = `ODVSecureWindowPolicy.appWide` (cài đặt người dùng, trạng thái cấp process) **hoặc** có màn nhạy cảm đang yêu cầu (`ODVSecureWindow`). Không còn lớp ghi đè nào khác.
3. Muốn chụp hoặc quay màn hình (kể cả màn Debug) để báo lỗi: tắt "Bảo vệ màn hình" ngay trên công tắc ở màn Debug hoặc ở Cài đặt.
4. `MainActivity` đang dừng khi màn Debug hiện nên `ODVApp` không dựng lại để đặt lại cờ; vì vậy công tắc ở Debug ghi cài đặt rồi **áp dụng ngay** `ODVSecureWindowPolicy.setAppWide(shouldSecureWholeWindow(...))`, dùng chung hàm quy tắc với `ODVApp`.

## Đánh đổi
Các màn nhạy cảm (Kết nối, Khóa, nhập PIN, Cài đặt) luôn chặn chụp, kể cả khi người dùng tắt Bảo vệ màn hình; trước đây "Luôn tắt" của Debug cho chụp được chúng. Người phát triển cần ảnh màn Kết nối thì chụp bằng công cụ ngoài app (adb `screencap` bị chặn tương tự) hoặc dùng gallery Foundations ở bản debug.

## Phương án đã cân nhắc
- Giữ ba chế độ nhưng ghi rõ độ ưu tiên: vẫn hai nguồn cho một cờ, dễ nhầm. Không chọn.
- Giữ riêng "Luôn tắt" cho Debug: vẫn ghi đè cài đặt người dùng. Không chọn.

## Hệ quả
- Tích cực: một nguồn sự thật, không còn xung đột, `DebugActivity` được bảo vệ khi người dùng bật.
- Tiêu cực: mất cách chụp màn hình các màn nhạy cảm trong bản debug.
- Rủi ro: không đáng kể (chỉ bản debug).
