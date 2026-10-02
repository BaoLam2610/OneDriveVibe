# Đặc tả giao diện ODV (bàn giao thiết kế cho code)

- **Áp dụng cho:** app Android ODV (Kotlin, Jetpack Compose)
- **Nguồn thiết kế:** canvas "ODV Foundations" trên Claude Design (bản v22, 02/10/2026). Có 5 page: Foundations, Khởi động, Danh sách, Màn xem, Cài đặt.
- **Tài liệu đi kèm:**
  - `claude/dac-ta-nghiep-vu.md`: nghiệp vụ, mã KN, BM, KH, TM, TV, DS, VD, AN, PD, CD…
  - `claude/odv-tokens.json`: toàn bộ token ở dạng dữ liệu, chuẩn W3C Design Tokens.
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
4. **Mã trùng giữa các page.** Một số mã lặp lại giữa các page, ví dụ D1 (Danh sách) và D1 (Cài đặt), hoặc P1 (PDF) và P1 (PIN trong Cài đặt). Luôn ghi kèm tên page: "Cài đặt · D1".

---

## 1. Quy tắc bắt buộc khi dựng

1. **Màu:**
   - Màn thường chỉ lấy màu từ token `color.light.*` / `color.dark.*` (mục 2.1).
   - Màn xem lấy từ `media.*` (mục 2.2).
   - Không viết mã hex trong màn hình.
2. **Theme:** có 2 theme Sáng/Tối, theo mục Cài đặt › Giao diện (Theo hệ thống / Sáng / Tối).
   - Màn xem video, ảnh, PDF **luôn nền đen chữ trắng** bất kể theme.
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
8. **Icon:**
   - Chỉ dùng bộ icon ODV ở mục 3: 24 × 24, nét 2dp, đầu tròn.
   - Không dùng Material Icons, không dùng emoji.
   - Tô màu bằng tint theo token.
9. **Viết hoa kiểu câu:**
   - Không viết hoa toàn bộ.
   - Câu chữ lấy nguyên văn từ thiết kế. Bảng câu chữ chính ở mục 5 và 7.
10. **Số và ngày theo vi-VN:** `12.480`, `4,9 MB`, `23/05/2026`, `1:26:02`. Số đổi liên tục hoặc cần thẳng cột dùng JetBrains Mono.
11. **Trạng thái:** mỗi component có đủ trạng thái ở mục 4: mặc định, nhấn, focus, tắt, lỗi, đang tải.
12. **Riêng tư:** các màn sau chặn chụp màn hình (`FLAG_SECURE`) và ẩn nội dung trong danh sách app gần đây (KN-10, KH-04, CH-05):
    - Kết nối (kể cả hộp thoại K6), Khóa, nhập PIN.
    - Cài đặt, ở cả hai nhóm Bảo mật và Kết nối, vì đây là một màn cuộn chung.

---

## 2. Token

Số liệu đầy đủ, dạng máy đọc được, nằm trong `claude/odv-tokens.json`. Các bảng dưới đây sinh từ cùng nguồn.

### 2.1 Màu theo theme

| Token | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `bg` | `#f4f5f7` | `#0c0e12` | Nền màn hình (Danh sách, Cài đặt, Kết nối, Khóa) |
| `surface` | `#ffffff` | `#15181e` | Thẻ, nhóm Cài đặt, Dialog, Sheet, ô nhập |
| `surface-2` | `#eceef2` | `#1e2229` | Nền phụ: Tabs, phím PIN, chip lọc, hàng đang nhấn |
| `line` | `#e1e4ea` | `#2a2f38` | Viền thẻ, kẻ giữa các hàng |
| `line-strong` | `#838b99` | `#687183` | Viền ô nhập, Radio, Switch tắt, nút secondary |
| `ink` | `#101217` | `#f2f4f7` | Chữ chính, icon |
| `ink-muted` | `#565e6d` | `#a3abb8` | Chữ phụ, mô tả, giá trị Cài đặt |
| `ink-faint` | `#9aa1ad` | `#5c6472` | Chữ rất phụ: đường dẫn kết quả tìm, mũi tên Breadcrumb |
| `volt` | `#c5f23a` | `#ccf55b` | Mảng nhấn: nút chính, Switch bật, chấm PIN, phần đã xem |
| `volt-pressed` | `#b0dc22` | `#dcfa86` | Nút chính khi nhấn |
| `on-volt` | `#101217` | `#101217` | Chữ và icon trên volt |
| `volt-soft` | `#eefac8` | `#222c0b` | Nền chip chọn, ô thư mục, phím PIN đang nhấn |
| `volt-text` | `#456100` | `#ccf55b` | Chữ/viền nhấn: nút ghost, Radio chọn, tiêu đề nhóm Cài đặt, focus ring |
| `kind-video` | `#d6336a` | `#ff6d9a` | Nhận diện video (chấm chip) |
| `kind-video-soft` | `#fde7ef` | `#3a1322` | Nền nhận diện video |
| `kind-photo` | `#167cc9` | `#56c2ff` | Nhận diện ảnh |
| `kind-photo-soft` | `#e2f1fc` | `#0e2a3d` | Nền nhận diện ảnh |
| `kind-pdf` | `#a86400` | `#ffb938` | Nhận diện PDF, tiến độ đọc |
| `kind-pdf-soft` | `#fdf0dc` | `#35260b` | Nền thumbnail PDF |
| `danger` | `#c8242f` | `#ff7276` | Lỗi, hành động nguy hiểm |
| `danger-soft` | `#fde8e9` | `#3a1416` | Nền nút danger tonal, icon lỗi |
| `warning` | `#935600` | `#ffc24d` | Cảnh báo |
| `warning-soft` | `#fff2d9` | `#33250a` | Nền Banner cảnh báo |
| `success` | `#12734b` | `#4cd99a` | Thành công |
| `success-soft` | `#e2f5eb` | `#0f2e20` | Nền Banner thành công |
| `inverse-surface` | `#20242c` | `#f2f4f7` | Nền Snackbar, bong bóng cuộn nhanh |
| `inverse-ink` | `#f2f4f7` | `#101217` | Chữ trên inverse-surface |
| `inverse-accent` | `#ccf55b` | `#456100` | Nút hành động trên Snackbar |
| `media-bg` | `#000000` | `#000000` | Nền màn xem (luôn đen) |

**Tương phản đã kiểm tra ở cả hai theme:**
- Chữ ≥ 4,5:1.
- Viền điều khiển và icon mang nghĩa ≥ 3:1.
- `volt` không dùng làm màu chữ trên nền sáng. Chữ nhấn dùng `volt-text`.

### 2.2 Màu màn xem (không đổi theo theme)

| Token | Giá trị | Dùng cho |
|---|---|---|
| `media.background` | `#000000` | Nền màn xem video/ảnh/PDF, không đổi theo theme |
| `media.on-media` | `#ffffff` | Chữ, icon trên media |
| `media.on-media-muted` | `#ffffffcc` | Chữ phụ trên media |
| `media.on-media-faint` | `#ffffff99` | Dòng tên tệp và meta nhỏ trên media |
| `media.scrim` | `#00000099` | Sau Dialog, Sheet, bảng bên phải |
| `media.scrim-controls` | `#00000080` | Lớp phủ khi thanh điều khiển video hiện |
| `media.scrim-strong` | `#000000b3` | Sau thẻ Tiếp theo sau 5 giây |
| `media.pill` | `#000000b3` | Nền viên thuốc: 2,4x, Cắt đầy, 12 / 248; nền HUD |
| `media.toolbar` | `#000000cc` | Thanh trên và dưới của PDF |
| `media.track` | `#ffffff33` | Rãnh thanh tua, rãnh vòng đếm |
| `media.buffer` | `#ffffff66` | Phần đã tải trước; viền nút trên media |
| `media.ripple` | `#ffffff26` | Gợn chạm đúp, nền nút mở khóa |
| `media.icon-well` | `#ffffff1f` | Vòng tròn sau icon lỗi trên media |
| `media.card` | `#1b1d22` | Thẻ trên media: Tiếp theo, Mất mạng |
| `media.pdf-canvas` | `#2a2c31` | Nền sau trang PDF |
| `media.accent` | `#c5f23a` | Nút phát, phần đã xem |
| `media.on-accent` | `#101217` | Icon trên nút phát |

### 2.3 Chữ

| Style | Họ | Cỡ / dòng (sp) | Đậm | Giãn chữ | Dùng ở |
|---|---|---|---|---|---|
| `display` | Be Vietnam Pro | 32 / 40 | 800 | -0.02em | Tiêu đề lớn: Kết nối OneDrive, Thiết lập bảo mật |
| `title` | Be Vietnam Pro | 22 / 28 | 700 | -0.01em | Thanh tiêu đề (OneDrive, Cài đặt), tiêu đề Dialog |
| `heading` | Be Vietnam Pro | 17 / 24 | 600 | 0 | Xem tiếp / Đọc tiếp, tiêu đề Sheet, dòng tiêu đề trong Bảo mật |
| `body` | Be Vietnam Pro | 15 / 22 | 400 | 0 | Nội dung, tên hàng Cài đặt |
| `body-strong` | Be Vietnam Pro | 15 / 22 | 600 | 0 | Tên tệp trong FileRow, nhóm ngày Thư viện, tên tệp trên trình xem |
| `button` | Be Vietnam Pro | 15 / 20 | 600 | 0 | Nhãn Button cỡ md |
| `button-sm` | Be Vietnam Pro | 14 / 20 | 600 | 0 | Nhãn Button cỡ sm, Chip, Tabs |
| `caption` | Be Vietnam Pro | 13 / 18 | 400 | 0 | Meta, mô tả hàng, trợ giúp dưới ô nhập |
| `label` | Be Vietnam Pro | 12 / 16 | 600 | 0 | Nhãn nhỏ |
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
- **Cài đặt:** `caption-strong` màu `volt-text` (tên nhóm) → `body` (tên mục) → `caption` (mô tả) → `body-sm` màu `ink-muted` (giá trị).
- **Cỡ chữ hệ thống:** tôn trọng tới 200%. Hàng tự cao lên, không cắt chữ. Điều khiển video giữ cỡ cố định.

### 2.4 Khoảng cách

| Token | Giá trị | Dùng cho |
|---|---|---|
| `space.half` | 2dp | Khe giữa các ô lưới Thư viện |
| `space.1` | 4dp | Icon với nhãn trong badge; giữa các mục Breadcrumb |
| `space.2` | 8dp | Bên trong điều khiển; giữa các chip; khoảng cách tối thiểu giữa vùng chạm |
| `space.3` | 12dp | Thumbnail với chữ trong FileRow; giữa các ContinueCard; khe lưới thẻ; padding Banner; khe phím PIN |
| `space.4` | 16dp | **Lề màn hình**; padding thẻ, hàng, Sheet; khoảng cách giữa các ô nhập |
| `space.6` | 24dp | Giữa các nhóm nội dung; padding Dialog; khoảng trên tiêu đề nhóm Cài đặt |
| `space.8` | 32dp | Khoảng dưới Sheet; lề màn lỗi trên media |

### 2.5 Bo góc

| Token | Giá trị | Dùng cho |
|---|---|---|
| `radius.xs` | 6dp | Badge thời lượng; ô Thư viện (4dp trong lưới dày, xem PhotoCell) |
| `radius.sm` | 10dp | Ô nhập, Banner, Snackbar, thumbnail FileRow, thumbnail thẻ tệp |
| `radius.md` | 14dp | Thẻ, ô thư mục, ContinueCard, nhóm Cài đặt, phím PIN, thẻ trên media |
| `radius.lg` | 22dp | Dialog, Bottom sheet (2 góc trên), bảng bên phải ở hướng ngang (2 góc trái) |
| `radius.full` | tròn | Button, IconButton, Chip, Tabs, Switch, ProgressBar, viên thuốc |

### 2.6 Kích thước cố định

| Token | Giá trị | Ghi chú |
|---|---|---|
| `tap-target` | 48dp | Vùng chạm tối thiểu |
| `icon-md` | 24dp |  |
| `icon-sm` | 18dp |  |
| `thumb-row` | 56dp | Thumbnail FileRow |
| `app-bar` | 56dp |  |
| `play-button` | 72dp | Nút phát giữa |
| `button` | 48dp |  |
| `button-sm` | 36dp |  |
| `field` | 52dp | Khung ô nhập |
| `chip` | 36dp |  |
| `tab-item` | 40dp |  |
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
| `mini-progress` | 3dp | Khi ẩn điều khiển video |
| `hud-width` | 56dp |  |
| `hud-height` | 200dp |  |
| `lock-hold-ring` | 72dp |  |
| `lock-hold-button` | 52dp |  |
| `countdown-ring` | 56dp |  |
| `side-panel-width` | 360dp | Bảng bên phải ở hướng ngang |

### 2.7 Độ nổi

- Phần lớn giao diện phẳng: phân tầng bằng `bg` → `surface` → `surface-2` và viền `line`.
- Chỉ ba chỗ có bóng:
  - `shadow.sm` (sáng `0 1 2 #1012170f, 0 1 3 #1012171a`): ContinueCard.
  - `shadow.lg` (sáng `0 2 6 #10121714, 0 8 24 -4 #10121729`; tối `0 12 32 -4 #000000cc`): Dialog, Snackbar, bong bóng cuộn nhanh.
  - `focus-ring`: khe 2dp màu nền, rồi vòng 2dp `volt-text`. Dùng khi focus bằng bàn phím hoặc D-pad, không bao giờ ẩn.
- Android không có bóng CSS. Dùng elevation xấp xỉ rồi so với ảnh tham chiếu. Ở theme tối gần như không thấy bóng; đó là đúng thiết kế.

### 2.8 Chuyển động

| Token | Giá trị | Dùng cho |
|---|---|---|
| `duration.fast` | 150ms | Nhấn nút (co 98%), Switch, chip, cross-fade thumbnail → ảnh gốc (AN-01), lỗi dưới ô nhập |
| `duration.base` | 250ms | Dialog, Sheet, Snackbar, hiện/ẩn điều khiển, đổi tab, đổi theme, zoom chạm đúp |
| `duration.slow` | 400ms | Mở rộng thumbnail thành màn xem, Banner trượt vào |
| `easing.ease-out` | cubic-bezier(0.2, 0, 0, 1) | Xuất hiện, mở rộng |
| `easing.ease-in` | cubic-bezier(0.4, 0, 1, 1) | Biến mất, thu nhỏ |
| `duration.controls-auto-hide` | 3000ms | Điều khiển video tự ẩn (VD-01) |
| `duration.snackbar` | 3000ms | Snackbar tự ẩn |
| `duration.autoplay-countdown` | 5000ms | Đếm ngược tự phát tiếp (VD-13), vòng đầy dần tuyến tính |
| `duration.toast-label` | 2000ms | Nhãn đổi khung hình / chế độ phát (VD-06, VD-20) |
| `duration.lock-hold` | 1000ms | Giữ để mở khóa thao tác (VD-08, đề xuất) |

**Quy tắc chuyển động:**
- Không nảy, không đàn hồi.
- Không có chuyển động lặp vô hạn, trừ chỉ báo đang tải.
- Khi hệ thống bật Giảm hiệu ứng:
  - Bỏ scale, trượt, gợn và mở rộng thumbnail. Thay bằng cross-fade ≤ 150ms hoặc hiện ngay.
  - Giữ thanh tiến độ và vòng đếm ngược, vì đó là thông tin.
- Rung nhẹ chỉ ở hai chỗ: bật/tắt khóa thao tác video, và nhập sai PIN.

### 2.9 Độ mờ

- `opacity.disabled` = 0.38: điều khiển bị tắt (nút Kết nối khi form chưa hợp lệ, nút "Video sau" ở video cuối, bàn phím khi khóa tạm).
- `opacity.unsupported` = 0.5: tệp không hỗ trợ khi bật "Hiện tệp không hỗ trợ" (TM-03).

---

## 3. Icon và logo

### 3.1 Bộ icon (42 icon)

- Lưới 24 × 24, nét 2dp, đầu và góc nét tròn, không tô.
- Chỉ `play` và `pause` tô đặc.
- Nguồn SVG nằm ở board "06 Icon" trên canvas. Chuyển sang Vector Drawable và giữ nguyên `pathData`.
- `rect` và `circle` trong SVG phải đổi thành path; không vẽ lại bằng tay.
- Cỡ dùng: 24dp (mặc định), 18dp (trong Chip, Tabs, nhãn), 16dp (dòng lỗi, mũi tên Breadcrumb), 32–36dp (trạng thái trống, nút giữa video).

| Icon | Dùng ở | Icon | Dùng ở |
|---|---|---|---|
| `play`, `pause` | Nút giữa video | `replay` | Nút giữa khi hết video (VD-21) |
| `prev`, `next` | Video trước/sau (VD-10) | `rewind`, `forward` | Tua ±10 giây, gợn chạm đúp |
| `playlist` | Chế độ: Tự phát tiếp | `stop-end` | Chế độ: Không lặp |
| `repeat-one` | Chế độ: Lặp một video | `repeat` | Chế độ: Lặp danh sách |
| `fit` | Đổi khung hình (VD-06) | `rotate` | Xoay Dọc ↔ Ngang (VD-07) |
| `info` | Thông tin tệp (VD-17, AN-05, PD-08) | `lock`, `unlock` | Khóa thao tác, khóa app, hộp thoại K6 |
| `sun`, `volume` | HUD độ sáng / âm lượng | `tune` | Kiểu đọc PDF |
| `folder`, `image`, `video`, `book` | Loại mục: thư mục, ảnh, video, PDF | `search`, `close` | Tìm kiếm, xóa từ khóa, đóng |
| `settings` | Mở Cài đặt (AppBar) | `arrow-left`, `chevron-right` | Quay lại, vào mục, Breadcrumb |
| `sort`, `grid`, `list` | Sắp xếp, đổi dạng lưới/danh sách | `cloud-off` | Offline, tệp chưa có trong cache |
| `sync` | Đồng bộ, đang lập chỉ mục | `check` | Thành công, chip đang chọn |
| `alert` | Lỗi, cảnh báo | `clock` | Banner secret sắp hết hạn |
| `eye`, `eye-off` | Hiện/ẩn ký tự ô bị che | `backspace`, `fingerprint` | Bàn phím PIN |
| `minus` | Checkbox chọn một phần | `fullscreen` | Dự phòng, **không dùng** (VD-07 bỏ nút toàn màn hình) |

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
- **Tabs (Thư mục / Thư viện):**
  - Khung `surface-2` tròn, padding 4. Mỗi tab cao 40, chia đều, icon 18 + chữ `button-sm`.
  - Tab chọn: nền `ink`, chữ `bg`. Tab không chọn: chữ `ink-muted`.
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
- **Snackbar:** lề 16 hai bên, cách đáy 24. Cao tối thiểu 48, padding 6/6/6/16, bo `sm`.
  - Nền `inverse-surface`, chữ `body-sm` màu `inverse-ink`.
  - Hành động: chữ đậm 14 màu `inverse-accent`, không xuống dòng.
  - Bóng `shadow.lg`, tự ẩn sau 3 giây.
- **Dialog:**
  - Rộng ≤ 360 (thiết kế 326–342), căn giữa. Nền `surface`, bo `lg`, padding 24, các khối cách nhau 16. Scrim `#00000099` phía sau.
  - Icon tròn 48, nền và icon theo tông: volt / warning / danger.
  - Tiêu đề `title`. Nội dung `body` màu `ink-muted`. Dòng mã lỗi `code` màu `ink-muted` ("Mã lỗi: AADSTS7000215").
  - Nút căn phải, cách nhau 8: nút phụ là ghost, nút chính là primary / danger / danger-solid.
  - Dialog nguy hiểm dùng role alertdialog.
  - Dialog **bắt buộc chọn** (K6): chạm scrim và Back hệ thống không đóng; chỉ đóng bằng một trong hai nút.
- **Bottom sheet:**
  - Rộng toàn màn, nền `surface`, bo `lg` hai góc trên, padding 8/16/32.
  - Tay nắm 36 × 4 màu `line-strong`, căn giữa. Tiêu đề `heading`.
  - Scrim `#00000099`.
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
  - Tiêu đề `title`, một dòng, cắt "…".
  - Bên phải: IconButton `search` "Tìm kiếm" và `settings` "Cài đặt".
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
- **MediaError** (codec VD-15, ảnh AN-07, PDF PD-07): khối căn giữa, lề 32.
  - Vòng 72 nền `#ffffff1f`, icon `alert` 32.
  - Tiêu đề `state-title`, nội dung `body` `#ffffffcc`, dòng tên tệp `timecode-sm` `#ffffff99`.
  - Nút secondary viền `#ffffff66` (ảnh không có nút; vuốt để sang ảnh khác).
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

**Luồng khởi động:** K1 → K3 → (thành công, config đã lưu) → K6 → "Thiết lập mã PIN" → B1 … B6 → B8 → Danh sách; hoặc K6 → "Để sau" → Danh sách. Back ở B1 quay lại K6 (BM-08). K3 thất bại → K5.

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

### 5.2 Danh sách

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

**Thứ tự khối, cách nhau 12:**
1. Padding trên 24 (thanh trạng thái).
2. AppBar.
3. Tabs (padding 4/16/0).
4. Banner (offline hoặc đang lập chỉ mục), nếu có.
5. Dải Xem tiếp (chỉ ở gốc Thư mục).
6. Breadcrumb.
7. SortBar.
8. Nội dung.

**Tab Thư viện:** chip lọc Tất cả / Ảnh / Video, rồi nhóm theo ngày, rồi lưới 4 cột. FastScroller cách đỉnh khoảng 250.

**Trạng thái:**
- **D6 offline:** banner neutral `cloud-off`. Thẻ chưa cache có dấu `cloud-off`. Bấm tệp chưa cache thì hiện Snackbar "Cần kết nối mạng để mở tệp này" kèm nút "Đóng".
- **D7 đang lập chỉ mục:** banner volt `sync` có ProgressBar 4 bên trong. Ô chưa tải là khối `surface-2`, không nhấp nháy.

### 5.3 Màn xem

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
- Mở video luôn ở hướng dọc. Nút xoay đổi Dọc ↔ Ngang và không gián đoạn phát.
- Back thoát ngay cả khi đang ngang, rồi trở về hướng dọc (VD-19).
- Mở lại video có vị trí đã lưu thì hiện Dialog V3 trước khi phát.
- Hết video: chế độ Không lặp, hoặc hết video cuối ở chế độ Tự phát tiếp, thì vào V19 (nút Phát lại, điều khiển không tự ẩn). Tự phát tiếp còn video sau thì vào V8.

### 5.4 Cài đặt

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

**Bố cục:**
- Cài đặt là **một màn cuộn**, 6 nhóm theo thứ tự: Hiển thị, Bảo mật, Video, PDF, Bộ nhớ đệm, Kết nối. Cuối màn có dòng phiên bản `meta` căn giữa.
- C1 đến C4 là bốn vị trí cuộn của cùng màn đó.
- Thanh tiêu đề: nút quay lại + "Cài đặt" (`title`), cao 56 cộng 24 thanh trạng thái. Khi đã cuộn thì có kẻ đáy `line`.

**Thứ tự hàng:**
- **Hiển thị:** Loại tệp hiển thị (ChipsRow) → Ẩn thư mục không có tệp phù hợp → Hiện tệp không hỗ trợ → Giao diện.
- **Bảo mật:** Bảo vệ ứng dụng → Đổi mã PIN → Mở khóa bằng sinh trắc học → Tự khóa khi rời app → Xóa dữ liệu khi nhập sai quá nhiều.
- **Video:** Bước tua khi chạm đúp → Tốc độ mặc định → Khung hình mặc định → Hướng màn hình khi mở video → Chế độ phát → Nhớ vị trí xem.
- **PDF:** Kiểu đọc.
- **Bộ nhớ đệm:** CacheUsage → Giới hạn tối đa → Xóa bộ nhớ đệm.
- **Kết nối:** Tài khoản (UPN), Tenant ID, Client ID, Đồng bộ gần nhất (InfoRow) → Cập nhật Client Secret → Ngày hết hạn secret → Ngắt kết nối.

**Luồng:**
- Các hành động sau đi qua P1 (Nhập mã PIN hiện tại), rồi mới tới hộp thoại hoặc màn tiếp theo; dòng phụ đề của P1 đổi theo mục đích:
  - Tắt bảo vệ
  - Bật xóa dữ liệu tự động
  - Đổi PIN
  - Cập nhật Client Secret
- Đổi PIN đi P1 → P2 → P3. PIN dễ đoán và PIN không khớp dùng cùng thông báo như B3 / B5.
- Cập nhật secret đi S1 → S2 (loading toàn màn, không có Hủy) → S3 (lỗi, giữ secret cũ) hoặc quay về Cài đặt kèm Snackbar S5.
- Khi bảo vệ đang **tắt** (kể cả khi người dùng chọn "Để sau" ở K6), công tắc "Bảo vệ ứng dụng" bật lên thì đi theo luồng B1 → … → B8 (CD-02). Hành động trong danh sách trên không cần P1 vì chưa có PIN.

---

## 6. Hành vi theo màn

| Tình huống | Hành vi |
|---|---|
| Nhấn nút | Co 98% trong 150ms. Nút chính đổi sang `volt-pressed`. Hàng danh sách phủ `surface-2` |
| Mở tệp từ Danh sách | Thumbnail mở rộng thành màn xem trong 400ms, nền chuyển sang đen. Quay lại thì thu về đúng ô cũ và giữ vị trí cuộn |
| Ảnh tải xong (AN-01) | Hiện thumbnail lớn ngay, cross-fade sang ảnh gốc trong 150ms. Chỉ báo tải nhỏ, không che ảnh |
| Điều khiển video (VD-01) | Chạm để hiện hoặc ẩn. Tự ẩn sau 3 giây không thao tác, trừ khi đang ở trạng thái Phát lại, đang tải, hoặc đang mở sheet hay dialog |
| Đổi khung hình / chế độ phát | Mỗi lần bấm xoay vòng, hiện viên thuốc nhãn 2 giây. Chế độ được nhớ |
| Zoom video (VD-18) | 1x–4x. Khi > 1x, một ngón kéo để di chuyển, tắt vuốt chỉnh sáng/âm lượng, hiện nút Đặt lại zoom. Zoom về 1x khi đổi video hoặc đổi hướng |
| Khóa thao tác (VD-08) | Chặn mọi chạm. Giữ nút mở khóa 1 giây, vòng volt chạy theo thời gian giữ. Rung nhẹ khi khóa và khi mở |
| Mất mạng (VD-16) | Phát hết phần đã tải trước rồi mới hiện thẻ Mất mạng. Không tự chuyển video khi mất mạng |
| Tự phát tiếp (VD-13) | Vòng đếm 5 giây đầy dần tuyến tính. Bấm Hủy thì ở lại màn, hiện trạng thái Phát lại |
| Hộp thoại hỏi PIN sau kết nối (K6, KN-13) | Hiện ngay khi kết nối thành công. Chạm ngoài và Back hệ thống không đóng. "Để sau" vào Danh sách; "Thiết lập mã PIN" mở B1. Back ở B1 quay lại K6 |
| Snackbar | Trượt lên từ đáy trong 250ms, ở lại 3 giây rồi mờ dần |
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
  | AppBar | "Tìm kiếm", "Cài đặt", "Lên một cấp", "Quay lại" |

- **Thứ tự đọc** theo thứ tự thị giác. Banner và dòng lỗi là vùng live, đọc ngay khi xuất hiện.
- **Mọi cử chỉ có nút tương đương:**
  - chạm đúp ↔ nút tua
  - vuốt sáng/âm lượng ↔ âm lượng hệ thống
  - vuốt đổi ảnh ↔ mũi tên khi bật TalkBack
- **Thao tác một tay:** điều khiển chính nằm nửa dưới màn hình.
- **Không dựa vào màu đơn thuần:** lỗi luôn có viền, icon `alert` và câu chữ. Loại tệp luôn có icon riêng.

---

## 8. Quyết định thiết kế

**Đã chốt:**
- PIN 6 số, nhập bằng bàn phím tự vẽ, đủ 6 số thì tự chuyển bước.
- Trong lúc khóa tạm thì ẩn phím sinh trắc học.
- "Quên mã PIN" có 2 bước xác nhận.
- **Không có StepBar** ở Kết nối và Thiết lập bảo mật (luồng có nhánh nên không còn đếm bước).
- Kết nối thành công thì **lưu config ngay** (chế độ thiết bị) rồi hiện **hộp thoại K6** hỏi thiết lập mã PIN. Không có thẻ tài khoản, loại drive hay thanh dung lượng (KN-08, KN-12, KN-13).
- K6 bắt buộc chọn một trong hai nút ("Thiết lập mã PIN" / "Để sau"), không đóng bằng chạm ngoài hay Back. Chỉ hiện một lần cho mỗi lần kết nối mới.
- "Để sau" vào thẳng Danh sách với bảo mật tắt; bật lại ở Cài đặt › Bảo mật.
- Màn Thiết lập bảo mật vào thẳng bước đặt PIN, không còn công tắc. Back ở B1 quay lại K6, giữ config đã lưu (BM-08).
- Màn Danh sách là màn Home. Biểu tượng bánh răng trên AppBar mở Cài đặt.
- Không có "Kệ truyện". PDF xem qua Thư mục và dải Đọc tiếp.
- Không có "Mở bằng ứng dụng khác".
- Video không có nút toàn màn hình. Nút xoay đổi hướng, luôn mở ở hướng dọc.
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
8. Nhảy trang PDF dùng bàn phím số của hệ thống.

---

## 9. Chênh lệch đã biết (xử lý trước hoặc trong lúc code)

| # | Chỗ lệch | Cách xử lý đề xuất |
|---|---|---|
| 1 | PD-08 có nút ⓘ mở thông tin PDF, nhưng thanh trên của PDF (P2, P4) mới có nút `tune` | Chờ người thiết kế quyết định vị trí nút ⓘ. Cho tới lúc đó, P8 là màn đích |
| 2 | File `foundations-0x-*.md` cũ của Design System còn ghi "passphrase", "video ngang tự xoay", nút "Lấp đầy" | Bỏ qua. Theo tài liệu này và đặc tả (PIN 6 số, VD-07, "Cắt đầy") |
| 3 | Foundations ghi `display` cho tiêu đề Danh sách và Cài đặt | Màn thật dùng `title` (22/28) trên AppBar. Theo màn thật |
| 4 | Foundations chỉ có 11 style chữ | Màn thật dùng thêm 10 style phụ (mục 2.3). Đã gộp vào token |
| 5 | Mã màn trùng giữa page (D1, P1, P5…) | Luôn ghi kèm tên page |
| 6 | Board "06 Icon" trên canvas đang hiện 37 icon. 5 icon `info`, `minus`, `fit`, `next`, `prev` đã dùng trên màn nhưng chưa có trên board | Dùng đủ 42 icon như mục 3.1. Bản cập nhật board đã sẵn sàng, chờ đăng |
| 7 | Khi xem với font thật, các board Foundations 02, 03, 07, 11, 12 bị tràn hoặc cắt chữ | Chỉ ảnh hưởng canvas, không ảnh hưởng app. Bản sửa chiều cao đã sẵn sàng, chờ đăng |
| 8 | Artboard K4 `ConnectSuccess` (bottom sheet "Đã kết nối OneDrive" có thanh dung lượng) vẫn còn trên canvas v22, nhưng KN-08 và KN-12 đã bỏ màn này | Không dựng K4. Cần xóa hoặc đánh dấu "đã bỏ" artboard này trên canvas |
| 9 | Các artboard K1, K2, K3, K5 (kể cả bản tối) và B1 đến B6, B8 trên canvas v22 còn hiện StepBar "BƯỚC 1 / 2", "BƯỚC 2 / 2" | Không dựng StepBar. Cần gỡ StepBar khỏi các artboard này trên canvas; style `step-label` trong token gỡ theo |
| 10 | Artboard B7 `SecOff`, `SecOffDark` và thẻ công tắc "Bảo vệ ứng dụng" trên B1 đến B6 vẫn còn trên canvas, nhưng BM-01 đã bỏ công tắc | Không dựng B7 và công tắc. Cần xóa hoặc đánh dấu "đã bỏ" trên canvas |
| 11 | Chưa có artboard K6 (hộp thoại hỏi thiết lập mã PIN, KN-13), cả bản sáng lẫn tối | Người thiết kế dựng theo mô tả ở mục 5.1. Trong lúc chờ, dev dựng theo mô tả và component Dialog (mục 4.2) |

---

## 10. Nghiệm thu: code có giống thiết kế không

**Thiết lập so ảnh:**
- Preview và screenshot test (Roborazzi hoặc Paparazzi) với cấu hình 390 × 844dp, mật độ 420dpi, font scale 1.0, cả theme sáng và tối.
- Mỗi mã ở mục 5 có ít nhất một ảnh test. Đặt tên theo `page_mã_artboard`, ví dụ `settings_D4_DlgDisc2`.
- So với ảnh xuất từ artboard tương ứng. Chấp nhận lệch ≤ 2dp về vị trí, kích thước khớp tuyệt đối, màu khớp tuyệt đối.

**Checklist cho mỗi màn:**
- [ ] Màu chỉ từ token, đúng cả theme sáng và tối. Màn xem luôn đen
- [ ] Chữ đúng style (họ, cỡ, dòng, đậm). Tắt font padding. Số đổi liên tục dùng JetBrains Mono
- [ ] Khoảng cách, bo góc, kích thước khớp mục 2 và 4. Lề màn 16
- [ ] Vùng chạm ≥ 48dp, cách nhau ≥ 8dp
- [ ] Đủ trạng thái: mặc định, nhấn, focus, tắt, lỗi, đang tải, trống, offline (nếu màn có)
- [ ] Câu chữ khớp nguyên văn, viết hoa kiểu câu, số và ngày theo vi-VN
- [ ] Nhãn TalkBack theo mục 7. Thứ tự đọc đúng
- [ ] Chuyển động đúng thời lượng và easing. Giảm hiệu ứng hoạt động
- [ ] Cỡ chữ 200%: không cắt chữ, hàng tự cao lên
- [ ] Chặn chụp màn hình ở các màn yêu cầu
- [ ] Edge-to-edge: không bị thanh hệ thống che, đúng màu thanh hệ thống

**Kiểm riêng màn xem video:**
- [ ] Mở luôn ở hướng dọc. Xoay không tải lại video. Back thoát một lần
- [ ] Tự ẩn sau 3 giây. Không tự ẩn ở trạng thái Phát lại hay đang tải
- [ ] Chạm đúp cộng dồn. Vuốt bị tắt khi zoom > 1x
- [ ] Đủ 4 trạng thái nút giữa: phát, dừng, phát lại, đang tải
- [ ] Nút trước/sau mờ đúng ở đầu/cuối danh sách phát

**Kiểm riêng luồng kết nối lần đầu:**
- [ ] Kết nối thành công thì config đã lưu trước khi K6 hiện. Đóng app ở K6 rồi mở lại vào thẳng Danh sách, K6 không hiện lại
- [ ] K6 không đóng được bằng chạm ngoài hay Back hệ thống
- [ ] "Để sau" vào Danh sách, Cài đặt › Bảo vệ ứng dụng ở trạng thái Tắt
- [ ] "Thiết lập mã PIN" mở B1; Back ở B1 quay lại K6; hoàn tất B8 thì config được mã hóa lại và lần mở sau hiện màn Khóa
- [ ] Không còn StepBar và không còn công tắc "Bảo vệ ứng dụng" ở K1 đến K6 và B1 đến B8
