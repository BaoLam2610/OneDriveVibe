package com.lambao.odv.feature.imageviewer

import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.model.ImageInfo

/**
 * Nội dung bảng thông tin (AN-05). Tên, dung lượng, ngày chụp và đường dẫn thư mục lấy từ Room nên hiện ngay; kích thước
 * ảnh và thiết bị chụp lấy theo yêu cầu từ Graph ([details]), nên offline hoặc lỗi thì [details] vẫn null và hai dòng đó ẩn.
 */
data class ImageViewerInfo(
    val item: DriveItem,
    val folderPath: List<FolderRef> = emptyList(),
    val details: ImageInfo? = null,
    /** Còn đang chờ Graph trả [details]. */
    val isLoadingDetails: Boolean = true,
)

/**
 * Trạng thái màn xem ảnh (AN-01 → AN-07). Ảnh gốc của từng trang không nằm ở đây mà do từng trang tự thu từ
 * [ImageViewerViewModel.originalOf]: chỉ trang đang hiện mới tải, nên State không phải giữ N trạng thái tải.
 */
data class ImageViewerState(
    /** Các ảnh để vuốt trước/sau theo ngữ cảnh mở (AN-03). */
    val images: List<DriveItem> = emptyList(),
    /** Đã nhận danh sách đầu tiên; phân biệt "chưa nạp" với "không có ảnh nào". */
    val isLoaded: Boolean = false,
    /** Chỉ số trang mở đầu, tính một lần ở danh sách đầu tiên. Các lần cập nhật sau do giao diện giữ đúng ảnh theo [currentId]. */
    val initialIndex: Int = 0,
    /** Ảnh đang xem. */
    val currentId: String? = null,
    /** Thanh công cụ hiện (AN-04). Ẩn thì vào chế độ toàn màn hình. */
    val controlsVisible: Boolean = true,
    /** Khác null khi bảng thông tin đang mở (AN-05). */
    val info: ImageViewerInfo? = null,
)

sealed interface ImageViewerIntent {
    /** Trang đã dừng ở ảnh [itemId] sau khi vuốt (AN-03). */
    data class PageChanged(val itemId: String) : ImageViewerIntent

    /** Chạm một lần (AN-04). */
    data object ToggleControls : ImageViewerIntent

    /** Chạm nút Thông tin (AN-05). */
    data object ShowInfo : ImageViewerIntent

    data object HideInfo : ImageViewerIntent
}

sealed interface ImageViewerEffect {
    /** Không còn ảnh nào để xem (ảnh cuối bị xóa trên OneDrive qua đồng bộ, DS-06): thoát màn. */
    data object Close : ImageViewerEffect
}
