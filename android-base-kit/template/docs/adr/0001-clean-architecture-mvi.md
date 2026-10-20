# ADR-0001: Clean Architecture + MVI

- **Ngày:** {{NGAY}}
- **Trạng thái:** accepted
- **Liên quan:** ADR-0002, ADR-0006, `conventions/mvi.md`

## Bối cảnh

App có nhiều màn với trạng thái phức tạp (tải, lỗi, offline, dialog), nhiều người cùng làm và có AI viết code. Cần một cấu trúc dễ đoán: nhìn tên file là biết logic ở đâu, test được ViewModel mà không cần Android.

## Quyết định

Ba tầng UI, Domain, Data theo Clean Architecture. Tầng UI theo MVI: mỗi màn một `State` (data class bất biến), `Intent` (sealed, vào qua `onIntent()` duy nhất), `Effect` (sealed, thao tác một lần, qua `Channel`). ViewModel kế thừa `BaseMviViewModel<S, I, E>` tự viết ở `:core:ui`. Mỗi màn tách `Screen` (nối ViewModel) và `Content` (stateless).

## Phương án đã cân nhắc

### MVVM thường (nhiều `StateFlow` lẻ, nhiều hàm public)
- Ưu: quen thuộc, ít code khuôn.
- Nhược: trạng thái rải nhiều luồng, khó đảm bảo nhất quán; API của ViewModel phình theo số nút.
- Lý do không chọn: khó test và khó review khi màn lớn.

### Framework MVI (Orbit, MVIKotlin)
- Ưu: có sẵn reducer, side effect, công cụ test.
- Nhược: thêm phụ thuộc và khái niệm; `BaseMviViewModel` khoảng 40 dòng đã đủ.
- Lý do không chọn: không đáng chi phí học và khóa vào thư viện.

## Hệ quả

### Tích cực
- Toàn bộ màn hình là một giá trị State: dễ preview, dễ test, dễ khôi phục.
- Một điểm vào `onIntent` làm review luồng sự kiện dễ.

### Tiêu cực
- Code khuôn (Contract, Screen, Content) cho mỗi màn.
- Phải phân biệt State và Effect đúng (dialog trong State, điều hướng là Effect).

### Rủi ro
- Effect qua Channel có thể mất khi vòng đời dừng: thứ không được mất phải nằm trong State (`conventions/mvi.md` mục 3).

## Cách xác nhận

- Mọi ViewModel kế thừa `BaseMviViewModel` (test kiến trúc).
- Mọi ViewModel có unit test trạng thái và intent (ADR-0009).
