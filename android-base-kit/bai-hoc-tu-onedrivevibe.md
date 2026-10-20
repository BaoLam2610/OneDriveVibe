# Bài học từ OneDriveVibe MVP1

OneDriveVibe là app Android chỉ đọc (xem video, ảnh, PDF từ một dịch vụ lưu trữ đám mây), làm trong khoảng hai tuần (2026-10-02 đến 2026-10-13): 11 lát tính năng, 8 phase refactor, 31 ADR. Người phát triển tự build và tự kiểm tay; AI (Claude Code) viết phần lớn code nhưng **không được chạy build**, và dự án **không có test tự động**.

Mỗi mục dưới đây có ba phần: **Chuyện gì đã xảy ra**, **Luật rút ra**, và **Trong khung**: luật đó nằm ở đâu trong `template/`. Mã như `R5`, `Lát 8`, `ADR-0016` là của OneDriveVibe, chỉ để truy lại nguồn.

---

## 1. Quy trình và tài liệu

### 1.1 Làm theo lát cắt dọc, phần rủi ro cao làm trước
- **Chuyện:** mỗi lát là một luồng chạy được từ màn hình xuống API. Token, mã hóa và đồng bộ (rủi ro cao) làm ở ba lát đầu, các màn xem làm sau. Cách này giữ được tiến độ đều; lỗi kiến trúc lộ ra sớm, lúc sửa còn rẻ.
- **Luật:** lát = một luồng người dùng làm trọn được. Không làm "tầng network trước, rồi tầng data". Thứ tự lát theo rủi ro, không theo số ADR.
- **Trong khung:** `docs/process/vertical-slice.md`.

### 1.2 Đặc tả có mã quy tắc
- **Chuyện:** mọi hành vi trong đặc tả có mã (`KN-09`, `DS-04`, `VD-14`...). Code comment, mục kiểm tay và review đều trỏ về mã đó, nên hỏi "vì sao code làm vậy" là tra ra ngay.
- **Luật:** mỗi quy tắc nghiệp vụ có mã `XX-NN` ổn định. Comment, tên mục kiểm tay và mô tả PR ghi mã.
- **Trong khung:** `docs/templates/dac-ta-nghiep-vu.md`, `conventions/naming.md`.

### 1.3 Đánh số lại làm tài liệu lệch nhau
- **Chuyện:** thứ tự lát bị đổi ba lần (PDF từ Lát 7 thành 8 rồi 9). Mỗi lần đổi để lại comment và tài liệu ghi số cũ; phải viết hẳn ghi chú "đọc theo bảng" ở đầu nhiều file. Hai lát chen ngang được đặt tên chữ (Lát D, Lát T) thì không gây lệch.
- **Luật:** ID của lát, bước, quy tắc **không bao giờ đổi**. Thứ tự làm nằm ở **một** bảng duy nhất. Lát chen ngang lấy ID mới chứ không đánh số lại.
- **Trong khung:** `docs/templates/ke-hoach.md`.

### 1.4 Một giá trị, một nguồn sự thật
- **Chuyện:** cỡ trang đồng bộ là 1000 ở tài liệu, 500 ở code. Tự khóa mặc định "1 phút" ở đặc tả, "Ngay lập tức" ở code. Nhiều lần phải rà soát chỉ để sửa câu lệch.
- **Luật:** mỗi giá trị chỉ có một nơi định nghĩa (đặc tả cho hành vi, `libs.versions.toml` cho phiên bản, file Constants cho hằng số). Đổi giá trị thì `grep` mọi nơi nhắc tới nó trong cùng commit.
- **Trong khung:** `CLAUDE.md` mục "Tài liệu", `CONTRIBUTING.md` Definition of Done.

### 1.5 ADR để `proposed`, có số đo rồi mới `accepted`
- **Chuyện:** ADR tải trước video (0024) bị ADR khác thay ngay hôm sau (0026), rồi lại được bổ sung (0028). ADR-0028 làm đúng: viết `proposed`, thêm công tắc A/B ở bản debug, đo trung vị thời gian tới khung hình đầu (301 ms so với 963 ms), rồi mới `accepted`.
- **Luật:** quyết định có rủi ro kỹ thuật thì làm spike hoặc đo trước. ADR ghi rõ "cách xác nhận" và ngưỡng để giữ hay bỏ. Không sửa ADR cũ; viết ADR mới và đánh dấu bản cũ `superseded`.
- **Trong khung:** `docs/adr/template.md` (mục "Cách xác nhận"), `docs/prompts/write-adr.md`.

### 1.6 Tách "code xong" với "đã kiểm"
- **Chuyện:** file tiến độ dùng `[ ]` chưa làm, `[~]` đang làm, `[x]` code xong chờ kiểm, `[v]` đã kiểm trên máy. Nhờ vậy luôn biết phần nào chưa ai chạy thử.
- **Luật:** giữ bốn trạng thái. "Xong" chỉ là `[v]`.
- **Trong khung:** `docs/templates/tien-do.md`.

### 1.7 Nhật ký quá dài thì không ai đọc lại
- **Chuyện:** nhật ký tiến độ phình tới khoảng 150 nghìn ký tự, mỗi dòng có khi hơn 2 nghìn chữ. Đầu mỗi phiên phải đọc theo từng đoạn mới tìm được thông tin.
- **Luật:** mỗi dòng nhật ký tối đa ba câu: làm gì, trạng thái, link. Chi tiết (nguyên nhân, danh sách kiểm) nằm ở mô tả PR hoặc file kế hoạch của lát.
- **Trong khung:** `docs/templates/tien-do.md`.

### 1.8 Ghi rõ những gì không kiểm được
- **Chuyện:** nhánh "xóa dữ liệu khi nhập sai PIN 10 lần" không kiểm tay được vì chưa có UI bật tùy chọn. Kế hoạch ghi thẳng "chỉ kiểm bằng đọc code", nên không ai tưởng nó đã chạy.
- **Luật:** mục kiểm nào không làm được thì ghi lý do và cách bù (đọc code, test, hoặc để lại lát sau).
- **Trong khung:** `docs/process/manual-test-checklist.md`.

### 1.9 Không tự chế giao diện và câu chữ
- **Chuyện:** chuỗi chưa có trong thiết kế được đánh dấu `[mới]` và chờ duyệt. Màu, cỡ chữ, khoảng cách chỉ lấy từ token. Khi thiết kế ghi vùng chạm 24 dp mà luật chung là ≥ 48 dp, dự án ghi rõ thành "chênh lệch đã biết" chứ không tự chọn một bên.
- **Luật:** thiếu giá trị thì hỏi. Chênh lệch giữa thiết kế và luật thì ghi vào bảng "chênh lệch đã biết" của tài liệu UI và chờ quyết.
- **Trong khung:** `docs/templates/thiet-ke-ui.md` mục 1 và 9, `conventions/compose-ui.md`.

---

## 2. Kiến trúc

### 2.1 Chốt luật tầng từ ngày đầu, và kiểm bằng công cụ
- **Chuyện:** ban đầu luật là "chỉ tạo UseCase khi có logic thật". Bốn ngày sau đổi thành bắt buộc (ADR-0016), phải refactor cả bốn phase R5a đến R5d với khoảng 27 UseCase, phần lớn chỉ một dòng. Luật "feature không import repository" **không ép được bằng Gradle** vì feature phải phụ thuộc domain, nên chỉ kiểm bằng `grep`.
- **Luật:** UseCase bắt buộc từ commit đầu. Viết test kiến trúc (Konsist) để CI chặn import `domain.repository` trong feature.
- **Trong khung:** ADR-0006, `architecture.md` mục "Luật phụ thuộc", `conventions/testing.md` mục "Test kiến trúc".

### 2.2 Chia gói interface của domain theo vai trò
- **Chuyện:** sau R5a, domain chia `repository` (dữ liệu nghiệp vụ, feature **không** được gọi thẳng), `platform` (thứ nền tảng cung cấp: mạng, sinh trắc học, múi giờ), `settings` (cài đặt người dùng đọc lúc chạy), `hook` (điểm móc cho tầng khác cài). Feature dùng thẳng `platform` và `settings` mà không cần bọc thêm một UseCase một dòng.
- **Luật:** giữ bốn gói này. Chỉ `repository` bị cấm với feature.
- **Trong khung:** `architecture.md` mục "Domain".

### 2.3 Repository chia theo việc, không theo nguồn dữ liệu
- **Chuyện:** `DriveRepository` gom kết nối, duyệt thư mục, thư viện và màn xem; về sau phải tách thành bốn interface.
- **Luật:** một repository phục vụ một nhóm việc nghiệp vụ. Khi interface quá khoảng 8 hàm hoặc phục vụ nhiều màn không liên quan thì tách.
- **Trong khung:** `conventions/data-layer.md`.

### 2.4 Lớp gọi API không biết credentials
- **Chuyện:** sáu chỗ ở tầng data tự load config rồi truyền credentials vào từng lời gọi API. R3 và R4a phải gom lại: lớp cơ sở chỉ nhận một nguồn bearer token, credentials cấp qua interface do tầng data cài.
- **Luật:** client API nhận `TokenSource` (hoặc dùng plugin `Auth` của Ktor). Không truyền credentials theo từng lời gọi.
- **Trong khung:** `conventions/data-layer.md` mục "Mạng", ADR-0005.

### 2.5 "Xóa sạch dữ liệu" là một cơ chế, không phải một hàm
- **Chuyện:** mỗi kho dữ liệu (Room, cache ảnh, cache video, DataStore...) đăng ký một `ConnectionResetter`; Ngắt kết nối gọi tất cả. Dù vậy vẫn sót: CSDL nội bộ của Media3 và hai tệp DataStore không bị xóa, tới A1 mới phát hiện, nhờ tab Lưu trữ của màn Debug.
- **Luật:** từ ngày đầu có registry `DataResetter`; mỗi kho mới đăng ký vào đó trong cùng PR tạo kho. Công cụ debug liệt kê mọi tệp trong thư mục dữ liệu của app để kiểm "đăng xuất xong còn gì".
- **Trong khung:** `conventions/data-layer.md` mục "Xóa dữ liệu", `debug-tools.md` tab Lưu trữ.

### 2.6 Hai feature cần cùng một thứ thì tách thành module core
- **Chuyện:** tab Short cần nền phát video của màn Xem video. Thay vì cho feature phụ thuộc feature, nền phát được tách thành `:core:media` (ADR-0025).
- **Luật:** feature không bao giờ phụ thuộc feature khác. Phần dùng chung lên `:core:*`.
- **Trong khung:** `architecture.md`, ADR-0002.

### 2.7 Hằng số: mỗi module một file, giá trị đã lưu không được đổi
- **Chuyện:** hằng số rải theo từng class, có số bị chép hai nơi (`MS_PER_DAY`). R4b gom về `XxxConstants.kt`. Tên tệp, tên khóa DataStore, tên khóa Keystore và byte định dạng dữ liệu được đánh dấu "không được đổi giá trị"; refactor so 47 hằng số với `git HEAD` bằng script.
- **Luật:** mỗi module một file `XxxConstants.kt` với `internal object` theo nhóm. Giá trị đã ghi xuống máy người dùng thì chỉ được chuyển chỗ, không được đổi. Hằng số bố cục UI thì đặt cạnh composable dùng nó.
- **Trong khung:** `conventions/kotlin.md` mục "Hằng số".

### 2.8 File quá lớn
- **Chuyện:** `ODVPlayerContent.kt` lên tới 1026 dòng, phải tách thành 7 file ở R7.
- **Luật:** file vượt khoảng 400 dòng thì tách. Composable tách ra không giữ state, chỉ nhận giá trị và lambda.
- **Trong khung:** `conventions/compose-ui.md`.

---

## 3. DI (Koin)

### 3.1 Thiếu binding chỉ lộ khi chạy
- **Chuyện:** mỗi lần thêm module Koin, người dùng phải bấm qua từng màn ở bản debug mới biết có thiếu binding không. Có lần crash ngay khi mở app sau khi tiến trình bị hệ điều hành thu hồi.
- **Luật:** có unit test `KoinVerifyTest` gọi `verify()` cho mọi module. CI chạy test này.
- **Trong khung:** ADR-0004, `conventions/di-koin.md`.

### 3.2 Thứ phải có lúc khởi động thì `createdAtStart`
- **Chuyện:** nguồn trần bộ nhớ đệm được tạo lười, nên `ImageLoader` dựng trước nó luôn nhận trần mặc định; trần người dùng đặt không bao giờ có hiệu lực (HIGH ở review Lát 7d).
- **Luật:** binding mà thứ khác đọc ngay lúc dựng thì `createdAtStart = true`, và ghi comment lý do.
- **Trong khung:** `conventions/di-koin.md`.

### 3.3 Ghi đè ở bản debug bằng thứ tự module
- **Chuyện:** bản debug ghi đè vài binding (chất lượng thumbnail, công tắc A/B) bằng cách đặt module debug ở cuối danh sách khi `startKoin`.
- **Luật:** module của `DebugTools` luôn nạp sau cùng. Bản release có cùng API nhưng danh sách rỗng.
- **Trong khung:** `debug-tools.md`.

---

## 4. Dữ liệu

### 4.1 Đổi schema mà quên tăng version thì crash
- **Chuyện:** ADR gốc ghi "giữ `version = 1` trước khi phát hành". Đổi cột mà không tăng version thì Room báo lỗi hash và crash. Phải viết ADR-0015 thay luật đó.
- **Luật:** mọi thay đổi schema tăng `version` và ghi vào "Lịch sử version". Export schema JSON vào git. Trước bản phát hành đầu dùng `fallbackToDestructiveMigration`; từ bản đầu bắt buộc có Migration kèm test.
- **Trong khung:** ADR-0008, `conventions/data-layer.md`, checklist PR.

### 4.2 Hai bản SQLite cùng mở một tệp làm hỏng CSDL
- **Chuyện:** crash `SIGSEGV` trong `libsqliteJni` và `file is not a database`. Nguyên nhân: tab Lưu trữ của màn Debug mở CSDL của Room (`BundledSQLiteDriver`) bằng SQLite hệ thống trong cùng tiến trình, làm mất khóa tệp.
- **Luật:** chỉ đọc CSDL của app qua chính kết nối của Room. Với app Android thuần, ưu tiên driver SQLite của hệ thống, trừ khi có lý do cần bản bundled.
- **Trong khung:** `debug-tools.md` mục "Trình duyệt CSDL", `tech-stack.md`.

### 4.3 Đo trên máy thật trước khi tăng giới hạn
- **Chuyện:** cỡ trang đồng bộ 1000 nhanh gấp ba lần 500, nhưng gây crash trên máy thật, nên phải chốt lại 500. Lý do được ghi vào KDoc của hằng số để không ai tăng lại.
- **Luật:** hằng số hiệu năng có KDoc ghi số đo và lý do chọn.
- **Trong khung:** `conventions/kotlin.md` mục "Hằng số".

### 4.4 Offline-first: UI chỉ đọc CSDL
- **Chuyện:** UI đọc Room, đồng bộ là luồng riêng. Mỗi trang đồng bộ làm danh sách sắp xếp và vẽ lại vô ích, nên thêm `conflate()` và `distinctUntilChanged()`. Danh sách lớn dùng Paging, sắp xếp bằng SQL có index.
- **Luật:** áp dụng khi dự án cần offline (quyết bằng ADR). Luồng đọc từ CSDL luôn có `distinctUntilChanged()`; danh sách không giới hạn kích thước thì dùng Paging.
- **Trong khung:** `conventions/data-layer.md`.

### 4.5 Đo dung lượng CSDL phải cộng cả `-wal`
- **Chuyện:** sau khi xóa dữ liệu, tab Lưu trữ vẫn báo CSDL 40 KB như trước, vì dữ liệu mới nằm trong tệp `-wal`. Không phân biệt được "đã xóa" với "còn trong WAL".
- **Luật:** đo CSDL thì cộng `-wal` và `-shm`. Xóa dữ liệu xong thì `VACUUM` và `wal_checkpoint(TRUNCATE)`.
- **Trong khung:** `debug-tools.md`, `conventions/data-layer.md`.

---

## 5. Coroutine, vòng đời, đồng thời

### 5.1 `runCatching` nuốt hủy coroutine
- **Luật:** không dùng `runCatching` hay `catch (e: Exception)` trần trong code suspend. Bắt rộng thì ném lại `CancellationException` trước.
- **Trong khung:** `conventions/error-handling.md`.

### 5.2 Tác vụ dài phải có chủ ở app scope, và biết bị cắt ngang
- **Chuyện:** lần đồng bộ đầu bị khóa app cắt ngang. Đồng bộ chỉ được gọi lại khi ViewModel được tạo mới, nên tab đang mở không bao giờ hiện lại banner. Phải thêm cờ "bị khóa cắt" và tự chạy tiếp khi mở khóa. Lúc sửa còn lộ thêm một chuyện: `reset()` hủy mọi coroutine con, nên phần theo dõi trạng thái khóa phải chạy ở một scope riêng.
- **Luật:** tác vụ sống lâu hơn một màn (đồng bộ, tải) do một coordinator singleton sở hữu, có trạng thái rõ ràng (đang chạy, xong, lỗi, bị cắt) và quy tắc chạy tiếp. Không đặt lời gọi "đồng bộ khi mở" trong `init` của ViewModel.
- **Trong khung:** `conventions/concurrency-time.md`.

### 5.3 Single-flight và "epoch" cho tài nguyên dùng chung
- **Chuyện:** token và link ký được nhiều nơi xin cùng lúc. Gộp thành một yêu cầu cho mỗi khóa (single-flight). Tăng "epoch" khi khóa app để kết quả về muộn không ghi lại link vào bộ nhớ (ADR-0029).
- **Luật:** tài nguyên đắt mà nhiều nơi xin cùng lúc thì single-flight bằng `Mutex` + `Deferred`. Kho có `reset()` thì có bộ đếm thế hệ để chặn ghi muộn.
- **Trong khung:** `conventions/concurrency-time.md`.

### 5.4 Dọn dẹp và lần ghi cuối phải chạy xong
- **Chuyện:** xóa tài khoản bị hủy giữa chừng thì để lại dữ liệu mồ côi. Lần lưu vị trí xem cuối cùng khi rời màn bị mất vì `viewModelScope` đã hủy. Một coroutine canh `finally` không chạy vì job cha bị hủy trước khi nó kịp bắt đầu.
- **Luật:** dọn dẹp và lần ghi cuối chạy trong `withContext(NonCancellable)`. Coroutine bắt buộc chạy tới `finally` thì khởi động `CoroutineStart.ATOMIC` hoặc `UNDISPATCHED`.
- **Trong khung:** `conventions/concurrency-time.md`.

### 5.5 Timeout không bọc được code chặn luồng
- **Chuyện:** bọc `withTimeout` quanh `withContext(IO) { blockingCall() }` không có tác dụng, vì `withContext` bị hủy vẫn chờ khối chặn chạy xong. Một chỗ khác dùng `runBlocking` không có thời hạn và giữ luồng tải tới 60 giây.
- **Luật:** code chặn luồng chạy trong `async` ở scope riêng, chờ bằng `withTimeoutOrNull`. Không dùng `runBlocking` trong code app; nếu bắt buộc thì phải có thời hạn.
- **Trong khung:** `conventions/concurrency-time.md`.

### 5.6 Đồng hồ là phụ thuộc
- **Chuyện:** `() -> Long` dùng làm đồng hồ thì không rõ đơn vị, không rõ là giờ thực hay đồng hồ đơn điệu, và Koin không phân biệt được hai lambda cùng kiểu. Hạn của link sống khoảng một giờ bị lệch vì `TimeSource.Monotonic` không đếm thời gian máy ngủ sâu.
- **Luật:** giờ thực thì tiêm `kotlin.time.Clock`; đo khoảng thời gian thì dùng `TimeSource`; hạn phải đếm cả lúc máy ngủ thì dùng `SystemClock.elapsedRealtime()` sau một interface.
- **Trong khung:** `conventions/concurrency-time.md`.

### 5.7 Hai bộ hẹn giờ cùng khóa một lần
- **Chuyện:** tự khóa có độ trễ: bộ hẹn giờ khóa app lúc ở nền, rồi `onStart` thấy đã quá hạn và khóa lần nữa, làm hộp thoại vân tay bị hủy ngầm.
- **Luật:** hành động có thể kích hoạt từ nhiều nguồn thì phải idempotent, hoặc có cờ "đã chạy".
- **Trong khung:** `conventions/concurrency-time.md`.

### 5.8 Process death phải kiểm mỗi lát
- **Chuyện:** nhiều lỗi chỉ lộ khi bật "Don't keep activities": tab Short khôi phục thứ tự cũ, vị trí ảnh bị kéo về ảnh mở đầu, route lưu cũ thiếu trường mới.
- **Luật:** mỗi lát có mục kiểm "Don't keep activities". Route thêm trường thì trường đó có giá trị mặc định để route cũ vẫn khôi phục được.
- **Trong khung:** `docs/process/manual-test-checklist.md`, `conventions/navigation.md`.

---

## 6. Compose và trải nghiệm

### 6.1 State hay Effect
- **Chuyện:** dialog lỗi đặt trong Effect thì mất khi xoay màn hình. ViewModel cơ sở ghi rõ: thứ không được mất thì đặt trong State, kèm một Intent báo "đã xử lý" để ViewModel xóa.
- **Luật:** dialog, sheet kết quả, cảnh báo nằm trong State. Điều hướng, snackbar nhẹ là Effect. Effect chỉ thu ở **một** nơi.
- **Trong khung:** `conventions/mvi.md`.

### 6.2 Biến thường mà composable đọc
- **Chuyện:** `loadedId` của controller là biến thường chứ không phải `mutableStateOf`. Sau khi nạp video, trang không dựng lại nên bề mặt phát không được gắn: có tiếng mà không có hình.
- **Luật:** mọi giá trị composable đọc mà có thể đổi đều phải là state của Compose hoặc `StateFlow`.
- **Trong khung:** `conventions/compose-ui.md`.

### 6.3 Đổi cấu trúc chuỗi modifier làm hủy cử chỉ
- **Chuyện:** khi zoom vượt 1x, modifier vuốt bị gỡ khỏi chuỗi. Compose gán lại nút nhận cử chỉ và hủy cử chỉ hai ngón đang chạy, nên zoom bị khựng.
- **Luật:** modifier cử chỉ luôn nằm trong chuỗi; bật tắt bằng cờ kiểm bên trong, không bằng `if` quanh modifier.
- **Trong khung:** `conventions/compose-ui.md`.

### 6.4 Nháy và khựng khi đổi bộ lọc
- **Chuyện:** đổi chip lọc thì xóa danh sách, hiện vòng quay rồi dựng lại; đổi tài khoản thì danh sách → vòng quay → danh sách. `crossfade(true)` bật toàn app làm ảnh đã cache vẫn nháy. Spinner lóe lên chỉ vài trăm mili giây.
- **Luật:** đổi bộ lọc thì giữ dữ liệu cũ cho tới khi có dữ liệu mới. Chỉ hiện trạng thái tải sau khoảng 200 ms, dùng skeleton thay vòng quay. Không bật hiệu ứng toàn cục; bật theo từng chỗ.
- **Trong khung:** `conventions/compose-ui.md` mục "Trạng thái tải".

### 6.5 Màu theme nháy lúc khởi động
- **Chuyện:** máy đang tối mà app chọn sáng thì thấy nền tối rồi mới sáng, vì cài đặt giao diện đọc bất đồng bộ từ DataStore. Sửa bằng một bản cache đọc đồng bộ (SharedPreferences) ở `onCreate`.
- **Luật:** cài đặt ảnh hưởng tới khung hình đầu tiên (theme, ngôn ngữ) có bản cache đọc đồng bộ.
- **Trong khung:** `conventions/compose-ui.md`, `conventions/resources-i18n.md`.

### 6.6 Đổi ngôn ngữ trong app
- **Chuyện:** đổi ngôn ngữ làm Activity bị tạo lại và màn hình chớp, nên thêm `locale|layoutDirection` vào `configChanges`. Sau đó chữ đọc qua `LocalContext.current.resources` không đổi theo, phải chuyển sang `LocalResources.current`. Locale định dạng số và ngày phải lấy từ `LocalLocale`, không từ `Locale.getDefault()`.
- **Luật:** ba luật trên áp dụng từ đầu. Không cache chuỗi hay `Resources` trong ViewModel.
- **Trong khung:** `conventions/resources-i18n.md`.

### 6.7 Key trùng trong lazy list làm crash
- **Chuyện:** đăng ký công cụ debug hai lần với cùng tiêu đề làm `LazyColumn` (key = tiêu đề) crash.
- **Luật:** key của lazy list phải duy nhất theo định nghĩa (id), không theo chữ hiển thị. Registry đăng ký trùng thì phải idempotent.
- **Trong khung:** `conventions/compose-ui.md`.

### 6.8 State của danh sách mất khi composable rời composition
- **Chuyện:** mở tìm kiếm làm lưới rời composition, nên `rememberLazyGridState` bên trong mất; đóng tìm thì lưới về đầu.
- **Luật:** state cuộn cần giữ qua các chế độ hiển thị thì hoist lên cấp không bị gỡ.
- **Trong khung:** `conventions/compose-ui.md`.

### 6.9 Recompose theo từng pixel
- **Chuyện:** kéo để xáo lại làm cả Pager recompose theo từng pixel, vì giá trị kéo được đọc ở cấp cao.
- **Luật:** giá trị đổi liên tục (kéo, cuộn, tiến độ) chỉ đọc ở composable nhỏ nhất cần nó, hoặc đọc trong lambda (`graphicsLayer { }`, `Modifier.offset { }`).
- **Trong khung:** `conventions/compose-ui.md` mục "Hiệu năng".

### 6.10 Khóa hướng màn hình
- **Chuyện:** rời màn video thì phải trả hướng về `PORTRAIT`. Trả về `UNSPECIFIED` là bỏ khóa dọc, app xoay tự do.
- **Luật:** màn đổi hướng phải trả lại đúng giá trị của manifest khi rời màn.
- **Trong khung:** `conventions/compose-ui.md`.

---

## 7. Gỡ lỗi và chẩn đoán

### 7.1 Công cụ debug trong app ngay sau lát đầu
- **Chuyện:** Lát D (nút bọ nổi, log API đầy đủ, log local, xem dữ liệu đã lưu, xem bảng CSDL) được chen ngay sau Lát 1. Gần như mọi lỗi về sau đều được chẩn đoán bằng log người dùng chụp từ màn này.
- **Luật:** `:tools:debug` dựng ở bootstrap, trước feature đầu tiên.
- **Trong khung:** `debug-tools.md`, `prompts/01-port-debug-tools.md` (của kit).

### 7.2 Log trước, sửa sau
- **Chuyện:** video không phát được trên một dòng máy. Ba bản sửa đầu (chặn decoder, đổi định dạng, bỏ metadata) đều dựa trên chẩn đoán sai "lỗi do Dolby Vision". Chỉ khi thêm log liệt kê profile của từng decoder mới thấy nguyên nhân thật: máy không có decoder HEVC 10-bit nào.
- **Luật:** lỗi phụ thuộc thiết bị thì làm bước B0 "chỉ thêm log" trước. Ghi các giả thuyết H1, H2... cùng cách phân xử từng cái, rồi mới sửa.
- **Trong khung:** `docs/process/vertical-slice.md` mục "Sửa lỗi khó".

### 7.3 Log truy vết tạm có tên riêng và được gỡ
- **Chuyện:** lỗi đồng bộ sau khi mở khóa không chốt được bằng đọc code (năm giả thuyết). Thêm log tạm có tiền tố riêng (`TRACE-SYNC`) ở tám chỗ, người dùng gửi log, xác nhận xong thì gỡ sạch bằng một lần `grep`.
- **Luật:** log tạm dùng một tiền tố tìm được bằng `grep`, có dòng nhật ký "đã thêm log tạm ở đâu", và gỡ trong commit riêng.
- **Trong khung:** `conventions/logging.md`.

### 7.4 "Vẫn bị" có khi là bản build cũ
- **Chuyện:** người dùng báo bản sửa không có tác dụng; log truy vết cho thấy nhiều khả năng bản build lúc đó chưa có bản sửa.
- **Luật:** màn Debug hiện commit hash và thời điểm build. Báo lỗi kèm hai thông tin đó.
- **Trong khung:** `debug-tools.md` tab Khác.

### 7.5 Hành vi riêng của bản debug phải nói rõ
- **Chuyện:** bản debug được chụp màn hình các màn nhạy cảm khi tắt "Bảo vệ màn hình" (ADR-0031); bản release luôn chặn. Bản release gỡ hết writer của log.
- **Luật:** mọi khác biệt debug/release đi qua cổng `DebugTools` (hai bản cùng API) và có ADR.
- **Trong khung:** `debug-tools.md`, ADR-0011.

---

## 8. Bảo mật

### 8.1 Không bao giờ log bí mật hay `e.message`
- **Chuyện:** `AppError.Unknown.toString()` bị ghi đè chỉ in tên loại, vì nội dung ngoại lệ có thể chứa URL ký. Log chỉ ghi mã lỗi và tên lớp ngoại lệ. Không cài plugin `Logging` của Ktor vì nó in header `Authorization`.
- **Luật:** như trên. Log API đầy đủ chỉ có ở bản debug, chỉ trong RAM.
- **Trong khung:** `conventions/security.md`, `conventions/logging.md`.

### 8.2 `FLAG_SECURE` là của từng Window
- **Chuyện:** bật "Bảo vệ màn hình" chỉ đặt cờ lên Window của `MainActivity`; màn Debug (Activity khác) vẫn chụp được dù chứa log có secret. Sửa bằng một chính sách cấp tiến trình và đăng ký mọi Window qua `ActivityLifecycleCallbacks`.
- **Luật:** chính sách chặn chụp màn hình là trạng thái cấp tiến trình, áp lên mọi Window đã đăng ký, kể cả Dialog.
- **Trong khung:** `conventions/security.md`.

### 8.3 Xóa dữ liệu nhạy cảm khỏi ViewModel khi khóa
- **Chuyện:** nếu app tự khóa lúc người dùng đang ở bước nhập PIN mới, PIN hiện tại vẫn nằm trong bộ nhớ của ViewModel (HIGH ở review Lát 7c).
- **Luật:** ViewModel giữ dữ liệu nhạy cảm (PIN, secret, OTP) phải quan sát trạng thái khóa hoặc đăng xuất và tự xóa.
- **Trong khung:** `conventions/security.md`.

### 8.4 Tắt sao lưu và không theo redirect mang bearer
- **Luật:** `allowBackup=false` kèm `dataExtractionRules` loại mọi miền (Android 12+ bỏ qua `allowBackup` khi chuyển máy). Client mang bearer token đặt `followRedirects = false`; tải URL ký bằng một client riêng không có bearer.
- **Trong khung:** `conventions/security.md`.

---

## 9. Làm việc với AI

### 9.1 AI không build thì lỗi biên dịch là chuyện thường
- **Chuyện:** gần như mỗi lát đều có lỗi biên dịch nhỏ: thiếu import, `await` trong lambda không suspend, chữ ký API lệch phiên bản. AI phải ghi "rủi ro build" khi bàn giao, người dùng gửi lỗi lại.
- **Luật:** ở dự án công ty, CI build và chạy unit test cho mọi PR. Khi AI không tự build được, nó phải liệt kê API nó chưa chắc chắn ở cuối phần bàn giao.
- **Trong khung:** `docs/process/lam-viec-voi-ai.md`.

### 9.2 Đối chiếu API với mã nguồn đúng phiên bản
- **Chuyện:** những chỗ AI đọc mã nguồn của đúng phiên bản thư viện (Room 3.0.3, Media3 1.11.1, Telephoto 0.19) thì chạy ngay. Còn đoán theo tài liệu chung thì thường sai. Đọc mã nguồn còn tìm ra những bẫy không có trong tài liệu, ví dụ chế độ cache của `DefaultPreloadManager` xóa cả video khỏi cache chung.
- **Luật:** dùng API ít phổ biến thì đọc mã nguồn hoặc tài liệu của đúng phiên bản trong `libs.versions.toml` (Context7, sources jar), không đoán.
- **Trong khung:** `docs/process/lam-viec-voi-ai.md`.

### 9.3 Không ghi đè file người dùng đã sửa tay
- **Chuyện:** người dùng đổi `setPull` thành `applyPull` để hết lỗi biên dịch, rồi AI ghi đè cả file và đưa lỗi trở lại.
- **Luật:** AI chỉ sửa bằng Edit, đọc lại file trước khi sửa. Không viết lại cả file đã có.
- **Trong khung:** `CLAUDE.md`, `docs/process/lam-viec-voi-ai.md`.

### 9.4 Lỗi "Platform declaration clash"
- **Chuyện:** `var x by ... private set` sinh ra `setX(...)`, nên khai báo thêm `fun setX(...)` cùng kiểu tham số trong cùng lớp sẽ lỗi.
- **Luật:** hàm cập nhật đặt tên `applyX` hoặc `updateX`.
- **Trong khung:** `conventions/kotlin.md`.

### 9.5 Sửa hàng loạt bằng script làm mất khai báo
- **Chuyện:** script chèn hằng số bằng cách cắt ở dấu `}` cuối file, xóa mất hai khai báo cấp cao nằm sau `object` (8 lỗi biên dịch). Một lệnh căn lề tự động làm mất một điều kiện `if`, khiến lớp phủ luôn che màn hình (màn đen sau Splash).
- **Luật:** sau khi sửa bằng script hoặc công cụ định dạng, so danh sách khai báo cấp cao và `git diff` với HEAD; không chỉ kiểm import.
- **Trong khung:** `docs/process/lam-viec-voi-ai.md`.

### 9.6 Review chỉ đọc code sau mỗi lát
- **Chuyện:** mỗi lát kết thúc bằng một lượt review Kotlin chỉ đọc code. Các lượt này tìm ra nhiều lỗi HIGH thật: tràn bộ đệm effect, dữ liệu nhạy cảm còn trong RAM, binding tạo lười, đóng cache khi player còn giữ.
- **Luật:** mỗi PR có review theo checklist; lát lớn chạy thêm review của agent.
- **Trong khung:** `docs/process/review-checklist.md`, `docs/prompts/review.md`.

---

## 10. Dự án mới nên làm khác

| OneDriveVibe | Dự án công ty | Lý do |
|---|---|---|
| Không test (ADR-0009) | Unit test ViewModel và UseCase có logic, `:core:testing`, test kiến trúc và test `verify()` của Koin | Refactor R0 đến R7 dựa hoàn toàn vào kiểm tay; nhiều lỗi hồi quy chỉ lộ khi người dùng thử |
| Không CI, không lint | CI (build, ktlint, detekt + compose-rules, Android Lint, unit test) từ ngày đầu | Plugin `quality` nằm trong checklist suốt dự án mà không lát nào nhận |
| KMP chỉ có target Android | Android thuần; domain là module Kotlin/JVM để Gradle ép "không có `android.*`" | Không trả phí cho tính di động không dùng tới |
| UseCase "khi cần" rồi mới bắt buộc | Bắt buộc từ commit đầu | Tránh refactor R5 |
| Nhật ký dài | Nhật ký ngắn, chi tiết ở PR | Mục 1.7 |
| Room bundled driver | Driver hệ thống (trừ khi cần bản bundled) | Mục 4.2 |
