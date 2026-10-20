# Coroutine, vòng đời, thời gian

## 1. Dispatcher

```kotlin
// :core:common
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
```

- Tiêm `DispatcherProvider`; không gọi thẳng `Dispatchers.IO` trong data/domain. Test dùng `TestDispatcherProvider` (`:core:testing`).
- Hàm suspend tự chịu trách nhiệm chuyển luồng (`withContext(dispatchers.io)` bên trong repository), người gọi không phải nhớ.
- Room và Ktor đã tự chạy nền; không bọc thêm `withContext(io)` quanh lời gọi DAO suspend hay `client.get`.
- Ngoại lệ có chủ đích (code Android cần `Dispatchers.Main.immediate`) thì ghi comment ở chỗ dùng.

## 2. Scope

| Scope | Dùng cho | Ghi chú |
|---|---|---|
| `viewModelScope` | Việc gắn với một màn | Mặc định |
| App scope (`CoroutineScope(SupervisorJob() + dispatchers.default)`, singleton Koin `named("app")`) | Việc sống lâu hơn một màn: đồng bộ, tải, dọn dẹp | Có handler bắt lỗi; có chủ (coordinator) |
| `rememberCoroutineScope()` | Animation, cuộn, snackbar trong UI | Không gọi repository từ đây |
| `GlobalScope` | **Cấm** | |
| `runBlocking` | **Cấm** trong code app | Nếu bắt buộc (API đồng bộ của thư viện bên thứ ba) thì có `withTimeout` |

## 3. Tác vụ dài: có chủ và biết bị cắt ngang

Tác vụ sống lâu hơn một màn (đồng bộ dữ liệu, tải tệp) do **một coordinator singleton** sở hữu:

```kotlin
class SyncCoordinator(
    private val engine: SyncEngine,
    private val appScope: CoroutineScope,
    private val clock: Clock,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()
    private val mutex = Mutex()
    private var job: Job? = null

    /** Single-flight: đang chạy thì không chạy thêm. */
    fun syncIfStale() { /* kiểm hạn bằng clock, rồi launchSync() */ }

    private fun launchSync() = appScope.launch(start = CoroutineStart.ATOMIC) { /* ... */ }

    /** Đăng xuất: hủy việc đang chạy, xóa trạng thái. */
    suspend fun reset() { /* ... */ }
}
```

- **Trạng thái rõ ràng:** đang chạy, xong, lỗi, **bị cắt ngang** (khóa app, mất mạng, đăng xuất) và quy tắc chạy tiếp cho từng loại.
- **Không đặt "đồng bộ khi mở màn" trong `init` của ViewModel.** ViewModel sống qua lần ra nền, nên mở lại app không gọi lại. Kích hoạt từ app shell (`ProcessLifecycleOwner` ON_START, mở khóa, có mạng lại) qua một UseCase.
- `reset()` hủy mọi coroutine con của scope đồng bộ. Phần **theo dõi** (quan sát trạng thái khóa, mạng) chạy ở scope riêng, nếu không sau `reset()` nó chết cho tới khi khởi động lại app.
- Hành động có thể bị kích hoạt từ nhiều nguồn (hẹn giờ và `onStart` cùng muốn khóa) phải idempotent hoặc có cờ "đã chạy".

## 4. Single-flight và thế hệ (epoch)

Tài nguyên đắt mà nhiều nơi xin cùng lúc (token, URL ký, cấu hình từ xa):

```kotlin
class TokenCache(private val fetch: suspend () -> AppResult<Token>, private val clock: Clock) {
    private val mutex = Mutex()
    private var cached: Token? = null
    private var epoch = 0L

    suspend fun get(): AppResult<Token> = mutex.withLock {
        cached?.takeIf { it.expiresAt > clock.now() + 60.seconds }?.let { return AppResult.Success(it) }
        val startedAt = epoch
        fetch().onSuccess { if (epoch == startedAt) cached = it } // kết quả của thế hệ cũ bị bỏ
    }

    /** Bỏ đúng token bị từ chối (401), không bỏ token mới hơn mà luồng khác vừa lấy. */
    suspend fun invalidate(rejected: Token) = mutex.withLock { if (cached == rejected) cached = null }

    /** Khóa app / đăng xuất. */
    suspend fun clear() = mutex.withLock { cached = null; epoch++ }
}
```

- Nhiều lời gọi cùng lúc chỉ tạo **một** request. Với nhiều khóa (mỗi id một URL) thì giữ `Map<Key, Deferred<T>>` chạy trong scope riêng, để người chờ bị hủy không hủy việc của người khác.
- Kho có `clear()`/`reset()` thì tăng `epoch`; việc đang chạy chỉ ghi kết quả nếu `epoch` không đổi. Nếu không, kết quả về muộn sẽ ghi lại dữ liệu vừa bị xóa (ví dụ ghi lại link ký sau khi khóa app).
- `401`: bỏ đúng token bị từ chối rồi thử lại **một** lần.

## 5. Phải chạy xong

- **Dọn dẹp và lần ghi cuối** (xóa dữ liệu khi đăng xuất, lưu vị trí khi rời màn) chạy trong `withContext(NonCancellable) { ... }`. `viewModelScope` bị hủy khi rời màn; không có `NonCancellable` thì lần ghi cuối mất.
- Coroutine mà `finally` **bắt buộc** chạy (hủy một tác vụ ngoài, nhả tài nguyên) khởi động bằng `start = CoroutineStart.ATOMIC` hoặc `UNDISPATCHED`. Với `DEFAULT`, nếu job cha bị hủy trước khi coroutine kịp chạy thì `finally` không bao giờ chạy.
- Dọn nhiều kho thì mỗi kho `try/catch` riêng: một kho lỗi không chặn các kho sau. Log tên kho và tên lớp ngoại lệ.

## 6. Timeout

- `withTimeout`/`withTimeoutOrNull` chỉ hủy được code **hợp tác** (suspend). Bọc quanh `withContext(io) { blockingCall() }` **không có tác dụng**: `withContext` bị hủy vẫn chờ khối chặn chạy xong.
- Code chặn luồng cần thời hạn: chạy trong `scope.async(dispatchers.io) { blockingCall() }` ở scope riêng, chờ bằng `withTimeoutOrNull(t) { deferred.await() }`; hết hạn thì bỏ kết quả và để việc tự kết thúc (hoặc gọi API hủy của thư viện nếu có).
- Mọi chờ đợi mạng có trần: `HttpTimeout` của Ktor cho request; `withTimeout` cho chuỗi nhiều bước.

## 7. Flow

- Luồng từ CSDL: `distinctUntilChanged()`; nếu nguồn phát dồn dập (mỗi trang đồng bộ) thì thêm `conflate()` trước bước tốn công (sắp xếp, map).
- Đổi tham số đầu vào: `MutableStateFlow` tham số + `flatMapLatest`. Đừng `collect` lồng nhau.
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` cho state suy ra từ luồng. Lưu ý: hết 5 giây không có ai thu thì luồng dừng; logic không được phụ thuộc vào việc luồng đó luôn chạy.
- `shareIn`/`stateIn` ở singleton thì dùng app scope, không dùng `viewModelScope`.
- `callbackFlow` luôn có `awaitClose { unregister() }`.

## 8. Thời gian

| Cần | Dùng | Không dùng |
|---|---|---|
| Giờ thực (hạn token, "đồng bộ quá 15 phút") | Tiêm `kotlin.time.Clock` (`Clock.System` ở Koin, `FixedClock` ở test) | `System.currentTimeMillis()` rải rác |
| Đo khoảng (thời lượng request) | `TimeSource.Monotonic.markNow()` | `Clock` (giờ máy có thể bị chỉnh) |
| Hạn phải đếm cả lúc máy ngủ sâu (URL ký sống 1 giờ) | `SystemClock.elapsedRealtime()` sau một interface `BootClock` | `TimeSource.Monotonic` (trên Android dựa vào `nanoTime`, không đếm lúc ngủ sâu) |
| Ngày theo lịch, múi giờ | `kotlinx-datetime`; múi giờ qua interface `platform` | `java.util.Calendar` trong domain |

- **Không** tiêm `() -> Long` làm đồng hồ: không rõ đơn vị, không rõ là giờ thực hay đơn điệu, và Koin không phân biệt được hai lambda cùng kiểu.
- Nhãn "Hôm nay/Hôm qua" và múi giờ phải tính lại khi qua nửa đêm hoặc khi app quay lại foreground, không chốt một lần lúc mở màn.
