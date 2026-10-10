package com.lambao.odv.feature.browser

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SearchResult
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.SyncStatus
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.model.ViewerContext

/** Một cấp trong đường dẫn từ thư mục gốc xuống thư mục đang xem (không gồm thư mục gốc). */
data class Crumb(val id: String, val name: String)

enum class BrowserErrorKind { Network, Other }

/** [code] để người dùng tra cứu (mã Graph hoặc HTTP); không chứa bí mật. Null với lỗi mạng. */
data class BrowserError(val kind: BrowserErrorKind, val code: String?)

/** Trạng thái tìm kiếm (DS-03). Màn đang ở chế độ tìm khi [BrowserState.search] khác null. */
data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
)

data class BrowserState(
    /** Rỗng = đang ở thư mục gốc (TM-01). */
    val path: List<Crumb> = emptyList(),
    /** Đã lọc loại tệp (TM-03) và sắp xếp theo [sort] với thư mục trước (TM-02). */
    val items: List<DriveItem> = emptyList(),
    val isLoading: Boolean = true,
    /** Lỗi tải nằm trong State để còn sau khi xoay màn hình; "Thử lại" xóa nó. */
    val error: BrowserError? = null,
    val sort: SortOrder = SortOrder(),
    val viewMode: ViewMode = ViewMode.List,
    val isSortSheetOpen: Boolean = false,
    val search: SearchState? = null,
    val sync: SyncStatus = SyncStatus(),
    /** Máy không có mạng (DS-05). */
    val isOffline: Boolean = false,
    /** Loại tệp được bật ở Cài đặt (CD-01, TM-03, DS-03); thay đổi áp dụng ngay cho danh sách và tìm kiếm. */
    val enabledKinds: Set<MediaKind> = MediaKind.entries.toSet(),
)

sealed interface BrowserIntent {
    /** Chạm một mục: thư mục thì đi vào, tệp thì mở trong màn xem. */
    data class Open(val item: DriveItem) : BrowserIntent

    /** Nút Back / mũi tên trên thanh tiêu đề: lên một cấp (TM-01). */
    data object GoUp : BrowserIntent

    /** Chạm breadcrumb: 0 là thư mục gốc, 1 là cấp đầu tiên dưới gốc, và cứ thế. */
    data class GoToCrumb(val index: Int) : BrowserIntent
    data object Retry : BrowserIntent

    /** Kéo để làm mới hoặc nút "Thử lại" ở banner lỗi đồng bộ (DS-04). */
    data object Refresh : BrowserIntent

    data object OpenSortSheet : BrowserIntent
    data object DismissSortSheet : BrowserIntent
    data class SelectSort(val order: SortOrder) : BrowserIntent
    data object ToggleViewMode : BrowserIntent

    data object OpenSearch : BrowserIntent
    data object CloseSearch : BrowserIntent
    data class QueryChanged(val query: String) : BrowserIntent

    /** Chạm một kết quả tìm: thư mục thì mở đúng thư mục đó, tệp thì mở trong màn xem. */
    data class OpenSearchResult(val result: SearchResult) : BrowserIntent
}

sealed interface BrowserEffect {
    /**
     * Mở tệp trong màn xem tương ứng. [context] là thư mục đang xem (cho kết quả tìm: thư mục chứa tệp) và kiểu sắp xếp đang
     * chọn, để màn xem vuốt trước/sau đúng thứ tự đang thấy (AN-03).
     */
    data class OpenFile(val item: DriveItem, val context: ViewerContext) : BrowserEffect
}

internal fun AppError.toBrowserError(): BrowserError = when (this) {
    AppError.Network, AppError.Timeout -> BrowserError(BrowserErrorKind.Network, null)
    is AppError.Http -> BrowserError(BrowserErrorKind.Other, code ?: status.toString())
    // AppLocked: app vừa khóa lại (CH-03), màn Khóa sẽ che; sau khi mở khóa người dùng tải lại.
    AppError.SecureStorage, AppError.AppLocked, is AppError.Unknown -> BrowserError(BrowserErrorKind.Other, null)
}
