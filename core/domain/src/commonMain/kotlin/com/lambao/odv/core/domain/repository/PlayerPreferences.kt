package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.VideoFit
import kotlinx.coroutines.flow.Flow

/**
 * Lựa chọn của màn xem video được nhớ giữa các lần xem: chế độ phát (VD-20) và khung hình (VD-06). Chỉ là tùy chọn hiển thị,
 * không chứa bí mật. Lát 9 (Cài đặt) đọc cùng nơi lưu này ("Chế độ phát", "Khung hình mặc định"); đọc lỗi thì dùng mặc định.
 */
interface PlayerPreferences {
    val playMode: Flow<PlayMode>
    val videoFit: Flow<VideoFit>

    suspend fun setPlayMode(mode: PlayMode)
    suspend fun setVideoFit(fit: VideoFit)
}
