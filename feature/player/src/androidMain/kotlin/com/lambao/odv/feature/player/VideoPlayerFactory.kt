@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase

/**
 * Dựng một [VideoPlayerController] cho mỗi lần mở màn xem. Chuỗi nguồn dữ liệu của ExoPlayer, từ trên xuống:
 * `CacheDataSource` (đọc/ghi [VideoCache]) → [StreamDataSource] (đổi địa chỉ ảo sang link ký, tự làm mới khi hết hạn, VD-14)
 * → `DefaultHttpDataSource` (không gửi `Authorization`). Mỗi player có [StreamUrlProvider] riêng nên link ký không sống lâu hơn màn xem.
 */
internal class VideoPlayerFactory(
    context: Context,
    private val videoCache: VideoCache,
    private val getStreamUrl: GetStreamUrlUseCase,
) {
    private val appContext = context.applicationContext

    /** Phải gọi trên luồng chính (ExoPlayer gắn với Looper nơi nó được tạo). Mở cache lần đầu có thể chờ đĩa ngắn. */
    suspend fun create(): VideoPlayerController {
        playerLog.i { "[Factory] dựng player" }
        val cache = videoCache.get()
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(PlayerConstants.HTTP_TIMEOUT_MS)
            .setReadTimeoutMs(PlayerConstants.HTTP_TIMEOUT_MS)
        val upstream = StreamDataSourceFactory(http, StreamUrlProvider(getStreamUrl))
        val cached = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            // Lỗi đọc/ghi cache (đĩa đầy, tệp hỏng) thì đọc thẳng từ mạng thay vì làm hỏng việc phát.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(object : CacheDataSource.EventListener {
                // Mỗi lần đọc từ cache là một dòng, quá dày cho bộ đệm log của màn Debug nên chỉ ghi sự cố.
                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) = Unit

                override fun onCacheIgnored(reason: Int) {
                    playerLog.w { "[Cache] bỏ qua cache, lý do=$reason (1 = lỗi cache, 2 = dữ liệu chưa biết độ dài)" }
                }
            })
        val player = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cached))
            .setLoadControl(DefaultLoadControl.Builder().setBackBuffer(PlayerConstants.BACK_BUFFER_MS, true).build())
            // Nhường tiếng cho cuộc gọi/ứng dụng khác và tự dừng khi rút tai nghe (cải tiến Lát 6).
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        playerLog.i { "[Factory] player sẵn sàng" }
        return VideoPlayerController(player, videoCache)
    }
}
