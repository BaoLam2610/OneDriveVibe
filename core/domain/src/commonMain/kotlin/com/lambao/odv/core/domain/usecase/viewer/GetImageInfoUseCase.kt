package com.lambao.odv.core.domain.usecase.viewer

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ImageInfo
import com.lambao.odv.core.domain.repository.ViewerRepository

/** Kích thước ảnh và thiết bị chụp cho bảng thông tin (AN-05), lấy theo yêu cầu từ Graph; offline trả lỗi và UI chỉ hiện dòng đã có trong Room. */
class GetImageInfoUseCase(
    private val viewer: ViewerRepository,
) {
    suspend operator fun invoke(itemId: String): AppResult<ImageInfo> = viewer.getImageInfo(itemId)
}
