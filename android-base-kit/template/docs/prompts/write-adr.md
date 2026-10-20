# Prompt: Viết ADR

Dùng khi có một quyết định ảnh hưởng kiến trúc (thư viện mới, đổi luật tầng, đổi cách lưu dữ liệu, khác biệt debug/release, thay một ADR cũ).

````text
Viết ADR cho {{TEN_DU_AN}} về: <quyết định>.

## Bối cảnh tôi biết
- Vấn đề: <...>
- Ràng buộc: <thời gian, đội, nền tảng, hiệu năng>
- Phương án đang cân nhắc: <A, B, C>
- Thiên về: <A, vì ...> (hoặc "chưa biết")
- ADR liên quan có thể bị thay: <ADR-NNNN mục ...>

## Làm
1. Đọc docs/adr/README.md, docs/adr/template.md và các ADR liên quan.
2. Nếu thiếu thông tin để so sánh phương án (hiệu năng, giấy phép, mức bảo trì, tương thích phiên bản): tra cứu (tài liệu chính thức, mã nguồn, release notes) và ghi nguồn.
3. Soạn ADR theo template, số kế tiếp, tên file NNNN-tieu-de-khong-dau.md:
   - Trạng thái `proposed`.
   - Mỗi phương án có ưu, nhược, lý do không chọn.
   - Mục "Cách xác nhận" có thước đo, ngưỡng giữ/bỏ, cách đo (công tắc A/B ở bản debug nếu là quyết định hiệu năng).
   - Nếu thay một phần ADR cũ: ghi rõ phần nào, và đề xuất dòng "Bị thay bởi ADR-NNNN (mục ...)" thêm vào ADR cũ (không sửa nội dung khác).
4. Liệt kê chỗ cần sửa theo: docs/adr/README.md, CLAUDE.md, docs/tech-stack.md, docs/conventions/*, comment trong code.
5. DỪNG chờ tôi duyệt nội dung trước khi sửa các file ở bước 4.
````
