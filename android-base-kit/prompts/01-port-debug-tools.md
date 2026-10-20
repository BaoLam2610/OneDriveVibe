# Prompt 01: Dựng công cụ debug trong app

Dựng `:tools:debug` theo `docs/debug-tools.md`. Là phase 6 của prompt 00, hoặc chạy riêng cho dự án đã có sẵn khung. Prompt này ở lại trong Base Kit (dùng một lần), không chép vào repo dự án.

**Mã nguồn tham chiếu (nếu có quyền đọc repo OneDriveVibe):** `tools/debug/src/androidMain/kotlin/.../tools/debug/` (khoảng 2.300 dòng: `DebugHooks`, `DebugActions`, `DebugStores`, `DebugSettings`, `StorageReader`, `DatabaseBrowser`, `ApiCurl`, `JsonTree`, `ODVDebugScreen`, `ODVApiDetailScreen`, `ODVTableDetailScreen`, `ODVDebugBugButton`), `androidApp/src/debug/.../debug/DebugTools.kt`, `androidApp/src/release/.../debug/DebugTools.kt`, `core/network/.../traffic/HttpTraffic.kt`, `core/designsystem/.../component/TopOverlay.kt`. Khi chép: đổi tiền tố `ODV` thành `Pfx`, bỏ các hook riêng của OneDriveVibe (thumbnail, tải trước video, PIN), đổi tên CSDL/SharedPreferences theo dự án mới, và **đọc lại toàn bộ trước khi chép** (không chép mù).

````text
Dựng công cụ debug trong app cho {{TEN_DU_AN}} theo docs/debug-tools.md (đọc hết trước). Đọc thêm CLAUDE.md, docs/architecture.md, docs/conventions/security.md, docs/conventions/logging.md, docs/adr/0011-cong-cu-debug.md.

<Nếu có mã tham chiếu: "Mã tham chiếu ở <đường dẫn>. Đọc để học cách làm, viết lại theo package và tiền tố của dự án này; bỏ phần riêng của dự án cũ.">

## Ràng buộc
- :app khai báo debugImplementation(project(":tools:debug")). src/main KHÔNG import :tools:debug; chỉ gọi cổng DebugTools (bản src/debug và src/release cùng API).
- :tools:debug chỉ phụ thuộc :core:common, :core:network, :core:database, :core:designsystem. Hành động cần domain đi qua DebugHooks do :app/src/debug gắn.
- Bản release: không recorder, Logger.setLogWriters(emptyList()).
- Log API đầy đủ (mặc định không che, có công tắc che), chỉ trong RAM. Clipboard bí mật gắn EXTRA_IS_SENSITIVE.
- Không mở CSDL của Room bằng SQLite hệ thống nếu Room dùng driver bundled; đọc qua kết nối của Room.
- Chuỗi trong công cụ debug viết thẳng (không qua res/values).
- Không ghi đè file có sẵn bằng Write; không commit.

## Các bước (DỪNG sau mỗi bước, báo: file, rủi ro build, cách kiểm, tên commit)
D1. Cổng và khung: DebugTools (debug/release), DebugLogging (Kermit: Logcat + DebugLogWriter, Verbose), DebugSettings (SharedPreferences pfx_debug, ActivityLifecycleCallbacks → PfxSecureWindowPolicy.track), DebugHooks, DebugActions (register idempotent), DebugActivity (exported=false, không LAUNCHER, closeIfOpen), PfxDebugScreen với 4 tab rỗng, PfxDebugBugButton (kéo thả, 48dp, bản Popup cho Dialog/BottomSheet qua LocalPfxTopOverlay, vị trí dùng chung), nối vào MainApplication và MainActivity.
D2. Tab API: HttpTrafficRecorder đã có ở :core:network (nếu chưa có thì thêm theo docs/debug-tools.md mục 3.1), ApiTrafficStore (vòng 200), danh sách (method, status màu, thời gian, URL che theo công tắc), tìm kiếm có đếm (debounce 250 ms), màn chi tiết (header, body, cây JSON mở/thu, chọn và sao chép, tìm có tô sáng, Sao chép cURL), dựng nội dung ngoài luồng chính, cắt body dài.
D3. Tab Log: DebugLogStore (vòng 2000), lọc theo tag, mức, tìm kiếm; log không bị xóa khi khóa/đăng xuất.
D4. Tab Lưu trữ: SharedPreferences (che khóa nhạy cảm), DataStore qua DebugHooks.dumpPreferences (PreferencesInspector ở :core:data), danh sách CSDL (lọc -journal/-wal/-shm/.lck, dung lượng cộng -wal/-shm), tên tệp bí mật (không giải mã), cây thư mục dữ liệu của app kèm dung lượng, nút Xóa dữ liệu local (hộp thoại xác nhận). Trình duyệt bảng: SqlSource (Room cho CSDL của app, SQLite hệ thống cho CSDL bên thứ ba), ẩn room_master_table/sqlite_%/android_%, 50 dòng/trang, ORDER BY rowid (trừ WITHOUT ROWID), nhảy trang, tìm LIKE có ESCAPE trên mọi cột, cột nhạy cảm che và không được tìm, BLOB hiện <blob N byte>, sao chép dòng/trang, xóa bảng có cảnh báo.
D5. Tab Khác: thông tin bản build (versionName, versionCode, variant, commit hash, thời điểm build qua BuildConfig), công tắc Che dữ liệu nhạy cảm, công tắc Bảo vệ màn hình điều khiển cài đặt thật (DebugHooks), mục từ DebugActions (Design gallery), khung cho công tắc A/B (interface ở domain.hook, bản mặc định ở module thật, bản debug ghi đè qua DebugTools.koinModules).
D6. Kiểm: chạy checklist ở docs/debug-tools.md mục 7; xác nhận releaseRuntimeClasspath không có :tools:debug; cập nhật docs/plan/tien-do.md.
````
