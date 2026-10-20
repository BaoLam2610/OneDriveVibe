# Công cụ debug trong app (`:tools:debug`)

Thiết kế lấy từ một dự án trước, nơi gần như mọi lỗi khó đều được chẩn đoán bằng log chụp từ màn này. Quyết định: ADR-0011. Dựng ở bootstrap, **trước** feature đầu tiên.

## 1. Mục tiêu

- Người kiểm trên máy thật thấy ngay: request/response API, log của app, dữ liệu đã lưu (SharedPreferences, DataStore, bảng CSDL), thông tin bản build.
- Có công tắc để thử A/B và thao tác nhanh (xóa dữ liệu local, xóa cache, mở design gallery).
- **Không có gì lọt vào bản release**: không module, không recorder, không writer log.

Không làm: gửi log lên máy chủ, ghi log xuống đĩa, chụp màn hình tự động.

## 2. Kiến trúc

```
:app
├── src/main/      MainApplication, MainActivity: chỉ gọi DebugTools.xxx, KHÔNG import :tools:debug
├── src/debug/     DebugTools (bản thật): nối :tools:debug, gắn hook, đăng ký action
└── src/release/   DebugTools (bản rỗng): cùng API, không làm gì, gỡ hết writer log

:tools:debug       (debugImplementation, Android library + Compose)
├── DebugActivity + PfxDebugScreen     4 tab: API, Log, Lưu trữ, Khác
├── PfxDebugBugButton                  nút bọ nổi, kéo thả
├── DebugLogStore + DebugLogWriter     bộ đệm vòng log Kermit
├── ApiTrafficStore                    cài HttpTrafficRecorder của :core:network
├── ApiDetailScreen, JsonTree, ApiCurl màn chi tiết request
├── StorageReader, DatabaseBrowser     tab Lưu trữ, màn chi tiết bảng
├── DebugHooks                         ô hàm để app gắn hành động cần domain
├── DebugActions                       registry mục ở tab Khác
└── DebugSettings                      cài đặt của công cụ debug (SharedPreferences riêng)
```

`:tools:debug` chỉ phụ thuộc `:core:common`, `:core:network` (interface recorder), `:core:database` (đọc CSDL qua Room), `:core:designsystem`. **Không** phụ thuộc domain hay feature: hành động cần domain (đăng xuất, xóa cache) đi qua `DebugHooks` do `:app/src/debug` gắn.

### 2.1 Cổng `DebugTools`

`:app/build.gradle.kts`: `debugImplementation(project(":tools:debug"))`.

```kotlin
// app/src/debug/kotlin/{{PACKAGE}}/debug/DebugTools.kt
object DebugTools {
    /** Nạp SAU CÙNG khi startKoin để ghi đè được binding mặc định (Koin 4 cho phép ghi đè). */
    val koinModules: List<Module> = listOf(
        debugModule, // single<HttpTrafficRecorder> { ApiTrafficStore }
        module { single<FeatureFlagSwitch> { DebugFeatureFlagSwitch } }, // ví dụ công tắc A/B
    )

    /** Gọi ở Application.onCreate, TRƯỚC startKoin. */
    fun install(application: Application) {
        DebugLogging.install()                 // Logcat + DebugLogWriter, mức Verbose
        DebugSettings.install(application)     // nạp cài đặt debug, track Window mọi Activity
        // Koin chỉ có sau startKoin nên hook lấy dependency LÚC BẤM, không lấy ở đây.
        DebugHooks.clearLocalData = {
            KoinPlatform.getKoin().get<LogoutUseCase>().invoke()
            application.startActivity(
                Intent(application, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
        DebugHooks.dumpPreferences = { KoinPlatform.getKoin().get<PreferencesInspector>().dump() }
        DebugActions.register(
            DebugAction("Design gallery", "Token, icon, component để so với thiết kế") { ctx ->
                ctx.startActivity(Intent(ctx, DesignGalleryActivity::class.java))
            },
        )
    }

    @Composable fun Overlay() = PfxDebugBugButton(asWindow = false)
    val topOverlay: (@Composable (asWindow: Boolean) -> Unit)? = { PfxDebugBugButton(asWindow = it) }
    fun onLoggedOut() = DebugActivity.closeIfOpen()
}
```

```kotlin
// app/src/release/kotlin/{{PACKAGE}}/debug/DebugTools.kt
object DebugTools {
    val koinModules: List<Module> = emptyList()
    fun install(application: Application) = Logger.setLogWriters(emptyList()) // release không ghi log
    @Composable fun Overlay() = Unit
    val topOverlay: (@Composable (asWindow: Boolean) -> Unit)? = null
    fun onLoggedOut() = Unit
}
```

Nối ở `src/main`:

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugTools.install(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(appModule + coreModules + featureModules + DebugTools.koinModules) // debug cuối cùng
        }
    }
}

// MainActivity.setContent { ... }
CompositionLocalProvider(LocalPfxTopOverlay provides DebugTools.topOverlay) {
    PfxTheme {
        Box(Modifier.fillMaxSize()) {
            AppNavDisplay(...)
            DebugTools.Overlay()  // vẽ cuối để nằm trên cùng
        }
    }
}
```

### 2.2 Điểm móc

```kotlin
/** App gắn ở DebugTools.install. Để null thì tính năng tương ứng ẩn khỏi màn Debug. */
object DebugHooks {
    @Volatile var clearLocalData: (suspend () -> Unit)? = null
    @Volatile var clearImageCache: (suspend () -> Unit)? = null
    @Volatile var dumpPreferences: (suspend () -> Map<String, Map<String, String>>)? = null
    @Volatile var screenProtection: (() -> Flow<Boolean>)? = null
    @Volatile var setScreenProtection: (suspend (Boolean) -> Unit)? = null
}

class DebugAction(val title: String, val description: String, val onClick: (Context) -> Unit)

object DebugActions {
    private val registered = mutableListOf<DebugAction>()
    val items: List<DebugAction> get() = registered.toList()
    fun register(action: DebugAction) {
        // Idempotent theo tiêu đề: LazyColumn dùng tiêu đề làm key, đăng ký trùng sẽ crash.
        registered.removeAll { it.title == action.title }
        registered += action
    }
}
```

Công tắc A/B: khai báo interface ở `domain.hook` (ví dụ `FeatureFlagSwitch`), bản mặc định ở module thật (luôn bật), bản debug đọc `DebugSettings` và ghi đè qua `DebugTools.koinModules`. Bản release không có module này nên luôn dùng mặc định.

## 3. Các tab

### 3.1 Log API

- `:core:network` khai báo:

  ```kotlin
  class HttpTrafficEntry(
      val method: String, val url: String, val status: Int?, val durationMs: Long,
      val requestHeaders: List<Pair<String, String>>, val requestBody: String?,
      val responseHeaders: List<Pair<String, String>>, val responseBody: String?,
      val error: String?, // tên loại lỗi, không phải message
  )
  interface HttpTrafficRecorder { fun record(entry: HttpTrafficEntry) }
  fun HttpTrafficEntry.masked(): HttpTrafficEntry // che Authorization, Cookie, token, secret trong URL/body
  ```

- Recorder được tiêm dạng **nullable** (`getOrNull<HttpTrafficRecorder>()`): bản release không có binding nên không tốn công dựng bản ghi.
- Ghi ở **một** chỗ cho mọi request. Cách đã chạy ổn: ghi trong lớp cơ sở `ApiService` (bọc mọi lời gọi). Cách gọn hơn là chặn ở `HttpSend` của Ktor (`client.plugin(HttpSend).intercept { ... }`); đọc body response ở đó cần cẩn thận vì body chỉ đọc được một lần, nên kiểm API đúng phiên bản Ktor trước khi chọn.
- Request lấy token cũng phải ghi (kể cả body có secret) vì đó thường là chỗ cần gỡ lỗi nhất.
- Cắt body quá dài (`MAX_BODY_CHARS`, khoảng 200 nghìn ký tự) và ghi "… cắt, còn N ký tự".
- `ApiTrafficStore`: bộ đệm vòng **200** request, chỉ trong RAM, là `StateFlow<List<DebugRequest>>`.
- Danh sách: method, status (màu theo nhóm), thời gian, URL (che nếu bật công tắc che). Tìm kiếm có đếm số khớp, debounce 250 ms.
- Màn chi tiết: header, body request, body response dạng cây JSON (mở/thu từng nhánh, "mở hết/thu hết"), chọn và sao chép chữ, tìm kiếm có tô sáng, **Sao chép cURL**. Dựng section, đếm khớp, pretty-print **ngoài luồng chính**.
- Clipboard chứa dữ liệu nhạy cảm thì gắn `ClipDescription.EXTRA_IS_SENSITIVE` (Android 13+).

### 3.2 Log local

- `DebugLogWriter : LogWriter` của Kermit đẩy vào `DebugLogStore`, bộ đệm vòng **2000** dòng (500 là quá ít khi log giữ qua lần khóa app).
- Mỗi dòng: thời gian, mức, tag, message, stacktrace cắt 4000 ký tự. Lọc theo tag, tìm kiếm.
- Log **không bị xóa** khi khóa app hay đăng xuất (để còn xem lại); chỉ màn Debug bị đóng.

### 3.3 Lưu trữ

- **SharedPreferences:** đọc mọi tệp trong `shared_prefs/`. Khóa khớp `secret|token|pin|password|credential|key` thì hiện `***`.
- **DataStore:** tệp protobuf không đọc thẳng được, nên app cài `PreferencesInspector` (interface ở `domain.hook`, impl ở `:core:data` đọc qua **đúng instance** DataStore app đang dùng) và gắn vào `DebugHooks.dumpPreferences`.
- **CSDL:** liệt kê `context.databaseList()`, lọc bỏ `-journal`, `-wal`, `-shm`, `.lck`. Dung lượng = tệp chính + `-wal` + `-shm`.
- **Kho bí mật:** chỉ liệt kê **tên** tệp, không bao giờ giải mã.
- **Liệt kê toàn bộ thư mục dữ liệu** của app (`filesDir`, `cacheDir`, `databases`, `shared_prefs`, `datastore`) kèm dung lượng: dùng để kiểm "đăng xuất xong còn sót gì".
- Nút **Xóa dữ liệu local** (có hộp thoại xác nhận): gọi `DebugHooks.clearLocalData` (đăng xuất thật + khởi động lại task). Cài đặt của công cụ debug được giữ.

#### Trình duyệt CSDL

```kotlin
internal interface SqlSource {
    suspend fun query(sql: String, args: List<String> = emptyList()): RawRows
    suspend fun execute(sql: String)
}
// CSDL của app: đi qua kết nối của chính Room (useReaderConnection / useWriterConnection,
// hoặc openHelper.readableDatabase tùy bản Room). Ghi qua Room thì InvalidationTracker
// tự báo Flow của UI.
// CSDL của bên thứ ba (vd. cache Media3): SQLiteDatabase.openDatabase(path, OPEN_READONLY).
```

- **Không bao giờ** mở CSDL của Room bằng `SQLiteDatabase` của hệ thống nếu Room dùng driver bundled (hỏng CSDL, xem tech-stack mục 3).
- Ẩn `room_master_table` (xóa nó thì Room báo hỏng), `sqlite_%`, `android_%`.
- Màn chi tiết bảng: 50 dòng một trang, `ORDER BY rowid` (bỏ nếu bảng `WITHOUT ROWID`), nhảy trang, tìm `LIKE ? ESCAPE '\'` trên mọi cột (escape `%`, `_`, `\`), tên bảng/cột đặt trong nháy kép. Cột nhạy cảm (tên nghi bí mật hoặc chứa `url`) bị che khi bật công tắc **và không được tìm** (tìm kiếm là cách dò giá trị bị che). BLOB hiện `<blob N byte>`.
- Nút xóa từng bảng, hộp thoại cảnh báo hệ quả (ví dụ xóa bảng dữ liệu mà giữ bảng trạng thái đồng bộ thì app tưởng đã đồng bộ nhưng danh sách trống).

### 3.4 Khác

- **Thông tin bản build:** `versionName`, `versionCode`, variant, **commit hash** và thời điểm build (đưa vào `BuildConfig` qua Gradle). Người kiểm báo lỗi kèm hai thông tin này.
- Công tắc **Che dữ liệu nhạy cảm** (mặc định tắt: hiện đầy đủ).
- Công tắc **Bảo vệ màn hình** điều khiển **chính cài đặt thật** của app (qua `DebugHooks.screenProtection`), không có trạng thái riêng của Debug.
- Các công tắc A/B, thao tác nhanh (xóa cache ảnh...), mục từ `DebugActions` (design gallery...).

## 4. Nút bọ nổi

- Vẽ ở gốc `MainActivity`, sau `NavDisplay` để nằm trên cùng; kéo thả; chạm mở `DebugActivity`. Vùng chạm 48dp.
- Dialog và BottomSheet là **cửa sổ riêng** nên che nút. Component `PfxDialog`/`PfxBottomSheet` của designsystem đọc `LocalPfxTopOverlay` và gọi nó với `asWindow = true`; bản này mở một `Popup` không lấy focus (chạm ngoài nút vẫn xuyên xuống dialog). Vị trí nút là state chung (`mutableStateOf`) cho cả hai bản.
- Nút hiện cả ở màn khóa (cần gỡ lỗi luồng khóa). Đánh đổi: ai cầm máy bản debug cũng mở được log. Chỉ dùng tài khoản và secret dành cho phát triển trên bản debug.

## 5. `DebugActivity`

- `exported="false"`, khai báo trong manifest của `:tools:debug`; không có intent-filter LAUNCHER (app chỉ có một icon).
- Không tự đặt `FLAG_SECURE` nhưng **theo chính sách bảo vệ màn hình cấp tiến trình** của app (`PfxSecureWindowPolicy.track(window)` qua `ActivityLifecycleCallbacks` ở `DebugSettings.install`), vì log ở đây có secret.
- Đóng khi đăng xuất hoặc khóa app (`closeIfOpen()`); giữ tham chiếu Activity hiện tại trong `companion` và xóa ở `onDestroy`.
- Chuỗi trong công cụ debug viết thẳng trong code (không qua `res/values`) vì chỉ dùng khi phát triển.

## 6. Cài đặt của công cụ debug

SharedPreferences riêng `pfx_debug` (hiện ở tab Lưu trữ), sống qua lần mở app sau. Khóa không còn dùng thì xóa khi `install` để không giữ rác. Giá trị hợp lệ được kiểm lúc đọc (giá trị lạ thì về mặc định).

## 7. Kiểm tay

- Nút bọ hiện trên mọi màn, kể cả trên Dialog/BottomSheet và màn khóa; kéo được.
- Tab API thấy request kèm token request; bật che thì `Authorization`, token, secret thành `***`; "Sao chép cURL" chạy lại được trên máy tính.
- Tab Log thấy log của luồng vừa làm; khóa app rồi mở lại, log còn nguyên.
- Tab Lưu trữ: prefs, DataStore, bảng CSDL; mở bảng lớn, sang trang cuối, tìm, xóa bảng khi đang đồng bộ rồi mở lại app (CSDL không hỏng).
- "Xóa dữ liệu local": về màn đầu; tab Lưu trữ không còn tệp dữ liệu nào ngoài `pfx_debug`.
- **Bản release:** `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep tools` không ra gì; APK release không có nút bọ, Logcat không có log của app.
