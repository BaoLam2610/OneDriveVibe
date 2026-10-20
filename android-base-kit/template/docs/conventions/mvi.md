# MVI

Quyết định: ADR-0001. Mỗi màn có ba kiểu: `State` (toàn bộ những gì màn hiển thị), `Intent` (hành động của người dùng), `Effect` (thao tác một lần). ViewModel kế thừa `BaseMviViewModel` ở `:core:ui`.

## 1. `BaseMviViewModel`

```kotlin
abstract class BaseMviViewModel<S : Any, I : Any, E : Any>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // UNLIMITED + trySend: không bao giờ treo và giữ đúng thứ tự.
    // Đừng đổi capacity mà vẫn giữ trySend: kênh đầy thì trySend bỏ effect mà không báo.
    private val _effects = Channel<E>(Channel.UNLIMITED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** Điểm vào duy nhất cho mọi hành động từ UI. */
    abstract fun onIntent(intent: I)

    /** Reducer thuần, an toàn khi gọi đồng thời. */
    protected fun setState(reduce: S.() -> S) = _state.update(reduce)

    protected fun sendEffect(effect: E) {
        _effects.trySend(effect)
    }
}
```

Không dùng framework MVI nặng (Orbit, MVIKotlin): lớp trên là đủ, dễ đọc, dễ test.

## 2. Contract

```kotlin
// CartContract.kt
data class CartState(
    val items: List<CartItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiError? = null,           // KHÔNG phải String, KHÔNG phải e.message
    val confirmRemove: CartItemUi? = null, // dialog: nằm trong State
)

sealed interface CartIntent {
    data object Retry : CartIntent
    data class RemoveClicked(val id: String) : CartIntent
    data object RemoveConfirmed : CartIntent
    data object RemoveDismissed : CartIntent   // Intent "đã xử lý" để xóa dialog khỏi State
    data class ItemClicked(val id: String) : CartIntent
}

sealed interface CartEffect {
    data class OpenProduct(val id: String) : CartEffect
    data object ShowRemovedSnackbar : CartEffect
}
```

## 3. State hay Effect

| Loại | Đặt ở | Ví dụ |
|---|---|---|
| Không được mất, hoặc phải còn sau khi xoay màn hình / đổi ngôn ngữ / process death | **State** + Intent báo "đã xử lý" để ViewModel xóa | Dialog lỗi, dialog xác nhận, sheet kết quả, cảnh báo, banner |
| Mất cũng không hại | **Effect** | Điều hướng, snackbar nhẹ, mở trình duyệt, rung |

Channel không bảo đảm effect được xử lý: UI nhận xong mà vòng đời dừng thì effect mất. Đó là lý do bảng trên tồn tại.

## 4. ViewModel

```kotlin
class CartViewModel(
    private val observeCart: ObserveCartUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
) : BaseMviViewModel<CartState, CartIntent, CartEffect>(CartState()) {

    init { observe() }

    override fun onIntent(intent: CartIntent) {
        when (intent) {
            CartIntent.Retry -> observe()
            is CartIntent.RemoveClicked -> setState { copy(confirmRemove = items.find { it.id == intent.id }) }
            CartIntent.RemoveConfirmed -> onRemoveConfirmed()
            CartIntent.RemoveDismissed -> setState { copy(confirmRemove = null) }
            is CartIntent.ItemClicked -> sendEffect(CartEffect.OpenProduct(intent.id))
        }
    }

    private var observeJob: Job? = null

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeCart().collect { result ->
                result.fold(
                    onSuccess = { items -> setState { copy(items = items.map { it.toUi() }, isLoading = false, error = null) } },
                    onFailure = { error -> setState { copy(isLoading = false, error = error.toUiError()) } },
                )
            }
        }
    }

    private fun onRemoveConfirmed() {
        val target = currentState.confirmRemove ?: return
        setState { copy(confirmRemove = null) }
        viewModelScope.launch {
            removeFromCart(target.id).onSuccess { sendEffect(CartEffect.ShowRemovedSnackbar) }
        }
    }
}
```

Luật:

- ViewModel chỉ phụ thuộc **UseCase** và interface `domain.platform` / `domain.settings`. Không import `domain.repository` (ADR-0006).
- Không giữ `Context`, `Resources`, chuỗi đã dịch, `View`, `Activity` trong ViewModel.
- Một hành động bấm đôi không được chạy hai lần: kiểm cờ trong State (`isSubmitting`) trước khi `launch`, hoặc giữ `Job` và bỏ qua khi đang chạy.
- Luồng thu dài (`collect`) có `Job` để hủy và chạy lại (Retry, đổi bộ lọc). Đổi tham số đầu vào thì dùng `flatMapLatest` trên một `MutableStateFlow` của tham số.
- Logic trình bày (ghép luồng, lọc theo UI, debounce tìm kiếm) ở ViewModel; luật nghiệp vụ ở UseCase.
- Dữ liệu nhạy cảm (mật khẩu, PIN, OTP) trong State phải được xóa khi khóa app hoặc đăng xuất (`security.md`).
- Tên hàm private không trùng tên tham số UseCase (`kotlin.md` mục 3).

## 5. Screen và Content

```kotlin
// CartScreen.kt: nối ViewModel, thu Effect, chuyển lambda điều hướng. Không có @Preview.
@Composable
fun PfxCartScreen(
    onOpenProduct: (String) -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val removedText = stringResource(R.string.cart_removed)

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is CartEffect.OpenProduct -> onOpenProduct(effect.id)
            CartEffect.ShowRemovedSnackbar -> snackbar.showSnackbar(removedText)
        }
    }

    PfxCartContent(state = state, onIntent = viewModel::onIntent, snackbar = snackbar)
}

// CartContent.kt: stateless, preview được, test UI được.
@Composable
fun PfxCartContent(state: CartState, onIntent: (CartIntent) -> Unit, snackbar: SnackbarHostState, modifier: Modifier = Modifier) { /* ... */ }
```

`CollectEffects` ở `:core:ui`:

```kotlin
/** Thu effect đúng MỘT nơi, chỉ khi STARTED, trên Main.immediate để không trễ một khung hình. */
@Composable
fun <E> CollectEffects(effects: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) { effects.collect { currentOnEffect(it) } }
        }
    }
}
```

Luật:

- Chỉ thu `effects` ở **một** nơi (`XxxScreen`). Hai nơi cùng thu sẽ chia nhau effect.
- `Content` nhận `state` + `onIntent` (hoặc vài lambda cụ thể), không nhận ViewModel.
- Màn nhận lambda điều hướng, không nhận back stack hay nav controller (ADR-0003).

## 6. Khôi phục sau process death

- Thứ người dùng nhập hoặc chọn mà mất thì khó chịu (ô nhập, tab đang chọn, seed xáo trộn) giữ bằng `SavedStateHandle` trong ViewModel hoặc `rememberSaveable` trong UI.
- Dữ liệu lấy lại được từ CSDL/API thì không cần lưu.
- ViewModel có khôi phục trạng thái từ snapshot thì phân biệt "ViewModel mới sinh sau process death" với "ViewModel còn sống, dữ liệu vừa bị reset", để không áp snapshot cũ sai lúc.
