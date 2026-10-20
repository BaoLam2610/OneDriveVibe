# Quy trình: lát cắt dọc

Một **lát** là một luồng người dùng làm trọn được, đi từ màn hình xuống API và CSDL. Không làm "tầng network trước, tầng data sau".

## 1. Thứ tự lát

- Rủi ro cao làm trước: xác thực và token, mã hóa, đồng bộ dữ liệu, thứ phụ thuộc thiết bị (camera, player). Màn trình bày đơn giản làm sau.
- Thứ tự nằm ở **một** bảng trong `docs/plan/ke-hoach.md`. ID lát không đổi; lát chen ngang lấy ID mới (ví dụ `AUTH2`, `D` cho công cụ debug), **không đánh số lại** lát khác.
- Lát 0 là bootstrap (khung app, chưa có tính năng); lát kế tiếp là công cụ debug (`docs/debug-tools.md`).

## 2. Các bước của một lát

| # | Bước | Ra | Ai |
|---|---|---|---|
| 1 | **Đặc tả**: thêm/sửa mục trong `docs/spec/` theo mẫu, mỗi quy tắc có mã `XX-NN` | Đặc tả đã duyệt | BA/PO + dev |
| 2 | **Thiết kế**: artboard và mô tả trong `docs/design/`; chuỗi, token mới | Thiết kế đã duyệt | Designer |
| 3 | **Kế hoạch lát**: chia bước con (a, b, c), quyết định cần chốt (Q1, Q2...), ADR cần viết, rủi ro build, danh sách kiểm tay | Mục lát trong `ke-hoach.md`, đã duyệt | Dev (+ AI) |
| 4 | **ADR** (nếu có quyết định kiến trúc): viết `proposed`; quyết định có rủi ro thì làm spike/đo trước | ADR | Dev |
| 5 | **Code theo bước con**, mỗi bước một commit build được. Test ViewModel đi cùng code | PR nhỏ | Dev (+ AI) |
| 6 | **Review** theo `review-checklist.md`; lát lớn chạy thêm review của AI (`docs/prompts/review.md`) | Mục cần sửa | Reviewer |
| 7 | **Kiểm tay** trên thiết bị theo danh sách của lát + `manual-test-checklist.md` | `[v]` trong tiến độ | Dev/QA |
| 8 | **Đóng lát**: cập nhật tiến độ, đặc tả, thiết kế khớp với cái đã làm; ADR `accepted` | | Dev |

- Bước 1 và 2 chưa xong thì không code phần giao diện; có thể làm trước phần domain/data nếu đặc tả hành vi đã chốt.
- Thiếu giá trị (màu, câu chữ, ngưỡng) thì **hỏi**, ghi vào mục "Điểm còn mở" của đặc tả; không tự chế.
- Phát hiện chênh lệch giữa thiết kế và luật chung (vùng chạm < 48dp) thì ghi vào "Chênh lệch đã biết" của tài liệu UI.

## 3. Trạng thái trong file tiến độ

`[ ]` chưa làm · `[~]` đang làm · `[x]` code xong, chờ kiểm · `[v]` đã kiểm trên thiết bị.

"Xong" chỉ là `[v]`. Mục kiểm không làm được thì ghi lý do và cách bù (đọc code, test, để lát sau).

## 4. Refactor

- Refactor thuần (không đổi hành vi, không đổi schema) đi nhánh và PR riêng, không trộn với tính năng hay sửa lỗi.
- Có kế hoạch phase (R0, R1...), mỗi phase build được độc lập và có danh sách kiểm tay riêng.
- Trước khi bắt đầu: một mốc đang chạy đúng (commit đã kiểm tay hoặc CI xanh).
- Dời code **nguyên văn**, không viết lại thuật toán trong cùng bước. Đổi tên/gói bằng script thì so khai báo cấp cao với HEAD.
- Giá trị đã lưu trên máy chỉ được chuyển chỗ; kiểm bằng `git diff`.

## 5. Sửa lỗi khó

Áp dụng cho lỗi phụ thuộc thiết bị, khó tái hiện, hoặc đọc code không chốt được nguyên nhân.

1. **Ghi triệu chứng** chính xác: máy, phiên bản Android, bản build (commit), các bước, log.
2. **Liệt kê giả thuyết** H1, H2, H3... Mỗi giả thuyết ghi: nếu đúng thì log/hành vi sẽ thấy gì.
3. **B0: chỉ thêm log** để phân xử giữa các giả thuyết (log chẩn đoán hoặc log truy vết tạm có tiền tố riêng, xem `conventions/logging.md`). Không sửa gì khác.
4. Đọc log, gạch giả thuyết sai. Nếu vẫn chưa rõ thì lặp bước 3.
5. **Sửa** theo giả thuyết còn lại, mỗi hướng sửa một bước (B1, B2...) có kiểm tay riêng.
6. Gỡ log tạm trong commit riêng; giữ log chẩn đoán có ích lâu dài.
7. Ghi kết luận (nguyên nhân thật, hướng đã loại và vì sao) vào PR hoặc ADR nếu ảnh hưởng kiến trúc.

Người báo "vẫn bị" thì trước hết xác nhận **bản build có bản sửa** (commit hash ở tab Khác của màn Debug).

## 6. Đo trước khi quyết

Quyết định hiệu năng (tải trước, cache, cỡ trang, số luồng):

- Thêm công tắc A/B ở bản debug (`debug-tools.md` mục 2.2) và log số đo.
- Đo đủ mẫu (≥ 15 lần mỗi nhánh), so **trung vị** (và p90 nếu đuôi dài quan trọng).
- Ghi số đo, máy đo và ngưỡng giữ/bỏ vào ADR; số đo quyết định hằng số thì ghi vào KDoc của hằng số.
