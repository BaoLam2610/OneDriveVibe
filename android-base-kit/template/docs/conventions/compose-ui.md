# Compose và giao diện

## 1. Token và design system

- **Màu, chữ, khoảng cách, bo góc, kích thước, chuyển động** lấy từ `:core:designsystem` (`PfxTheme.colors`, `PfxTheme.typography`, `PfxSpacing`, `PfxRadius`, `PfxSize`, `PfxMotion`). Không viết hex, không viết số `dp` tự đặt trong màn hình.
- Token dựng từ file token của thiết kế (`docs/design/tokens.json` hoặc tương đương). Thiếu token thì **hỏi**, không tự chế.
- Tắt dynamic color. Theme XML (`Theme.{{TEN_DU_AN}}`) chỉ đặt nền cửa sổ và thanh hệ thống, không đặt bảng màu.
- Icon chỉ dùng bộ của thiết kế (`PfxIcon`), không Material Icons, không emoji. Tô bằng tint theo token.
- Chữ: `includeFontPadding = false` và `LineHeightStyle(Alignment.Center, Trim.None)` đặt sẵn trong token chữ. Thiếu hai thiết lập này, chữ có dấu tiếng Việt lệch 1 đến 3dp so với thiết kế.
- Vùng chạm ≥ 48dp kể cả khi hình nhỏ hơn (`Modifier.minimumInteractiveComponentSize()` hoặc padding). Thiết kế ghi nhỏ hơn 48dp thì ghi vào "chênh lệch đã biết" của tài liệu UI, không tự chọn.
- Component dùng chung có đủ trạng thái: mặc định, nhấn, focus, tắt, lỗi, đang tải.
- Design gallery (Activity chỉ ở bản debug) hiện mọi token và component để so mắt với thiết kế.

## 2. Viết composable

- Tham số theo thứ tự: dữ liệu bắt buộc → lambda sự kiện → `modifier: Modifier = Modifier` → tham số tùy chọn → slot `content`. `modifier` áp vào phần tử ngoài cùng và chỉ áp **một** lần.
- Composable public có tiền tố `Pfx` (`PfxPrimaryButton`). Composable private trong file không cần.
- Tách `Screen` (nối ViewModel) và `Content` (stateless) như `mvi.md` mục 5. `Content` có `@Preview` cho trạng thái chính (có dữ liệu, rỗng, lỗi, đang tải).
- **Composable tách ra không giữ state**: nhận giá trị và lambda. State nằm ở một chỗ (Content hoặc ViewModel).
- File vượt khoảng **400 dòng** thì tách theo vùng màn hình (`XxxTopBar.kt`, `XxxList.kt`, `XxxSheets.kt`).
- Mọi giá trị composable đọc mà có thể đổi phải là **state của Compose** (`mutableStateOf`, `StateFlow` thu bằng `collectAsStateWithLifecycle`). Biến thường trong controller mà UI đọc thì UI không bao giờ dựng lại.
- Thu `StateFlow` bằng `collectAsStateWithLifecycle()`. Ngoại lệ có chủ ý (như cổng khóa cần đọc ngay cả khi nền) thì ghi comment.
- Đọc `Resources` bằng `LocalResources.current`, không bằng `LocalContext.current.resources` (xem `resources-i18n.md`).

## 3. Trạng thái tải và chuyển trạng thái

Mục tiêu là không nháy, không khựng.

- **Đổi bộ lọc, sắp xếp, phạm vi:** giữ dữ liệu cũ trên màn cho tới khi dữ liệu mới về. Không xóa danh sách rồi bật `isLoading`.
- **Trễ trước khi hiện trạng thái tải:** chỉ hiện skeleton/spinner nếu chờ quá khoảng **200 ms** (helper `rememberDelayed(isLoading, 200.ms)` ở `:core:ui`). Tải nhanh hơn thì người dùng không thấy gì nháy.
- **Skeleton thay vòng quay** cho danh sách và lưới (`PfxListSkeleton`, `PfxGridSkeleton`). Hiệu ứng shimmer đứng yên khi người dùng bật "Giảm chuyển động".
- **Không bật hiệu ứng toàn cục.** Ví dụ `ImageLoader.crossfade(true)` cho cả app làm ảnh đã cache vẫn mờ dần mỗi lần hiện. Bật theo từng chỗ cần.
- Trạng thái của màn là một `sealed` hoặc tập cờ rõ ràng (đang tải lần đầu, có dữ liệu, rỗng, lỗi). `AnimatedContent`/`Crossfade` giữa chúng theo `PfxMotion`; chuyển "đang tải → có dữ liệu" trong 200 ms đầu thì không animate.
- **Màu ở khung hình đầu tiên:** cài đặt giao diện (sáng/tối) đọc từ DataStore là bất đồng bộ, nên có một bản cache đọc đồng bộ (SharedPreferences) ở `MainActivity.onCreate` trước `setContent`, và `setTheme` của cửa sổ theo bản cache đó.

## 4. Danh sách

- `LazyColumn`/`LazyVerticalGrid` có `key` **duy nhất theo id**, không theo chữ hiển thị (key trùng làm crash). Có `contentType` khi danh sách nhiều loại ô.
- Danh sách không giới hạn kích thước dùng Paging (`collectAsLazyPagingItems`).
- `LazyListState` cần giữ qua các chế độ hiển thị (mở/đóng tìm kiếm, đổi tab) thì **hoist** lên cấp không bị gỡ khỏi composition; nếu không thì quay lại sẽ về đầu danh sách.
- Model UI có `List` thì đánh dấu `@Immutable` hoặc dùng `kotlinx.collections.immutable`, để Compose bỏ qua recompose khi không đổi.

## 5. Hiệu năng

- Giá trị đổi liên tục (vị trí kéo, cuộn, tiến độ phát) chỉ đọc ở **composable nhỏ nhất** cần nó, hoặc đọc trong lambda của modifier (`Modifier.offset { }`, `graphicsLayer { }`, `drawBehind { }`) để chỉ chạy layout/draw, không recompose. Đọc ở cấp cao thì cả cây recompose theo từng pixel.
- `derivedStateOf` cho giá trị suy ra từ state đổi nhanh (`listState.firstVisibleItemIndex > 0`).
- `remember(key)` cho đối tượng đắt (formatter, `Brush`, `Path`); key gồm mọi thứ đối tượng phụ thuộc (kể cả locale).
- Lambda truyền xuống nhiều tầng thì ổn định (method reference `viewModel::onIntent`, hoặc `remember`).
- Bật Compose compiler metrics khi nghi recompose thừa; kiểm bằng Layout Inspector (recomposition counts).

## 6. Cử chỉ

- Modifier cử chỉ **luôn nằm trong chuỗi**; bật/tắt bằng cờ kiểm **bên trong** handler. Không bọc `if (enabled) Modifier.pointerInput(...)`: đổi cấu trúc chuỗi làm Compose gắn lại nút nhận cử chỉ và hủy cử chỉ đang chạy (ví dụ zoom hai ngón bị khựng khi vượt 1x).
- Cử chỉ lồng nhau (kéo để làm mới trong pager, thanh tua đè lên thanh điều hướng) giải bằng `NestedScrollConnection` hoặc `PointerEventPass.Initial`, và có log có tiền tố riêng để gỡ lỗi trên máy.
- Vùng chết ở mép màn hình cho cử chỉ vuốt toàn màn (tránh xung đột với vuốt Back và kéo thanh hệ thống).

## 7. Edge-to-edge, hướng màn hình, thanh hệ thống

- `enableEdgeToEdge()` ở `MainActivity`; nội dung padding theo `WindowInsets`. Không dùng Accompanist System UI Controller.
- Icon thanh trạng thái sáng/tối theo theme của app (không theo theme hệ thống) qua `WindowInsetsControllerCompat`.
- App khóa hướng trong manifest thì màn nào đổi hướng (video toàn màn hình) phải **trả về đúng giá trị của manifest** (`PORTRAIT`) khi rời màn, không phải `UNSPECIFIED` (đó là bỏ khóa).
- Thay đổi cấu hình mà app tự xử lý (`configChanges`) phải ghi trong manifest kèm comment lý do (xem `resources-i18n.md`).

## 8. Trợ năng

- Mọi icon bấm được có `contentDescription` từ `strings.xml`; icon trang trí là `null`.
- Nhóm thông tin đọc một lần bằng `Modifier.semantics(mergeDescendants = true)`.
- Không truyền đạt thông tin chỉ bằng màu.
- Kiểm bằng TalkBack các luồng chính ở mỗi lát.
