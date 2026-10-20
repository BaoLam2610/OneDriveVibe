# DI với Koin

Quyết định: ADR-0004. Koin 4 (dùng BOM), `koin-android`, `koin-androidx-compose`; test bằng `koin-test`.

## 1. Tổ chức module

| Module Gradle | Module Koin | Chứa |
|---|---|---|
| `:core:network` | `networkModule` | `HttpClient`, API service, token |
| `:core:database` | `databaseModule` | Database, DAO |
| `:core:data` | `dataModule`, `useCaseModule`, và `val coreModules = listOf(networkModule, databaseModule, dataModule, useCaseModule)` | Repository impl, DataStore, UseCase |
| `:feature:<ten>` | `<ten>Module` | ViewModel, thứ riêng của feature |
| `:app` | `appModule` | Impl của interface `platform` cần Activity/Application, app shell |
| `:tools:debug` | `debugModule` (qua `DebugTools.koinModules`) | Recorder, ghi đè binding cho bản debug |

- **UseCase đăng ký ở `useCaseModule` trong `:core:data`**, không ở `:core:domain`, để domain không phụ thuộc Koin.
- `:app` chỉ thấy `coreModules`, không cần phụ thuộc trực tiếp `:core:network` hay `:core:database`.
- `startKoin` ở `MainApplication`: `modules(appModule + coreModules + featureModules + DebugTools.koinModules)`. **Module debug luôn đứng cuối** để ghi đè được.
- Logger Koin mức `ERROR` (mức thấp hơn in tên lớp và tham số).

## 2. Khai báo

```kotlin
val dataModule = module {
    singleOf(::CartRepositoryImpl) bind CartRepository::class
    singleOf(::DataStoreThemeSettings) bind ThemeSettings::class
    single<Clock> { Clock.System }
    single<DispatcherProvider> { DefaultDispatcherProvider }
}

val useCaseModule = module {
    factoryOf(::ObserveCartUseCase)
    factoryOf(::RemoveFromCartUseCase)
}

val cartModule = module {
    viewModelOf(::CartViewModel)
}
```

- Ưu tiên **`singleOf(::X) bind Y::class`** / `factoryOf` / `viewModelOf` khi mọi tham số constructor đều inject được. Koin phân giải theo **kiểu**, không theo vị trí.
- Dùng lambda (`single { X(get(), get(named("download"))) }`) khi cần qualifier, `getAll()`, giá trị không inject được (đặt tham số có tên), hoặc `getOrNull()`.
- Hai binding cùng kiểu thì **bắt buộc** `named(...)`. Tên qualifier là hằng số trong file Constants.
- Repository, client mạng, database, coordinator: `single`. UseCase: `factory`. ViewModel: `viewModel`.
- Binding mà thứ khác đọc **ngay lúc dựng** (cấu hình mà `ImageLoader` hay `HttpClient` cần) thì `single(createdAtStart = true)` và ghi comment lý do. Tạo lười thì thứ dựng trước nó nhận giá trị mặc định mãi.
- Phụ thuộc runtime giữa module (network cần một interface do data cài, ví dụ `TokenSource`) chỉ đi qua Koin, không qua import. Ghi comment ở chỗ khai báo: "cài bởi `:core:data` (`XxxImpl`)".

## 3. Lấy dependency

- Composable: `koinViewModel()` cho ViewModel; tránh `koinInject()` trong composable màn hình (đưa qua ViewModel). Component designsystem không lấy từ Koin.
- ViewModel theo màn của Navigation 3: dùng decorator `rememberViewModelStoreNavEntryDecorator` (xem `navigation.md`) để mỗi entry có ViewModel riêng.
- Lớp không phải Android: nhận qua constructor. **Không** gọi `KoinPlatform.getKoin().get()` hay `inject()` trong code nghiệp vụ. Ngoại lệ có chủ ý: hook của công cụ debug (lấy lúc bấm vì Koin chỉ có sau `startKoin`).

## 4. Kiểm binding: `verify()`

Thiếu binding chỉ lộ **khi chạy**, có khi chỉ khi tiến trình được khôi phục sau process death. Vì vậy có test:

```kotlin
// app/src/test/kotlin/{{PACKAGE}}/KoinVerifyTest.kt
class KoinVerifyTest {
    @Test
    fun allModulesResolve() {
        module { includes(appModule, *coreModules.toTypedArray(), *featureModules.toTypedArray()) }
            .verify(
                extraTypes = listOf(
                    Context::class, Application::class, SavedStateHandle::class,
                    // Kiểu do Android/Koin cung cấp lúc chạy, verify() không tự biết.
                ),
            )
    }
}
```

- Thêm module hoặc binding mới thì test này phải pass (CI chạy).
- `verify()` kiểm constructor theo kiểu; binding dạng lambda có `get()` bên trong thì vẫn nên khởi động thật một lần ở bản debug (mở các màn chính).
- Kiểm lại tên API `verify` theo đúng bản `koin-test` khi khởi tạo.

## 5. Ghi đè ở bản debug

- Interface ở `domain.hook` (ví dụ `FeatureFlagSwitch`) có bản mặc định trong module thật.
- `DebugTools.koinModules` (src/debug) khai báo lại cùng kiểu với bản đọc `DebugSettings`. Đứng sau trong danh sách nên thắng.
- Bản release không có module này nên luôn dùng mặc định. Xem `debug-tools.md`.
