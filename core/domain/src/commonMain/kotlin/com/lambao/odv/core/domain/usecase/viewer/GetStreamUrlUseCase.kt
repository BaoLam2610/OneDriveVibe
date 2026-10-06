package com.lambao.odv.core.domain.usecase.viewer

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.VideoStreamRepository

/**
 * Link phát mới của video (VD-11, VD-14). Link do OneDrive ký sẵn, sống khoảng 1 giờ nên trình phát hỏi lại mỗi lần mở kết nối. Link là
 * bí mật (chứa `tempauth`): không ghi log, không lưu đĩa, không đưa vào khóa cache.
 */
class GetStreamUrlUseCase(
    private val streams: VideoStreamRepository,
) {
    suspend operator fun invoke(itemId: String): AppResult<String> = streams.streamUrl(itemId)
}
