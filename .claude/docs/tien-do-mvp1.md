# Tiến độ MVP1

Kế hoạch: [ke-hoach-mvp1.md](ke-hoach-mvp1.md). Cập nhật file này mỗi khi bắt đầu hoặc xong một bước.

Ký hiệu: `[ ]` chưa làm · `[~]` đang làm · `[x]` xong, chờ kiểm tay · `[v]` đã kiểm tay trên thiết bị

## Đang làm

- **Lát:** 1, Kết nối → Danh sách thư mục gốc (Lát 0 còn chờ kiểm tay)
- **Bước:** code 1a–1c xong, đã review Kotlin + bảo mật và sửa; chờ người dùng build và kiểm tay
- **Ghi chú:** Lát 1 chia 1a (network, security, domain, data), 1b (màn Kết nối, Thiết lập bảo mật), 1c (tab Thư mục, điều hướng khởi động). Công tắc bảo mật chỉ hiển thị trạng thái tắt, Lát 2 mới bật được. Chuỗi đánh dấu [mới] trong strings.xml cần duyệt.

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

- [x] `:core:network`: Ktor, token Client Credentials, TK-01 → TK-03, che log
- [x] `:core:security`: mã hóa config bằng Keystore
- [x] `:core:domain` / `:core:data`: repository config và drive
- [x] `:feature:auth`: màn Kết nối
- [x] `:feature:auth`: màn Thiết lập bảo mật (nhánh tắt)
- [x] `:feature:browser`: tab Thư mục qua API (TM-07)
- [x] Điều hướng khởi động
- [x] Review Kotlin + bảo mật
- [ ] Kiểm tay

## Lát D: Công cụ debug (chen sau Lát 1, ADR-0012)

- [x] ADR-0012, Kermit trong `:core:common`
- [x] `HttpTrafficRecorder` + làm sạch trong `:core:network`; thay `withRetry` bằng `HttpRequestRetry`
- [x] `:tools:debug`: bộ đệm log/API, `DebugActivity` 4 tab (API, Log, Lưu trữ, Khác), nút bọ nổi
- [x] `DebugTools` bản debug/release; một app duy nhất, gallery mở từ tab "Khác"
- [x] Mở rộng: nút bọ trên cùng, màn chi tiết log API, tìm kiếm API/Log, FLAG_SECURE toàn app, log API không che (ADR-0013)
- [ ] Review Kotlin + bảo mật (đợt mở rộng)
- [x] Review Kotlin + bảo mật
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
| 2026-10-02 | Lát 1: dựng network (token single-flight, retry), security (Keystore AES-GCM), domain/data, feature:auth, feature:browser, điều hướng khởi động; chờ review và build |
| 2026-10-02 | Lát D (chen sau Lát 1): công cụ debug `:tools:debug` + Kermit + HttpRequestRetry, sửa Run mở nhầm gallery; ADR-0012 |
| 2026-10-02 | Sửa lỗi: thêm `ApiService` làm lớp cơ sở cho `GraphApi`; gộp gallery vào Debug (một app), bỏ FLAG_SECURE màn Debug, nút X đóng Activity; header Kết nối/Bảo mật cố định |
| 2026-10-02 | Lát D mở rộng: nút bọ luôn trên cùng (kể cả Dialog/BottomSheet), màn chi tiết log API (JSON đẹp, +/-, sao chép, tìm kiếm có đếm), tìm kiếm Log local, tùy chọn FLAG_SECURE toàn app, log API đầy đủ không che; ADR-0013 |
