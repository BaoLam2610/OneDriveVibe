# Tiến độ MVP1

Kế hoạch: [ke-hoach-mvp1.md](ke-hoach-mvp1.md). Cập nhật file này mỗi khi bắt đầu hoặc xong một bước.

Ký hiệu: `[ ]` chưa làm · `[~]` đang làm · `[x]` xong, chờ kiểm tay · `[v]` đã kiểm tay trên thiết bị

## Đang làm

- **Lát:** 0, Bộ khung app
- **Bước:** code xong, đã review; chờ người dùng build và kiểm tay
- **Ghi chú:** đổi ngôn ngữ ở Lát 0 kiểm qua Cài đặt hệ thống (Android 13+), vì màn Cài đặt làm ở Lát 9. `UiError` làm ở Lát 1.

## Đã xong trước kế hoạch

- [v] Foundations: `:core:designsystem` (token, theme, icon, logo, component board 05, 08–18), gallery debug
- [x] `ODVSystemBars` (icon system bar theo theme app), `ODVScaffold` (AppBar cố định)

## Lát 0: Bộ khung app

- [x] Khai báo thư viện trong `libs.versions.toml` và build file
- [x] `:core:common`: `AppError`, `Result`, dispatcher
- [x] `BaseMviViewModel`
- [x] Khởi động Koin
- [x] Navigation 3: `NavDisplay`, route rỗng
- [x] Đa ngôn ngữ VI/EN
- [x] Tắt sao lưu tự động (CH-04)
- [x] `.claude/docs/tech-stack.md`
- [x] Review (`/ecc:kotlin-review`)
- [ ] Kiểm tay

## Lát 1: Kết nối → Danh sách thư mục gốc

- [ ] `:core:network`: Ktor, token Client Credentials, TK-01 → TK-03, che log
- [ ] `:core:security`: mã hóa config bằng Keystore
- [ ] `:core:domain` / `:core:data`: repository config và drive
- [ ] `:feature:auth`: màn Kết nối
- [ ] `:feature:auth`: màn Thiết lập bảo mật (nhánh tắt)
- [ ] `:feature:browser`: tab Thư mục qua API (TM-07)
- [ ] Điều hướng khởi động
- [ ] Review Kotlin + bảo mật
- [ ] Kiểm tay

## Lát 2: Mã PIN và màn Khóa

- [ ] Argon2id + Keystore
- [ ] Đặt PIN (BM-01 → BM-07)
- [ ] Màn Khóa (KH-01 → KH-06)
- [ ] Tự khóa, xóa token khi khóa
- [ ] Sinh trắc học
- [ ] Review Kotlin + bảo mật
- [ ] Kiểm tay

## Lát 3: Đồng bộ delta và offline

- [ ] Room: schema, DAO
- [ ] Delta sync (DB-01 → DB-05)
- [ ] Tab Thư mục đọc từ Room, sắp xếp, lưới/danh sách, lọc
- [ ] Kéo làm mới, tự đồng bộ, offline, gỡ tệp đã xóa
- [ ] Tìm kiếm
- [ ] TK-05, TK-06
- [ ] Review
- [ ] Kiểm tay

## Lát 4: Thumbnail, bộ nhớ đệm, tab Thư viện

- [ ] Cache (BN-01 → BN-03)
- [ ] Tab Thư viện (TV-01 → TV-06)
- [ ] Review
- [ ] Kiểm tay

## Lát 5: Xem ảnh

- [ ] AN-01 → AN-07
- [ ] Review
- [ ] Kiểm tay

## Lát 6: Xem video

- [ ] 6a: phát và điều khiển cơ bản
- [ ] 6b: phần còn lại
- [ ] Review
- [ ] Kiểm tay

## Lát 7: Xem PDF

- [ ] PD-01 → PD-08
- [ ] Review
- [ ] Kiểm tay

## Lát 8: Xem tiếp / Đọc tiếp

- [ ] DS-02, VD-12, PD-05
- [ ] Review
- [ ] Kiểm tay

## Lát 9: Cài đặt đầy đủ

- [ ] CD-01 → CD-10
- [ ] Review
- [ ] Kiểm tay

## Nhật ký

| Ngày | Việc |
|---|---|
| 2026-10-02 | Duyệt kế hoạch MVP1 |
| 2026-10-02 | Lát 0: dựng khung app (catalog thư viện, common, MVI, Koin, Navigation 3, VI/EN, tắt backup, tech-stack.md); đã review, chờ kiểm tay |
