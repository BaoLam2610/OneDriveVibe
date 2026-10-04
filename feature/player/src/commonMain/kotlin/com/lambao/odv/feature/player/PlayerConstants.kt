package com.lambao.odv.feature.player

import co.touchlab.kermit.Logger

/**
 * Mọi hằng số của `:feature:player` nằm ở đây để dễ tìm và dễ chỉnh. [TAG] là thẻ log duy nhất của module: lọc "Player" ở tab Log
 * của màn Debug (ADR-0012) để xem trọn đường đi của một lần phát. Mỗi dòng log mở đầu bằng khâu phát ra nó, ví dụ `[Url]`,
 * `[Source]`, `[Cache]`, `[Player]`, `[UI]`.
 *
 * Quy tắc log (CH-06): không bao giờ ghi link phát (chứa `tempauth`), không ghi `Authorization`, không ghi message của ngoại lệ
 * mạng (có thể chứa URL). Chỉ ghi mã, tên lớp ngoại lệ, kích thước, thời gian và id rút gọn ([shortId]).
 */
internal object PlayerConstants {

    /** Thẻ log duy nhất của module. */
    const val TAG = "Player"

    // --- Nguồn dữ liệu (StreamDataSource) ---

    /** Địa chỉ ảo ExoPlayer thấy: `odv-video://item/{itemId}`. Link ký thật chỉ xuất hiện lúc mở kết nối. */
    const val STREAM_SCHEME = "odv-video"
    const val STREAM_AUTHORITY = "item"

    /**
     * Link ký của OneDrive sống khoảng 1 giờ (VD-14). Làm mới sớm hơn một chút (45 phút) để không phải chờ một lần thất bại
     * mới biết link hết hạn giữa lúc đang xem.
     */
    const val URL_TTL_MS = 45L * 60 * 1000

    /** Mã HTTP link hết hạn thường trả (onedrive-graph-api.md mục 9.2): lấy link mới rồi mở lại một lần (VD-14). */
    val EXPIRED_STATUSES: Set<Int> = setOf(401, 403)

    /** Mã HTTP cho tệp không còn. 401/403 không vào đây vì đã thử làm mới link. */
    val REMOVED_STATUSES: Set<Int> = setOf(404, 410)

    /** Mạng chậm hoặc CDN ì ạch thì 8 giây mặc định hay làm đứt kết nối oan; nới lên 15 giây. */
    const val HTTP_TIMEOUT_MS = 15_000

    // --- Cache và buffer ---

    /** Thư mục cache video trong `cacheDir` (không sao lưu, CH-04). */
    const val CACHE_DIR_NAME = "video"

    /** Trần dung lượng cache video (BN-01), tạm cố định; Lát 9 nối với giới hạn cache trong Cài đặt (CD). */
    const val CACHE_MAX_BYTES = 2L * 1024 * 1024 * 1024

    /** Giữ lại chừng này đã phát phía sau để tua lùi ngắn không phải tải lại. */
    const val BACK_BUFFER_MS = 30_000

    // --- Phân loại lỗi ---

    /** Số tầng nguyên nhân duyệt khi phân loại và ghi log một lỗi. */
    const val CAUSE_DEPTH = 8

    // --- Giao diện và cử chỉ ---

    /** Bước tua mặc định (VD-03). Lát 9 nối với Cài đặt. */
    const val SEEK_STEP_SECONDS = 10
    const val SEEK_STEP_MS = SEEK_STEP_SECONDS * 1000L

    /** Tốc độ khi giữ lâu để phát nhanh; nhả tay thì về tốc độ đã chọn. */
    const val BOOST_SPEED = 2f
    const val NORMAL_SPEED = 1f

    /** Các tốc độ phát (VD-05). */
    val SPEEDS: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    /** Khung video ở hướng dọc: rộng toàn màn, tỉ lệ 16:9, căn giữa theo chiều dọc (thiet-ke-ui.md mục 4.5). */
    const val FRAME_ASPECT = 16f / 9f

    /**
     * Vùng giữa (chạm đúp để tạm dừng/phát) rộng chừng này so với bề ngang khung cử chỉ, căn giữa; hai bên còn lại là vùng tua lùi
     * và tua tới. Tính theo bề ngang thực của khung nên đúng cả hướng dọc lẫn hướng ngang (VD-19).
     */
    const val CENTER_ZONE_FRACTION = 0.3f

    /**
     * Cửa sổ chạm đúp: lần chạm thứ hai trong chừng này sau lần đầu (cùng vùng) là chạm đúp. Ngắn hơn ngưỡng hệ thống (~300 ms) để
     * điều khiển hiện sớm hơn. Khi điều khiển đang ẩn, lần chạm đầu **chờ hết cửa sổ này mới hiện điều khiển**, nhờ vậy chạm đúp không
     * làm điều khiển nháy lên rồi mới tua.
     */
    const val DOUBLE_TAP_WINDOW_MS = 200L

    /** Thời gian mờ hiện/ẩn của thanh điều khiển. */
    const val CONTROLS_FADE_MS = 120

    /** Sóng lan trong vùng ripple chạm đúp: thời gian, số vòng và độ trễ giữa các vòng (tỉ lệ thời gian). */
    const val WAVE_DURATION_MS = 650
    const val WAVE_RING_COUNT = 2
    const val WAVE_RING_DELAY = 0.3f

    /** Biểu tượng Phát/Tạm dừng hiện giữa màn sau khi chạm đúp ở vùng giữa, mờ dần trong chừng này. */
    const val CENTER_FEEDBACK_MS = 600

    /** Nhịp cập nhật vị trí: dày khi đang phát, thưa khi dừng. */
    const val PROGRESS_TICK_MS = 250L
    const val IDLE_TICK_MS = 1000L

    /** Gợn chạm đúp và bộ cộng dồn "+20 giây" sống thêm chừng này sau lần chạm cuối. */
    const val SEEK_FEEDBACK_MS = 700L

    // --- Mở màn và lấy link ---

    /**
     * Chờ video được chạm xuất hiện trong danh sách phát (Room) chừng này khi mở màn. Lúc quét lần đầu chưa xong (TM-07) Room có thể
     * chưa có thư mục hoặc mới có một phần; hết thời gian mà vẫn không có thì đóng màn thay vì phát nhầm video khác.
     */
    const val START_WAIT_MS = 15_000L

    /** Lấy link phát tối đa chừng này: Graph bị throttle thì `HttpRequestRetry` có thể chờ tới 60 giây và giữ luồng tải của ExoPlayer. */
    const val URL_FETCH_TIMEOUT_MS = 15_000L

    // --- Lát 6b ---

    /** Đếm ngược trước khi tự chuyển video (VD-13, `duration.autoplay-countdown`). */
    const val AUTOPLAY_COUNTDOWN_MS = 5000

    /** Viên thuốc nhãn đổi khung hình / chế độ phát hiện chừng này (VD-06, VD-20, `duration.toast-label`). */
    const val LABEL_MS = 2000L

    /** HUD độ sáng/âm lượng ở lại chừng này sau lần vuốt cuối. */
    const val HUD_HOLD_MS = 800L

    /**
     * Độ nhạy vuốt độ sáng/âm lượng (VD-04): vuốt hết chiều cao khung đổi chừng này lần toàn dải, nên 2,2 nghĩa là vuốt chừng 45%
     * chiều cao là đi hết dải. (Bản đầu là 1,2 và người dùng thấy chậm.)
     */
    const val SWIPE_FULL_RANGE_RATIO = 2.2f

    /** Vuốt dọc bắt đầu sát mép hoặc sát đáy thì bỏ qua: mép là cử chỉ Back của hệ thống, đáy là thanh tua và cử chỉ về Home. */
    const val SWIPE_EDGE_DEAD_ZONE_DP = 24
    const val SWIPE_BOTTOM_DEAD_ZONE_DP = 56

    /** Vuốt từ mép trên kéo thanh trạng thái/điều hướng của hệ thống xuống (khi đang ẩn) thì không được coi là vuốt độ sáng/âm lượng. */
    const val SWIPE_TOP_DEAD_ZONE_DP = 56

    /** Zoom tối đa (VD-18), và ngưỡng coi là đang zoom (sai số làm tròn của cử chỉ). */
    const val MAX_ZOOM = 4f
    const val ZOOMED_THRESHOLD = 1.02f

    /** Khi khóa thao tác (VD-08), gợi ý "Giữ để mở khóa" hiện chừng này sau mỗi lần chạm rồi mờ đi. */
    const val LOCK_HINT_MS = 3000L

    // --- Log ---

    /** Id Graph dài và khó đọc; log chỉ ghi chừng này ký tự đầu. */
    const val LOG_ID_LENGTH = 8
}

/** Logger duy nhất của module, gắn [PlayerConstants.TAG]. */
internal val playerLog: Logger = Logger.withTag(PlayerConstants.TAG)

/** Id rút gọn để ghi log. */
internal fun String.shortId(): String = take(PlayerConstants.LOG_ID_LENGTH)
