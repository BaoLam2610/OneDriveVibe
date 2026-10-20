# Làm việc với AI agent

Áp dụng cho Claude Code và các agent khác. Luật ngắn cho agent nằm ở `CLAUDE.md`; file này giải thích cho người.

## 1. Vai trò

| Việc | Người | AI |
|---|---|---|
| Chốt nghiệp vụ, thiết kế, câu chữ | ✔ | Đề xuất, hỏi khi thiếu |
| Kế hoạch lát, chia bước | Duyệt | Soạn |
| Code, test, tài liệu kỹ thuật | Review | Viết |
| Build, chạy trên máy, kiểm tay | ✔ (hoặc CI) | Chỉ khi `CLAUDE.md` cho phép |
| Commit, push, merge | ✔ | Chỉ khi được yêu cầu rõ |

## 2. Một phiên làm việc

1. AI đọc `docs/plan/tien-do.md` (mục "Đang làm") và các file liên quan.
2. Việc lớn: AI lập kế hoạch (bước con, quyết định cần chốt, rủi ro, danh sách kiểm), **chờ duyệt** rồi mới code.
3. AI code theo bước con; sau mỗi bước: cập nhật tiến độ, gợi ý tên commit, liệt kê **rủi ro build** (API chưa chắc, phiên bản cần đối chiếu) và **mục kiểm tay**.
4. Người build (hoặc CI), gửi lại lỗi biên dịch / log nếu có.
5. Lát xong: chạy review (`docs/prompts/review.md`), sửa theo mức, kiểm tay, đóng lát.

## 3. Luật cho AI (đã trả giá ở dự án trước)

- **Không ghi đè file có sẵn bằng Write.** Chỉ Edit; Read lại file trước khi sửa nếu nó có thể đã bị sửa tay. Dự án trước từng mất bản sửa tay của người vì AI viết lại cả file.
- **Không đoán API.** API ít phổ biến hoặc mới: đọc mã nguồn hoặc tài liệu của đúng phiên bản trong `libs.versions.toml` (Context7, sources jar). Những chỗ AI đọc mã nguồn đúng phiên bản thì chạy ngay; đoán theo tài liệu chung thì thường sai.
- **Sửa hàng loạt bằng script:** so danh sách khai báo cấp cao (class, fun, val cấp file) với HEAD, không chỉ kiểm import. Sau khi chạy công cụ định dạng, xem `git diff` (dự án trước từng mất một điều kiện `if` vì căn lề tự động, gây màn đen).
- **Không tự chế** màu, khoảng cách, câu chữ, ngưỡng nghiệp vụ. Thiếu thì hỏi hoặc đánh dấu `[mới]`.
- **Đổi một giá trị thì `grep` toàn repo** (code + tài liệu) và sửa mọi chỗ trong cùng thay đổi.
- **Dời code nguyên văn** khi refactor; không "tiện tay" viết lại thuật toán.
- **Hàm cập nhật đặt `applyX`/`updateX`**, không `setX`.
- **Lỗi khó: log trước, sửa sau** (`vertical-slice.md` mục 5). Không sửa theo giả thuyết chưa phân xử.
- **Báo cáo trung thực:** chưa build thì nói "chưa build"; mục kiểm không làm được thì nói rõ.

## 4. Viết yêu cầu cho AI

Một yêu cầu tốt có:

- **Việc gì**, kèm mã quy tắc và artboard: "Làm CART-05 đến CART-09, artboard C3, C4".
- **Phạm vi**: module, file được sửa; thứ **không** được đụng.
- **Quyết định đã chốt** và câu hỏi còn mở.
- **Cách kiểm**: test nào phải có, mục kiểm tay nào.
- **Lệnh skill/prompt** nếu có (`docs/prompts/add-feature.md`).

Tránh: "làm cho đẹp", "tối ưu", "sửa hết lỗi" không có phạm vi.

## 5. Review do AI

- Review chỉ đọc code (không sửa), báo theo mức CRITICAL/HIGH/MEDIUM/LOW, mỗi mục có file:dòng và kịch bản lỗi cụ thể.
- Lát lớn: chia review theo vùng (data/domain và UI/feature) để mỗi lượt đọc kỹ hơn.
- Người quyết mục nào sửa, mục nào ghi nhận; mục hoãn ghi vào tiến độ kèm lý do.
