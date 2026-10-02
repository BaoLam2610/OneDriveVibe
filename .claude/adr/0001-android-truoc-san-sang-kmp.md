# ADR-0001: Android trước, sẵn sàng KMP

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
MVP1 làm cho Android, MVP2 mở rộng sang iOS và dùng Kotlin Multiplatform (KMP). Cần tránh phải viết lại phần lõi khi sang MVP2, nhưng cũng không muốn trả giá của KMP đầy đủ (Mac, Xcode, build iOS) khi chưa cần.

## Quyết định
Các module dùng chung (`domain`, `data`, `network`, `database`, `security`, `common`) dùng plugin `kotlin("multiplatform")` nhưng **chỉ khai báo `androidTarget()`** ở MVP1. Không dùng API `java.*`/`android.*` trong `commonMain`; phần phụ thuộc nền tảng (player, PDF, lưu bí mật, tác vụ nền) đặt sau interface trong `domain`/`core`.

## Phương án đã cân nhắc
### Android thuần rồi viết lại sang KMP
- Ưu: nhanh nhất ở MVP1, không phải để ý ràng buộc `commonMain`.
- Nhược: phải tách module và đổi thư viện (Room, Ktor, kotlinx-datetime...) khi sang MVP2.
- Lý do không chọn: chi phí chuyển đổi lớn hơn chi phí giữ kỷ luật ngay từ đầu.
### KMP đầy đủ, thêm target iOS ngay từ đầu
- Ưu: phát hiện sớm mọi chỗ không portable.
- Nhược: cần Mac và Xcode, build chậm hơn, phải viết `actual` iOS khi chưa có UI iOS.
- Lý do không chọn: chưa cần iOS ở MVP1.

## Hệ quả
### Tích cực
- MVP2 chỉ cần thêm target `iosArm64`/`iosSimulatorArm64` và viết các `actual`.
### Tiêu cực
- Cấu hình Gradle KMP phức tạp hơn Android thuần.
### Rủi ro
- Lỡ dùng API Android trong `commonMain` thì chỉ lộ khi thêm target iOS. Giảm thiểu bằng danh sách "tránh dùng" ở `tech-stack.md` §3.
