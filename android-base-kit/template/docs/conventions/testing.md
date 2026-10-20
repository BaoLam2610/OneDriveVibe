# Test

Quyết định: ADR-0009. **Bắt buộc:** unit test cho ViewModel (mới hoặc đổi hành vi), test `verify()` của Koin, test kiến trúc. **Nên có:** test cho UseCase có logic thật, mapper phức tạp, policy ở domain. **Từ bản phát hành đầu:** test migration Room. Không đặt chỉ tiêu % coverage; tiêu chí là "hành vi trong đặc tả có test".

## 1. Chạy ở đâu

| Loại | Thư mục | Runner | Chạy bằng |
|---|---|---|---|
| ViewModel, UseCase, mapper, policy | `src/test` | JUnit 4 trên JVM | `./gradlew testDebugUnitTest` |
| Koin `verify()`, test kiến trúc | `:app/src/test` | JUnit 4 | như trên |
| Migration Room | `:core:database/src/androidTest` | AndroidJUnitRunner | `./gradlew connectedDebugAndroidTest` |

Không viết UI test Compose ở giai đoạn đầu; `Content` stateless + preview là đủ để soát giao diện. Khi cần, thêm sau bằng ADR.

## 2. Module `:core:testing`

Mọi module có test khai báo `testImplementation(project(":core:testing"))` (convention plugin tự thêm).

### 2.1 Dispatcher và đồng hồ

```kotlin
/** Thay Dispatchers.Main bằng dispatcher test cho mỗi test. */
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}

/** Đồng hồ chỉnh được bằng tay. */
class FixedClock(var now: Instant = Instant.parse("2026-01-01T00:00:00Z")) : Clock {
    override fun now(): Instant = now
    fun advance(by: Duration) { now += by }
}
```

- `UnconfinedTestDispatcher` làm mặc định cho `Main`: coroutine trong `viewModelScope` chạy ngay tới điểm treo đầu tiên, test dễ đọc. Cần kiểm thứ tự chặt (debounce, nhiều coroutine chen nhau) thì dùng `StandardTestDispatcher` + `advanceUntilIdle()` / `advanceTimeBy()`.
- `runTest` dùng **cùng** dispatcher với rule: `runTest(main.dispatcher) { ... }` khi cần điều khiển thời gian ảo.

### 2.2 Helper cho MVI

```kotlin
class MviTestScope<S, E>(val states: ReceiveTurbine<S>, val effects: ReceiveTurbine<E>)

/** Thu song song state và effect của một BaseMviViewModel bằng Turbine. */
suspend fun <S : Any, I : Any, E : Any> BaseMviViewModel<S, I, E>.testMvi(
    block: suspend MviTestScope<S, E>.() -> Unit,
) = turbineScope {
    val scope = MviTestScope(state.testIn(this), effects.testIn(this))
    scope.block()
    scope.states.cancelAndIgnoreRemainingEvents()
    scope.effects.cancelAndIgnoreRemainingEvents()
}
```

Tên API Turbine (`turbineScope`, `testIn`, `expectMostRecentItem`) kiểm lại theo đúng phiên bản.

### 2.3 Fake

Ưu tiên **fake tự viết** thay vì mock: fake là code thật, đọc được, dùng lại được ở nhiều test.

```kotlin
class FakeCartRepository : CartRepository {
    private val items = MutableStateFlow<AppResult<List<CartItem>>>(AppResult.Success(emptyList()))
    var removeResult: AppResult<Unit> = AppResult.Success(Unit)
    val removedIds = mutableListOf<String>()

    fun emit(list: List<CartItem>) { items.value = AppResult.Success(list) }
    fun failWith(error: AppError) { items.value = AppResult.Failure(error) }

    override fun observeCart(): Flow<AppResult<List<CartItem>>> = items
    override suspend fun remove(id: String): AppResult<Unit> {
        removedIds += id
        return removeResult
    }
}
```

- Fake của interface `domain.repository`, `domain.platform`, `domain.settings` dùng ở nhiều feature đặt ở `:core:testing`; fake chỉ một feature dùng thì đặt trong `src/test` của feature đó.
- Fake ghi lại lời gọi (`removedIds`) và cho cấu hình kết quả (`removeResult`), không chứa logic nghiệp vụ.
- Test ViewModel dùng **UseCase thật + repository giả**. Không fake UseCase trừ khi UseCase có phụ thuộc nặng.
- MockK chỉ cho lớp của bên thứ ba khó fake. Không `mockk<CartRepository>()` khi đã có `FakeCartRepository`.

## 3. Viết test ViewModel

```kotlin
class CartViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeCartRepository()
    private fun createViewModel() = CartViewModel(
        observeCart = ObserveCartUseCase(repo),
        removeFromCart = RemoveFromCartUseCase(repo),
    )

    @Test
    fun `hiện danh sách khi tải xong`() = runTest {
        repo.emit(listOf(cartItem(id = "a")))

        createViewModel().testMvi {
            val state = states.expectMostRecentItem()
            assertEquals(listOf("a"), state.items.map { it.id })
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `CART-07 xác nhận xóa thì gọi repository và báo snackbar`() = runTest {
        repo.emit(listOf(cartItem(id = "a")))
        val vm = createViewModel()

        vm.testMvi {
            vm.onIntent(CartIntent.RemoveClicked("a"))
            assertEquals("a", states.expectMostRecentItem().confirmRemove?.id)

            vm.onIntent(CartIntent.RemoveConfirmed)
            assertEquals(CartEffect.ShowRemovedSnackbar, effects.awaitItem())
            assertNull(states.expectMostRecentItem().confirmRemove)
        }
        assertEquals(listOf("a"), repo.removedIds)
    }

    @Test
    fun `lỗi mạng hiện UiError NoConnection, không hiện chuỗi`() = runTest {
        repo.failWith(AppError.Network)
        createViewModel().testMvi {
            assertEquals(UiError.NoConnection, states.expectMostRecentItem().error)
        }
    }
}
```

Mỗi ViewModel nên có test cho:

- [ ] Trạng thái đầu và sau khi tải xong.
- [ ] Mỗi Intent làm đổi State hoặc phát Effect đúng.
- [ ] Đường lỗi: `AppError` → đúng `UiError`, cờ tải được tắt.
- [ ] Chống bấm đôi (hành động chỉ chạy một lần khi Intent đến hai lần liên tiếp).
- [ ] Dialog/sheet trong State được xóa bởi Intent "đã xử lý".
- [ ] Quy tắc nghiệp vụ có mã trong đặc tả: tên test bắt đầu bằng mã (`CART-07 ...`).
- [ ] Debounce, retry, hủy luồng cũ khi đổi tham số (dùng `StandardTestDispatcher` + thời gian ảo).

Luật:

- Tên test là câu mô tả hành vi, có thể viết tiếng Việt trong dấu backtick.
- Mỗi test kiểm **một** hành vi. Arrange, Act, Assert tách rõ.
- Không `Thread.sleep`, không `delay` thật: dùng thời gian ảo.
- Không test chi tiết cài đặt (biến private, số lần gọi `setState`); test cái người dùng thấy (State, Effect) và tác động ra ngoài (lời gọi vào fake).

## 4. Test UseCase và domain

- UseCase chỉ ủy quyền một dòng: không cần test riêng (đã đi qua test ViewModel).
- UseCase có nhánh, ghép nhiều repository, có luật (tính tổng, chính sách khóa, tự cân bằng tỉ lệ): test riêng với fake.
- Policy thuần (`LockoutPolicy.penaltyFor(attempts)`) test bằng bảng giá trị (`@Test` lặp qua danh sách cặp vào/ra).

## 5. Test `verify()` của Koin

Xem `di-koin.md` mục 4. Bắt buộc pass trước khi merge PR thêm binding.

## 6. Test kiến trúc (Konsist)

```kotlin
class ArchitectureTest {

    @Test
    fun `feature và app không import domain repository`() {
        Konsist.scopeFromProduction()
            .files
            .filter { it.path.contains("/feature/") || it.path.contains("/app/src/main/") }
            .assertFalse { file -> file.hasImport { it.name.startsWith("{{PACKAGE}}.core.domain.repository") } }
    }

    @Test
    fun `feature không import feature khác`() { /* so package của file với package được import */ }

    @Test
    fun `ViewModel kế thừa BaseMviViewModel`() {
        Konsist.scopeFromProduction().classes()
            .withNameEndingWith("ViewModel")
            .assertTrue { it.hasParent { parent -> parent.name == "BaseMviViewModel" } }
    }

    @Test
    fun `UseCase có hậu tố UseCase và nằm trong domain usecase`() { /* ... */ }
}
```

Kiểm lại tên API Konsist theo đúng phiên bản. Nếu chưa dùng Konsist, bản tối thiểu là một bước CI:

```bash
! grep -rn "^import {{PACKAGE}}.core.domain.repository" feature app/src/main
```

## 7. Test migration Room (từ bản phát hành đầu)

- Mỗi `Migration(n, n+1)` có một test: tạo CSDL ở version `n` từ schema JSON đã export, chèn dữ liệu mẫu, chạy migration, kiểm dữ liệu và schema ở version `n+1` (`MigrationTestHelper`).
- Thư mục `schemas/` phải được đưa vào `androidTest` assets (cấu hình trong convention plugin của database).
