# Prompt: Thêm một feature (một lát cắt dọc)

Dùng khi đặc tả và thiết kế của feature đã có trong `docs/spec/` và `docs/design/`. Thay các chỗ `<...>`.

````text
Làm lát <ID lát> của {{TEN_DU_AN}}: <tên tính năng>.

## Đầu vào
- Đặc tả: docs/spec/<file>.md mục <...>, quy tắc <NHOM-01> đến <NHOM-NN>.
- Thiết kế: docs/design/thiet-ke-ui.md mục <...>, artboard <X1> đến <Xn>.
- API: <endpoint, tài liệu, ví dụ response (đã che dữ liệu thật)>.
- Quyết định đã chốt: <Q1: ...; Q2: ...>.
- Không làm trong lát này: <...>.

## Đọc trước
CLAUDE.md, docs/architecture.md, docs/conventions/ (mvi, compose-ui, data-layer, error-handling, testing, resources-i18n), docs/process/vertical-slice.md, và code của feature gần nhất để làm theo cùng kiểu (<:feature:sample hoặc feature khác>).

## Bước 1: Kế hoạch (KHÔNG viết code)
Trả về:
1. Danh sách bước con (<ID>a domain + data, <ID>b màn + ViewModel + test, <ID>c lỗi/offline/chuỗi/hoàn thiện), mỗi bước build được.
2. File sẽ tạo/sửa theo module.
3. Thay đổi schema Room (nếu có): bảng/cột, version mới.
4. UseCase mới (tên theo nghiệp vụ), interface domain mới (repository / platform / settings / hook).
5. Kho dữ liệu mới cần DataResetter.
6. Chuỗi mới; chuỗi chưa có trong thiết kế đánh dấu [mới].
7. Chỗ thiết kế/đặc tả thiếu hoặc mâu thuẫn → câu hỏi.
8. Rủi ro (API mới, phụ thuộc thiết bị) và ADR cần viết.
9. Danh sách kiểm tay của lát (cụ thể, theo mã quy tắc) + mục áp dụng của docs/process/manual-test-checklist.md.
DỪNG chờ duyệt.

## Bước 2: Làm từng bước con
Sau mỗi bước con, DỪNG và báo:
- File đã tạo/sửa.
- Test đã viết (ViewModel: trạng thái đầu, mỗi Intent, đường lỗi → UiError, chống bấm đôi, quy tắc có mã).
- Rủi ro build, lệnh kiểm.
- Cập nhật docs/plan/tien-do.md (trạng thái [x], một dòng nhật ký ≤ 3 câu).
- Tên commit gợi ý.

## Luật
- Feature chỉ gọi UseCase và interface platform/settings; không import domain.repository; không phụ thuộc feature khác.
- State lưu UiError, dialog nằm trong State; Effect chỉ thu ở Screen.
- Đổi schema thì tăng version + ghi Lịch sử version + schema JSON.
- Không hex, không dp tự đặt, không chuỗi cứng, vùng chạm ≥ 48dp, trạng thái tải sau 200 ms.
- Không ghi đè file có sẵn bằng Write; không commit; không tự chế giá trị thiết kế.
- Đổi giá trị nghiệp vụ hoặc UI thì sửa đặc tả/thiết kế và grep mọi chỗ nhắc tới.
- Hết lát: tự rà theo docs/process/review-checklist.md và liệt kê mục chưa đạt.
````
