@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.media.ExoPlayerFactory
import com.lambao.odv.core.media.VideoCache

/**
 * Dựng một [VideoPlayerController] cho mỗi lần mở màn xem. Phần dựng ExoPlayer (chuỗi nguồn dữ liệu, cache, decoder, Dolby Vision) đã
 * chuyển sang [ExoPlayerFactory] của `:core:media` để tab Short dùng chung (ADR-0025); ở đây chỉ gắn nó vào controller của màn Xem video.
 */
internal class VideoPlayerFactory(
    private val exoPlayers: ExoPlayerFactory,
    private val videoCache: VideoCache,
) {
    /** Phải gọi trên luồng chính (ExoPlayer gắn với Looper nơi nó được tạo). Mở cache lần đầu có thể chờ đĩa ngắn. */
    suspend fun create(): VideoPlayerController {
        val handle = exoPlayers.create()
        return VideoPlayerController(handle.player, videoCache, handle.decoders)
    }
}
