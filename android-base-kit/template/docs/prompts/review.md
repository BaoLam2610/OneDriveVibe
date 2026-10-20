# Prompt: Review code

Dùng sau mỗi lát hoặc trước khi merge PR lớn. Có thể chia hai lượt (data/domain và UI/feature) cho lát lớn.

````text
Review code {{TEN_DU_AN}}, CHỈ ĐỌC, không sửa file nào, không chạy build.

## Phạm vi
- <Diff của nhánh hiện tại so với main | PR #... | các module: ...>
- Lượt này tập trung: <toàn bộ | data + domain | UI + feature>
- Bối cảnh: lát <ID>, đặc tả <mục/mã>, ADR liên quan <...>.

## Đọc trước
CLAUDE.md, docs/architecture.md, docs/process/review-checklist.md, các file docs/conventions/ liên quan tới phần thay đổi.

## Kiểm
Đi qua từng mục của docs/process/review-checklist.md. Ưu tiên các loại lỗi sau (đã gây sự cố thật ở dự án trước):
- Hủy coroutine bị nuốt (runCatching, catch Exception); NonCancellable thiếu ở dọn dẹp/lần ghi cuối; timeout bọc code chặn luồng.
- Tác vụ dài gắn vào ViewModel thay vì coordinator; ghi muộn sau reset/đăng xuất (thiếu epoch); single-flight thiếu.
- Dữ liệu nhạy cảm còn trong ViewModel sau khi khóa/đăng xuất; bí mật hoặc e.message trong log.
- Đổi schema Room mà không tăng version; kho mới không đăng ký DataResetter; giá trị đã lưu bị đổi.
- Binding Koin thiếu, cùng kiểu không qualifier, tạo lười mà cần lúc dựng.
- Giá trị composable đọc không phải state; modifier cử chỉ bị thêm/bớt bằng if; key lazy list trùng; state cuộn không hoist; recompose theo từng pixel.
- Nháy/khựng: xóa dữ liệu khi đổi bộ lọc, hiệu ứng toàn cục, spinner không có độ trễ.
- Đổi ngôn ngữ: LocalContext.resources, Locale.getDefault, chuỗi cache trong ViewModel.
- Route thêm trường không có mặc định; process death khôi phục sai.
- Luật tầng: feature import domain.repository hoặc feature khác.

## Cách báo
- Tổng: APPROVE | WARNING | BLOCK, số mục theo mức.
- Mỗi mục: [MỨC] tiêu đề ngắn — file:dòng — kịch bản cụ thể (đầu vào/trạng thái → kết quả sai) — đề xuất sửa (1 đến 3 câu).
- Mức: CRITICAL (lỗ hổng, mất dữ liệu, crash chắc chắn), HIGH (lỗi hành vi chắc chắn xảy ra), MEDIUM (trường hợp biên, khó bảo trì), LOW (gọn hơn).
- Chỉ báo mục đã kiểm bằng cách đọc code thật; mục chưa chắc ghi "cần xác nhận" và cách xác nhận. Không báo lỗi định dạng mà ktlint/detekt đã bắt.
- Cuối: danh sách mục kiểm tay nên làm thêm vì review không chắc được.
````
