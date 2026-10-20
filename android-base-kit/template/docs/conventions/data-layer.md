# Tầng dữ liệu

## 1. Repository

- Interface ở `domain.repository`, impl ở `:core:data` (`XxxRepositoryImpl`). Chỉ UseCase gọi repository.
- **Chia theo nhóm việc nghiệp vụ**, không theo nguồn dữ liệu. Quá khoảng 8 hàm, hoặc phục vụ nhiều màn không liên quan, thì tách.
- Trả `AppResult<T>` cho thao tác một lần; `Flow<T>` cho dữ liệu quan sát. Không trả DTO hay entity ra ngoài `:core:data`.
- Mapper là extension function (`OrderDto.toDomain()`, `OrderEntity.toDomain()`, `Order.toEntity()`) đặt trong `:core:data`. `network` và `database` không biết model domain.
- Không truyền credentials theo từng lời gọi: repository nhận API service đã có nguồn token (mục 2.2).

## 2. Mạng (Ktor)

### 2.1 Client

```kotlin
fun createApiClient(
    engine: HttpClientEngine,
    json: Json,
    tokens: BearerTokenSource,
): HttpClient = HttpClient(engine) {
    expectSuccess = false                 // tự đổi status sang AppError, không để Ktor ném
    followRedirects = false               // bearer không đi theo redirect sang máy chủ khác
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        connectTimeoutMillis = HttpConstants.CONNECT_TIMEOUT_MS
        requestTimeoutMillis = HttpConstants.REQUEST_TIMEOUT_MS
    }
    install(HttpRequestRetry) {
        maxRetries = HttpConstants.MAX_RETRIES
        retryOnExceptionIf { _, cause -> cause.isTransientNetworkError() }
        retryIf { _, response -> response.status.value == 429 || response.status.value >= 500 }
        delayMillis { retry -> /* Retry-After nếu có (có trần), không thì backoff */ }
    }
    install(Auth) {
        bearer {
            loadTokens { tokens.current() }
            refreshTokens { tokens.refresh(oldTokens) }   // single-flight bên trong tokens
        }
    }
    defaultRequest { url(NetworkConstants.BASE_URL) }
    // KHÔNG install(Logging): in Authorization và body chứa token.
}
```

- `Json { ignoreUnknownKeys = true; explicitNulls = false }` dùng chung, một instance.
- **Không cài plugin `Logging`.** Lưu lượng cho màn Debug đi qua `HttpTrafficRecorder` (chỉ bản debug, xem `debug-tools.md`).
- Tải URL đã ký (ảnh, tệp) bằng **client riêng không có bearer**.
- Retry: lỗi mạng, timeout, `429`, `5xx`, tối đa 2 lần, tôn trọng `Retry-After` có trần. **Không** retry `400`, `403`, `404`. `401` do plugin `Auth` xử lý: làm mới token và thử lại **một** lần.
- Plugin `Auth` của Ktor không đủ (luồng token đặc biệt) thì tự viết `BearerTokenSource` theo `concurrency-time.md` mục 4, và giữ cùng luật.

### 2.2 API service

```kotlin
class OrderApi(private val client: HttpClient) {
    suspend fun getOrder(id: String): AppResult<OrderDto> = client.safeGet("orders/$id")
}

/** Một chỗ duy nhất đổi exception và status sang AppError. */
internal suspend inline fun <reified T> HttpClient.safeGet(path: String, block: HttpRequestBuilder.() -> Unit = {}): AppResult<T> =
    catching(mapError = ::toNetworkError) {
        val response = get(path, block)
        if (!response.status.isSuccess()) return AppResult.Failure(response.toHttpError())
        response.body<T>()
    }
```

- DTO là `@Serializable data class` với tên trường khớp API (`@SerialName` khi khác quy ước Kotlin). Trường có thể vắng thì nullable hoặc có mặc định.
- `$select`/`fields` để chỉ lấy trường đang dùng (nhẹ hơn, nhanh hơn).
- Phân trang theo `nextLink`/cursor của API; Paging chỉ dùng cho danh sách đọc từ CSDL hoặc trực tiếp từ API khi không cache.

## 3. Room

### 3.1 Version và migration (ADR-0008)

- **Mọi thay đổi schema** (thêm/bỏ/đổi cột, bảng, index) **tăng `version`** ở `@Database` và ghi một dòng vào "Lịch sử version" (KDoc của lớp database):

  ```kotlin
  /**
   * Lịch sử version:
   * 1: order, order_item
   * 2: thêm cột order.note (PR #123)
   */
  @Database(entities = [...], version = 2, exportSchema = true)
  abstract class AppDatabase : RoomDatabase()
  ```

  Quên tăng thì Room báo lỗi hash và crash.
- `exportSchema = true`, `room { schemaDirectory("$projectDir/schemas") }`, commit thư mục `schemas/`.
- **Trước bản phát hành đầu:** `fallbackToDestructiveMigration(dropAllTables = true)` được phép (CSDL cũ bị xóa và tải lại). **Từ bản phát hành đầu:** bắt buộc viết `Migration` hoặc `@AutoMigration`, kèm test bằng `MigrationTestHelper`; gỡ fallback destructive (hoặc giới hạn bằng `fallbackToDestructiveMigrationFrom`).
- Tên bảng, tên cột đã phát hành coi như giá trị đã lưu: đổi tên phải qua migration.

### 3.2 DAO

- Thao tác nhiều bước (ghi một trang dữ liệu đồng bộ cùng con trỏ trang) trong **một** `@Transaction`, để tắt app giữa chừng không để dữ liệu nửa vời.
- Luồng đọc trả `Flow<...>`; ở repository thêm `distinctUntilChanged()` (và `conflate()` nếu nguồn phát dồn dập).
- Sắp xếp và lọc bằng SQL có index, không nạp cả bảng rồi sắp ở Kotlin. Danh sách lớn trả `PagingSource`.
- Tìm kiếm không phân biệt dấu: lưu thêm cột khóa đã bỏ dấu, chữ thường (`nameKey`) và tìm trên cột đó.
- Hai truy vấn phải giữ cùng điều kiện và thứ tự (một cho Paging, một cho danh sách đầy đủ) thì dùng chung một `const val` SQL.
- Chỉ đọc CSDL qua Room. Công cụ debug đọc qua kết nối của Room (xem `debug-tools.md`).

## 4. DataStore

```kotlin
/** Bọc DataStore: đọc an toàn (IOException → giá trị mặc định), ghi không làm crash, enum lưu theo tên. */
class PreferencesDataSource(private val store: DataStore<Preferences>) {
    fun <T> observe(key: Preferences.Key<T>, default: T): Flow<T> =
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[key] ?: default }
            .distinctUntilChanged()

    fun <E : Enum<E>> observeEnum(key: Preferences.Key<String>, default: E, entries: EnumEntries<E>): Flow<E> =
        observe(key, default.name).map { name -> entries.firstOrNull { it.name == name } ?: default }

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        try { store.edit(block) } catch (e: IOException) { log.w { "Ghi DataStore lỗi: ${e::class.simpleName}" } }
    }

    suspend fun clear() = edit { it.clear() }
}
```

- Mỗi nhóm cài đặt một tệp DataStore; **tên tệp và tên khóa** ở file Constants, ghi "không được đổi giá trị".
- Enum lưu bằng **tên** (`name`), không bằng `ordinal`; giá trị lạ thì về mặc định.
- Đọc-sửa-ghi phải nguyên tử: một lần `edit { }` đọc và ghi, không `first()` rồi `edit` riêng.
- Cài đặt ảnh hưởng khung hình đầu tiên (theme) có thêm bản cache đọc đồng bộ (xem `compose-ui.md` mục 3).
- Dữ liệu nhạy cảm (token làm mới, thông tin đăng nhập) **không** vào DataStore thường: dùng Keystore/Tink (xem `security.md`).

## 5. Xóa dữ liệu (đăng xuất, đổi tài khoản)

```kotlin
// domain.hook
fun interface DataResetter { suspend fun reset() }

// UseCase
class LogoutUseCase(private val resetters: List<DataResetter>, /* ... */) {
    suspend operator fun invoke() = withContext(NonCancellable) {
        resetters.forEach { resetter ->
            try { resetter.reset() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                log.w { "Không xóa được ${resetter::class.simpleName}: ${e::class.simpleName}" }
            }
        }
    }
}
// Koin: factory { LogoutUseCase(getAll<DataResetter>()) }
```

- **Mỗi kho dữ liệu mới** (bảng Room, tệp DataStore, thư mục cache, CSDL của thư viện bên thứ ba, token trong RAM) **đăng ký một `DataResetter` trong cùng PR tạo kho đó.**
- Xóa Room: `clearAllTables()` rồi `VACUUM` và `PRAGMA wal_checkpoint(TRUNCATE)` (nếu không, dữ liệu cũ còn trong tệp và trong `-wal`).
- Thư viện có CSDL hoặc cache riêng (Media3, Coil, WebView) phải được đóng rồi xóa tệp, không chỉ gỡ từng mục.
- Kho có `reset()` mà còn việc đang chạy thì chặn ghi muộn bằng `epoch` (`concurrency-time.md` mục 4).
- Kiểm bằng tab Lưu trữ của màn Debug: sau đăng xuất không còn tệp dữ liệu nào ngoài cài đặt của công cụ debug.

## 6. Offline-first (tùy dự án, quyết bằng ADR)

Nếu dự án cần dùng được khi mất mạng:

- UI **chỉ** đọc CSDL; đồng bộ là luồng riêng do coordinator ở app scope sở hữu (`concurrency-time.md` mục 3).
- Đồng bộ ghi từng trang kèm con trỏ trang trong cùng transaction, để chạy tiếp được từ trang dở.
- Dọn mục không còn trên máy chủ bằng "mã lượt quét" (`scanId`): trang cuối xóa mục có `scanId` cũ trong cùng transaction.
- Lỗi tạm thời: chờ `Retry-After` hoặc backoff có trần, chạy tiếp từ trang dở, dừng sau N lần không tiến triển.
- Có trạng thái "lần đồng bộ đầu chưa xong" để UI hiện banner và, nếu cần, đọc thẳng API cho tới khi xong.

## 7. Tệp và cache

- Thư mục cache tách theo loại (`cacheDir/images`, `cacheDir/files`), mỗi loại một trần dung lượng và LRU.
- Tải tệp lớn: ghi `.part`, tải tiếp bằng `Range`, chỉ đổi tên khi đủ byte; khóa cache theo **id + phiên bản nội dung** (ETag/cTag), không theo URL ký (URL đổi mỗi giờ).
- Đo dung lượng CSDL thì cộng `-wal` và `-shm`.
