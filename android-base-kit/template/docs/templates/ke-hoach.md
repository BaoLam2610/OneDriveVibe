# Kế hoạch triển khai: {{TEN_DU_AN}} <MVP>

> **Mẫu.** Chép sang `docs/plan/ke-hoach.md`. File này chỉ đổi khi **kế hoạch** đổi; tiến độ hằng ngày ghi ở `tien-do.md`.

- **Trạng thái:** <nháp / đã duyệt {{NGAY}}>
- **Tiến độ thực tế:** [tien-do.md](tien-do.md)

## 1. Nguyên tắc

- Làm theo **lát cắt dọc** (`docs/process/vertical-slice.md`); rủi ro cao làm trước.
- Mỗi lát kết thúc bằng: test ViewModel, review theo checklist, danh sách kiểm tay trên thiết bị.
- ID lát **không đổi**; lát chen ngang lấy ID mới, không đánh số lại.

## 2. Thứ tự lát (bảng duy nhất)

| Thứ tự | ID | Nội dung | ADR | Module chính | Trạng thái |
|---|---|---|---|---|---|
| 1 | `L0` | Bộ khung app (bootstrap) | 0001 đến 0011 | tất cả core, app | |
| 2 | `D` | Công cụ debug | 0011 | tools:debug | |
| 3 | `AUTH` | <Đăng nhập → màn chính rỗng> | | network, data, feature:auth | |
| 4 | `<ID>` | <...> | | | |

Đổi thứ tự thì chỉ sửa cột "Thứ tự" của bảng này, ghi một dòng vào mục 5.

## 3. Các lát

### `L0`: Bộ khung app

- Dựng khung theo `docs/architecture.md`, `docs/tech-stack.md`, `docs/debug-tools.md`.
- **Kiểm tay:** app mở vào màn trống; đổi ngôn ngữ, đổi theme; bản release build được, không có nút bọ.

### `<ID>`: <Tên lát>

- **Đặc tả:** <mục, mã quy tắc `NHOM-01` đến `NHOM-NN`>
- **Thiết kế:** <mục, artboard>
- **Quyết định đã chốt:** Q1 <...>; Q2 <...>
- **Bước con:**
  - `<ID>a`: domain + data (<UseCase, repository, bảng Room: có tăng version không>)
  - `<ID>b`: màn hình + ViewModel + test
  - `<ID>c`: trạng thái lỗi, offline, chuỗi VI/EN, review
- **ADR cần viết:** <...>
- **Rủi ro:** <API mới dùng lần đầu, phụ thuộc thiết bị, đổi schema>
- **Không làm ở lát này (hoãn có chủ ý):** <...>
- **Kiểm tay:** <danh sách cụ thể của lát> + các mục áp dụng của `manual-test-checklist.md`.

## 4. Thư viện chưa có trong ADR (đã duyệt)

| Việc | Chọn | Ghi chú |
|---|---|---|
| | | |

## 5. Lịch sử thay đổi kế hoạch

| Ngày | Thay đổi | Lý do |
|---|---|---|
| {{NGAY}} | Bản đầu | |
