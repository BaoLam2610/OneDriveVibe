# Đặc tả nghiệp vụ: {{TEN_DU_AN}}

> **Mẫu.** Chép sang `docs/spec/dac-ta-nghiep-vu.md` rồi điền. Dự án lớn có thể tách mỗi nhóm màn một file (`docs/spec/03-gio-hang.md`) nhưng giữ một mục lục.
>
> Đặc tả quyết định **hành vi** (cái gì xảy ra). Hình thức (màu, cỡ, bố cục) nằm ở tài liệu thiết kế. Mỗi quy tắc có mã `NHOM-NN` **không bao giờ đổi**; quy tắc bị bỏ thì ghi "~~NHOM-NN~~ đã bỏ (ngày, lý do)" chứ không xóa số.

- **Nền tảng:** Android (Kotlin, Jetpack Compose), minSdk {{MIN_SDK}}
- **Người dùng:** <ai dùng, bao nhiêu, bối cảnh>
- **Nguồn dữ liệu:** <backend, API, xác thực>
- **Ngôn ngữ:** <Tiếng Việt (mặc định) + English>
- **Quyền / phạm vi dữ liệu:** <app được làm gì, không được làm gì>
- **Cập nhật:** {{NGAY}}

---

## 1. Thuật ngữ

| Thuật ngữ | Nghĩa trong app |
|---|---|
| <Giỏ hàng> | <...> |

## 2. Luồng tổng thể

```
Mở app → Splash → (chưa đăng nhập) Đăng nhập → Màn chính
                 → (đã đăng nhập) Màn chính
```

<Mô tả các nhánh: lần đầu, đã có phiên, phiên hết hạn, mất mạng.>

## 3. Mô tả màn hình

### 3.1 <Tên màn> (`<NHOM>`)

**Mục đích:** <một câu>.

**Mở từ:** <màn/nút nào>. **Đi tới:** <màn nào>.

**Thành phần**

| Thành phần | Bắt buộc | Kiểu | Kiểm tra / ghi chú |
|---|---|---|---|
| <Email> | Có | Ô nhập | Định dạng email |
| <Nút Đăng nhập> | | Nút chính | Tắt khi ô chưa hợp lệ |

**Quy tắc**

- **<NHOM>-01** <Hành vi, viết được thành test: "Khi X thì Y".>
- **<NHOM>-02** <...>

**Trạng thái**

| Trạng thái | Khi nào | Hiển thị |
|---|---|---|
| Đang tải | <...> | <skeleton sau 200 ms> |
| Rỗng | <...> | <...> |
| Lỗi | <...> | <bảng lỗi bên dưới> |

**Bảng lỗi** (theo **mã lỗi**, không theo message của máy chủ)

| Mã / điều kiện | Thông báo | Hành động |
|---|---|---|
| Không có mạng | <...> | Thử lại |
| `401` | <...> | Về màn đăng nhập |
| `<ma_loi_api>` | <...> | <...> |

**Ngoài phạm vi:** <những gì màn này cố ý không làm>.

## 4. Quy tắc chung

### 4.1 Phiên và bảo mật (`SEC`)

- **SEC-01** <Token, khóa, chụp màn hình, sao lưu...>

### 4.2 Đồng bộ và offline (`SYNC`)

- **SYNC-01** <Khi nào tải, khi nào làm mới, dùng được khi mất mạng không>

### 4.3 Bộ nhớ đệm (`CACHE`)

- **CACHE-01** <Trần, khi nào xóa>

### 4.4 Đăng xuất / xóa dữ liệu

- <Xóa những gì; giữ những gì (ngôn ngữ? giao diện?)>

## 5. Cài đặt

| Nhóm | Mục | Giá trị | Mặc định | Mã |
|---|---|---|---|---|
| Hiển thị | Giao diện | Theo hệ thống / Sáng / Tối | Theo hệ thống | SET-01 |
| Hiển thị | Ngôn ngữ | Theo hệ thống / Tiếng Việt / English | Theo hệ thống | SET-02 |

Mỗi giá trị mặc định chỉ ghi ở **bảng này**; nơi khác trỏ về đây.

## 6. Phạm vi phiên bản

| Tính năng | MVP | Sau MVP |
|---|---|---|
| <...> | ✔ | |

## 7. Điểm còn mở

| # | Câu hỏi | Người quyết | Hạn |
|---|---|---|---|
| Q1 | <...> | <...> | <...> |

## 8. Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| {{NGAY}} | Bản đầu |
