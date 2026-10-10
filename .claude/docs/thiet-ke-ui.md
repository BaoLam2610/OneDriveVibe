# Đặc tả giao diện OneDriveVibe (bàn giao thiết kế cho code)

- **Áp dụng cho:** app Android OneDriveVibe (Kotlin, Jetpack Compose). Tên app đổi từ "ODV" thành OneDriveVibe (chốt 2026-10-07); tiền tố code UI vẫn là `ODV`, và tên canvas/file nguồn "ODV Foundations", "ODV Settings" giữ nguyên.
- **Nguồn thiết kế:** canvas "ODV Foundations" trên Claude Design (bản v22, 02/10/2026). Có 5 page: Foundations, Khởi động, Danh sách, Màn xem, Cài đặt. Lát 8 (2026-10-10) cần thêm page **Short** và sửa page Danh sách, Cài đặt (danh sách việc ở **mục 11**).
- **Phân công (chốt 2026-10-10):** tài liệu này được cập nhật ở topic tài liệu; việc vẽ trên Claude Design làm ở **topic riêng**, theo đúng mục 11 và các mục được dẫn tới, không tự chế giá trị.
- **Tài liệu đi kèm:**
    - `docs/dac-ta-nghiep-vu.md`: nghiệp vụ, mã KN, BM, KH, DH, TM, TV, DS, SV, VD, AN, PD, CD…
    - `docs/odv-tokens.json`: toàn bộ token ở dạng dữ liệu, chuẩn W3C Design Tokens.
- **Khung thiết kế:** điện thoại 390 × 844dp, hướng dọc. Video ngang dùng khung 844 × 390dp.

---

## 0. Cách dùng tài liệu này

1. **Thứ tự ưu tiên khi có mâu thuẫn:**
    - Đặc tả nghiệp vụ quyết định *cái gì xảy ra*.
    - Tài liệu này quyết định *trông thế nào, kích thước bao nhiêu, câu chữ ra sao*.
    - Artboard trên canvas là *ảnh tham chiếu* để so.
    - Tài liệu này thay cho các file `foundations-0x-*.md` cũ của Design System ở những chỗ khác nhau (xem mục 9).
2. **Mỗi màn có một mã** ở mục 5, kèm tên artboard. Khi làm một màn:
    - Mở artboard đó trên canvas.
    - Dựng bằng token và component ở mục 2 đến 4.
    - So ảnh theo mục 10.
3. **Không tự chế giá trị.** Khi cần màu, cỡ chữ, khoảng cách hay câu chữ chưa có trong tài liệu, ghi lại và hỏi người thiết kế. Không chọn giá trị "gần giống". Giá trị mới phải được thêm vào thiết kế và tài liệu này trước khi dùng.
4. **Mã trùng giữa các page.** Một số mã lặp lại giữa các page, ví dụ D1 (Danh sách) và D1 (Cài đặt), hoặc P1 (PDF) và P1 (PIN trong Cài đặt). Luôn ghi kèm tên page: "Cài đặt · D1". Mã mới của Lát 8 dùng tiền tố riêng để tránh trùng: **N** (thanh điều hướng đáy) và **SH** (Short), vì **S** đã dùng cho Splash và Secret.

---

## 1. Quy tắc bắt buộc khi dựng

1. **Màu:**
    - Màn thường chỉ lấy màu từ token `color.light.*` / `color.dark.*` (mục 2.1).
    - Màn xem và tab Short lấy từ `media.*` (mục 2.2).
    - Không viết mã hex trong màn hình.
2. **Theme:** có 2 theme Sáng/Tối, theo mục Cài đặt › Giao diện (Theo hệ thống / Sáng / Tối).
    - Màn xem video, ảnh, PDF và **tab Short** **luôn nền đen chữ trắng** bất kể theme (SV-10). Ở tab Short, thanh điều hướng đáy cũng dùng bản nền tối N4 (mục 4.7).
    - Riêng Dialog và Bottom sheet mở trong màn xem vẫn theo theme.
3. **Chữ:**
    - Chỉ dùng 21 style ở mục 2.3, đơn vị `sp`.
    - Tắt font padding và căn chữ giữa chiều cao dòng (Compose: `includeFontPadding = false`, `LineHeightStyle(Center, Trim.None)`). Không có hai thiết lập này, chữ có dấu lệch 1–3dp so với thiết kế.
4. **Font:**
    - Nhúng Be Vietnam Pro (400, 500, 600, 700, 800) và JetBrains Mono (400, 500) vào `res/font`. Cả hai lấy từ Google Fonts, giấy phép OFL.
    - Không dùng Roboto thay thế.
5. **Kích thước:** lưới 4dp. Mọi khoảng cách, kích thước và bo góc lấy từ mục 2.4 đến 2.6.
6. **Vùng chạm:**
    - Mọi thứ bấm được có vùng chạm ≥ 48dp, kể cả khi hình nhỏ hơn (Chip 36dp, nút X trên thẻ Xem tiếp 28dp).
    - Các vùng chạm cách nhau ≥ 8dp.
7. **Edge-to-edge:**
    - Nội dung vẽ dưới thanh trạng thái và thanh điều hướng, padding theo WindowInsets.
    - Thanh hệ thống có nền `bg` ở màn thường và `media.background` ở màn xem.
    - Video ngang ẩn thanh hệ thống (VD-07).
    - **Thanh điều hướng đáy** (N1) cộng thêm phần lề dưới của thanh điều hướng hệ thống vào chiều cao, nền thanh kéo dài xuống sát cạnh màn.
    - **Tab Short** (đã vẽ ở page Short, 2026-10-10): video vẽ tràn dưới thanh trạng thái, icon thanh trạng thái màu sáng như màn xem.
8. **Icon:**
    - Chỉ dùng bộ icon ODV ở mục 3: 24 × 24, nét 2dp, đầu tròn.
    - Không dùng Material Icons, không dùng emoji.
    - Tô màu bằng tint theo token.
9. **Viết hoa kiểu câu:**
    - Không viết hoa toàn bộ.
    - Câu chữ lấy nguyên văn từ thiết kế. Bảng câu chữ chính ở mục 5 và 7.
10. **Số và ngày theo ngôn ngữ đang dùng (CD-10; đổi từ "vi-VN cố định" ngày 2026-10-07):** bản tiếng Việt ra `12.480`, `4,9 MB`, `23/05/2026`, `1:26:02`; bản English ra `12,480`, `4.9 MB`, `5/23/2026`. Trong Compose lấy locale bằng `odvLocale()` (`:core:designsystem`), không viết `Locale.forLanguageTag("vi-VN")`. Số đổi liên tục hoặc cần thẳng cột dùng JetBrains Mono.
11. **Trạng thái:** mỗi component có đủ trạng thái ở mục 4: mặc định, nhấn, focus, tắt, lỗi, đang tải.
12. **Riêng tư:** các màn sau chặn chụp màn hình (`FLAG_SECURE`) và ẩn nội dung trong danh sách app gần đây (KN-10, KH-04, CH-05):
    - Kết nối (kể cả hộp thoại K6), Khóa, nhập PIN.
    - Thiết lập bảo mật (cùng nhóm màn nhập PIN, kể cả màn PIN mở từ Cài đặt).
    - Cài đặt **không** nằm trong danh sách luôn chặn (đổi 2026-10-08, ADR-0022): chụp được khi tắt "Bảo vệ màn hình". Đánh đổi: Tenant ID, Client ID đã che bớt lộ trong ảnh chụp. Từ Lát 8 Cài đặt là tab, cờ được áp lại mỗi lần đổi tab (DH-08).
    - Khi bật Cài đặt › Bảo mật › **Bảo vệ màn hình** (CD-12, mặc định tắt): **mọi màn** của app chặn chụp/quay màn hình và ẩn nội dung ở danh sách app gần đây, kể cả hộp thoại và màn Debug (ADR-0020). Cài đặt này không phụ thuộc có bật PIN hay không.

---

## 2. Token

Số liệu đầy đủ, dạng máy đọc được, nằm trong `docs/odv-tokens.json`. Các bảng dưới đây sinh từ cùng nguồn.

### 2.1 Màu theo theme

| Token | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `bg` | `#f4f5f7` | `#0c0e12` | Nền màn hình (Danh sách, Cài đặt, Kết nối, Khóa) |
| `surface` | `#ffffff` | `#15181e` | Thẻ, nhóm Cài đặt, Dialog, Sheet, ô nhập, nền thanh điều hướng đáy |
| `surface-2` | `#eceef2` | `#1e2229` | Nền phụ: phím PIN, chip lọc, hàng đang nhấn |
| `line` | `#e1e4ea` | `#2a2f38` | Viền thẻ, kẻ giữa các hàng, kẻ trên thanh điều hướng đáy |
| `line-strong` | `#838b99` | `#687183` | Viền ô nhập, Radio, Switch tắt, nút secondary |
| `ink` | `#101217` | `#f2f4f7` | Chữ chính, icon, nhãn mục đang chọn trên thanh đáy |
| `ink-muted` | `#565e6d` | `#a3abb8` | Chữ phụ, mô tả, giá trị Cài đặt, icon và nhãn mục chưa chọn trên thanh đáy |
| `ink-faint` | `#9aa1ad` | `#5c6472` | Chữ rất phụ: đường dẫn kết quả tìm, mũi tên Breadcrumb |
| `volt` | `#c5f23a` | `#ccf55b` | Mảng nhấn: nút chính, Switch bật, chấm PIN, phần đã xem |
| `volt-pressed` | `#b0dc22` | `#dcfa86` | Nút chính khi nhấn |
| `on-volt` | `#101217` | `#101217` | Chữ và icon trên volt |
| `volt-soft` | `#eefac8` | `#222c0b` | Nền chip chọn, ô thư mục, phím PIN đang nhấn, viên chọn trên thanh đáy |
| `volt-text` | `#456100` | `#ccf55b` | Chữ/viền nhấn: nút ghost, Radio chọn, tiêu đề nhóm Cài đặt, focus ring, icon mục đang chọn trên thanh đáy |
| `kind-video` | `#d6336a` | `#ff6d9a` | Nhận diện video (chấm chip) |
| `kind-video-soft` | `#fde7ef` | `#3a1322` | Nền nhận diện video |
| `kind-photo` | `#167cc9` | `#56c2ff` | Nhận diện ảnh |
| `kind-photo-soft` | `#e2f1fc` | `#0e2a3d` | Nền nhận diện ảnh |
| `kind-pdf` | `#a86400` | `#ffb938` | Nhận diện PDF, tiến độ đọc |
| `kind-pdf-soft` | `#fdf0dc` | `#35260b` | Nền thumbnail PDF |
| `danger` | `#c8242f` | `#ff7276` | Lỗi, hành động nguy hiểm, chấm nhắc đã hết hạn |
| `danger-soft` | `#fde8e9` | `#3a1416` | Nền nút danger tonal, icon lỗi |
| `warning` | `#935600` | `#ffc24d` | Cảnh báo, chấm nhắc sắp hết hạn |
| `warning-soft` | `#fff2d9` | `#33250a` | Nền Banner cảnh báo |
| `success` | `#12734b` | `#4cd99a` | Thành công |
| `success-soft` | `#e2f5eb` | `#0f2e20` | Nền Banner thành công |
| `inverse-surface` | `#20242c` | `#f2f4f7` | Nền Snackbar, bong bóng cuộn nhanh |
| `inverse-ink` | `#f2f4f7` | `#101217` | Chữ trên inverse-surface |
| `inverse-accent` | `#ccf55b` | `#456100` | Nút hành động trên Snackbar |
| `media-bg` | `#000000` | `#000000` | Nền màn xem và tab Short (luôn đen) |

**Tương phản đã kiểm tra ở cả hai theme:**
- Chữ ≥ 4,5:1.
- Viền điều khiển và icon mang nghĩa ≥ 3:1.
- `volt` không dùng làm màu chữ trên nền sáng. Chữ nhấn dùng `volt-text`.

### 2.2 Màu màn xem (không đổi theo theme)

Áp dụng cho màn xem video, ảnh, PDF và tab Short.

| Token | Giá trị | Dùng cho |
|---|---|---|
| `media.background` | `#000000` | Nền màn xem video/ảnh/PDF và tab Short, không đổi theo theme; nền thanh đáy bản N4 |
| `media.on-media` | `#ffffff` | Chữ, icon trên media; nhãn mục đang chọn trên N4 |
| `media.on-media-muted` | `#ffffffcc` | Chữ phụ trên media; tên tệp ở tab Short |
| `media.on-media-faint` | `#ffffff99` | Dòng tên tệp và meta nhỏ trên media; icon và nhãn mục chưa chọn trên N4; dòng thư mục ở tab Short |
| `media.scrim` | `#00000099` | Sau Dialog, Sheet, bảng bên phải; điểm đầu dải gradient sau tên tệp ở tab Short |
| `media.scrim-controls` | `#00000080` | Lớp phủ khi thanh điều khiển video hiện |
| `media.scrim-strong` | `#000000b3` | Sau thẻ Tiếp theo sau 5 giây |
| `media.pill` | `#000000b3` | Nền viên thuốc: 2,4x, Cắt đầy, 12 / 248; nền HUD; nền vòng tạm dừng và vòng xáo lại ở tab Short |
| `media.toolbar` | `#000000cc` | Thanh trên và dưới của PDF |
| `media.track` | `#ffffff33` | Rãnh thanh tua, rãnh vòng đếm, rãnh thanh tiến độ tab Short |
| `media.buffer` | `#ffffff66` | Phần đã tải trước; viền nút trên media |
| `media.ripple` | `#ffffff26` | Gợn chạm đúp, nền nút mở khóa |
| `media.icon-well` | `#ffffff1f` | Vòng tròn sau icon lỗi trên media; kẻ trên thanh đáy bản N4 |
| `media.card` | `#1b1d22` | Thẻ trên media: Tiếp theo, Mất mạng |
| `media.pdf-canvas` | `#2a2c31` | Nền sau trang PDF |
| `media.accent` | `#c5f23a` | Nút phát, phần đã xem, phần đã xem ở thanh tiến độ tab Short |
| `media.on-accent` | `#101217` | Icon trên nút phát |

Viên chọn và icon mục đang chọn trên thanh đáy bản N4 dùng **bản tối** của `volt-soft` (`#222c0b`) và `volt-text` (`#ccf55b`) bất kể theme, vì nền N4 luôn đen.

### 2.3 Chữ

| Style | Họ | Cỡ / dòng (sp) | Đậm | Giãn chữ | Dùng ở |
|---|---|---|---|---|---|
| `display` | Be Vietnam Pro | 32 / 40 | 800 | -0.02em | Tiêu đề lớn: Kết nối OneDrive, Thiết lập bảo mật |
| `title` | Be Vietnam Pro | 22 / 28 | 700 | -0.01em | Thanh tiêu đề (OneDrive, Thư viện, Cài đặt), tiêu đề Dialog |
| `heading` | Be Vietnam Pro | 17 / 24 | 600 | 0 | Xem tiếp / Đọc tiếp, tiêu đề Sheet, dòng tiêu đề trong Bảo mật |
| `body` | Be Vietnam Pro | 15 / 22 | 400 | 0 | Nội dung, tên hàng Cài đặt |
| `body-strong` | Be Vietnam Pro | 15 / 22 | 600 | 0 | Tên tệp trong FileRow, nhóm ngày Thư viện, tên tệp trên trình xem, tên tệp ở tab Short |
| `button` | Be Vietnam Pro | 15 / 20 | 600 | 0 | Nhãn Button cỡ md |
| `button-sm` | Be Vietnam Pro | 14 / 20 | 600 | 0 | Nhãn Button cỡ sm, Chip |
| `caption` | Be Vietnam Pro | 13 / 18 | 400 | 0 | Meta, mô tả hàng, trợ giúp dưới ô nhập, dòng thư mục ở tab Short |
| `label` | Be Vietnam Pro | 12 / 16 | 600 | 0 | Nhãn nhỏ, nhãn mục trên thanh điều hướng đáy |
| `timecode` | JetBrains Mono | 13 / 16 | 500 | 0 | 12:04 / 1:26:02, tốc độ 1x, số trang |
| `code` | JetBrains Mono | 13 / 20 | 400 | 0 | ID đã che a1b2••••9f0e, mã lỗi |
| `screen-title` | Be Vietnam Pro | 24 / 32 | 700 | -0.01em | Nhập mã PIN, Tạm khóa nhập mã PIN |
| `state-title` | Be Vietnam Pro | 20 / 28 | 700 | 0 | Trạng thái trống, lỗi trên media |
| `body-sm` | Be Vietnam Pro | 14 / 20 | 400 | 0 | Snackbar, Breadcrumb, Banner |
| `body-sm-strong` | Be Vietnam Pro | 14 / 20 | 600 | 0 | Tên trong thẻ lưới, mục cuối Breadcrumb |
| `caption-strong` | Be Vietnam Pro | 13 / 18 | 600 | 0 | Nhãn ô nhập, tên nhóm Cài đặt |
| `meta` | Be Vietnam Pro | 12 / 16 | 400 | 0 | Meta trong thẻ lưới, đường dẫn kết quả tìm |
| `step-label` | JetBrains Mono | 12 / 16 | 500 | 0.04em | **Không còn dùng** (đã bỏ StepBar). Giữ trong token cho tới khi người thiết kế gỡ |
| `timecode-sm` | JetBrains Mono | 12 / 16 | 400 | 0 | Badge thời lượng, viên thuốc 72 / 310 |
| `timecode-xs` | JetBrains Mono | 11 / 16 | 400 | 0 | Badge thời lượng trong ô Thư viện |
| `timer` | JetBrains Mono | 56 / 64 | 500 | -0.02em | Đếm ngược khóa tạm 00:30 |

**Phân cấp mẫu trên từng màn:**
- **Danh sách:** `title` (thanh tiêu đề) → `heading` (Xem tiếp / Đọc tiếp) → `body-strong` hoặc `body-sm-strong` (tên tệp) → `caption` / `meta`.
- **Xem video:** chỉ có `body-strong` (tên tệp) và `timecode`.
- **Short:** chỉ có `body-strong` (tên tệp) và `caption` (thư mục).
- **Cài đặt:** `caption-strong` màu `volt-text` (tên nhóm) → `body` (tên mục) → `caption` (mô tả) → `body-sm` màu `ink-muted` (giá trị).
- **Cỡ chữ hệ thống:** tôn trọng tới 200%. Hàng tự cao lên, không cắt chữ. Điều khiển video và nhãn thanh điều hướng đáy giữ cỡ cố định (nhãn dài thì cắt "…", không xuống dòng).

### 2.4 Khoảng cách

| Token | Giá trị | Dùng cho |
|---|---|---|
| `space.half` | 2dp | Khe giữa các ô lưới Thư viện |
| `space.1` | 4dp | Icon với nhãn trong badge; giữa các mục Breadcrumb; giữa viên chọn và nhãn trên thanh đáy |
| `space.2` | 8dp | Bên trong điều khiển; giữa các chip; khoảng cách tối thiểu giữa vùng chạm |
| `space.3` | 12dp | Thumbnail với chữ trong FileRow; giữa các ContinueCard; khe lưới thẻ; padding Banner; khe phím PIN |
| `space.4` | 16dp | **Lề màn hình**; padding thẻ, hàng, Sheet; khoảng cách giữa các ô nhập; lề tên tệp ở tab Short |
| `space.6` | 24dp | Giữa các nhóm nội dung; padding Dialog; khoảng trên tiêu đề nhóm Cài đặt |
| `space.8` | 32dp | Khoảng dưới Sheet; lề màn lỗi trên media |

### 2.5 Bo góc

| Token | Giá trị | Dùng cho |
|---|---|---|
| `radius.xs` | 6dp | Badge thời lượng; ô Thư viện (4dp trong lưới dày, xem PhotoCell) |
| `radius.sm` | 10dp | Ô nhập, Banner, Snackbar, thumbnail FileRow, thumbnail thẻ tệp |
| `radius.md` | 14dp | Thẻ, ô thư mục, ContinueCard, nhóm Cài đặt, phím PIN, thẻ trên media |
| `radius.lg` | 22dp | Dialog, Bottom sheet (2 góc trên), bảng bên phải ở hướng ngang (2 góc trái) |
| `radius.full` | tròn | Button, IconButton, Chip, Switch, ProgressBar, viên thuốc, viên chọn trên thanh đáy |

### 2.6 Kích thước cố định

| Token | Giá trị | Ghi chú |
|---|---|---|
| `tap-target` | 48dp | Vùng chạm tối thiểu |
| `icon-md` | 24dp |  |
| `icon-sm` | 18dp |  |
| `thumb-row` | 56dp | Thumbnail FileRow |
| `app-bar` | 56dp |  |
| `play-button` | 72dp | Nút phát giữa; vòng tạm dừng ở tab Short |
| `button` | 48dp |  |
| `button-sm` | 36dp |  |
| `field` | 52dp | Khung ô nhập |
| `chip` | 36dp |  |
| `tab-item` | 40dp | **Không còn dùng** từ Lát 8 (bỏ Tabs). Giữ trong token cho tới khi người thiết kế gỡ |
| `switch-width` | 52dp |  |
| `switch-height` | 32dp |  |
| `radio` | 22dp |  |
| `settings-row-min` | 56dp |  |
| `info-row-min` | 48dp |  |
| `file-row-min` | 72dp |  |
| `snackbar-min` | 48dp |  |
| `dialog-max-width` | 360dp |  |
| `sheet-handle-width` | 36dp |  |
| `sheet-handle-height` | 4dp |  |
| `pin-dot` | 16dp |  |
| `pin-dot-gap` | 16dp |  |
| `keypad-key-height` | 56dp |  |
| `keypad-gap` | 12dp |  |
| `keypad-width` | 358dp |  |
| `folder-card-thumb` | 104dp |  |
| `file-card-thumb` | 120dp |  |
| `continue-card-width` | 232dp |  |
| `continue-card-thumb` | 130dp |  |
| `media-progress` | 4dp | Thanh xem dở trên thumbnail |
| `fast-scroller-touch` | 28dp |  |
| `fast-scroller-handle-width` | 12dp |  |
| `fast-scroller-handle-height` | 36dp |  |
| `empty-icon-well` | 72dp |  |
| `loader-ring` | 72dp |  |
| `seek-touch` | 24dp |  |
| `seek-track` | 4dp |  |
| `seek-thumb` | 16dp |  |
| `mini-progress` | 3dp | Khi ẩn điều khiển video; thanh tiến độ ở tab Short |
| `hud-width` | 56dp |  |
| `hud-height` | 200dp |  |
| `lock-hold-ring` | 72dp |  |
| `lock-hold-button` | 52dp |  |
| `countdown-ring` | 56dp |  |
| `side-panel-width` | 360dp | Bảng bên phải ở hướng ngang |
| `nav-bar` | 64dp | **Mới (Lát 8), chưa có trong `odv-tokens.json`**. Chiều cao thanh điều hướng đáy, chưa tính lề dưới của hệ thống |
| `nav-indicator-width` | 56dp | **Mới (Lát 8), chưa có trong `odv-tokens.json`**. Viên chọn phía sau icon |
| `nav-indicator-height` | 32dp | **Mới (Lát 8), chưa có trong `odv-tokens.json`** |
| `nav-dot` | 8dp | **Mới (Lát 8), chưa có trong `odv-tokens.json`**. Chấm nhắc trên mục Cài đặt (DH-07) |

Ba token `nav-bar`, `nav-indicator-*`, `nav-dot` của thanh điều hướng đáy (mục 4.7) đã có trên board Bố cục của Foundations và trong `odv-tokens.json`.

### 2.7 Độ nổi

- Phần lớn giao diện phẳng: phân tầng bằng `bg` → `surface` → `surface-2` và viền `line`.
- Chỉ ba chỗ có bóng:
    - `shadow.sm` (sáng `0 1 2 #1012170f, 0 1 3 #1012171a`): ContinueCard.
    - `shadow.lg` (sáng `0 2 6 #10121714, 0 8 24 -4 #10121729`; tối `0 12 32 -4 #000000cc`): Dialog, Snackbar, bong bóng cuộn nhanh.
    - `focus-ring`: khe 2dp màu nền, rồi vòng 2dp `volt-text`. Dùng khi focus bằng bàn phím hoặc D-pad, không bao giờ ẩn.
- Thanh điều hướng đáy **không có bóng**, chỉ có kẻ trên 1dp.
- Android không có bóng CSS. Dùng elevation xấp xỉ rồi so với ảnh tham chiếu. Ở theme tối gần như không thấy bóng; đó là đúng thiết kế.

### 2.8 Chuyển động

| Token | Giá trị | Dùng cho |
|---|---|---|
| `duration.fast` | 150ms | Nhấn nút (co 98%), Switch, chip, cross-fade thumbnail → ảnh gốc (AN-01), lỗi dưới ô nhập, đổi tab trên thanh đáy, hiện/ẩn biểu tượng tạm dừng ở tab Short |
| `duration.base` | 250ms | Dialog, Sheet, Snackbar, hiện/ẩn điều khiển, đổi theme, zoom chạm đúp, vuốt sang video kế ở tab Short, thanh đáy đổi giữa bản thường và bản N4 |
| `duration.slow` | 400ms | Mở rộng thumbnail thành màn xem, Banner trượt vào |
| `easing.ease-out` | cubic-bezier(0.2, 0, 0, 1) | Xuất hiện, mở rộng |
| `easing.ease-in` | cubic-bezier(0.4, 0, 1, 1) | Biến mất, thu nhỏ |
| `duration.controls-auto-hide` | 3000ms | Điều khiển video tự ẩn (VD-01) |
| `duration.snackbar` | 3000ms | Snackbar tự ẩn |
| `duration.autoplay-countdown` | 5000ms | Đếm ngược tự phát tiếp (VD-13), vòng đầy dần tuyến tính |
| `duration.toast-label` | 2000ms | Nhãn đổi khung hình / chế độ phát (VD-06, VD-20); viên "Đã xáo lại" ở tab Short |
| `duration.lock-hold` | 1000ms | Giữ để mở khóa thao tác (VD-08, đề xuất) |

**Quy tắc chuyển động:**
- Không nảy, không đàn hồi.
- Không có chuyển động lặp vô hạn, trừ chỉ báo đang tải.
- Đổi tab trên thanh đáy: nội dung cross-fade, **không trượt ngang** (các tab không có thứ tự không gian). Viên chọn hiện bằng fade + giãn ngang từ giữa.
- Khi hệ thống bật Giảm hiệu ứng:
    - Bỏ scale, trượt, gợn và mở rộng thumbnail. Thay bằng cross-fade ≤ 150ms hoặc hiện ngay.
    - Vuốt chuyển video ở tab Short vẫn theo ngón tay, nhưng phần tự chạy tới vị trí dừng rút còn 150ms.
    - Giữ thanh tiến độ và vòng đếm ngược, vì đó là thông tin.
- Rung nhẹ chỉ ở hai chỗ: bật/tắt khóa thao tác video, và nhập sai PIN.

### 2.9 Độ mờ

- `opacity.disabled` = 0.38: điều khiển bị tắt (nút Kết nối khi form chưa hợp lệ, nút "Video sau" ở video cuối, bàn phím khi khóa tạm).
- `opacity.unsupported` = 0.5: tệp không hỗ trợ khi bật "Hiện tệp không hỗ trợ" (TM-03). **Hiện chưa dùng**: tùy chọn đó đã bỏ (chốt 2026-10-07), tệp không hỗ trợ luôn ẩn.

---

## 3. Icon và logo

### 3.1 Bộ icon (43 icon)

- Lưới 24 × 24, nét 2dp, đầu và góc nét tròn, không tô.
- Chỉ `play` và `pause` tô đặc.
- Nguồn SVG nằm ở board "06 Icon" trên canvas. Chuyển sang Vector Drawable và giữ nguyên `pathData`.
- `rect` và `circle` trong SVG phải đổi thành path; không vẽ lại bằng tay.
- Cỡ dùng: 24dp (mặc định, kể cả thanh điều hướng đáy), 18dp (trong Chip, nhãn), 16dp (dòng lỗi, mũi tên Breadcrumb), 32–36dp (trạng thái trống, nút giữa video).

| Icon | Dùng ở | Icon | Dùng ở |
|---|---|---|---|
| `play`, `pause` | Nút giữa video; vòng tạm dừng ở tab Short | `replay` | Nút giữa khi hết video (VD-21) |
| `prev`, `next` | Video trước/sau (VD-10) | `rewind`, `forward` | Tua ±10 giây, gợn chạm đúp |
| `playlist` | Chế độ: Tự phát tiếp | `stop-end` | Chế độ: Không lặp |
| `repeat-one` | Chế độ: Lặp một video | `repeat` | Chế độ: Lặp danh sách |
| `fit` | Đổi khung hình (VD-06) | `rotate` | Xoay Dọc ↔ Ngang (VD-07) |
| `info` | Thông tin tệp (VD-17, AN-05, PD-08) | `lock`, `unlock` | Khóa thao tác, khóa app, hộp thoại K6 |
| `sun`, `volume` | HUD độ sáng / âm lượng | `tune` | Kiểu đọc PDF |
| `folder`, `image`, `video`, `book` | Loại mục: thư mục, ảnh, video, PDF; `folder` và `image` cũng là icon mục Thư mục, Thư viện trên thanh đáy | `search`, `close` | Tìm kiếm, xóa từ khóa, đóng |
| `settings` | Mục Cài đặt trên thanh điều hướng đáy (từ Lát 8; trước đó ở AppBar) | `arrow-left`, `chevron-right` | Quay lại, vào mục, Breadcrumb |
| `sort`, `grid`, `list` | Sắp xếp, đổi dạng lưới/danh sách | `cloud-off` | Offline, tệp chưa có trong cache |
| `sync` | Đồng bộ, đang lập chỉ mục, xáo lại ở tab Short | `check` | Thành công, chip đang chọn |
| `alert` | Lỗi, cảnh báo | `clock` | Banner secret sắp hết hạn |
| `eye`, `eye-off` | Hiện/ẩn ký tự ô bị che | `backspace`, `fingerprint` | Bàn phím PIN |
| `minus` | Checkbox chọn một phần | `fullscreen` | Dự phòng, **không dùng** (VD-07 bỏ nút toàn màn hình) |
| `short` | **Mới (Lát 8), chưa có trên board.** Mục Short trên thanh đáy. Gợi ý hình: khung chữ nhật đứng bo góc (tỉ lệ khoảng 9:16) có tam giác phát ở giữa, cùng quy cách nét | | |

### 3.2 Logo

- **Mark:** ô volt `#c5f23a` bo góc, góc trên phải gập `#8db31a`, nút phát `#101217` ở giữa. Màu cố định, không tint.
- **Bản một màu:**
    - Bản ink dùng trên nền volt.
    - Bản trắng dùng trên media.
- **Cỡ:** nhỏ nhất 24dp. Splash dùng 112dp. Kết nối dùng 40dp, đặt phía trên tiêu đề. AppBar Danh sách dùng 28dp. Màn Khóa dùng 48dp. Vòng tải dùng 36dp, nằm trong vòng 72dp.
- **App icon (adaptive):** nền `#101217`, mark nằm trong vùng an toàn 66/108.

---

## 4. Component

Đơn vị dp. Màu ghi theo token. "Nhãn" là nhãn TalkBack.

### 4.1 Điều khiển

- **Button**:
    - Cỡ md cao 48, padding ngang 24, chữ `button`. Cỡ sm cao 36, padding 16, chữ `button-sm`. Bo tròn, có thể kèm icon 20/18 cách chữ 8.
    - Các kiểu:

      | Kiểu | Nền | Chữ | Viền |
          |---|---|---|---|
      | primary | `volt` | `on-volt` | không |
      | secondary | trong suốt | `ink` | 1dp `line-strong` |
      | ghost | trong suốt | `volt-text` | không, padding ngang 16 |
      | tonal | `surface-2` | `ink` | không |
      | danger | `danger-soft` | `danger` | không |
      | danger-solid | `danger` | trắng (tối: `#101217`) | không |

    - Các trạng thái:
        - **Nhấn:** primary chuyển sang `volt-pressed`, các kiểu khác phủ `surface-2` hoặc `volt-soft`, kèm co 98%.
        - **Tắt:** mờ 0.38.
        - **Đang tải:** spinner 20dp thay icon, nút bị khóa.
    - Nút toàn chiều rộng đặt cách đáy 24–32dp.
- **IconButton:** 48 × 48 tròn, icon 24.
    - Kiểu standard trong suốt; tonal có nền `surface-2`; khi chọn nền `volt-soft`, icon `volt-text`.
    - Trên media: icon trắng, nền trong suốt.
- **TextField:**
    - Nhãn `caption-strong` nằm trên, cách 6. Khung cao 52, bo `sm`, nền `surface`, viền 1dp `line-strong`, padding trái 14.
    - Chữ nhập là `body`. Riêng Client ID khi hiện ký tự dùng JetBrains Mono.
    - Dòng trợ giúp `caption` màu `ink-muted` nằm dưới, cách 6.
    - Các trạng thái:
        - **Focus:** viền 2dp `volt-text`.
        - **Lỗi:** viền 2dp `danger`, dòng lỗi có icon `alert` 16 kèm chữ `danger`.
        - **Tắt:** nền `surface-2`, mờ 0.6.
    - Ô bị che có nút mắt 48 ở bên phải, nhãn "Hiện ký tự" / "Ẩn ký tự".
- **Checkbox / Radio:**
    - Hàng cao tối thiểu 48, nút 22, khoảng cách tới chữ 12. Chữ `body`, mô tả `caption` màu `ink-muted` nằm dưới.
    - Radio chọn: vòng 2dp `volt-text` có chấm 10dp. Radio chưa chọn: vòng `line-strong`.
- **Switch:** 52 × 32, tròn.
    - Bật: nền `volt`, núm 24 màu `on-volt` nằm bên phải.
    - Tắt: nền `surface-2`, viền 2dp `line-strong`, núm 16 màu `line-strong` nằm bên trái.
    - Núm trượt trong 150ms.
- **Chip:** cao 36, padding ngang 14, chữ `button-sm`, tròn.
    - Chưa chọn: viền 1dp `line-strong`, có thể kèm chấm 8dp màu kind.
    - Chọn: nền `volt-soft`, chữ và viền `volt-text`, icon `check` 18.
    - Nhãn TalkBack có thêm ", đang chọn".
- **Tabs (Thư mục / Thư viện):** **đã bỏ từ Lát 8** (ADR-0023), thay bằng thanh điều hướng đáy (mục 4.7). Không dựng component này nữa. Mô tả cũ để tham khảo: khung `surface-2` tròn, padding 4, mỗi tab cao 40, tab chọn nền `ink` chữ `bg`.
- **ProgressBar:** tròn, cao 4 (trong hàng) hoặc 6 (tải PDF).
    - Rãnh `surface-2`, phần đã có `volt-text`.
    - Trên media: rãnh `#ffffff33`, phần đã có `volt`.
- **StepBar:** **đã bỏ.** Màn Kết nối và Thiết lập bảo mật không còn nhãn "BƯỚC 1 / 2" hay thanh đoạn (luồng có nhánh nên không còn đếm bước). Không dựng component này.

### 4.2 Thông báo và lớp phủ

- **Banner:** padding 12, bo `sm`, icon 20 + chữ `body-sm` 500.
    - Năm tông màu:

      | Tông | Nền | Chữ / icon |
          |---|---|---|
      | neutral | `surface-2` | `ink` |
      | volt | `volt-soft` | `volt-text` |
      | warning | `warning-soft` | `warning` |
      | danger | `danger-soft` | `danger` |
      | success | `success-soft` | `success` |

    - Nút hành động bên phải là chữ đậm 14, cao 36.
    - Banner danger đọc ngay (role alert). Các tông khác là role status.
- **Snackbar:** lề 16 hai bên, cách đáy 24 (ở Màn chính: cách **mép trên thanh điều hướng đáy** 16, không đè lên thanh). Cao tối thiểu 48, padding 6/6/6/16, bo `sm`.
    - Nền `inverse-surface`, chữ `body-sm` màu `inverse-ink`.
    - Hành động: chữ đậm 14 màu `inverse-accent`, không xuống dòng.
    - Bóng `shadow.lg`, tự ẩn sau 3 giây.
- **Dialog:**
    - Rộng ≤ 360 (thiết kế 326–342), căn giữa. Nền `surface`, bo `lg`, padding 24, các khối cách nhau 16. Scrim `#00000099` phía sau (phủ cả thanh điều hướng đáy).
    - Icon tròn 48, nền và icon theo tông: volt / warning / danger.
    - Tiêu đề `title`. Nội dung `body` màu `ink-muted`. Dòng mã lỗi `code` màu `ink-muted` ("Mã lỗi: AADSTS7000215").
    - Nút căn phải, cách nhau 8: nút phụ là ghost, nút chính là primary / danger / danger-solid.
    - Dialog nguy hiểm dùng role alertdialog.
    - Dialog **bắt buộc chọn** (K6): chạm scrim và Back hệ thống không đóng; chỉ đóng bằng một trong hai nút.
- **Bottom sheet:**
    - Rộng toàn màn, nền `surface`, bo `lg` hai góc trên, padding 8/16/32.
    - Tay nắm 36 × 4 màu `line-strong`, căn giữa. Tiêu đề `heading`.
    - Scrim `#00000099`. Sheet mở **đè lên** thanh điều hướng đáy (thanh nằm dưới scrim).
- **FullScreenLoader** (K3, B8, Cài đặt · S2):
    - Phủ toàn màn, kể cả thanh tiêu đề, bằng `bg` có alpha `f0` (94%).
    - Ở giữa là vòng 72: rãnh `line` 4dp, cung `volt-text` 4dp xoay 1 vòng/giây, mark 36 nằm trong.
    - Bên dưới: tiêu đề `title`, phụ đề `body` màu `ink-muted`, rồi danh sách bước (icon 18 + chữ `body`):
        - xong: `check` màu `success`
        - đang chạy: spinner `volt-text` và chữ đậm 600
        - chờ: chấm rỗng 8dp viền `line-strong`
    - Không có nút Hủy (đã chốt).
- **Scrim:** `#00000099` sau Dialog, Sheet và bảng bên phải.

### 4.3 PIN

- **PinDots:** 6 chấm 16, cách 16, dòng cao 24.
    - Trống: viền 2dp `line-strong`.
    - Đã nhập: nền `volt`, viền 2dp `volt-text`.
    - Lỗi: cả 6 chấm `danger`.
    - Dòng thông báo bên dưới cao 20, chữ 13/20 500. Lỗi có icon `alert` 16 màu `danger`.
- **Keypad:** lưới 3 cột, rộng 358, khe 12. Phím cao 56, bo `md`, nền `surface-2`, số 26/32 600.
    - Thứ tự cố định: 1–9, rồi [sinh trắc học hoặc ô trống] [0] [xóa].
    - Phím đang nhấn: nền `volt-soft` và viền 2dp `volt-text`.
    - Khi khóa tạm: cả bàn phím mờ 0.38.
    - Nhãn: "Xóa số cuối", "Mở bằng sinh trắc học".
- **Hành vi:**
    - Đủ 6 số thì tự chuyển bước hoặc tự kiểm tra, không có nút xác nhận.
    - Bàn phím tự vẽ, không mở bàn phím hệ thống (BM-07, KH-05).

### 4.4 Danh sách

- **AppBar:** cao 56, padding ngang 4.
    - Bên trái: ở gốc là mark 28, cách lề 12. Trong thư mục con là nút `arrow-left` "Lên một cấp".
    - Tiêu đề `title`, một dòng, cắt "…". Tab Thư mục: tên thư mục đang mở ("OneDrive" ở gốc). Tab Thư viện: "Thư viện" (đã vẽ ở D1 đến D8 bản mới).
    - Bên phải: chỉ còn IconButton `search` "Tìm kiếm". **Nút `settings` đã bỏ từ Lát 8** (Cài đặt là tab, DH-01).
- **AppBar tìm kiếm:** nút quay lại, rồi ô tìm cao 48 tròn nền `surface-2` gồm icon `search` 20 màu `ink-muted`, chữ `body` (placeholder "Tìm tên tệp hoặc thư mục") và nút X "Xóa từ khóa".
- **Breadcrumb:** padding ngang 16, chữ `body-sm`.
    - Mục trước màu `ink-muted`, cao chạm tối thiểu 32. Mục cuối dùng `body-sm-strong`. Giữa các mục là mũi tên 16 màu `ink-faint`, cách 4.
    - Đường dẫn dài thì rút gọn thành "… › cấp áp chót › cấp cuối".
- **SortBar:** Button tonal sm có icon `sort` và nhãn "Tên · A đến Z". Bên phải là IconButton đổi dạng lưới/danh sách.
- **FolderCard (lưới):** ô 104 cao, bo `md`, nền `volt-soft`, icon `folder` 36 màu `volt-text`. Tên `body-sm-strong` một dòng, meta `meta` ("24 mục"), cách 6.
- **FileCard (lưới):** thumbnail cao 120, bo `sm`, center-crop.
    - Video: icon `video` 18 trắng ở góc trên trái (cách 6), badge thời lượng ở góc dưới phải.
    - PDF: nền `kind-pdf-soft`, icon `book` 36 màu `kind-pdf`.
    - Tệp chưa có trong cache khi offline: nút tròn 24 nền `#00000099`, icon `cloud-off` 14, ở góc trên phải.
    - Thanh xem dở cao 4 sát đáy: rãnh `#ffffff66`, phần đã xem `volt`. Với PDF là rãnh `line` và phần đã đọc `kind-pdf`.
    - Lưới 2 cột, khe 16 dọc và 12 ngang.
- **Badge thời lượng:** nền `#00000099`, chữ trắng `timecode-sm`, padding 2/6, bo 6. Trong ô Thư viện: `timecode-xs`, padding 0/4, bo 4.
- **FileRow:** cao tối thiểu 72, padding 8/16, kẻ đáy 1dp `line`.
    - Thumbnail 56 bo `sm`:
        - Thư mục: nền `volt-soft`, icon `volt-text`.
        - PDF: nền `kind-pdf-soft`.
        - Không hỗ trợ: nền `surface-2`, icon `alert` màu `ink-muted`.
    - Tên `body-strong` một dòng. Meta `caption` màu `ink-muted`; thời lượng trong meta dùng JetBrains Mono.
    - Thanh xem dở rộng 160, cao 4. Kết quả tìm có thêm dòng đường dẫn `meta` màu `ink-faint`.
    - Thư mục có `chevron-right` 20 ở cuối. Tệp không hỗ trợ mờ 0.5.
    - Từ khớp khi tìm kiếm được tô nền `volt-soft`, bo 3.
- **ContinueCard (dải Xem tiếp / Đọc tiếp):**
    - Thẻ rộng 232, nền `surface`, bo `md`, bóng `shadow.sm`. Thumbnail cao 130, thanh tiến độ 4 sát đáy thumbnail.
    - Nút X "Xóa khỏi dải": vùng chạm 40, hình tròn 28 nền `#00000099`, icon 16 trắng.
    - Chữ padding 10/12: tên `body-sm-strong`, phụ `meta`.
    - Dải cuộn ngang, padding 16, thẻ cách 12. Tiêu đề dải `heading`.
    - Dải chỉ hiện ở gốc Thư mục; trong thư mục con thì ẩn.
- **DateHeader (Thư viện):** `body-strong`, padding 12/16/8, nền `bg`, dính khi cuộn. Số mục ở bên phải dùng `body` màu `ink-muted`.
- **PhotoCell:** lưới 4 cột ở hướng dọc, khe 2, ô vuông bo 4, center-crop. Video có icon `video` 14 ở góc trên trái và badge ở góc dưới phải. Ô chưa tải có nền `surface-2`.
- **FastScroller:**
    - Vùng chạm rộng 28 sát mép phải. Rãnh 2dp màu `line`.
    - Tay nắm 12 × 36, bo 6, nền `volt`, viền 2dp `volt-text`.
    - Khi kéo hiện bong bóng `inverse-surface`, chữ 14/20 600 ("Tháng 9, 2026"), bóng `shadow.lg`.
    - Rãnh kết thúc ở mép trên thanh điều hướng đáy, không chạy xuống dưới thanh.
- **EmptyState:**
    - Căn giữa, padding ngang 40. Vòng 72 nền `surface-2`, icon 32 màu `ink-muted`.
    - Tiêu đề `state-title`, nội dung `body` màu `ink-muted`, tối đa một nút secondary.

### 4.5 Màn xem

- **ViewerTopBar:** nằm trên media, cách đỉnh 24 ở hướng dọc và 12 ở hướng ngang.
    - Nút `arrow-left` "Quay lại", tên tệp `body-strong` trắng một dòng, rồi các nút phải:
        - video: `info`, `lock`
        - ảnh: `info`
        - PDF: `tune` (xem mục 9)
    - Ảnh có thêm dải gradient đen 70% → trong suốt, cao 96, phía sau thanh.
- **Cụm điều khiển giữa (video):** hàng ngang căn giữa màn, gồm `prev`, `rewind`, nút giữa, `forward`, `next`, cách nhau 12 ở hướng dọc và 20 ở hướng ngang.
    - Nút giữa tròn 72, nền `volt`, icon 36 màu `#101217`. Có bốn trạng thái: `pause` (đang phát), `play` (đang dừng), `replay` (hết video), spinner (đang tải).
    - Nút trước/sau mờ 0.38 khi ở đầu/cuối danh sách phát.
- **SeekBar:** vùng chạm cao 24. Rãnh 4 tròn `#ffffff33`, phần đã tải `#ffffff66`, phần đã xem `volt`. Núm 16 `volt`.
    - Nhãn: "Vị trí phát, 12 phút 4 giây trên 1 giờ 26 phút" (đọc bằng lời).
- **ViewerBottomBar (video):** lề trái 16, phải 8, cách đáy 24 (ngang 8).
    - Dòng trên là SeekBar. Dòng dưới: thời gian `timecode` ("12:04 / 1:26:02") chiếm chỗ trống, rồi nút Tốc độ (chữ `timecode` "1x", cao 48), Chế độ phát, Khung hình, Xoay màn hình.
    - **Không có nút toàn màn hình** (VD-07).
- **Lớp phủ khi điều khiển hiện:** `#00000080` phủ toàn màn. Điều khiển tự ẩn sau 3 giây và mờ dần 250ms.
- **MiniProgress:** khi điều khiển ẩn, thanh 3dp sát đáy khung video (hướng dọc), rãnh `#ffffff33`, phần đã xem `volt`.
- **Khung video ở hướng dọc:** rộng toàn màn, tỉ lệ 16:9, căn giữa theo chiều dọc. Hướng ngang lấp chiều cao.
- **Gợn chạm đúp (VD-03):** nửa hình elip nền `#ffffff26` phủ nửa trái hoặc nửa phải của khung video. Icon `rewind` / `forward` 32 kèm nhãn 14 600 ("-10 giây", "+20 giây"). Chạm liên tiếp thì cộng dồn nhãn, không tạo gợn mới.
- **HUD vuốt (VD-04):** khung 56 × 200, bo 28, nền `#000000b3`, cách mép 32.
    - Phần trăm `timecode-sm` ở trên. Rãnh dọc 6 `#ffffff33`, mức hiện tại `volt`. Icon `sun` (nửa trái) hoặc `volume` (nửa phải) ở dưới.
- **Viên thuốc nhãn:** nền `#000000b3`, tròn, padding 6/14 (đậm: 8/18, chữ 14 600).
    - "Cắt đầy" và "Lặp một video" (kèm icon 18) đặt trên cụm giữa, hiện 2 giây.
    - "2,4x" đặt cách đỉnh 56.
- **Đặt lại zoom (VD-18):** nút cao 44, padding 20, viền 1dp `#ffffff66`, nền `#000000b3`, chữ 14 600, cách đáy 112. Dòng gợi ý `caption` nằm dưới.
- **Khóa thao tác (VD-08):**
    - Viên thuốc "Đã khóa thao tác" kèm icon `lock` 16, cách đỉnh 92.
    - Nút mở khóa: tròn 52 nền `#ffffff26` có icon `unlock` 26, nằm trong vòng 72 (rãnh `#ffffff33`, cung `volt` chạy theo thời gian giữ). Nút cách đáy 96, kèm chữ "Giữ để mở khóa" 14 600 và "Mọi thao tác chạm khác đang bị chặn" 13.
- **Thẻ trên media** (Tiếp theo, Mất mạng): lề 16, nền `#1b1d22`, bo `md`, padding 16.
    - **Tiếp theo:** vòng đếm 56 (rãnh `#ffffff33`, cung `volt`) có số giây `timecode` 18 ở giữa; chữ "Tiếp theo sau 5 giây" và tên video sau; nút "Hủy" viền `#ffffff66`.
    - **Mất mạng:**
        - Tiêu đề có icon `cloud-off`, kèm một đoạn hướng dẫn.
        - Nút "Tiếp tục" màu volt, mờ 0.38 tới khi có mạng, kèm chữ "Nút bật lại khi có mạng".
        - Thẻ chỉ hiện sau khi đã phát hết phần đã tải trước.
- **MediaError** (codec VD-15, ảnh AN-07, PDF PD-07, Short SH7): khối căn giữa, lề 32.
    - Vòng 72 nền `#ffffff1f`, icon `alert` 32.
    - Tiêu đề `state-title`, nội dung `body` `#ffffffcc`, dòng tên tệp `timecode-sm` `#ffffff99`.
    - Nút secondary viền `#ffffff66` (ảnh và Short không có nút; vuốt để sang mục khác).
- **Bảng thông tin tệp:**
    - Hướng dọc: bottom sheet.
    - Hướng ngang: bảng bên phải rộng 360, cao toàn màn, bo `lg` hai góc trái, có nút đóng; hàng gọn hơn, padding 7.
    - Mỗi hàng: nhãn `ink-muted` bên trái, giá trị bên phải, kẻ đáy `line`, padding dọc 10. Số liệu dùng JetBrains Mono.
    - Video tạm dừng khi bảng mở.
    - Trường nào không có dữ liệu thì ẩn hàng.
- **Bảng tốc độ:**
    - Hướng dọc: bottom sheet gồm 7 radio, 1x có thêm " · Bình thường".
    - Hướng ngang: bảng bên phải 360, radio gọn cao 40.
- **PDF:**
    - Nền sau trang `#2a2c31`. Trang rộng toàn màn, cách nhau 12.
    - Thanh trên và dưới nền `#000000cc`. Thanh dưới cao 88 gồm nút số trang (viền `#ffffff66`, cao 40, chữ `timecode`, "72 / 310") và thanh tiến độ 4.
    - Khi ẩn thanh: viên thuốc số trang ở góc dưới phải.
    - Đang tải: vòng 72 nền `kind-pdf-soft` có icon `book`, tên tệp, ProgressBar 6, "% · dung lượng" `timecode`, nút Hủy.
- **Ảnh:**
    - Bộ đếm "12 / 248" dạng viên thuốc cách đáy 32.
    - Khi đang tải ảnh gốc: viên thuốc nhỏ có spinner 14 và chữ "Đang tải ảnh gốc", nằm dưới ảnh.
    - Đang zoom: viên thuốc phần trăm ở trên và gợi ý "Chạm đúp để về vừa khung" ở dưới.

### 4.6 Cài đặt

- **SettingsGroup:** tiêu đề `caption-strong` màu `volt-text`, padding 24/16/8, nằm ngoài thẻ. Thẻ có lề 16, nền `surface`, viền 1dp `line`, bo `md`, các hàng kẻ `line` ở giữa (hàng cuối không có kẻ).
- **SettingsRow:** cao tối thiểu 56, padding 8/16, phần tử bên phải cách 16. Tên `body`, mô tả `caption` màu `ink-muted`. Có 6 kiểu:
    - SwitchRow
    - ValueRow: giá trị `body-sm` màu `ink-muted` và `chevron-right` 20, mở sheet chọn
    - NavRow: chỉ có `chevron-right`, mở màn con
    - InfoRow: cao 48, giá trị `code` màu `ink-muted`
    - ActionRow: không có phần tử bên phải, mở dialog
    - DangerRow: tên màu `danger` đậm 600
- **ChipsRow (Loại tệp hiển thị):** 3 chip Ảnh / Video / PDF dưới mô tả, cách 10, có chấm màu kind.
    - Khi chỉ còn một chip bật: không cho tắt chip đó, và hiện dòng `caption` màu `warning` có icon `alert` 16: "Phải bật ít nhất 1 loại tệp".
- **CacheUsage:** tiêu đề và "1,4 GB / 2 GB" (`code`) trên một dòng.
    - Thanh phân đoạn cao 8, bo 4, rãnh `surface-2`, các đoạn cách 1dp, độ dài tính theo giới hạn: thumbnail `line-strong`, ảnh `kind-photo`, video `kind-video`, PDF `kind-pdf`.
    - Chú thích lưới 2 cột: ô màu 10 bo 3, tên `ink-muted`, dung lượng mono căn phải.
- **OptionRow (trong sheet):** radio, có thể kèm mô tả. Chạm là đặt giá trị và đóng sheet ngay, **không có nút Lưu**.
- **Mức hộp thoại xác nhận:**

  | Mức | Icon | Nút chính | Dùng cho |
    |---|---|---|---|
  | Cảnh báo, bước 1 | warning | danger (tonal) | Ngắt kết nối bước 1, Quên PIN bước 1, Xóa bộ nhớ đệm, Bật xóa dữ liệu tự động |
  | Nguy hiểm, bước 2 | danger | danger-solid | Xóa toàn bộ dữ liệu |
  | Cảnh báo, hành động an toàn | warning | primary | Giảm giới hạn bộ nhớ đệm |
  | Tắt bảo vệ | warning | danger-solid | Tắt bảo vệ ứng dụng |

### 4.7 Thanh điều hướng đáy (NavBar, Lát 8, DH-01 → DH-08)

Thay cho Tabs (mục 4.1). Đã có artboard N1 đến N4 ở board 20 "Thanh điều hướng đáy" của ODV Foundations (2026-10-10): N1 (4 mục, sáng/tối, nhấn, focus), N2 (3 mục), N3 (chấm warning/danger), N4 (nền đen cho tab Short). Số liệu dưới đây khớp thiết kế; nếu thấy lệch khi dựng thì hỏi lại, không tự sửa.

- **Bố cục:**
    - Cao `nav-bar` 64 cộng lề dưới của thanh điều hướng hệ thống. Rộng toàn màn.
    - Nền `surface`, kẻ trên 1dp `line`. Không bóng.
    - Tối đa 4 mục theo thứ tự **Thư mục (`folder`), Thư viện (`image`), Short (`short`), Cài đặt (`settings`)**. Khi Short ẩn (DH-01) còn 3 mục: Thư mục, Thư viện, Cài đặt.
    - Các mục chia đều chiều ngang; cả cột của một mục là vùng chạm (luôn ≥ 48dp).
    - Mỗi mục: viên chọn 56 × 32 (`nav-indicator-*`, bo tròn) chứa icon 24, bên dưới cách 4 là nhãn `label` (12/16, 600), một dòng, cắt "…".
- **Trạng thái (bản thường, theo theme):**

  | Trạng thái | Viên chọn | Icon | Nhãn |
    |---|---|---|---|
  | Đang chọn | `volt-soft` | `volt-text` | `ink` |
  | Chưa chọn | trong suốt | `ink-muted` | `ink-muted` |
  | Nhấn | phủ `surface-2` lên viên (chưa chọn) | như trạng thái gốc | như trạng thái gốc |
  | Focus | `focus-ring` quanh viên | | |

- **Bản N4 (khi đang ở tab Short, SV-10):** nền `media.background`, kẻ trên 1dp `media.icon-well`. Chưa chọn: icon và nhãn `media.on-media-faint`. Đang chọn (Short): viên `#222c0b` (`volt-soft` bản tối), icon `#ccf55b` (`volt-text` bản tối), nhãn `media.on-media`. Đổi giữa bản thường và N4 bằng cross-fade 250ms.
- **Chấm nhắc (N3, DH-07):** chấm tròn 8 (`nav-dot`) ở góc trên phải viên của mục Cài đặt, lệch vào trong 4. Màu `warning` khi secret sắp hết hạn, `danger` khi đã hết hạn. Hiện cả khi mục Cài đặt đang chọn.
- **Khi nào ẩn (DH-05):** không vẽ ở màn xem, Khóa, Kết nối, Thiết lập bảo mật, màn con của Cài đặt (P1 đến P3, S1 đến S5, Thiết lập bảo mật từ Cài đặt) và khi bàn phím đang mở để tìm kiếm. Dialog và Bottom sheet phủ lên trên thanh.
- **Nhãn TalkBack:** "Thư mục, tab 1 trên 4, đang chọn"; mục có chấm thêm ", có thông báo" (vd. "Cài đặt, tab 4 trên 4, có thông báo").

### 4.8 Short (Lát 8, SV-01 → SV-16)

Đã có artboard SH1 đến SH9 (cộng bản "Đã xáo lại") ở page Short (2026-10-10). Số liệu dưới đây dựng từ token có sẵn; nếu thấy lệch khi dựng thì hỏi lại, không tự sửa.

- **Vùng video:** từ đỉnh màn (vẽ tràn dưới thanh trạng thái) tới mép trên thanh đáy N4. Nền `media.background`. Không có AppBar, không có nút nào trên video.
- **Khung hình (SV-08):**
    - Video dọc gần tỉ lệ vùng video: Cắt đầy (lấp kín, cắt phần thừa).
    - Video dọc lệch tỉ lệ (vd. 3:4) và video ngang, vuông: Vừa khung, căn giữa, phần trống nền đen.
- **Tên tệp (SV-09):** góc dưới trái, lề trái và phải 16, cách mép dưới vùng video 16.
    - Dòng 1: tên tệp `body-strong` màu `media.on-media-muted`, một dòng, cắt "…".
    - Dòng 2: thư mục chứa tệp `caption` màu `media.on-media-faint`, một dòng, cắt "…".
    - Phía sau hai dòng có dải gradient từ `media.scrim` (dưới) sang trong suốt (trên), cao 96, để chữ đọc được trên video sáng.
    - Không phải nút bấm, không có vùng chạm riêng.
- **Thanh tiến độ (SV-07):** cao `mini-progress` 3, sát mép trên thanh đáy, rộng toàn màn. Rãnh `media.track`, phần đã xem `media.accent`. Chỉ hiển thị, không kéo.
- **Tạm dừng (SV-06):** vòng 72 nền `media.pill` ở giữa vùng video, icon `play` 36 màu `media.on-media`. Hiện suốt lúc tạm dừng, fade 150ms. Đang phát thì không có gì ở giữa.
- **Đang tải (SH5):** thumbnail của video làm nền (Vừa khung), spinner 36 màu `media.on-media` ở giữa.
- **Kéo để xáo lại (SV-03, SH6):** chỉ ở video đầu. Kéo xuống thì một vòng 40 nền `media.pill` có icon `sync` 24 trắng trượt xuống từ đỉnh (dưới thanh trạng thái), xoay khi đang xáo. Xáo xong hiện viên thuốc nhãn "Đã xáo lại" (kiểu viên thuốc nhãn ở mục 4.5) 2 giây ở trên vùng video. Câu chữ là [mới], chờ duyệt.
- **Lỗi định dạng (SH7):** MediaError (mục 4.5) không có nút, không có đếm ngược; tiêu đề "Thiết bị không hỗ trợ phát định dạng này". Tên tệp góc dưới vẫn hiện.
- **Mất mạng (SH8):** thẻ Mất mạng (mục 4.5) ở giữa vùng video, giữ nút "Tiếp tục".
- **Danh sách rỗng (SH9, SV-15):** EmptyState trên nền đen: vòng 72 nền `media.icon-well`, icon `video` 32 màu `media.on-media-muted`, tiêu đề `state-title` `media.on-media`, nội dung `body` `media.on-media-muted`, một nút secondary viền `media.buffer` chữ `media.on-media`. Câu chữ [mới], chờ duyệt:
    - Tiêu đề: "Chưa có video ngắn"
    - Nội dung: "Không có video nào dài tối đa N phút. Tăng thời lượng trong Cài đặt › Video."
    - Nút: "Mở Cài đặt" (chuyển sang tab Cài đặt)
- **Nhãn TalkBack:** vùng video "Video ngắn, <tên tệp>, đang phát" hoặc "..., đã tạm dừng"; hành động chạm "Tạm dừng" / "Phát"; thêm hai hành động tùy chỉnh "Video kế tiếp", "Video trước" thay cho vuốt; ở video đầu thêm "Xáo lại danh sách".

---

## 5. Màn hình

Ảnh tham chiếu: mở artboard cùng tên trên canvas, đúng page. Kích thước tính bằng dp.

### 5.1 Khởi động (Splash, Kết nối, Thiết lập bảo mật, Khóa)

| Mã | Màn / trạng thái | Kích thước | Ảnh tham chiếu (artboard) |
|---|---|---|---|
| S1 | Splash · Sáng | 390×844 | `SplashLight` |
| K1 | Kết nối · Trống | 390×844 | `ConnectEmpty` |
| K2 | Kết nối · Lỗi định dạng (KN-06) | 390×844 | `ConnectInvalid` |
| K3 | Kết nối · Đang kết nối (KN-07) | 390×844 | `ConnectLoading` |
| K5 | Kết nối · Lỗi kèm mã (KN-09) | 390×844 | `ConnectError` |
| K6 | Kết nối · Hộp thoại hỏi thiết lập mã PIN (KN-13). **Chưa có artboard, cần thiết kế** | 390×844 | `ConnectPinPrompt` (dự kiến) |
| S1 | Splash · Tối | 390×844 | `SplashDark` |
| K1 | Kết nối · Trống · Tối | 390×844 | `ConnectEmptyDark` |
| K5 | Kết nối · Lỗi kèm mã · Tối | 390×844 | `ConnectErrorDark` |
| K6 | Hộp thoại hỏi thiết lập mã PIN · Tối. **Chưa có artboard** | 390×844 | `ConnectPinPromptDark` (dự kiến) |
| B1 | Bảo mật · Đặt mã PIN (BM-01, BM-02) | 390×844 | `SecEmpty` |
| B2 | Bảo mật · Đang nhập (BM-05) | 390×844 | `SecTyping` |
| B3 | Bảo mật · PIN dễ đoán (BM-06) | 390×844 | `SecWeak` |
| B4 | Bảo mật · Nhập lại PIN | 390×844 | `SecConfirm` |
| B5 | Bảo mật · Hai lần không khớp | 390×844 | `SecMismatch` |
| B6 | Bảo mật · Đã khớp + sinh trắc học | 390×844 | `SecDone` |
| B8 | Bảo mật · Đang hoàn tất (BM-04) | 390×844 | `SecLoading` |
| B1 | Bảo mật · Đặt mã PIN · Tối | 390×844 | `SecEmptyDark` |
| B6 | Bảo mật · Đã khớp · Tối | 390×844 | `SecDoneDark` |
| B8 | Bảo mật · Đang hoàn tất · Tối | 390×844 | `SecLoadingDark` |
| L1 | Khóa · Nhập mã PIN (KH-01) | 390×844 | `LockEmpty` |
| L2 | Khóa · Hộp thoại sinh trắc học (KH-01) | 390×844 | `LockBio` |
| L3 | Khóa · Sai mã PIN | 390×844 | `LockWrong` |
| L4 | Khóa · Sắp xóa dữ liệu (KH-06) | 390×844 | `LockWarn` |
| L5 | Khóa · Bị khóa 30 giây (KH-02) | 390×844 | `LockCool30` |
| L6 | Khóa · Bị khóa 2 phút (KH-02) | 390×844 | `LockCool120` |
| L7 | Khóa · Quên mã PIN (KH-03) | 390×844 | `LockForgot1` |
| L8 | Khóa · Xác nhận xóa dữ liệu (KH-03) | 390×844 | `LockForgot2` |
| L1 | Khóa · Nhập mã PIN · Tối | 390×844 | `LockEmptyDark` |
| L3 | Khóa · Sai mã PIN · Tối | 390×844 | `LockWrongDark` |
| L5 | Khóa · Bị khóa 30 giây · Tối | 390×844 | `LockCool30Dark` |
| L8 | Khóa · Xác nhận xóa · Tối | 390×844 | `LockForgot2Dark` |

Mã B7 (`SecOff`, `SecOffDark`, màn thiết lập với công tắc đã tắt) **đã bỏ** cùng công tắc "Bảo vệ ứng dụng" (BM-01). Các mã B còn lại giữ nguyên, không đánh số lại. Mã K4 (`ConnectSuccess`) cũng đã bỏ.

**Luồng khởi động:** K1 → K3 → (thành công, config đã lưu) → K6 → "Thiết lập mã PIN" → B1 … B6 → B8 → Màn chính (tab Thư mục); hoặc K6 → "Để sau" → Màn chính (tab Thư mục). Back ở B1 quay lại K6 (BM-08). K3 thất bại → K5. Mở khóa (L1) xong thì về đúng tab và trạng thái trước khi khóa (KH-07). Page Khởi động **không đổi hình** ở Lát 8.

**Bố cục chung:**
- **Splash:** mark 112 căn giữa. Dòng "Phim · Ảnh · Truyện trên OneDrive" `caption` 500 màu `ink-muted`, cách đáy 56. Chỉ hiện khi khởi động nguội.
- **Kết nối:**
    - Padding 48/16/32.
    - Đầu trang: mark 40, rồi tiêu đề `display` cách 16. Không có StepBar. Phụ đề `body` màu `ink-muted`.
    - Form cách đầu trang 28, gồm 4 TextField cách nhau 16. Nút "Kết nối" toàn chiều rộng nằm sát đáy.
    - Kết nối thành công (KN-08): khi K3 kết thúc thì hiện hộp thoại K6 ngay trên màn Kết nối. Không có bottom sheet, thẻ tài khoản, loại drive, UPN hay thanh dung lượng (KN-12).
    - K5: Dialog danger có dòng mã lỗi. Nút "Sửa Client Secret" (primary) và "Đóng".
    - **K6 (KN-13):** Dialog bắt buộc chọn (mục 4.2), role dialog, không đóng bằng chạm scrim hay Back.
        - Icon `lock` 48 tông volt.
        - Tiêu đề `title`: "Thiết lập mã PIN?"
        - Nội dung `body` màu `ink-muted`: "Mã PIN 6 số sẽ khóa ứng dụng mỗi khi mở. Nếu bỏ qua, bất kỳ ai cầm máy đang mở khóa đều xem được OneDrive của bạn. Bạn có thể bật lại trong Cài đặt › Bảo mật."
        - Nút căn phải, cách 8: "Để sau" (ghost), "Thiết lập mã PIN" (primary).
        - Câu chữ tự viết, chờ xác nhận (đề xuất 7).
- **Thiết lập bảo mật:**
    - Padding 40/16/24 (khi có bàn phím) hoặc 32, các khối cách 16.
    - Đầu trang: icon `lock` 40 màu `volt-text`, rồi tiêu đề `display`. Không có StepBar. Không có thẻ công tắc "Bảo vệ ứng dụng".
    - Khối PIN: tiêu đề `heading`, PinDots, dòng thông báo, rồi bàn phím sát đáy.
    - Nút Back (hệ thống và mũi tên trên đầu trang) quay lại K6 (BM-08).
- **Khóa:**
    - Padding 64/16/16. Mark 48 và tiêu đề `screen-title`, PinDots, thông báo.
    - Bàn phím ở đáy, bên dưới là nút ghost "Quên mã PIN".
    - Không hiện tên tệp hay bất kỳ nội dung nào.
    - Khóa tạm: đồng hồ `timer` "00:30", thanh tiến độ rộng 220, ghi chú `caption`. Ẩn phím sinh trắc học (KH-02, đã chốt).

### 5.2 Danh sách (tab Thư mục và Thư viện của Màn chính)

| Mã | Màn / trạng thái | Kích thước | Ảnh tham chiếu (artboard) |
|---|---|---|---|
| D1 | Thư mục · Dạng lưới (TM-01, TM-06, DS-02) | 390×844 | `ListFolderGrid` |
| D2 | Thư mục · Dạng danh sách, breadcrumb rút gọn | 390×844 | `ListFolderList` |
| D3 | Thư viện · Nhóm theo ngày, chip, cuộn nhanh | 390×844 | `ListLibrary` |
| D4 | Tìm kiếm · Có kết quả (DS-03) | 390×844 | `ListSearch` |
| D5 | Sắp xếp · Bottom sheet (TM-05) | 390×844 | `ListSort` |
| D6 | Offline (DS-05) | 390×844 | `ListOffline` |
| D7 | Thư viện · Đang lập chỉ mục (TV-06) | 390×844 | `ListIndexing` |
| D8 | Thư mục · Chưa có tệp phù hợp | 390×844 | `ListEmpty` |
| D4 | Tìm kiếm · Không có kết quả | 390×844 | `ListSearchEmpty` |
| D1 | Thư mục · Dạng lưới · Tối | 390×844 | `ListFolderGridDark` |
| D3 | Thư viện · Tối | 390×844 | `ListLibraryDark` |
| D4 | Tìm kiếm · Tối | 390×844 | `ListSearchDark` |
| D7 | Đang lập chỉ mục · Tối | 390×844 | `ListIndexingDark` |
| D9 | Thư mục · Banner secret sắp hết hạn (CD-06, Q4) | 390×844 | `ListExpireBanner` |

**Lát 8: toàn bộ artboard D1 đến D8 (kể cả bản tối) cần vẽ lại** theo bố cục mới dưới đây (mục 11). Cho tới khi vẽ lại, phần nội dung giữa AppBar và đáy vẫn so theo artboard cũ.

**Thứ tự khối (từ Lát 8), cách nhau 12:**
1. Padding trên 24 (thanh trạng thái).
2. AppBar (chỉ còn nút Tìm kiếm bên phải).
3. Banner (offline, đang lập chỉ mục, hoặc secret sắp hết hạn), nếu có.
4. Dải Xem tiếp (chỉ ở gốc Thư mục).
5. Breadcrumb.
6. SortBar.
7. Nội dung, cuộn được, kết thúc ở mép trên thanh đáy.
8. Thanh điều hướng đáy N1 (mục 4.7), cố định ở đáy.

Bỏ khối **Tabs** (trước đây ở vị trí 3).

**Tab Thư viện:** chip lọc Tất cả / Ảnh / Video, rồi nhóm theo ngày, rồi lưới 4 cột. FastScroller cách đỉnh khoảng 250, kết thúc ở mép trên thanh đáy.

**Trạng thái:**
- **D4 tìm kiếm:** bàn phím mở nên **không có thanh đáy** (DH-05).
- **D5 sắp xếp:** sheet phủ lên thanh đáy.
- **D6 offline:** banner neutral `cloud-off`. Thẻ chưa cache có dấu `cloud-off`. Bấm tệp chưa cache thì hiện Snackbar "Cần kết nối mạng để mở tệp này" kèm nút "Đóng" (Snackbar nằm trên thanh đáy).
- **D7 đang lập chỉ mục:** banner volt `sync` có ProgressBar 4 bên trong. Ô chưa tải là khối `surface-2`, không nhấp nháy. Thanh đáy là **bản 3 mục** N2 (Short chưa hiện vì đồng bộ lần đầu chưa xong).
- **D9 secret sắp hết hạn:** dùng lại Banner C7 (tông warning, icon `clock`, nút "Cập nhật") ở vị trí 3; mục Cài đặt trên thanh đáy có chấm nhắc N3. Nút "Cập nhật" chuyển sang tab Cài đặt rồi mở luồng cập nhật secret.

### 5.3 Màn xem

Page Màn xem **không đổi hình** ở Lát 8 (thanh điều hướng đáy ẩn ở các màn này).

| Mã | Màn / trạng thái | Kích thước | Ảnh tham chiếu (artboard) |
|---|---|---|---|
| V1 | Video · Điều khiển hiện (VD-01, VD-02, VD-10) | 390×844 | `ViewVideoControls` |
| V2 | Video · Điều khiển ẩn (VD-01) | 390×844 | `ViewVideoHidden` |
| V3 | Video · Hỏi xem tiếp (VD-12) | 390×844 | `ViewVideoResume` |
| V4 | Video · Chạm đúp tua (VD-03) | 390×844 | `ViewVideoDouble` |
| V5 | Video · Vuốt chỉnh độ sáng (VD-04) | 390×844 | `ViewVideoSwipe` |
| V6 | Video · Tốc độ phát (VD-05) | 390×844 | `ViewVideoSpeed` |
| V7 | Video · Khóa thao tác (VD-08) | 390×844 | `ViewVideoLocked` |
| V8 | Video · Tự phát kế tiếp (VD-13) | 390×844 | `ViewVideoNext` |
| V9 | Video · Không hỗ trợ định dạng (VD-15) | 390×844 | `ViewVideoCodec` |
| V10 | Video · Mất mạng (VD-16) | 390×844 | `ViewVideoNet` |
| V11 | Video · Đang tải | 390×844 | `ViewVideoBuffer` |
| V12 | Video · Hướng ngang, nút xoay (VD-07, VD-19) | 844×390 | `ViewVideoLand` |
| V3 | Video · Hỏi xem tiếp · Tối | 390×844 | `ViewVideoResumeDark` |
| V6 | Video · Tốc độ phát · Tối | 390×844 | `ViewVideoSpeedDark` |
| V13 | Video · Khung hình: Cắt đầy (VD-06) | 390×844 | `ViewVideoFit` |
| V14 | Video · Zoom hai ngón (VD-18) | 390×844 | `ViewVideoZoom` |
| V15 | Video · Thông tin tệp (VD-17) | 390×844 | `ViewVideoInfo` |
| V15 | Video · Thông tin tệp · Tối | 390×844 | `ViewVideoInfoDark` |
| V16 | Video · Thông tin ở hướng ngang (VD-17) | 844×390 | `ViewVideoInfoLand` |
| V4b | Video · Chạm đúp trái, tua lùi (VD-03) | 390×844 | `ViewVideoDoubleL` |
| V5b | Video · Vuốt lên bên phải, âm lượng (VD-04) | 390×844 | `ViewVideoSwipeVol` |
| V17 | Video · Đang tải khi điều khiển hiện | 390×844 | `ViewVideoLoadCtl` |
| V18 | Video · Đổi chế độ phát (VD-20) | 390×844 | `ViewVideoMode` |
| V19 | Video · Hết video, nút Phát lại (VD-21) | 390×844 | `ViewVideoReplay` |
| V6 | Video · Tốc độ phát ở hướng ngang (VD-05) | 844×390 | `ViewVideoSpeedLand` |
| A1 | Ảnh · Vừa khung, thanh công cụ hiện (AN-01) | 390×844 | `ViewImgFit` |
| A2 | Ảnh · Toàn màn hình (AN-04) | 390×844 | `ViewImgImmersive` |
| A3 | Ảnh · Đang zoom (AN-02) | 390×844 | `ViewImgZoom` |
| A4 | Ảnh · Thông tin (AN-05) | 390×844 | `ViewImgInfo` |
| A5 | Ảnh · Lỗi định dạng (AN-07) | 390×844 | `ViewImgError` |
| A4 | Ảnh · Thông tin · Tối | 390×844 | `ViewImgInfoDark` |
| P1 | PDF · Đang tải (PD-01) | 390×844 | `ViewPdfDownload` |
| P2 | PDF · Cuộn dọc, thanh công cụ hiện (PD-02, PD-04) | 390×844 | `ViewPdfRead` |
| P3 | PDF · Thanh công cụ ẩn (PD-06) | 390×844 | `ViewPdfHidden` |
| P4 | PDF · Lật trang ngang (PD-02) | 390×844 | `ViewPdfPaged` |
| P5 | PDF · Nhảy tới trang (PD-04) | 390×844 | `ViewPdfJump` |
| P6 | PDF · Không mở được (PD-07) | 390×844 | `ViewPdfError` |
| P7 | PDF · Đọc xong (PD-05) | 390×844 | `ViewPdfDone` |
| P5 | PDF · Nhảy tới trang · Tối | 390×844 | `ViewPdfJumpDark` |
| P8 | PDF · Thông tin tệp (PD-08) | 390×844 | `ViewPdfInfo` |
| P8 | PDF · Thông tin tệp · Tối | 390×844 | `ViewPdfInfoDark` |

**Luồng:**
- Mở video ở hướng theo Cài đặt › Video › Hướng màn hình khi mở video (mặc định **dọc**). Toàn app khóa dọc; riêng màn video xoay ngang bằng nút xoay, rời màn thì về dọc. Nút xoay đổi Dọc ↔ Ngang và không gián đoạn phát.
- Back thoát ngay cả khi đang ngang, rồi trở về hướng dọc (VD-19), về đúng tab và vị trí đã mở (DH-02).
- Mở lại video có vị trí đã lưu thì hiện Dialog V3 trước khi phát.
- Hết video: chế độ Không lặp, hoặc hết video cuối ở chế độ Tự phát tiếp, thì vào V19 (nút Phát lại, điều khiển không tự ẩn). Tự phát tiếp còn video sau thì vào V8.

### 5.4 Cài đặt (tab Cài đặt của Màn chính)

| Mã | Màn / trạng thái | Kích thước | Ảnh tham chiếu (artboard) |
|---|---|---|---|
| C1 | Cài đặt · Hiển thị và Bảo mật (CD-01, CD-02) | 390×844 | `SetTop` |
| C2 | Cài đặt · Video | 390×844 | `SetVideo` |
| C3 | Cài đặt · PDF và Bộ nhớ đệm (CD-07) | 390×844 | `SetStorage` |
| C4 | Cài đặt · Kết nối (CD-04, CD-05, CD-06) | 390×844 | `SetConn` |
| C5 | Cài đặt · Bảo vệ ứng dụng đang tắt (CD-03) | 390×844 | `SetSecOff` |
| C6 | Cài đặt · Chỉ còn 1 loại tệp (CD-01) | 390×844 | `SetTypes` |
| C7 | Cài đặt · Secret sắp hết hạn (CD-06) | 390×844 | `SetExpireBanner` |
| C1 | Cài đặt · Hiển thị và Bảo mật · Tối | 390×844 | `SetTopDark` |
| C3 | Cài đặt · Bộ nhớ đệm · Tối | 390×844 | `SetStorageDark` |
| C4 | Cài đặt · Kết nối · Tối | 390×844 | `SetConnDark` |
| O1 | Chọn · Giao diện | 390×844 | `OptTheme` |
| O2 | Chọn · Tự khóa khi rời app | 390×844 | `OptLock` |
| O3 | Chọn · Chế độ phát (VD-13, VD-20) | 390×844 | `OptMode` |
| O4 | Chọn · Khung hình mặc định (VD-06) | 390×844 | `OptFit` |
| O5 | Chọn · Tốc độ mặc định (VD-05) | 390×844 | `OptSpeed` |
| O6 | Chọn · Thời lượng tối đa của Short (CD-13), kèm bản đang kéo | 390×844 | `OptShortMax` |
| D1 | Xác nhận · Tắt bảo vệ ứng dụng (CD-03) | 390×844 | `DlgSecOff` |
| D2 | Xác nhận · Bật xóa dữ liệu tự động (CD-08) | 390×844 | `DlgWipe` |
| D3 | Xác nhận · Ngắt kết nối, bước 1 (CD-05) | 390×844 | `DlgDisc1` |
| D4 | Xác nhận · Ngắt kết nối, bước 2 (CD-05) | 390×844 | `DlgDisc2` |
| D5 | Xác nhận · Xóa bộ nhớ đệm | 390×844 | `DlgCache` |
| D6 | Xác nhận · Giảm giới hạn bộ nhớ đệm (CD-07) | 390×844 | `DlgShrink` |
| D4 | Xác nhận · Ngắt kết nối, bước 2 · Tối | 390×844 | `DlgDisc2Dark` |
| P1 | Nhập mã PIN hiện tại (CD-03, CD-04, CD-09) | 390×844 | `PinCurrent` |
| P2 | Đổi mã PIN · Nhập mã mới (CD-09) | 390×844 | `PinNew` |
| P3 | Đổi mã PIN · Nhập lại (CD-09) | 390×844 | `PinNewConfirm` |
| S1 | Cập nhật Client Secret · Nhập (CD-04) | 390×844 | `SecretForm` |
| S2 | Cập nhật Client Secret · Đang kiểm tra, loading toàn màn hình (KN-07) | 390×844 | `SecretChecking` |
| S3 | Cập nhật Client Secret · Lỗi, giữ secret cũ | 390×844 | `SecretError` |
| S3 | Cập nhật Client Secret · Lỗi · Tối | 390×844 | `SecretErrorDark` |
| S4 | Ngày hết hạn secret (CD-06) | 390×844 | `SecretExpiry` |
| S5 | Đã cập nhật Client Secret | 390×844 | `SecretSaved` |

**Lát 8: C1 đến C7 (kể cả bản tối) cần vẽ lại** phần thanh tiêu đề và thêm thanh đáy N1 (mục 11). O1 đến O5, D1 đến D6 (trừ việc thanh đáy nằm dưới scrim), P1 đến P3, S1 đến S5 **không đổi hình**.

**Bố cục:**
- Cài đặt là **một màn cuộn**, 6 nhóm theo thứ tự: Hiển thị, Bảo mật, Video, PDF, Bộ nhớ đệm, Kết nối. Cuối màn có dòng phiên bản `meta` căn giữa.
- C1 đến C4 là bốn vị trí cuộn của cùng màn đó.
- **Thanh tiêu đề (từ Lát 8):** chỉ có chữ "Cài đặt" (`title`), lề trái 16, **không có nút quay lại** vì Cài đặt là tab (đã vẽ ở C1 đến C7 bản mới). Cao 56 cộng 24 thanh trạng thái. Khi đã cuộn thì có kẻ đáy `line`.
- **Đáy:** thanh điều hướng đáy N1 với mục Cài đặt đang chọn. Nội dung cuộn kết thúc ở mép trên thanh.

**Thứ tự hàng:**
- **Hiển thị:** Loại tệp hiển thị (ChipsRow) → Giao diện → Ngôn ngữ. Hai hàng "Ẩn thư mục không có tệp phù hợp" và "Hiện tệp không hỗ trợ" có trong artboard nhưng **đã bỏ, không dựng** (TM-04, chốt 2026-10-07). Hàng Ngôn ngữ (CD-10) đã có artboard (2026-10-10), dựng theo kiểu ValueRow + sheet như Giao diện.
- **Bảo mật:** Bảo vệ ứng dụng → Đổi mã PIN → Mở khóa bằng sinh trắc học → **Bảo vệ màn hình** (SwitchRow, CD-12, không phụ thuộc PIN nên luôn hiện kể cả khi bảo vệ ứng dụng tắt) → Tự khóa khi rời app → Xóa dữ liệu khi nhập sai quá nhiều. Sheet O2 Tự khóa có các mốc: Ngay lập tức / 10 giây / 30 giây / 1 / 5 / 15 phút (mặc định 1 phút; 10 và 30 giây thêm 2026-10-07).
- **Video:** Bước tua khi chạm đúp → Tốc độ mặc định → Khung hình mặc định → Hướng màn hình khi mở video → Chế độ phát → Nhớ vị trí xem → **Thời lượng tối đa của Short** (ValueRow, giá trị "3 phút", mở sheet O6; chỉ hiện khi loại Video đang bật; thêm 2026-10-10, CD-13).
- **PDF:** Kiểu đọc.
- **Bộ nhớ đệm:** CacheUsage (theo loại) → Giới hạn tối đa → **Tỉ lệ chia theo loại** → Xóa bộ nhớ đệm. Hai hàng giữa mở sheet (ValueRow), **ngoài thiết kế, chờ duyệt** (chốt Q1 2026-10-07, ADR-0021):
    - Giới hạn tối đa: thanh trượt `ODVSlider` từ 1 đến 10 GB, bước 1 GB (mặc định 2 GB), hiện giá trị đang kéo, nút Áp dụng chỉ bật khi giá trị đổi. Thiết kế gốc chỉ có 4 mốc 1/2/5/10 GB.
    - Tỉ lệ chia theo loại (board "Slider và tỉ lệ chia" của ODV Foundations, đã có thiết kế): dưới tiêu đề và câu hướng dẫn là **SplitBar** cao 12 (các đoạn cách 2, bo 3, tô màu loại; bên phải tổng "100%" kèm dấu check `success`), rồi 4 hàng slider (Thumbnail `line-strong`, Ảnh `kind-photo`, Video `kind-video`, PDF `kind-pdf`), mỗi hàng 0 đến 100%. Kéo một hàng thì ba hàng còn lại tự tính lại để tổng luôn 100. Hai nút: Mặc định (10/30/45/15, tắt khi đang là mặc định) và Áp dụng (tắt khi chưa đổi).
    - `ODVSlider`: rãnh cao 8 bo 4 màu `surface-2`, phần đã có tô màu loại; núm `volt` 24 viền 2 `volt-text`; **đang kéo** núm 32 kèm vòng 6 `volt-soft`; vùng chạm cao 48, hai đầu lùi 12; focus ring quanh núm; vô hiệu mờ 0.38. Không dùng bong bóng nổi (ngón tay che): giá trị hiện ở viên góc phải của hàng (`code` 13, "45%" đậm vừa + dung lượng mờ 75%), đổi sang nền `volt-soft` chữ `volt-text` khi đang kéo. TalkBack đọc "Video 45%, 921,6 MB"; slider giới hạn 1 đến 10 GB dùng cùng kiểu nhưng màu `volt-text`, không có ô loại (thiết kế chưa vẽ riêng).
    - Giảm giới hạn hoặc đổi tỉ lệ làm loại nào vượt trần mới thì dọn ngay (D6 cho trường hợp giảm giới hạn, CD-07).
- **Kết nối:** Tài khoản (UPN), Tenant ID, Client ID, Đồng bộ gần nhất (InfoRow) → Cập nhật Client Secret → Ngày hết hạn secret → Ngắt kết nối.

**O6 · Thời lượng tối đa của Short (đã vẽ `OptShortMax`, 2026-10-10; mô tả dưới đây cần đối chiếu lại với artboard, nhất là nút Áp dụng, trước khi dựng):** Bottom sheet cùng kiểu sheet Giới hạn tối đa của bộ nhớ đệm: tiêu đề `heading` "Thời lượng tối đa của Short", câu hướng dẫn `caption` màu `ink-muted` "Chỉ video dài tối đa bằng mốc này mới xuất hiện ở tab Short.", một `ODVSlider` màu `volt-text` từ 3 đến 10, bước 1, viên giá trị "3 phút" ở góc phải, nút Áp dụng (tắt khi chưa đổi). Mặc định 3 phút. Câu chữ là [mới]. TalkBack đọc "Thời lượng tối đa, 3 phút".

**Luồng:**
- Các hành động sau đi qua P1 (Nhập mã PIN hiện tại), rồi mới tới hộp thoại hoặc màn tiếp theo; dòng phụ đề của P1 đổi theo mục đích:
    - Tắt bảo vệ
    - Bật xóa dữ liệu tự động
    - Đổi PIN
    - Cập nhật Client Secret
- Đổi PIN đi P1 → P2 → P3. PIN dễ đoán và PIN không khớp dùng cùng thông báo như B3 / B5.
- Cập nhật secret đi S1 → S2 (loading toàn màn, không có Hủy) → S3 (lỗi, giữ secret cũ) hoặc quay về Cài đặt kèm Snackbar S5. Đã dựng (Lát 7e, 2026-10-08): S2 chỉ có tiêu đề và phụ đề, chưa có danh sách ba bước ("Đã lấy access token", "Đang kiểm tra OneDrive…", "Lưu secret mới") vì use case chạy một lần, không báo từng bước. Chuỗi lỗi hết hạn, mạng, lỗi khác, "hết hạn hôm nay", "đã hết hạn" và dòng báo ngày sai định dạng ở S4 là chuỗi [mới], thiết kế chưa có.
- Khi bảo vệ đang **tắt** (kể cả khi người dùng chọn "Để sau" ở K6), công tắc "Bảo vệ ứng dụng" bật lên thì đi theo luồng B1 → … → B8 (CD-02). Hành động trong danh sách trên không cần P1 vì chưa có PIN.
- Các màn con (P1 đến P3, S1 đến S5, B1 đến B8 mở từ Cài đặt) che thanh đáy; Back hoặc hoàn tất thì về tab Cài đặt đúng vị trí cuộn (DH-02, DH-05).

### 5.5 Short (tab Short của Màn chính, Lát 8)

Page mới **"Short"** trên canvas (page riêng, vì Short có thanh đáy còn page Màn xem thì không). Đã vẽ SH1 đến SH9 cộng bản "Đã xáo lại" (viên thuốc sau khi xáo, SV-03). Page có hai ghi chú chứa luồng và toàn bộ SV-01 đến SV-16, DH-01 đến DH-08.

| Mã | Màn / trạng thái | Kích thước | Ảnh tham chiếu (artboard) |
|---|---|---|---|
| SH1 | Short · Video dọc, Cắt đầy, tên tệp, thanh tiến độ (SV-07, SV-08, SV-09) | 390×844 | `ShortPortrait` |
| SH2 | Short · Video ngang, Vừa khung (SV-08) | 390×844 | `ShortLandscape` |
| SH3 | Short · Video dọc lệch tỉ lệ (3:4), Vừa khung (SV-08) | 390×844 | `ShortPortraitFit` |
| SH4 | Short · Đang tạm dừng (SV-06) | 390×844 | `ShortPaused` |
| SH5 | Short · Đang tải | 390×844 | `ShortBuffer` |
| SH6 | Short · Kéo xuống ở video đầu để xáo lại (SV-03) | 390×844 | `ShortReshuffle` |
| SH7 | Short · Không hỗ trợ định dạng (SV-14) | 390×844 | `ShortCodec` |
| SH8 | Short · Mất mạng (SV-14) | 390×844 | `ShortNet` |
| SH9 | Short · Không có video phù hợp (SV-15) | 390×844 | `ShortEmpty` |

Tab Short không có bản sáng/tối riêng (luôn nền đen); thanh đáy luôn là bản N4. Thành phần chi tiết ở mục 4.8.

**Luồng:**
- Vào tab Short: phát video đang dừng ở đó (lần đầu trong phiên: video đầu của danh sách đã xáo, SV-02).
- Vuốt lên: video sau; vuốt xuống: video trước (SV-04). Ở video đầu, kéo xuống là xáo lại (SH6). Ở video cuối, vuốt lên không chuyển.
- Chạm: tạm dừng (SH4) / phát tiếp.
- Rời tab, app xuống nền hoặc bị khóa: tạm dừng; quay lại thì phát tiếp đúng video và vị trí (SV-11).
- SH9 "Mở Cài đặt": chuyển sang tab Cài đặt, cuộn tới nhóm Video.

---

## 6. Hành vi theo màn

| Tình huống | Hành vi |
|---|---|
| Nhấn nút | Co 98% trong 150ms. Nút chính đổi sang `volt-pressed`. Hàng danh sách phủ `surface-2` |
| Mở tệp từ Danh sách | Thumbnail mở rộng thành màn xem trong 400ms, nền chuyển sang đen, thanh đáy bị che. Quay lại thì thu về đúng ô cũ và giữ vị trí cuộn |
| Đổi tab trên thanh đáy (DH-02) | Nội dung cross-fade 150ms, không trượt. Tab hiện đúng trạng thái và vị trí cuộn lần trước |
| Chạm lại tab đang chọn (DH-04) | Thư mục: cuộn lên đầu, đã ở đầu thì về thư mục gốc. Thư viện, Cài đặt: cuộn lên đầu. Short: không làm gì |
| Back ở Màn chính (DH-03) | Lùi trong tab trước (lên một cấp, thoát tìm kiếm, đóng sheet). Ở gốc tab khác Thư mục thì chuyển sang tab Thư mục. Ở gốc Thư mục thì thoát app |
| Mục Short xuất hiện (DH-01) | Khi đồng bộ lần đầu xong: thanh đổi từ 3 sang 4 mục, các mục giãn lại trong 250ms, không nhảy |
| Vuốt chuyển video ở Short (SV-04) | Trang theo ngón tay, thả thì chạy tới video kế trong 250ms ease-out; chưa quá nửa thì về chỗ cũ |
| Tạm dừng ở Short (SV-06) | Vòng `play` hiện bằng fade 150ms, ẩn khi phát tiếp |
| Kéo để xáo lại (SV-03) | Vòng `sync` theo ngón tay, thả quá ngưỡng thì xoay tới khi xáo xong, rồi viên "Đã xáo lại" 2 giây |
| Ảnh tải xong (AN-01) | Hiện thumbnail lớn ngay, cross-fade sang ảnh gốc trong 150ms. Chỉ báo tải nhỏ, không che ảnh |
| Điều khiển video (VD-01) | Chạm để hiện hoặc ẩn. Tự ẩn sau 3 giây không thao tác, trừ khi đang ở trạng thái Phát lại, đang tải, hoặc đang mở sheet hay dialog |
| Đổi khung hình / chế độ phát | Mỗi lần bấm xoay vòng, hiện viên thuốc nhãn 2 giây. Chế độ được nhớ |
| Zoom video (VD-18) | 1x–4x. Khi > 1x, một ngón kéo để di chuyển, tắt vuốt chỉnh sáng/âm lượng, hiện nút Đặt lại zoom. Zoom về 1x khi đổi video hoặc đổi hướng |
| Khóa thao tác (VD-08) | Chặn mọi chạm. Giữ nút mở khóa 1 giây, vòng volt chạy theo thời gian giữ. Rung nhẹ khi khóa và khi mở |
| Mất mạng (VD-16) | Phát hết phần đã tải trước rồi mới hiện thẻ Mất mạng. Không tự chuyển video khi mất mạng |
| Tự phát tiếp (VD-13) | Vòng đếm 5 giây đầy dần tuyến tính. Bấm Hủy thì ở lại màn, hiện trạng thái Phát lại |
| Hộp thoại hỏi PIN sau kết nối (K6, KN-13) | Hiện ngay khi kết nối thành công. Chạm ngoài và Back hệ thống không đóng. "Để sau" vào Màn chính (tab Thư mục); "Thiết lập mã PIN" mở B1. Back ở B1 quay lại K6 |
| Snackbar | Trượt lên từ đáy (ở Màn chính: từ mép trên thanh đáy) trong 250ms, ở lại 3 giây rồi mờ dần |
| Kéo để đồng bộ (DS-04) | Icon `sync` xoay khi đang chạy, dừng ngay khi xong, không nảy |
| Lỗi nhập (KN-06) | Dòng lỗi hiện trong 150ms, không rung |
| Nhập sai PIN | Chấm đỏ, rung nhẹ, dòng lỗi. Bộ đếm theo KH-02 |
| Đổi theme | Cross-fade toàn màn trong 250ms |
| Lựa chọn trong sheet | Chạm là áp dụng ngay và đóng sheet trong 250ms |

---

## 7. Trợ năng

- **Nhãn TalkBack nói việc sẽ xảy ra, không tả hình:**

  | Thành phần | Nhãn |
    |---|---|
  | Nút giữa video | "Phát" / "Tạm dừng" / "Phát lại" / "Đang tải video" |
  | Tua | "Tua lùi 10 giây" / "Tua tới 10 giây" (theo cài đặt bước tua) |
  | Trước / sau | "Video trước" / "Video kế tiếp" |
  | Thanh tua | "Vị trí phát, 12 phút 4 giây trên 1 giờ 26 phút" |
  | Tốc độ, chế độ, khung hình | "Tốc độ phát 1x", "Chế độ phát: Tự phát tiếp", "Khung hình: Vừa khung" |
  | Xoay, thông tin, khóa | "Xoay màn hình", "Thông tin tệp", "Khóa thao tác", "Giữ để mở khóa thao tác" |
  | Ô bị che | "Hiện ký tự" / "Ẩn ký tự" |
  | PinDots | "Mã PIN, đã nhập 3 trên 6 số" |
  | Phím PIN đặc biệt | "Xóa số cuối", "Mở bằng sinh trắc học" |
  | Chip lọc | "Ảnh, đang chọn" |
  | Ô Thư viện | "Video, mau-01.mp4, 1 giờ 26 phút, 974 MB, 23 tháng 5, 2026" |
  | Nút số trang PDF | "Đi tới trang, hiện ở trang 72 trên 310" |
  | Tệp chưa cache | "Chưa có trong bộ nhớ đệm" |
  | AppBar | "Tìm kiếm", "Lên một cấp", "Quay lại" |
  | Thanh điều hướng đáy | "Thư mục, tab 1 trên 4, đang chọn"; "Cài đặt, tab 4 trên 4, có thông báo" |
  | Vùng video Short | "Video ngắn, mau-01.mp4, đang phát" / "..., đã tạm dừng"; hành động "Tạm dừng" / "Phát", "Video kế tiếp", "Video trước", "Xáo lại danh sách" (chỉ ở video đầu) |

- **Thứ tự đọc** theo thứ tự thị giác. Banner và dòng lỗi là vùng live, đọc ngay khi xuất hiện. Thanh điều hướng đáy đọc sau cùng.
- **Mọi cử chỉ có nút tương đương:**
    - chạm đúp ↔ nút tua
    - vuốt sáng/âm lượng ↔ âm lượng hệ thống
    - vuốt đổi ảnh ↔ mũi tên khi bật TalkBack
    - vuốt chuyển video ở Short ↔ hành động tùy chỉnh "Video kế tiếp" / "Video trước"
    - kéo xáo lại ở Short ↔ hành động tùy chỉnh "Xáo lại danh sách"
- **Thao tác một tay:** điều khiển chính nằm nửa dưới màn hình; thanh điều hướng đáy nằm trong vùng ngón cái.
- **Không dựa vào màu đơn thuần:** lỗi luôn có viền, icon `alert` và câu chữ. Loại tệp luôn có icon riêng. Mục đang chọn trên thanh đáy có cả viên chọn lẫn màu nhãn; chấm nhắc có thêm ", có thông báo" trong nhãn TalkBack.

---

## 8. Quyết định thiết kế

**Đã chốt:**
- PIN 6 số, nhập bằng bàn phím tự vẽ, đủ 6 số thì tự chuyển bước.
- Trong lúc khóa tạm thì ẩn phím sinh trắc học.
- "Quên mã PIN" có 2 bước xác nhận.
- **Không có StepBar** ở Kết nối và Thiết lập bảo mật (luồng có nhánh nên không còn đếm bước).
- Kết nối thành công thì **lưu config ngay** (chế độ thiết bị) rồi hiện **hộp thoại K6** hỏi thiết lập mã PIN. Không có thẻ tài khoản, loại drive hay thanh dung lượng (KN-08, KN-12, KN-13).
- K6 bắt buộc chọn một trong hai nút ("Thiết lập mã PIN" / "Để sau"), không đóng bằng chạm ngoài hay Back. Chỉ hiện một lần cho mỗi lần kết nối mới.
- "Để sau" vào thẳng Màn chính (tab Thư mục) với bảo mật tắt; bật lại ở Cài đặt › Bảo mật.
- Màn Thiết lập bảo mật vào thẳng bước đặt PIN, không còn công tắc. Back ở B1 quay lại K6, giữ config đã lưu (BM-08).
- **Màn chính có thanh điều hướng đáy** (chốt 2026-10-10, ADR-0023, thay cho "Màn Danh sách là Home, bánh răng trên AppBar mở Cài đặt"): 4 mục Thư mục, Thư viện, Short, Cài đặt; Cài đặt là tab; mỗi tab giữ trạng thái riêng; bỏ Tabs và nút bánh răng.
- **Tab Short** (chốt 2026-10-10): chỉ video, không nút tương tác; video dọc lấp đầy, video ngang như thường; thanh đáy nền tối ở tab này; tên tệp mờ ở góc dưới; kéo ở video đầu để xáo lại; mục Short chỉ hiện khi bật Video và đã đồng bộ xong lần đầu.
- Không có "Kệ truyện". PDF xem qua Thư mục và dải Đọc tiếp.
- Không có "Mở bằng ứng dụng khác".
- Video không có nút toàn màn hình. Nút xoay đổi hướng; mở ở hướng dọc theo mặc định, đổi được ở Cài đặt › Video (VD-19).
- Loading toàn màn hình khi kiểm tra Client Secret, không có nút Hủy.

**Đề xuất, cần xác nhận trước khi code (nếu không ai phản đối thì làm theo thiết kế):**
1. Mở khóa thao tác video bằng cách **giữ 1 giây**.
2. Nút "Tiếp tục" ở thẻ Mất mạng tắt cho tới khi có mạng lại.
3. Khung hình mặc định có 4 lựa chọn, thêm "Nhớ lần gần nhất" (mặc định).
4. Khi tắt bảo vệ ứng dụng, ẩn Đổi PIN, sinh trắc học, tự khóa và xóa dữ liệu.
5. Lựa chọn trong sheet áp dụng ngay, không có nút Lưu.
6. Tắt bảo vệ, bật xóa dữ liệu tự động, đổi PIN và cập nhật secret đều cần PIN hiện tại.
7. Câu chữ tự viết, chưa có trong đặc tả:
    - lỗi ảnh, lỗi PDF
    - "Đã đọc xong. Đã gỡ khỏi Đọc tiếp."
    - các hộp thoại xóa bộ nhớ đệm và giảm giới hạn
    - thông báo "Lỗi khác" ở KN-09
    - nội dung hộp thoại K6: tiêu đề "Thiết lập mã PIN?", đoạn giải thích, nút "Để sau" và "Thiết lập mã PIN"
    - Short: "Đã xáo lại", trạng thái trống SH9, sheet O6
8. Nhảy trang PDF dùng bàn phím số của hệ thống.
9. **Thanh điều hướng đáy (Lát 8, đã vẽ, đã chốt):** số liệu ở mục 4.7 (cao 64, viên 56 × 32, nhãn `label`, chọn `volt-soft`/`volt-text`), đổi tab bằng cross-fade không trượt.
10. **Short (Lát 8, đã vẽ, đã chốt):** page canvas riêng "Short"; video vẽ tràn dưới thanh trạng thái; tên tệp có dải gradient `media.scrim` phía sau; thanh tiến độ 3dp sát mép trên thanh đáy; vòng tạm dừng 72 nền `media.pill`.
11. **Tab Cài đặt (đã vẽ, đã chốt):** thanh tiêu đề chỉ có chữ "Cài đặt", bỏ mũi tên quay lại. Tab Thư viện có tiêu đề "Thư viện".
12. **O6 (đã vẽ `OptShortMax`; đối chiếu nút Áp dụng với artboard khi dựng):** chọn thời lượng tối đa bằng thanh trượt 3 đến 10 phút + nút Áp dụng (cùng kiểu sheet Giới hạn tối đa), không dùng danh sách radio.

---

## 9. Chênh lệch đã biết (xử lý trước hoặc trong lúc code)

| # | Chỗ lệch | Cách xử lý đề xuất |
|---|---|---|
| 1 | PD-08 có nút ⓘ mở thông tin PDF, nhưng thanh trên của PDF (P2, P4) mới có nút `tune` | Chờ người thiết kế quyết định vị trí nút ⓘ. Cho tới lúc đó, P8 là màn đích |
| 2 | File `foundations-0x-*.md` cũ của Design System còn ghi "passphrase", "video ngang tự xoay", nút "Lấp đầy" | Bỏ qua. Theo tài liệu này và đặc tả (PIN 6 số, VD-07, "Cắt đầy") |
| 3 | Foundations ghi `display` cho tiêu đề Danh sách và Cài đặt | Màn thật dùng `title` (22/28) trên AppBar. Theo màn thật |
| 4 | Foundations chỉ có 11 style chữ | Màn thật dùng thêm 10 style phụ (mục 2.3). Đã gộp vào token |
| 5 | Mã màn trùng giữa page (D1, P1, P5…) | Luôn ghi kèm tên page |
| 6 | Board "06 Icon" trên canvas đang hiện 37 icon. 5 icon `info`, `minus`, `fit`, `next`, `prev` đã dùng trên màn nhưng chưa có trên board; icon `short` (Lát 8) chưa có | Dùng đủ 43 icon như mục 3.1. Bản cập nhật board (42 icon) đã sẵn sàng, chờ đăng; `short` cần vẽ |
| 7 | Khi xem với font thật, các board Foundations 02, 03, 07, 11, 12 bị tràn hoặc cắt chữ | Chỉ ảnh hưởng canvas, không ảnh hưởng app. Bản sửa chiều cao đã sẵn sàng, chờ đăng |
| 8 | Artboard K4 `ConnectSuccess` (bottom sheet "Đã kết nối OneDrive" có thanh dung lượng) vẫn còn trên canvas v22, nhưng KN-08 và KN-12 đã bỏ màn này | Không dựng K4. Cần xóa hoặc đánh dấu "đã bỏ" artboard này trên canvas |
| 9 | Các artboard K1, K2, K3, K5 (kể cả bản tối) và B1 đến B6, B8 trên canvas v22 còn hiện StepBar "BƯỚC 1 / 2", "BƯỚC 2 / 2" | Không dựng StepBar. Cần gỡ StepBar khỏi các artboard này trên canvas; style `step-label` trong token gỡ theo |
| 10 | Phần đã dựng ở Cài đặt (Lát 7) mà **còn thiếu artboard**, chuỗi VI/EN đánh dấu [mới], chờ duyệt: sheet Hướng màn hình khi mở video; sheet Kiểu đọc PDF. **Đã có artboard (2026-10-10):** hàng Ngôn ngữ (CD-10), hàng Bảo vệ màn hình (CD-12), các mốc Tự khóa 10 và 30 giây | Dựng theo kiểu ValueRow/SwitchRow + sheet đã có (mục 4.6). Không thêm màu, cỡ chữ hay khoảng cách mới. Cần người thiết kế bổ sung artboard cho hai sheet còn thiếu |
| 11 | Bộ nhớ đệm: giới hạn tùy chỉnh 1 đến 10 GB và tỉ lệ chia theo loại (Q1, ADR-0021) dùng **thanh trượt `ODVSlider`**, nút Áp dụng và Mặc định; artboard `SetStorage`, `OptCache` chỉ có 4 mốc 1/2/5/10 GB | Theo mục 5.4. `ODVSlider`, `ODVSliderRow`, `ODVSplitBar` đã có board "Slider và tỉ lệ chia" trong ODV Foundations và đã dựng (2026-10-08); riêng sheet Giới hạn tối đa (1 đến 10 GB) vẫn chưa có artboard |
| 12 | Banner secret sắp hết hạn (C7, CD-06) chỉ có ở Cài đặt trong artboard, nhưng đã chốt hiện cả ở màn Danh sách (Q4) | Dùng lại Banner C7 ở đầu Danh sách (D9, mục 5.2); cần vẽ artboard `ListExpireBanner` |
| 13 | Video lỗi ở Tự phát tiếp / Lặp danh sách: thẻ lỗi kèm đếm ngược 5 giây và nút Hủy (VD-15); artboard V9 chỉ có thẻ lỗi tĩnh | Dùng lại vòng đếm ngược của thẻ "Tiếp theo" (V8) trong thẻ lỗi V9 |
| 14 | Artboard B7 `SecOff`, `SecOffDark` và thẻ công tắc "Bảo vệ ứng dụng" trên B1 đến B6 vẫn còn trên canvas, nhưng BM-01 đã bỏ công tắc | Không dựng B7 và công tắc. Cần xóa hoặc đánh dấu "đã bỏ" trên canvas |
| 15 | Chưa có artboard K6 (hộp thoại hỏi thiết lập mã PIN, KN-13), cả bản sáng lẫn tối | Người thiết kế dựng theo mô tả ở mục 5.1. Trong lúc chờ, dev dựng theo mô tả và component Dialog (mục 4.2) |
| 16 | **Lát 8 (đã xử lý 2026-10-10):** Danh sách (D1 đến D8, D9) và Cài đặt (C1 đến C7) đã vẽ lại với thanh đáy, bỏ Tabs, bánh răng và mũi tên quay lại; đã có NavBar N1 đến N4, page Short (SH1 đến SH9 và bản "Đã xáo lại"), O6 (kèm bản đang kéo), D9, icon `short` (43 icon). Hai hàng "Ẩn thư mục…" và "Hiện tệp không hỗ trợ" đã bỏ khỏi artboard. Sheet "Tỉ lệ chia theo loại" bỏ mã O6 để không trùng | Dựng theo artboard cùng mục 4.7, 4.8, 5.2, 5.4, 5.5; nếu số liệu artboard lệch mô tả thì hỏi lại, không tự chế giá trị |
| 17 | Token `nav-bar`, `nav-indicator-width`, `nav-indicator-height`, `nav-dot` (mục 2.6): **đã có** trong `odv-tokens.json` và board Bố cục (2026-10-10); `tab-item` không còn dùng | Dùng token. Gỡ `tab-item` cùng lúc với `step-label` khi người thiết kế gỡ khỏi board |

---

## 10. Nghiệm thu: code có giống thiết kế không

**Thiết lập so ảnh:**
- Preview và screenshot test (Roborazzi hoặc Paparazzi) với cấu hình 390 × 844dp, mật độ 420dpi, font scale 1.0, cả theme sáng và tối.
- Mỗi mã ở mục 5 có ít nhất một ảnh test. Đặt tên theo `page_mã_artboard`, ví dụ `settings_D4_DlgDisc2`.
- So với ảnh xuất từ artboard tương ứng. Chấp nhận lệch ≤ 2dp về vị trí, kích thước khớp tuyệt đối, màu khớp tuyệt đối.

**Checklist cho mỗi màn:**
- [ ] Màu chỉ từ token, đúng cả theme sáng và tối. Màn xem và tab Short luôn đen
- [ ] Chữ đúng style (họ, cỡ, dòng, đậm). Tắt font padding. Số đổi liên tục dùng JetBrains Mono
- [ ] Khoảng cách, bo góc, kích thước khớp mục 2 và 4. Lề màn 16
- [ ] Vùng chạm ≥ 48dp, cách nhau ≥ 8dp
- [ ] Đủ trạng thái: mặc định, nhấn, focus, tắt, lỗi, đang tải, trống, offline (nếu màn có)
- [ ] Câu chữ khớp nguyên văn, viết hoa kiểu câu, số và ngày theo ngôn ngữ đang dùng (CD-10)
- [ ] Nhãn TalkBack theo mục 7. Thứ tự đọc đúng
- [ ] Chuyển động đúng thời lượng và easing. Giảm hiệu ứng hoạt động
- [ ] Cỡ chữ 200%: không cắt chữ, hàng tự cao lên
- [ ] Chặn chụp màn hình ở các màn yêu cầu
- [ ] Edge-to-edge: không bị thanh hệ thống che, đúng màu thanh hệ thống

**Kiểm riêng màn xem video:**
- [ ] Mở ở hướng theo Cài đặt (mặc định dọc). Xoay không tải lại video. Back thoát một lần
- [ ] Tự ẩn sau 3 giây. Không tự ẩn ở trạng thái Phát lại hay đang tải
- [ ] Chạm đúp cộng dồn. Vuốt bị tắt khi zoom > 1x
- [ ] Đủ 4 trạng thái nút giữa: phát, dừng, phát lại, đang tải
- [ ] Nút trước/sau mờ đúng ở đầu/cuối danh sách phát

**Kiểm riêng luồng kết nối lần đầu:**
- [ ] Kết nối thành công thì config đã lưu trước khi K6 hiện. Đóng app ở K6 rồi mở lại vào thẳng Màn chính, K6 không hiện lại
- [ ] K6 không đóng được bằng chạm ngoài hay Back hệ thống
- [ ] "Để sau" vào Màn chính (tab Thư mục), Cài đặt › Bảo vệ ứng dụng ở trạng thái Tắt
- [ ] "Thiết lập mã PIN" mở B1; Back ở B1 quay lại K6; hoàn tất B8 thì config được mã hóa lại và lần mở sau hiện màn Khóa
- [ ] Không còn StepBar và không còn công tắc "Bảo vệ ứng dụng" ở K1 đến K6 và B1 đến B8

**Kiểm riêng sinh trắc học (Lát 2, ADR-0014):**
- [ ] Máy hỗ trợ: sau khi PIN khớp, B6 hỏi bật; "Bật" hiện hộp thoại hệ thống, "Để sau" vào Màn chính. Máy không hỗ trợ thì bỏ qua B6
- [ ] Đã bật: mở màn Khóa tự hiện hộp thoại sinh trắc học (L2); phím sinh trắc học ở góc trái dưới bàn phím; hủy hộp thoại thì nhập PIN như thường
- [ ] Đang khóa tạm (KH-02): ẩn phím sinh trắc học và không tự hiện hộp thoại
- [ ] Thêm hoặc đổi vân tay trong Cài đặt máy: lần mở sau sinh trắc học không còn, PIN vẫn dùng được
- [ ] Đổi PIN hoặc tắt bảo vệ thì sinh trắc học bị tắt, bật lại được
- [ ] Quên PIN và ngắt kết nối xóa luôn khóa sinh trắc học

**Kiểm riêng thanh điều hướng đáy (Lát 8, ADR-0023):**
- [ ] 4 mục khi bật Video và đã đồng bộ xong; 3 mục khi đang lập chỉ mục hoặc tắt Video; mục Short hiện ra không làm thanh nhảy
- [ ] Đổi tab giữ đúng thư mục, bộ lọc, vị trí cuộn; quay lại từ màn xem cũng vậy; bật "Don't keep activities" rồi mở lại vẫn đúng
- [ ] Back theo DH-03; chạm lại tab theo DH-04
- [ ] Thanh ẩn ở màn xem, Khóa, màn con Cài đặt và khi bàn phím tìm kiếm mở; Dialog và sheet phủ lên thanh
- [ ] Chấm nhắc trên Cài đặt đúng màu sắp hết hạn / đã hết hạn
- [ ] Cài đặt không còn mũi tên quay lại; Danh sách không còn nút bánh răng
- [ ] Lề dưới đúng với cả điều hướng bằng cử chỉ và bằng ba nút của hệ thống

**Kiểm riêng tab Short (Lát 8, ADR-0024):**
- [ ] Video dọc gần tỉ lệ màn lấp đầy; video 3:4 và video ngang Vừa khung; video quay bằng điện thoại có cờ xoay hiển thị đúng hướng
- [ ] Tên tệp và thư mục đọc được trên video sáng; thanh tiến độ 3dp sát mép trên thanh đáy
- [ ] Thanh đáy là bản N4 nền đen ở cả theme sáng; đổi sang tab khác thì về bản thường
- [ ] Chạm tạm dừng/phát; vuốt chuyển video; kéo ở video đầu xáo lại và video đầu mới khác video cũ
- [ ] Đủ trạng thái SH1 đến SH9

---

## 11. Việc cần làm trên Claude Design (bàn giao cho topic vẽ)

Danh sách này là nhiệm vụ của topic vẽ Claude Design (chốt 2026-10-10). Vẽ đúng theo các mục được dẫn; giá trị nào chưa có trong tài liệu thì ghi lại và hỏi, không tự chế. **Trạng thái (2026-10-10): 11.1 đến 11.4 đã vẽ xong và đã cập nhật mục 5, 9, `odv-tokens.json`;** còn lại 11.5 (K6 và các việc tồn).

**11.1 Foundations**
| Việc | Artboard | Theo |
|---|---|---|
| Thêm component thanh điều hướng đáy, 4 mục, sáng và tối, đủ trạng thái chọn / chưa chọn / nhấn / focus | `NavBar` (N1) | Mục 4.7 |
| Bản 3 mục (Short ẩn) | `NavBarCompact` (N2) | Mục 4.7, DH-01 |
| Chấm nhắc trên mục Cài đặt, tông warning và danger | `NavBarDot` (N3) | Mục 4.7, DH-07 |
| Bản nền tối ở tab Short | `NavBarMedia` (N4) | Mục 4.7, SV-10 |
| Thêm icon `short` vào board "06 Icon" (cùng quy cách nét 2dp, đầu tròn) | board "06 Icon" | Mục 3.1 |
| Thêm 4 token `nav-*` vào bảng kích thước | board kích thước | Mục 2.6 |
| Đánh dấu "đã bỏ" component Tabs (Thư mục / Thư viện) | board component | Mục 4.1 |

**11.2 Page mới "Short"** (luôn nền đen, thanh đáy bản N4)
| Mã | Artboard | Theo |
|---|---|---|
| SH1 | `ShortPortrait` | Mục 4.8, 5.5 |
| SH2 | `ShortLandscape` | Mục 4.8, 5.5 |
| SH3 | `ShortPortraitFit` | Mục 4.8, 5.5 |
| SH4 | `ShortPaused` | Mục 4.8 |
| SH5 | `ShortBuffer` | Mục 4.8 |
| SH6 | `ShortReshuffle` | Mục 4.8 |
| SH7 | `ShortCodec` | Mục 4.5 MediaError, 4.8 |
| SH8 | `ShortNet` | Mục 4.5 thẻ Mất mạng, 4.8 |
| SH9 | `ShortEmpty` | Mục 4.8 |

**11.3 Sửa page Danh sách**
- D1 đến D8 và các bản tối (13 artboard): bỏ Tabs, AppBar bỏ nút `settings`, thêm thanh đáy N1 (mục Thư mục hoặc Thư viện đang chọn), theo thứ tự khối mục 5.2. D7 dùng N2. D4 không có thanh đáy. D5 sheet phủ lên thanh đáy.
- Thêm D9 `ListExpireBanner`: banner secret ở đầu Danh sách, thanh đáy có chấm N3 (mục 5.2).

**11.4 Sửa page Cài đặt**
- C1 đến C7 và các bản tối (10 artboard): thanh tiêu đề bỏ mũi tên quay lại, thêm thanh đáy N1 (mục Cài đặt đang chọn); C7 có chấm N3 (mục 5.4).
- C2 `SetVideo`: thêm hàng "Thời lượng tối đa của Short" cuối nhóm Video (mục 5.4).
- Thêm O6 `OptShortMax` (mục 5.4).

**11.5 Việc tồn từ trước (không thuộc Lát 8, làm cùng đợt nếu tiện)**
- K6 `ConnectPinPrompt`, `ConnectPinPromptDark` (mục 5.1).
- Xóa hoặc đánh dấu "đã bỏ": K4 `ConnectSuccess`, B7 `SecOff`/`SecOffDark`, StepBar trên K1 đến K5 và B1 đến B8, thẻ công tắc trên B1 đến B6 (mục 9, #8, #9, #14).
- Artboard cho phần Cài đặt đã dựng mà chưa có hình (mục 9, #10, #11).

**Không đổi:** page Khởi động và page Màn xem.