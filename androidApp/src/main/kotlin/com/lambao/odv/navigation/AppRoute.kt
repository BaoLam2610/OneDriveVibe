package com.lambao.odv.navigation

import androidx.navigation3.runtime.NavKey
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ViewerContext
import kotlinx.serialization.Serializable

/**
 * Các đích điều hướng (ADR-0003). Mỗi route là một [NavKey] @Serializable để back stack lưu được qua xoay màn hình,
 * đổi ngôn ngữ (CD-10) và khi hệ thống thu hồi tiến trình.
 *
 * Lát 0 chỉ có các màn khởi động và Danh sách, nội dung còn rỗng. Màn xem và Cài đặt thêm khi làm lát tương ứng.
 */
sealed interface AppRoute : NavKey {

    /** Splash (S1). Lát 1 sẽ quyết định đi tiếp Kết nối hay Danh sách tại đây (đặc tả mục 2). */
    @Serializable
    data object Splash : AppRoute

    /** Kết nối OneDrive (KN). */
    @Serializable
    data object Connect : AppRoute

    /**
     * Thiết lập bảo mật (BM). [fromSettings] là mở từ Cài đặt (CD-02, công tắc Bảo vệ ứng dụng): xong hoặc Back thì quay về Cài đặt,
     * không đặt lại back stack về Danh sách như khi đi từ hộp thoại KN-13.
     */
    @Serializable
    data class SecuritySetup(val fromSettings: Boolean = false) : AppRoute

    /** Màn Khóa (KH). */
    @Serializable
    data object Lock : AppRoute

    /** Danh sách: tab Thư mục và Thư viện. */
    @Serializable
    data object Home : AppRoute

    /** Cài đặt (CD, Lát 7). Mở từ nút bánh răng ở AppBar Danh sách. */
    @Serializable
    data object Settings : AppRoute

    /**
     * Màn nhập PIN trong Cài đặt (P1 → P3). [purpose] là tên `PinPurpose` của `:feature:settings` (chuỗi để route lưu được qua process
     * death mà không kéo kiểu của feature vào route).
     */
    @Serializable
    data class SettingsPin(val purpose: String) : AppRoute

    /** Form Cập nhật Client Secret (CD-04, Lát 7e). Mở từ Cài đặt (sau P1 nếu bảo vệ bật) và từ banner hết hạn ở Danh sách. */
    @Serializable
    data object UpdateSecret : AppRoute

    /**
     * Màn xem ảnh (AN). Chỉ mang tham số tối thiểu nên lưu được qua process death; màn tự nạp danh sách ảnh từ Room theo
     * ngữ cảnh mở. [libraryFilter] khác null là mở từ tab Thư viện (tên `LibraryFilter`); ngược lại là mở từ tab Thư mục
     * ([folderId] null = gốc) theo kiểu sắp xếp [sortField], [sortDirection]. Dùng chuỗi thay vì enum của domain vì domain
     * không phụ thuộc kotlinx.serialization.
     */
    @Serializable
    data class ImageViewer(
        val startItemId: String,
        val folderId: String? = null,
        val sortField: String = SortField.Name.name,
        val sortDirection: String = SortDirection.Ascending.name,
        val libraryFilter: String? = null,
    ) : AppRoute

    /**
     * Màn xem video (VD). Cùng dạng tham số với [ImageViewer]: ngữ cảnh mở quyết định danh sách phát (VD-10), màn tự nạp
     * các video cùng ngữ cảnh từ Room.
     */
    @Serializable
    data class VideoPlayer(
        val startItemId: String,
        val folderId: String? = null,
        val sortField: String = SortField.Name.name,
        val sortDirection: String = SortDirection.Ascending.name,
        val libraryFilter: String? = null,
    ) : AppRoute
}

internal fun ViewerContext.toImageViewerRoute(startItemId: String): AppRoute.ImageViewer = when (this) {
    is ViewerContext.Folder -> AppRoute.ImageViewer(startItemId, folderId, sort.field.name, sort.direction.name)
    is ViewerContext.Library -> AppRoute.ImageViewer(startItemId, libraryFilter = filter.name)
}

internal fun AppRoute.ImageViewer.toViewerContext(): ViewerContext =
    viewerContextOf(folderId, sortField, sortDirection, libraryFilter)

internal fun ViewerContext.toVideoPlayerRoute(startItemId: String): AppRoute.VideoPlayer = when (this) {
    is ViewerContext.Folder -> AppRoute.VideoPlayer(startItemId, folderId, sort.field.name, sort.direction.name)
    is ViewerContext.Library -> AppRoute.VideoPlayer(startItemId, libraryFilter = filter.name)
}

internal fun AppRoute.VideoPlayer.toViewerContext(): ViewerContext =
    viewerContextOf(folderId, sortField, sortDirection, libraryFilter)

/** Dựng lại ngữ cảnh mở từ các trường của route (màn xem ảnh và video dùng chung dạng tham số). */
private fun viewerContextOf(folderId: String?, sortField: String, sortDirection: String, libraryFilter: String?): ViewerContext {
    val filter = libraryFilter?.let { name -> LibraryFilter.entries.firstOrNull { it.name == name } }
    if (filter != null) return ViewerContext.Library(filter)
    return ViewerContext.Folder(
        folderId,
        SortOrder(
            field = SortField.entries.firstOrNull { it.name == sortField } ?: SortField.Name,
            direction = SortDirection.entries.firstOrNull { it.name == sortDirection } ?: SortDirection.Ascending,
        ),
    )
}
