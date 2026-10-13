# ADR-0031: Bản debug cho các màn nhạy cảm theo "Bảo vệ màn hình"

**Ngày**: 2026-10-13
**Trạng thái**: accepted (bổ sung ADR-0022 và ADR-0020, chỉ cho bản debug)
**Người quyết định**: LamBao

## Bối cảnh
ADR-0022 giữ các màn Kết nối, Khóa, Thiết lập bảo mật, nhập PIN, Cập nhật Client Secret và Thêm tài khoản **luôn** đặt `FLAG_SECURE` (`ODVSecureWindow()`), bất kể công tắc "Bảo vệ màn hình". Khi phát triển, người dùng cần chụp hoặc quay các màn này để viết tài liệu, vẽ thiết kế và báo lỗi; tắt công tắc ở Cài đặt hoặc ở tab Khác của màn Debug vẫn không chụp được, dễ hiểu nhầm là công tắc hỏng.

## Quyết định
1. `ODVSecureWindowPolicy` có thêm cờ `sensitiveScreensFollowAppWide` (mặc định `false`). Khi `true`, `FLAG_SECURE` của một cửa sổ chỉ theo `appWide`; số màn đang gọi `ODVSecureWindow()` không còn ép cờ.
2. Chỉ `DebugTools.install` (source set `debug` của `:androidApp`) đặt cờ này `true`. Source set `release` không có dòng đó nên **bản release giữ nguyên ADR-0022**: các màn nhạy cảm luôn chặn chụp.
3. `appWide` vẫn là `screenProtection || (PIN bật && Android 12 trở xuống)`, nên người dùng debug đang bật PIN trên Android 12 trở xuống vẫn bị chặn (không có API ẩn riêng ảnh ở danh sách app gần đây).

## Đánh đổi
Ở bản debug, khi tắt "Bảo vệ màn hình", ảnh chụp các màn nhạy cảm có thể lộ Client Secret đang gõ. Chấp nhận vì bản debug chỉ chạy trên máy phát triển, và người dùng chủ động tắt công tắc; công tắc bật là mặc định.

## Phương án đã cân nhắc
- Bỏ `ODVSecureWindow()` ở các màn này cho mọi bản dựng: làm yếu riêng tư của bản phát hành. Không chọn.
- Thêm công tắc riêng trong màn Debug: thêm trạng thái thứ hai dễ lệch với công tắc thật (ADR-0020 đã bỏ một ghi đè riêng vì lý do đó). Không chọn.

## Hệ quả
- Tích cực: một công tắc duy nhất quyết định chụp được hay không ở bản debug; release không đổi.
- Tiêu cực: hành vi khác nhau giữa debug và release; khi kiểm tay riêng tư phải thử trên bản release.
- Rủi ro: thấp; cờ chỉ được đặt trong source set `debug`.
