# Đặc tả giao diện: {{TEN_DU_AN}}

> **Mẫu.** Chép sang `docs/design/thiet-ke-ui.md`. File token (`docs/design/tokens.json` hoặc bản xuất từ Figma) là nguồn sự thật cho giá trị; file này mô tả cách dùng, component, màn hình và các chênh lệch.
>
> Tài liệu UI quyết định **hình thức**. Hành vi theo đặc tả nghiệp vụ.

- **Nguồn thiết kế:** <link Figma / thư mục bản xuất>
- **Cập nhật:** {{NGAY}}

## 0. Cách dùng tài liệu này

- Dev dựng theo mục 1 (bắt buộc), lấy giá trị từ mục 2, component từ mục 4, màn từ mục 5.
- Chỗ thiết kế chưa có: **hỏi**, ghi vào mục 8 (đề xuất) hoặc mục 9 (chênh lệch); không tự chế.

## 1. Quy tắc bắt buộc khi dựng

1. **Màu:** chỉ lấy từ token mục 2.1. Không hex trong màn hình. Không dynamic color.
2. **Theme:** <Sáng / Tối, theo Cài đặt>. <Màn nào luôn nền tối bất kể theme?>
3. **Chữ:** chỉ dùng các style ở mục 2.3, đơn vị `sp`. `includeFontPadding = false`, `LineHeightStyle(Center, Trim.None)`.
4. **Font:** <tên font, độ đậm, giấy phép>. Không thay bằng font hệ thống.
5. **Kích thước:** lưới <4dp>. Khoảng cách, kích thước, bo góc từ mục 2.4 đến 2.6.
6. **Vùng chạm:** ≥ 48dp kể cả khi hình nhỏ hơn; các vùng chạm cách nhau ≥ 8dp.
7. **Edge-to-edge:** nội dung vẽ dưới thanh hệ thống, padding theo `WindowInsets`. <Màu nền thanh hệ thống>.
8. **Icon:** chỉ dùng bộ icon mục 3 (<cỡ, nét>). Không Material Icons, không emoji.
9. **Viết hoa kiểu câu.** Câu chữ lấy nguyên văn từ thiết kế (mục 5).
10. **Số và ngày** theo ngôn ngữ đang dùng: VI `12.480`, `4,9 MB`, `23/05/2026`; EN `12,480`, `4.9 MB`, `5/23/2026`.
11. **Trạng thái:** mỗi component có đủ mặc định, nhấn, focus, tắt, lỗi, đang tải.
12. **Riêng tư:** <màn nào chặn chụp màn hình>.
13. **Chuyển động:** theo mục 2.8; tôn trọng "Giảm chuyển động".
14. **Trạng thái tải:** skeleton, hiện sau 200 ms (mục 4.x).

## 2. Token

### 2.1 Màu theo theme

| Token | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `bg` | | | Nền màn |
| `surface-1` | | | Thẻ, sheet |
| `ink` | | | Chữ chính |
| `ink-2` | | | Chữ phụ |
| `accent` | | | Nút chính, liên kết |
| `danger` | | | Lỗi, xóa |

### 2.2 Màu không đổi theo theme (nếu có)

### 2.3 Chữ

| Style | Font | Cỡ (sp) | Cao dòng | Độ đậm | Dùng cho |
|---|---|---|---|---|---|
| `title-lg` | | | | | |
| `body` | | | | | |
| `label` | | | | | |

### 2.4 Khoảng cách · 2.5 Bo góc · 2.6 Kích thước cố định · 2.7 Độ nổi · 2.8 Chuyển động · 2.9 Độ mờ

| Token | Giá trị | Dùng cho |
|---|---|---|
| | | |

## 3. Icon và logo

| Icon | Tên trong code (`PfxIcon.X`) | Dùng ở |
|---|---|---|
| | | |

## 4. Component

### 4.1 <Nút>

| Biến thể | Cao | Bo | Chữ | Màu nền / chữ | Trạng thái |
|---|---|---|---|---|---|
| Chính | | | | | mặc định, nhấn, tắt, đang tải |

### 4.2 <Thông báo và lớp phủ: Dialog, BottomSheet, Snackbar, Banner, Skeleton>

### 4.3 <Danh sách: hàng, thẻ, lưới>

## 5. Màn hình

### 5.1 <Tên màn> (artboard `X1` đến `X5`)

| Artboard | Trạng thái | Ghi chú |
|---|---|---|
| X1 | Có dữ liệu | |
| X2 | Rỗng | |
| X3 | Lỗi | |

**Câu chữ**

| Vị trí | Tiếng Việt | English |
|---|---|---|
| Tiêu đề | | |

**Bố cục:** <thứ tự phần tử, khoảng cách giữa các khối (token)>.

## 6. Hành vi theo màn

<Cuộn, ẩn hiện thanh, cử chỉ, chuyển cảnh giữa các màn (token chuyển động).>

## 7. Trợ năng

| Phần tử | Nhãn TalkBack (VI / EN) |
|---|---|
| | |

## 8. Quyết định và đề xuất thiết kế

| # | Nội dung | Trạng thái (đề xuất / đã chốt) | Ngày |
|---|---|---|---|
| 1 | | | |

## 9. Chênh lệch đã biết

Chỗ thiết kế khác luật chung (mục 1) hoặc khác đặc tả, hoặc phần đã dựng mà chưa có artboard. **Chờ quyết, không tự chọn.**

| # | Chỗ | Thiết kế ghi | Luật / đặc tả ghi | Đang làm theo | Người quyết |
|---|---|---|---|---|---|
| 1 | <Thanh tua> | <vùng chạm 24dp> | <≥ 48dp> | <...> | |

## 10. Nghiệm thu

- [ ] So từng artboard với bản dựng (cạnh nhau), cả theme Sáng và Tối.
- [ ] Cỡ chữ hệ thống lớn nhất không vỡ bố cục.
- [ ] TalkBack đọc đúng nhãn mục 7.

## 11. Việc cần vẽ

| # | Artboard | Lý do | Trạng thái |
|---|---|---|---|
| 1 | | <đã dựng chưa có artboard / mới> | |
