package com.lambao.odv.feature.library

import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SyncStatus
import com.lambao.odv.core.domain.model.ViewerContext

/**
 * Trạng thái tìm kiếm trong Thư viện (TV-07). Màn đang ở chế độ tìm khi [LibraryState.search] khác null.
 *
 * [query] là chữ trong ô nhập (đổi theo từng phím); [appliedQuery] là từ khóa đã chờ hết khoảng debounce và đang được dùng để tìm, đã bỏ khoảng
 * trắng hai đầu. Lưới kết quả, trạng thái "không có kết quả" và ngữ cảnh màn xem đều theo [appliedQuery] để khớp với cái người dùng đang thấy.
 */
data class LibrarySearchState(
    val query: String = "",
    val appliedQuery: String = "",
)

/**
 * Trạng thái tab Thư viện (TV-01 → TV-07). Danh sách ảnh/video không nằm ở đây mà ở luồng phân trang
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
    val sync: SyncStatus = SyncStatus(),
    /** Máy không có mạng (DS-05). */
    val isOffline: Boolean = false,
    /** Loại tệp được bật ở Cài đặt (CD-01). Thư viện chỉ có ảnh và video; PDF không ảnh hưởng. */
    val enabledKinds: Set<MediaKind> = MediaKind.entries.toSet(),
    /** Tên hiển thị theo drive id của tài khoản, để banner "Đang lập chỉ mục" ghi tên tài khoản ở phạm vi Tất cả (D7, TA-05). */
    val accountNames: Map<String, String> = emptyMap(),
    /** Đang tìm kiếm (TV-07) khi khác null; kết quả nằm ở luồng phân trang `LibraryViewModel.searchPages`. */
    val search: LibrarySearchState? = null,
) {
    /**
     * Chip lọc Tất cả / Ảnh / Video (TV-03) chỉ có nghĩa khi cả ảnh lẫn video đang bật; chỉ còn một loại thì "Tất cả" trùng với loại
     * đó nên ẩn cả hàng chip.
     */
    val showFilters: Boolean get() = MediaKind.Image in enabledKinds && MediaKind.Video in enabledKinds
}

sealed interface LibraryIntent {
    /** Chạm chip Tất cả / Ảnh / Video (TV-03). */
    data class SelectFilter(val filter: LibraryFilter) : LibraryIntent

    /** Chạm một ô: mở trong màn xem (Lát 5, 6). */
    data class Open(val item: DriveItem) : LibraryIntent

    /** "Thử lại" ở banner đồng bộ lỗi. */
    data object Refresh : LibraryIntent

    /** Chạm nút Tìm kiếm trên AppBar (TV-07). */
    data object OpenSearch : LibraryIntent

    /** Nút quay lại của thanh tìm, hoặc Back hệ thống khi đang tìm (TV-07). */
    data object CloseSearch : LibraryIntent

    /** Chữ trong ô tìm đổi (TV-07). */
    data class QueryChanged(val query: String) : LibraryIntent
}

sealed interface LibraryEffect {
    /** Mở tệp trong màn xem tương ứng; [context] là bộ lọc đang chọn để màn xem vuốt đúng danh sách (AN-03). */
    data class OpenFile(val item: DriveItem, val context: ViewerContext) : LibraryEffect
}
