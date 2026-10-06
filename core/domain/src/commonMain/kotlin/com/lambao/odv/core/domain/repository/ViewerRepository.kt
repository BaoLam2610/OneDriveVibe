package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ImageInfo
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.ViewerContext
import kotlinx.coroutines.flow.Flow

/** Dữ liệu cho các màn xem ảnh và video: danh sách để chuyển trước/sau và thông tin ảnh. */
interface ViewerRepository {

    /**
     * Các mục loại [kind] để chuyển trước/sau trong màn xem: vuốt ảnh (AN-03) hoặc danh sách phát video (VD-10), đúng thứ
     * tự người dùng thấy ở nơi mở: tab Thư mục theo [ViewerContext.Folder.sort], tab Thư viện mới nhất trước và chỉ trong
     * bộ lọc đang chọn. Mỗi màn xem chỉ lấy đúng loại của nó (ảnh không lẫn video, PDF có màn riêng). Đọc từ Room nên
     * dùng được offline, tự phát lại khi đồng bộ đổi dữ liệu (mục bị xóa trên OneDrive tự biến mất, DS-06).
     */
    fun observeViewerItems(context: ViewerContext, kind: MediaKind): Flow<List<DriveItem>>

    /**
     * Kích thước ảnh và thiết bị chụp cho bảng thông tin (AN-05). Gọi Graph theo yêu cầu cho một ảnh (không lưu Room),
     * nên offline trả lỗi và UI chỉ hiện các dòng đã có trong Room.
     */
    suspend fun getImageInfo(itemId: String): AppResult<ImageInfo>
}
