package com.lambao.odv.core.domain.usecase.viewer

import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.repository.ViewerRepository
import kotlinx.coroutines.flow.Flow

/**
 * Các mục loại [MediaKind] để chuyển trước/sau trong màn xem ảnh (AN-03) hoặc video (VD-10), đúng thứ tự người dùng thấy ở nơi mở
 * ([ViewerContext]). Đọc từ Room nên dùng được offline, tự phát lại khi đồng bộ đổi dữ liệu (DS-06).
 */
class ObserveViewerItemsUseCase(
    private val viewer: ViewerRepository,
) {
    operator fun invoke(context: ViewerContext, kind: MediaKind): Flow<List<DriveItem>> = viewer.observeViewerItems(context, kind)
}
