package com.lambao.odv.core.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** Hằng số đồng bộ delta (ADR-0007, DS-04). */
internal object SyncConstants {
    /** Quá ngần này kể từ lần đồng bộ trọn vẹn trước thì tự đồng bộ khi mở app (DS-04). */
    const val STALE_AFTER_MS = 15L * 60 * 1000

    /** Lỗi tạm thời liên tiếp không tiến triển tối đa bao nhiêu lần thì dừng (TK-05, TK-06). */
    const val MAX_TRANSIENT_RETRIES = 5
    const val MAX_RETRY_AFTER_SECONDS = 300L
    const val MAX_BACKOFF_MS = 60_000L
}

/** Hằng số của kho dữ liệu drive (Room). */
internal object DriveConstants {
    /** Giới hạn độ sâu khi dựng đường dẫn cha của kết quả tìm, chống vòng lặp nếu dữ liệu cha-con bị hỏng. */
    const val MAX_PATH_DEPTH = 64
    const val REF_CHUNK = 500

    /** Số mục mỗi trang của Thư viện: bội của 4 cột để hàng cuối của trang không lẻ. */
    const val LIBRARY_PAGE_SIZE = 100
}

/** Hằng số cache thumbnail (BN-01 → BN-03). */
internal object ThumbnailConstants {
    /** Số thumbnail tải song song tối đa: lưới Thư viện bật hàng chục ô cùng lúc, không nên mở hàng chục kết nối. */
    const val MAX_PARALLEL_DOWNLOADS = 6

    /** Cỡ thumbnail Graph có sẵn, dùng khi cỡ tùy chỉnh bị từ chối (HTTP 400). */
    const val FALLBACK_SIZE = "medium"

    // Chất lượng trung bình có chủ ý: đủ rõ trên lưới mà nhẹ và nhanh (người dùng không cần cao). Ô Thư viện rộng khoảng
    // 90dp (~250px ở xxhdpi) nên 240 hơi mềm nhưng chấp nhận được; thẻ Thư mục 2 cột rộng khoảng 160dp.
    const val MIN_SIDE = 60
    const val MAX_SIDE = 1600

    /** Bộ nhớ RAM cho thumbnail đã giải mã: 15% heap, thấp hơn mặc định của Coil vì Thư viện có thể cuộn rất dài. */
    const val MEMORY_PERCENT = 0.15
}

/** Hằng số kho tệp tải về dùng chung cho ảnh gốc và PDF (AN-01, BN-01 → BN-03). */
internal object ResumableFileConstants {
    /** Phát tiến trình tối đa mỗi chừng này byte để UI không bị ngập (tệp vài chục MB tải theo khúc 64 KB). */
    const val PROGRESS_STEP_BYTES = 256L * 1024

    const val PART_SUFFIX = ".part"

    /** Số khóa chia theo băm của khóa tệp: một tệp chỉ có một luồng ghi, không cần giữ Mutex riêng cho từng tệp đã xem. */
    const val LOCK_STRIPES = 16

    val NON_ALPHANUMERIC = Regex("[^A-Za-z0-9]")
}

// Trần dung lượng cache từng loại không còn là hằng số: người dùng đặt ở Cài đặt (CD) và đọc qua CacheBudgetProvider (7d).

/**
 * Tên lưu trên máy. **Không được đổi giá trị:** đổi là người dùng mất dữ liệu (config không đọc lại được, mất lựa chọn đã lưu,
 * mất bộ đếm sai PIN). Chỉ chuyển chỗ khi refactor.
 */
internal object StorageNames {
    /** Tên bí mật của config; cũng là AAD của phong bì PIN. */
    const val CONFIG = "connection_config"

    /** Tệp DataStore `{tên}.preferences_pb`. */
    const val BROWSER_PREFERENCES = "browser"
    const val PLAYER_PREFERENCES = "player"
    const val SETTINGS_PREFERENCES = "settings"

    /** Thư mục con của `cacheDir`. */
    const val THUMBNAILS_DIR = "thumbnails"
    const val ORIGINALS_DIR = "originals"
}

/** Khóa trong DataStore Preferences. **Không được đổi giá trị** (xem [StorageNames]). */
internal object PreferenceKeys {
    val SORT_FIELD = stringPreferencesKey("sort_field")
    val SORT_DIRECTION = stringPreferencesKey("sort_direction")
    val VIEW_MODE = stringPreferencesKey("view_mode")
    val LAST_LIST_TAB = stringPreferencesKey("last_list_tab")
    val PLAY_MODE = stringPreferencesKey("play_mode")
    val VIDEO_FIT = stringPreferencesKey("video_fit")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ENABLED_KINDS = stringSetPreferencesKey("enabled_kinds")
    val SEEK_STEP_SECONDS = intPreferencesKey("seek_step_seconds")
    val SHORT_MAX_MINUTES = intPreferencesKey("short_max_minutes")
    val DEFAULT_SPEED = floatPreferencesKey("default_speed")

    /** Vắng mặt nghĩa là "nhớ lần gần nhất"; có mặt thì là tên `VideoFit` cố định. */
    val DEFAULT_VIDEO_FIT = stringPreferencesKey("default_video_fit")
    val OPEN_VIDEO_LANDSCAPE = booleanPreferencesKey("open_video_landscape")
    val REMEMBER_VIDEO_POSITION = booleanPreferencesKey("remember_video_position")
    val PDF_READING_STYLE = stringPreferencesKey("pdf_reading_style")
    val SECRET_EXPIRY_EPOCH_DAY = longPreferencesKey("secret_expiry_epoch_day")
    val WIPE_ON_TOO_MANY_FAILURES = booleanPreferencesKey("wipe_on_too_many_failures")
    val AUTO_LOCK_DELAY = stringPreferencesKey("auto_lock_delay")
    val SCREEN_PROTECTION = booleanPreferencesKey("screen_protection")
    val CACHE_LIMIT_GB = intPreferencesKey("cache_limit_gb")

    /** "thumbnail,ảnh,video,pdf" theo phần trăm, vd `10,30,45,15`; sai định dạng hoặc tổng khác 100 thì dùng mặc định. */
    val CACHE_SHARES = stringPreferencesKey("cache_shares")
}
