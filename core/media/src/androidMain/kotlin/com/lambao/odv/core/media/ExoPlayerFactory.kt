@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.core.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.MediaSourceFactorySupplier
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.NextVideoPreloadSwitch
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

/**
 * Một [ExoPlayer] vừa dựng cùng [VideoDecoders] của nó; người gọi (màn Xem video, tab Short) sở hữu và phải `release()` player.
 * [nextPreload] chỉ có khi dựng bằng [ExoPlayerFactory.createWithPreload] (tab Short, ADR-0028); người gọi phải `release()` nó **trước** player.
 */
class ExoPlayerHandle(
    val player: ExoPlayer,
    val decoders: VideoDecoders,
    val streamUrls: StreamUrlProvider,
    val nextPreload: NextVideoPreload? = null,
)

/** Các phần dựng dùng chung giữa [ExoPlayerFactory.create] và [ExoPlayerFactory.createWithPreload]. */
private class PlayerParts(
    val streamUrls: StreamUrlProvider,
    val decoders: VideoDecoders,
    val renderers: RenderersFactory,
    val loadControl: LoadControl,
    private val cached: CacheDataSource.Factory,
) {
    /**
     * Mỗi lần gọi là một [DefaultMediaSourceFactory] mới nhưng cùng chuỗi nguồn dữ liệu và cùng [streamUrls]. Video Dolby Vision profile 8 được coi là
     * HEVC Main10 khi màn hình không có Dolby Vision, để renderer FFmpeg nhận được (DolbyVisionAsHevc).
     */
    fun mediaSourceFactory(): DefaultMediaSourceFactory =
        DefaultMediaSourceFactory(cached, DolbyVisionAsHevcExtractorsFactory(decoders.displaySupportsDolbyVision()))
}

/**
 * Dựng [ExoPlayer] dùng chung cho màn Xem video và tab Short (ADR-0025). Chuỗi nguồn dữ liệu, từ trên xuống:
 * `CacheDataSource` (đọc/ghi [VideoCache]) → [StreamDataSource] (đổi địa chỉ ảo sang link ký, tự làm mới khi hết hạn, VD-14)
 * → `DefaultHttpDataSource` (không gửi `Authorization`). Player của màn Xem video có [StreamUrlProvider] riêng nên link ký không sống lâu hơn người
 * dùng player đó; player của tab Short dùng [SharedStreamUrls] chung với bộ tải trước và poster (ADR-0029). Chuyển nguyên văn từ `VideoPlayerFactory` của `:feature:player`, không đổi hành vi.
 */
class ExoPlayerFactory(
    context: Context,
    private val videoCache: VideoCache,
    private val getStreamUrl: GetStreamUrlUseCase,
    private val nextPreloadSwitch: NextVideoPreloadSwitch,
    private val dispatchers: DispatcherProvider,
    private val sharedUrls: SharedStreamUrls,
) {
    private val appContext = context.applicationContext

    /** Phải gọi trên luồng chính (ExoPlayer gắn với Looper nơi nó được tạo). Mở cache lần đầu có thể chờ đĩa ngắn. */
    suspend fun create(): ExoPlayerHandle {
        mediaLog.i { "[Factory] dựng player" }
        // Màn Xem video: provider riêng của player này, link chết theo màn xem.
        val parts = parts(StreamUrlProvider(getStreamUrl, dispatchers))
        val player = ExoPlayer.Builder(appContext, parts.renderers)
            .setMediaSourceFactory(parts.mediaSourceFactory())
            .setLoadControl(parts.loadControl)
            .withPlaybackSettings()
            .build()
        mediaLog.i { "[Factory] player sẵn sàng" }
        return ExoPlayerHandle(player, parts.decoders, parts.streamUrls)
    }

    /**
     * Như [create] nhưng player đi kèm một `DefaultPreloadManager` để tải trước **một** video kế tiếp vào RAM (tab Short, ADR-0028). Manager buộc
     * player dựng qua `Builder.buildExoPlayer`, hàm này đặt luồng phát của player là luồng tải trước và ghi đè nguồn, renderer, load control; vì vậy
     * cả ba lấy từ cùng [PlayerParts] với [create] để player không đổi hành vi.
     *
     * **Không gọi `Builder.setCache`** (ADR-0028): khi gỡ mục hoặc `reset()`, manager ở chế độ cache gọi `removeResource` và xóa cả video khỏi cache chung
     * mà màn Xem video cũng dùng (VD-11, BN-03). Phần đĩa vẫn do `CacheDataSource` trong chuỗi nguồn dữ liệu và [VideoPreloader] đảm nhận.
     */
    suspend fun createWithPreload(): ExoPlayerHandle {
        mediaLog.i { "[Factory] dựng player kèm tải trước" }
        // Tab Short: dùng chung provider với bộ tải trước và poster để mỗi video chỉ lấy link một lần (ADR-0029).
        val parts = parts(sharedUrls.provider)
        val nextPreload = NextVideoPreload(nextPreloadSwitch)
        val sources = object : MediaSourceFactorySupplier {
            // Cố ý bỏ qua: nguồn dữ liệu và cache đã nằm trong factory của PlayerParts (xem KDoc createWithPreload).
            override fun setCache(cache: Cache?): MediaSourceFactorySupplier = this

            override fun setDataSourceFactory(dataSourceFactory: DataSource.Factory?): MediaSourceFactorySupplier = this

            override fun get(): MediaSource.Factory = parts.mediaSourceFactory()
        }
        val builder = DefaultPreloadManager.Builder(appContext, nextPreload.control)
            .setRenderersFactory(parts.renderers)
            .setLoadControl(parts.loadControl)
            .setMediaSourceFactorySupplier(sources)
        val player = builder.buildExoPlayer(ExoPlayer.Builder(appContext).withPlaybackSettings())
        nextPreload.attach(builder.build())
        mediaLog.i { "[Factory] player kèm tải trước sẵn sàng" }
        return ExoPlayerHandle(player, parts.decoders, parts.streamUrls, nextPreload)
    }

    // Nhường tiếng cho cuộc gọi/ứng dụng khác và tự dừng khi rút tai nghe (cải tiến Lát 6).
    private fun ExoPlayer.Builder.withPlaybackSettings(): ExoPlayer.Builder = this
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)

    private suspend fun parts(streamUrls: StreamUrlProvider): PlayerParts {
        val cache = videoCache.get()
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(MediaConstants.HTTP_TIMEOUT_MS)
            .setReadTimeoutMs(MediaConstants.HTTP_TIMEOUT_MS)
        // [streamUrls] do người gọi cấp (riêng cho màn Xem video, dùng chung cho tab Short) để chủ player xóa được link ký khi app bị khóa (CH-03).
        val upstream = StreamDataSourceFactory(http, streamUrls)
        val cached = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            // Lỗi đọc/ghi cache (đĩa đầy, tệp hỏng) thì đọc thẳng từ mạng thay vì làm hỏng việc phát.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(object : CacheDataSource.EventListener {
                // Mỗi lần đọc từ cache là một dòng, quá dày cho bộ đệm log của màn Debug nên chỉ ghi sự cố.
                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) = Unit

                override fun onCacheIgnored(reason: Int) {
                    mediaLog.w { "[Cache] bỏ qua cache, lý do=$reason (1 = lỗi cache, 2 = dữ liệu chưa biết độ dài)" }
                }
            })
        val decoders = VideoDecoders(appContext)
        // Bộ chọn tự viết để chặn được bộ giải mã vừa chết giữa chừng (VideoDecoders). setEnableDecoderFallback lo phần còn lại: bộ giải
        // mã *khởi tạo* lỗi thì Media3 tự thử bộ kế tiếp; còn lỗi *đang giải mã* thì do VideoPlayerController xử lý.
        // FFmpeg (NextLib, ADR-0018) ở chế độ ON: đứng sau renderer của máy, ExoPlayer chọn renderer báo hỗ trợ tốt nhất nên FFmpeg chỉ
        // được dùng khi decoder của máy không nhận định dạng (HEVC Main10 trên Helio G99: cả MTK lẫn decoder phần mềm của Android chỉ
        // có Main). Máy giải mã được thì vẫn dùng phần cứng như cũ. Không dùng PREFER (sẽ ép mọi video HEVC xuống phần mềm).
        val renderers = NextRenderersFactory(appContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setMediaCodecSelector(decoders.selector)
            .setEnableDecoderFallback(true)
        val loadControl = DefaultLoadControl.Builder().setBackBuffer(MediaConstants.BACK_BUFFER_MS, true).build()
        return PlayerParts(streamUrls, decoders, renderers, loadControl, cached)
    }
}
