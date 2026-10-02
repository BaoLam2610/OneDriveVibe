# ADR-0010: Chia module, tách nhỏ phần core

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Cần ranh giới rõ giữa phần dùng chung (sẽ thành KMP) và phần chỉ có ở Android, và cần ép quy tắc phụ thuộc của Clean Architecture bằng cấu trúc module.

## Quyết định
Foundation dựng `:app`, `:build-logic` và các module core: `common`, `domain`, `data`, `network`, `database`, `security`, `designsystem`. Module `:feature:*` (auth, browser, library, player, imageviewer, pdfviewer, settings) **tạo khi bắt đầu làm feature đó**. Quy tắc phụ thuộc: `domain` không phụ thuộc module khác (trừ `common`); `feature` → `domain`, không gọi thẳng `data`/`network`.

## Phương án đã cân nhắc
### Một module `app`
- Ưu: đơn giản, build nhanh ở đầu.
- Nhược: không có ranh giới ép buộc; tách KMP sau rất tốn công.
- Lý do không chọn: mâu thuẫn ADR-0001.
### Gộp `network`, `database`, `security` vào `core:data`
- Ưu: ít module hơn.
- Nhược: khó tách `expect/actual` cho iOS, trộn các lý do thay đổi.
- Lý do không chọn: giữ tách riêng.
### Tạo đủ cả module feature ngay từ foundation
- Nhược: nhiều module rỗng không dùng tới.
- Lý do không chọn: tạo khi cần.

## Hệ quả
### Tích cực
- Vi phạm quy tắc phụ thuộc bị chặn bởi cấu trúc.
### Tiêu cực
- Nhiều file build, cần convention plugin để đỡ lặp.
### Rủi ro
- Chia quá nhỏ làm chậm build. Nếu thấy vậy thì gộp lại và ghi ADR thay thế.
