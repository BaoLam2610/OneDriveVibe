package com.lambao.odv.feature.browser

import com.lambao.odv.core.domain.model.MediaKind

/** Hằng số của tab Thư mục (TM-01 → TM-07, DS-03). */
internal object BrowserConstants {
    /** Chờ người dùng ngừng gõ rồi mới tìm (DS-03). */
    const val SEARCH_DEBOUNCE_MS = 250L

    /** Loại tệp được bật (TM-03, DS-03). Lát 1 coi cả ba loại đều bật; bật/tắt từng loại là Lát 9 (CD-01). */
    val ENABLED_KINDS: Set<MediaKind> = MediaKind.entries.toSet()
}
