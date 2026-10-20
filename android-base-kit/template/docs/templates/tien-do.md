# Tiến độ: {{TEN_DU_AN}} <MVP>

> **Mẫu.** Chép sang `docs/plan/tien-do.md`. Đọc đầu mỗi phiên làm việc. Cập nhật khi bắt đầu hoặc xong một bước.

Kế hoạch: [ke-hoach.md](ke-hoach.md).

Ký hiệu: `[ ]` chưa làm · `[~]` đang làm · `[x]` code xong, chờ kiểm · `[v]` đã kiểm trên thiết bị. **Chỉ `[v]` là xong.**

## Đang làm

<!-- Tối đa 5 dòng. Mỗi dòng: ID lát/bước, trạng thái, việc kế tiếp, người. Xong thì xóa khỏi đây. -->

- **`<ID>b`** `[~]`: <việc>. Kế tiếp: <...>. (<người>)

## Tồn đọng đã biết

<!-- Việc hoãn có chủ ý, mục review chưa sửa, mục kiểm tay không làm được. Mỗi mục có lý do và nơi xử lý. -->

| Mục | Mức | Lý do hoãn | Xử lý ở |
|---|---|---|---|
| <...> | MEDIUM | <...> | <lát / chưa xếp> |

## `L0`: Bộ khung app

- [ ] Gradle, version catalog, `build-logic`
- [ ] `:core:common`, `:core:domain`, `:core:ui`, `:core:testing`
- [ ] `:core:designsystem` (token, theme)
- [ ] `:core:network`, `:core:database`, `:core:data`
- [ ] `:app`: Koin, Navigation 3, edge-to-edge, đa ngôn ngữ, tắt sao lưu
- [ ] Lint, detekt, CI
- [ ] `KoinVerifyTest`, test kiến trúc
- [ ] Kiểm tay

## `<ID>`: <Tên lát>

- [ ] `<ID>a` <...>
- [ ] `<ID>b` <...>
- [ ] Test ViewModel
- [ ] Review
- [ ] Kiểm tay

## Nhật ký

<!-- Mới nhất ở trên. MỖI DÒNG TỐI ĐA 3 CÂU: làm gì, trạng thái, link PR/commit. Chi tiết (nguyên nhân, danh sách kiểm) nằm ở PR. -->

| Ngày | Việc |
|---|---|
| {{NGAY}} | Khởi tạo tài liệu dự án. |
