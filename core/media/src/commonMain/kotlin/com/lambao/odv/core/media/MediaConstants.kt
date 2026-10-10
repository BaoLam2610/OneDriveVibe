package com.lambao.odv.core.media

import co.touchlab.kermit.Logger

/**
 * Hằng số của nền phát video dùng chung (ADR-0025), chuyển từ `PlayerConstants` của `:feature:player` khi tách `:core:media`.
 * [TAG] giữ nguyên "ODVPlayer" để tab Log của màn Debug (ADR-0012) vẫn lọc "Player" thấy trọn đường đi của một lần phát ở cả màn
 * Xem video lẫn tab Short. Quy tắc log (CH-06): không ghi link phát (chứa `tempauth`), không ghi `Authorization`, không ghi message
 * của ngoại lệ mạng; chỉ ghi mã, tên lớp ngoại lệ, kích thước, thời gian và id rút gọn.
 */
object MediaConstants {

    /** Thẻ log duy nhất của nền phát (cùng thẻ với `:feature:player`). */
    const val TAG = "ODVPlayer"

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

    /** Lấy link phát tối đa chừng này: Graph bị throttle thì `HttpRequestRetry` có thể chờ tới 60 giây và giữ luồng tải của ExoPlayer. */
    const val URL_FETCH_TIMEOUT_MS = 15_000L

    // --- Cache và buffer ---

    /** Thư mục cache video trong `cacheDir` (không sao lưu, CH-04). */
    const val CACHE_DIR_NAME = "video"

    // Trần dung lượng cache video không còn là hằng số: người dùng đặt ở Cài đặt (CD) và đọc qua CacheBudgetProvider (7d).

    /** Giữ lại chừng này đã phát phía sau để tua lùi ngắn không phải tải lại. */
    const val BACK_BUFFER_MS = 30_000

    // --- Phân loại lỗi ---

    /** Số tầng nguyên nhân duyệt khi phân loại và ghi log một lỗi phát. */
    const val CAUSE_DEPTH = 8

    // --- Log ---

    /** Id Graph dài và khó đọc; log chỉ ghi chừng này ký tự đầu. */
    const val LOG_ID_LENGTH = 8
}

/** Logger của nền phát, gắn [MediaConstants.TAG]. */
internal val mediaLog: Logger = Logger.withTag(MediaConstants.TAG)

/** Id rút gọn để ghi log. */
// Lấy phần cuối: id OneDrive trong cùng một drive có tiền tố giống hệt nhau nên log cắt đầu không phân biệt được video.
internal fun String.shortId(): String = takeLast(MediaConstants.LOG_ID_LENGTH)
