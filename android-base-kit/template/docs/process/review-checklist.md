# Checklist review

Dùng cho mọi PR. Mục nào không áp dụng thì bỏ qua. Mức: **CRITICAL** (chặn merge, lỗ hổng/mất dữ liệu/crash), **HIGH** (lỗi hành vi chắc chắn xảy ra), **MEDIUM** (lỗi ở trường hợp biên, khó bảo trì), **LOW** (gọn hơn, đẹp hơn).

## Kiến trúc

- [ ] Feature và `:app` không import `domain.repository` (chỉ UseCase, `platform`, `settings`).
- [ ] Feature không phụ thuộc feature khác; phần dùng chung đã lên `:core:*`.
- [ ] Domain không có `android.*`, Ktor, Room, Koin, Compose.
- [ ] DTO/entity không lọt ra ngoài `:core:data`; mapper nằm ở `data`.
- [ ] UseCase tên theo nghiệp vụ, một `invoke`.
- [ ] Đổi hoặc bỏ thứ gì so với ADR có comment nêu lý do và ADR.

## MVI và Compose

- [ ] Dialog, sheet, cảnh báo nằm trong State (không phải Effect), có Intent xóa.
- [ ] Effect chỉ thu ở một nơi (`XxxScreen`), qua `CollectEffects`.
- [ ] State lưu `UiError`, không lưu `String` hay `e.message`.
- [ ] Hành động bấm đôi không chạy hai lần.
- [ ] Giá trị composable đọc mà có thể đổi là state của Compose / `StateFlow`.
- [ ] Modifier cử chỉ không bị thêm/bớt bằng `if` (bật tắt bằng cờ bên trong).
- [ ] Lazy list có `key` duy nhất theo id; state cuộn cần giữ đã hoist.
- [ ] Giá trị đổi liên tục chỉ đọc ở composable nhỏ nhất hoặc trong lambda của modifier.
- [ ] Đổi bộ lọc không xóa dữ liệu cũ; trạng thái tải hiện sau khoảng 200 ms; không bật hiệu ứng toàn cục.
- [ ] Không hex, không `dp` tự đặt, không Material Icons; chữ lấy từ token; vùng chạm ≥ 48dp.
- [ ] `Content` stateless có preview cho các trạng thái chính.
- [ ] File không quá khoảng 400 dòng.

## Coroutine và vòng đời

- [ ] Không `runCatching`/`catch (e: Exception)` nuốt `CancellationException`.
- [ ] Không `GlobalScope`, không `runBlocking`; dispatcher qua `DispatcherProvider`; thời gian qua `Clock`.
- [ ] Dọn dẹp và lần ghi cuối trong `NonCancellable`.
- [ ] Timeout không bọc quanh code chặn luồng (dùng `async` + `withTimeoutOrNull`).
- [ ] Tác vụ sống lâu hơn màn có chủ ở app scope; không "đồng bộ khi mở" trong `init` của ViewModel.
- [ ] Tài nguyên dùng chung có single-flight; kho có `reset()` chặn ghi muộn bằng epoch.
- [ ] Coroutine ở scope không có handler bắt mọi lỗi không phải hủy.
- [ ] `callbackFlow` có `awaitClose`.

## Dữ liệu

- [ ] **Đổi schema Room: đã tăng `version`, ghi "Lịch sử version", commit schema JSON**; đã phát hành thì có Migration + test.
- [ ] Ghi nhiều bước trong một transaction.
- [ ] Luồng đọc CSDL có `distinctUntilChanged()`.
- [ ] Kho dữ liệu mới đã đăng ký `DataResetter`.
- [ ] Tên tệp, khóa DataStore, khóa Keystore, tên bảng **không đổi giá trị** (so `git diff`).
- [ ] Enum lưu theo tên; đọc-sửa-ghi DataStore trong một `edit`.
- [ ] Hằng số ở file Constants, có KDoc lý do (hằng số hiệu năng có số đo).

## Mạng

- [ ] Không cài plugin `Logging`; không log token, header, body.
- [ ] Không retry `400/403/404`; `401` thử lại đúng một lần sau khi làm mới token.
- [ ] Client có bearer không theo redirect; URL ký tải bằng client không bearer.
- [ ] Mọi lời gọi có timeout.

## DI

- [ ] Binding mới có trong module; `KoinVerifyTest` pass.
- [ ] Hai binding cùng kiểu có qualifier.
- [ ] Binding đọc ngay lúc dựng thứ khác có `createdAtStart`.
- [ ] Không `KoinPlatform.getKoin().get()` trong code nghiệp vụ.

## Bảo mật

- [ ] Log không có bí mật, dữ liệu cá nhân, `e.message`.
- [ ] Màn nhạy cảm có `FLAG_SECURE`; Window mới (Activity, Dialog) theo chính sách cấp tiến trình.
- [ ] Dữ liệu nhạy cảm trong ViewModel được xóa khi khóa/đăng xuất; không vào `SavedStateHandle`.
- [ ] Component mới `exported="false"` nếu không cần mở từ ngoài.

## Chuỗi và định dạng

- [ ] Không chuỗi cứng trong UI; có đủ bản mọi ngôn ngữ; chuỗi chưa có trong thiết kế đánh dấu `[mới]`.
- [ ] `LocalResources.current`, không `LocalContext.current.resources`.
- [ ] Định dạng số/ngày theo `LocalLocale`, không `Locale.getDefault()`.
- [ ] `plurals` và tham số `%1$s`, không ghép chuỗi.

## Test

- [ ] ViewModel mới hoặc đổi hành vi có test (trạng thái, intent, lỗi, bấm đôi).
- [ ] Test dùng fake, không sleep thật, mỗi test một hành vi.
- [ ] Quy tắc có mã trong đặc tả có test mang mã đó.

## Tài liệu

- [ ] Đổi giá trị nghiệp vụ/UI: đặc tả và thiết kế đã sửa, đã `grep` không còn chỗ mâu thuẫn.
- [ ] ADR mới nếu đổi quyết định kiến trúc; ADR cũ đánh dấu `superseded`.
- [ ] File tiến độ đã cập nhật.
