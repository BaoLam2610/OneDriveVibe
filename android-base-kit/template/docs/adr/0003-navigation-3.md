# ADR-0003: Navigation 3

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** `conventions/navigation.md`

## Bối cảnh

App cần back stack khôi phục được sau process death, route có tham số kiểu an toàn, ViewModel theo từng màn, và có thể có thanh điều hướng đáy với trạng thái riêng mỗi tab.

## Quyết định

Dùng Navigation 3 (`androidx.navigation3`): back stack là danh sách `NavKey` do app sở hữu (`rememberNavBackStack`), `NavDisplay` + `entryProvider`, decorator `rememberSaveableStateHolderNavEntryDecorator` và `rememberViewModelStoreNavEntryDecorator`. Route là `sealed interface AppRoute : NavKey`, `@Serializable`, ở `:app`. Màn nhận lambda điều hướng, không nhận back stack.

## Phương án đã cân nhắc

### Navigation Compose 2.x (`NavHost`, `composable<Route>`)
- Ưu: phổ biến, nhiều ví dụ.
- Nhược: back stack nằm trong `NavController`, khó thao tác trực tiếp; nhiều tầng/tab cần lồng `NavHost`.
- Lý do không chọn: Navigation 3 đơn giản hơn cho back stack tự quản và tab lồng.

### Tự viết hoàn toàn
- Lý do không chọn: phải tự lo lưu trạng thái, ViewModel theo màn, predictive back.

## Hệ quả

### Tích cực
- Thao tác back stack là thao tác danh sách, dễ đọc và dễ test.
- Màn không phụ thuộc thư viện điều hướng (chỉ lambda).

### Tiêu cực
- Thư viện mới hơn, ít ví dụ hơn; tên API phải đối chiếu đúng phiên bản.

### Rủi ro
- Route đã lưu khi process death phải tương thích ngược: trường thêm sau có mặc định.

## Cách xác nhận

- Kiểm tay "Don't keep activities" mỗi lát khôi phục đúng màn và tham số.
