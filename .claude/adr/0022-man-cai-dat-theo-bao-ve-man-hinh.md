# ADR-0022: Màn Cài đặt chụp được khi tắt "Bảo vệ màn hình"

**Ngày**: 2026-10-08
**Trạng thái**: accepted (thay phần "Cài đặt luôn chặn chụp" trong CH-05, KN-10 áp dụng cho Cài đặt, và mục Đánh đổi của ADR-0020)
**Người quyết định**: LamBao

## Bối cảnh
CH-05 cho màn Cài đặt luôn đặt `FLAG_SECURE` vì màn có nhóm Bảo mật và Kết nối (Tenant ID, Client ID đã che bớt). Sau khi thêm "Bảo vệ màn hình" (CD-12), người dùng tắt công tắc nhưng vẫn không chụp được màn Cài đặt trong khi `DebugActivity` chụp được, dễ hiểu nhầm là lỗi.

## Quyết định
1. Màn Cài đặt **không** tự yêu cầu `FLAG_SECURE` (bỏ `ODVSecureWindow()` ở `ODVSettingsScreen`). Cờ của nó chỉ theo "Bảo vệ màn hình" (`ODVSecureWindowPolicy.appWide`) như mọi màn thường.
2. Các màn **Kết nối, Khóa, Thiết lập bảo mật, nhập PIN** (kể cả màn PIN mở từ Cài đặt) vẫn luôn chặn chụp.

## Đánh đổi
Khi tắt "Bảo vệ màn hình", ảnh chụp màn Cài đặt lộ Tenant ID và Client ID đã che bớt (`a1b2••••9f0e`), UPN và thời điểm đồng bộ. Client Secret không bao giờ hiện ở màn này. Người dùng đã chấp nhận. Ảnh app ở danh sách app gần đây của màn Cài đặt chỉ được ẩn khi bật PIN (Android 13 trở lên) hoặc bật "Bảo vệ màn hình".

## Phương án đã cân nhắc
- Chỉ chặn khi nhóm Bảo mật hoặc Kết nối đang hiện: cả hai nằm trong một màn cuộn, khó làm chính xác. Không chọn.
- Giữ Cài đặt luôn chặn, chỉ ghi chú ở mô tả công tắc: không đáp ứng nhu cầu chụp màn Cài đặt. Không chọn.

## Hệ quả
- Tích cực: công tắc "Bảo vệ màn hình" nay quyết định đúng với màn Cài đặt; hành vi nhất quán với màn Debug.
- Tiêu cực: yếu hơn một chút về riêng tư khi công tắc tắt.
- Rủi ro: thấp, thông tin lộ đã được che bớt.
