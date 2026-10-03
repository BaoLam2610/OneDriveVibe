package com.lambao.odv.feature.library

import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter

/** Phần của trạng thái đồng bộ mà tab Thư viện cần để hiện banner (TV-06, DS-05). */
data class LibrarySync(
    val isSyncing: Boolean = false,
    /** Số mục đã quét khi quét lần đầu ("đã quét N mục"). */
    val scannedCount: Int = 0,
    /** Đã quét xong drive ít nhất một lần. */
    val initialSyncDone: Boolean = false,
    /** Lần đồng bộ gần nhất lỗi (không tính app đang khóa). */
    val failed: Boolean = false,
)

/**
 * Trạng thái tab Thư viện (TV-01 → TV-06). Danh sách ảnh/video không nằm ở đây mà ở luồng phân trang
 * `LibraryViewModel.pages`; State chỉ giữ số mục theo ngày để dựng tiêu đề nhóm và độ dài lưới.
 */
data class LibraryState(
    val filter: LibraryFilter = LibraryFilter.All,
    /** Số mục theo từng ngày, mới nhất trước (TV-01). Rỗng khi chưa có gì hoặc đang đổi bộ lọc. */
    val days: List<LibraryDay> = emptyList(),
    /** Đã nhận được [days] cho [filter] hiện tại; phân biệt "chưa nạp" với "không có mục nào". */
    val daysLoaded: Boolean = false,
    /** Độ lệch múi giờ dùng để nhóm ngày; giao diện dùng đúng giá trị này để định dạng tiêu đề. */
    val utcOffsetMs: Long = 0L,
    val sync: LibrarySync = LibrarySync(),
    /** Máy không có mạng (DS-05). */
    val isOffline: Boolean = false,
)

sealed interface LibraryIntent {
    /** Chạm chip Tất cả / Ảnh / Video (TV-03). */
    data class SelectFilter(val filter: LibraryFilter) : LibraryIntent

    /** Chạm một ô: mở trong màn xem (Lát 5, 6). */
    data class Open(val item: DriveItem) : LibraryIntent

    /** "Thử lại" ở banner đồng bộ lỗi. */
    data object Refresh : LibraryIntent
}

sealed interface LibraryEffect {
    /** Mở tệp trong màn xem tương ứng. Màn xem làm ở Lát 5–6; tới lúc đó nơi gọi chưa xử lý. */
    data class OpenFile(val item: DriveItem) : LibraryEffect
}
