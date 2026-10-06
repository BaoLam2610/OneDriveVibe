package com.lambao.odv.core.data

import androidx.datastore.preferences.core.stringPreferencesKey

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

    /** Trần dung lượng cache thumbnail (BN-01). Lát 9 nối giới hạn này với Cài đặt (CD). */
    const val CACHE_MAX_BYTES = 200L * 1024 * 1024

    /** Bộ nhớ RAM cho thumbnail đã giải mã: 15% heap, thấp hơn mặc định của Coil vì Thư viện có thể cuộn rất dài. */
    const val MEMORY_PERCENT = 0.15
}

/** Hằng số kho ảnh gốc (AN-01, BN-01 → BN-03). */
internal object OriginalImageConstants {
    /** Phát tiến trình tối đa mỗi chừng này byte để UI không bị ngập (ảnh gốc vài chục MB tải theo khúc 64 KB). */
    const val PROGRESS_STEP_BYTES = 256L * 1024

    const val PART_SUFFIX = ".part"

    /** Số khóa chia theo băm của khóa tệp: một ảnh chỉ có một luồng ghi, không cần giữ Mutex riêng cho từng ảnh đã xem. */
    const val LOCK_STRIPES = 16

    val NON_ALPHANUMERIC = Regex("[^A-Za-z0-9]")

    /** Trần dung lượng cache ảnh gốc (BN-01), tạm cố định; Lát 9 nối với giới hạn cache trong Cài đặt (CD). */
    const val CACHE_MAX_BYTES = 1024L * 1024 * 1024
}

/**
 * Tên lưu trên máy. **Không được đổi giá trị:** đổi là người dùng mất dữ liệu (config không đọc lại được, mất lựa chọn đã lưu,
 * mất bộ đếm sai PIN). Chỉ chuyển chỗ khi refactor.
 */
internal object StorageNames {
    /** Tên bí mật của config; cũng là AAD của phong bì PIN. */
    const val CONFIG = "connection_config"

    /** Bộ đếm sai PIN. Phải khớp `KeystoreConstants.LAST_WIPED_FILE` (tên bí mật + `.bin`) ở `:core:security`. */
    const val LOCKOUT = "lock_state"

    /** Tệp DataStore `{tên}.preferences_pb`. */
    const val BROWSER_PREFERENCES = "browser"
    const val PLAYER_PREFERENCES = "player"

    /** Thư mục con của `cacheDir`. */
    const val THUMBNAILS_DIR = "thumbnails"
    const val ORIGINALS_DIR = "originals"
}

/** Khóa trong DataStore Preferences. **Không được đổi giá trị** (xem [StorageNames]). */
internal object PreferenceKeys {
    val SORT_FIELD = stringPreferencesKey("sort_field")
    val SORT_DIRECTION = stringPreferencesKey("sort_direction")
    val VIEW_MODE = stringPreferencesKey("view_mode")
    val PLAY_MODE = stringPreferencesKey("play_mode")
    val VIDEO_FIT = stringPreferencesKey("video_fit")
}
