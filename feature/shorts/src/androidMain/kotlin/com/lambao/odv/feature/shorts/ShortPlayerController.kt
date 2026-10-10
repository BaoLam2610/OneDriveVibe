@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.shorts

import android.os.SystemClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.media.ExoPlayerHandle
import com.lambao.odv.core.media.PlayerFailure
import com.lambao.odv.core.media.VideoCache
import com.lambao.odv.core.media.VideoDecoders
import com.lambao.odv.core.media.codecExceptionDetail
import com.lambao.odv.core.media.describe
import com.lambao.odv.core.media.failedDecoderName
import com.lambao.odv.core.media.streamUri
import com.lambao.odv.core.media.toFailure
import com.lambao.odv.core.media.videoCacheKey

/**
 * Cầu nối giữa **một** ExoPlayer của tab Short và Compose (ADR-0024): giữ trạng thái phát dưới dạng state để giao diện đọc trực tiếp.
 * Chỉ khác [VideoPlayerController] của màn Xem video ở chỗ không có tua, tốc độ, khóa thao tác; video tự lặp lại (SV-05) và tốc độ luôn
 * 1x. Mọi hàm gọi trên luồng chính. Do [ShortPlayerHolder] sở hữu để đổi tab chỉ tạm dừng chứ không giải phóng (SV-11).
 */
@Stable
internal class ShortPlayerController(
    handle: ExoPlayerHandle,
    private val videoCache: VideoCache,
) : Player.Listener {

    val player: ExoPlayer = handle.player
    private val decoders: VideoDecoders = handle.decoders

    /** Số lần đã chặn bộ giải mã chết và phát lại cho video đang nạp (giới hạn [ShortsConstants.MAX_DECODER_RETRIES]). */
    private var decoderRetries = 0

    /**
     * Video đang nạp và đợt danh sách ([ShortsState.generation]) của nó, để biết khi nào cần nạp lại và khi nào chỉ cần phát tiếp.
     * **Phải là Compose state**: giao diện quyết định dựng bề mặt phát theo [loadedId]; là biến thường thì trang không dựng lại sau khi
     * [load] đổi nó, bề mặt không được gắn và video đứng hình dù tiếng và thanh tiến độ vẫn chạy (lỗi 2026-10-11).
     */
    var loadedId by mutableStateOf<String?>(null)
        private set
    var loadedGeneration by mutableIntStateOf(-1)
        private set

    /** Mốc (elapsedRealtime) bắt đầu nạp video hiện tại, để log thời gian tới khung hình đầu. */
    private var loadStartedAt = 0L

    var isPlaying by mutableStateOf(false)
        private set
    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var bufferedPositionMs by mutableLongStateOf(0L)
        private set

    /** 0 khi chưa biết; trước khi player đọc được thì dùng thời lượng Room để thanh tiến độ có sẵn. */
    var durationMs by mutableLongStateOf(0L)
        private set

    /** Tỉ lệ khung hình thật sau khi giải mã (đã tính hướng xoay và pixel ratio, SV-08); 0 khi chưa biết. */
    var videoAspect by mutableFloatStateOf(0f)
        private set

    /** Id video đã vẽ khung hình đầu. So theo id: ngay sau khi chuyển video, khung hình cuối của video trước còn lưu trên TextureView. */
    var renderedItemId by mutableStateOf<String?>(null)
        private set

    var failure by mutableStateOf<PlayerFailure?>(null)
        private set
    var failedItemId by mutableStateOf<String?>(null)
        private set

    /** Người dùng chủ động tạm dừng (SV-06) hoặc video nằm chờ sau khi khôi phục (DH-06): không tự phát tiếp khi quay lại tab. */
    var userPaused by mutableStateOf(false)
        private set

    init {
        player.addListener(this)
        // SV-05: chạy hết thì tự lặp lại từ đầu, tốc độ luôn 1x, có tiếng (âm lượng chỉnh bằng phím của máy).
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.setPlaybackSpeed(1f)
        player.volume = 1f
        // Ghi bộ giải mã video thật sự được dùng và số khung rớt, để so phần cứng với FFmpeg dự phòng (ADR-0018) khi vuốt nhanh.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                shortsLog.i { "[Short][Decoder] bộ giải mã $decoderName (khởi tạo ${initializationDurationMs}ms)" }
            }

            override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
                shortsLog.w { "[Short][Decoder] rớt $droppedFrames khung trong ${elapsedMs}ms vị trí=${player.currentPosition}ms" }
            }
        })
    }

    /**
     * Nạp [item] để phát từ [startPositionMs]. Địa chỉ ảo kèm khóa cache theo id và `cTag` (BN-02): link ký thật chỉ được lấy lúc mở
     * kết nối ở `StreamDataSource`. [autoPlay] false cho video khôi phục sau khi tiến trình bị thu hồi (nằm chờ ở trạng thái tạm dừng).
     */
    suspend fun load(item: DriveItem, generation: Int, startPositionMs: Long, autoPlay: Boolean) {
        failure = null
        failedItemId = null
        // Video mới: bộ giải mã bị chặn vì video trước không được áp sang video này.
        decoders.reset()
        decoderRetries = 0
        renderedItemId = null
        videoAspect = 0f
        positionMs = startPositionMs.coerceAtLeast(0L)
        bufferedPositionMs = positionMs
        durationMs = item.durationMs ?: 0L
        loadedId = item.id
        loadedGeneration = generation
        loadStartedAt = SystemClock.elapsedRealtime()
        userPaused = !autoPlay
        val key = videoCacheKey(item.id, item.cTag)
        shortsLog.i { "[Short] nạp id=${item.id.takeLast(ID_LOG_LENGTH)} bắt đầu=${startPositionMs}ms tự phát=$autoPlay thời lượng(Room)=${item.durationMs}ms" }
        val mediaItem = MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(streamUri(item.id))
            .setCustomCacheKey(key)
            .build()
        player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
        player.playWhenReady = autoPlay
        player.prepare()
        shortsLog.d { "[Short] đã prepare id=${item.id.takeLast(ID_LOG_LENGTH)} gen=$generation" }
        videoCache.dropStaleVersions(item.id, key)
    }

    /** Chạm một lần để tạm dừng/phát tiếp (SV-06). */
    fun togglePlay() {
        if (player.playWhenReady) {
            player.pause()
            userPaused = true
        } else {
            player.play()
            userPaused = false
        }
        shortsLog.i { "[Short] chạm đổi trạng thái: tạm dừng bởi người dùng=$userPaused vị trí=${player.currentPosition}ms" }
    }

    /** Tạm dừng do vòng đời (đổi tab, xuống nền, bị khóa; SV-11): không đặt [userPaused] để quay lại thì tự phát tiếp. */
    fun pause() {
        shortsLog.d { "[Short] tạm dừng theo vòng đời, vị trí=${player.currentPosition}ms người dùng đã dừng=$userPaused" }
        player.pause()
    }

    /** Phát tiếp sau khi quay lại, trừ khi người dùng đã tạm dừng hoặc video đang lỗi. */
    fun resume() {
        val can = !userPaused && loadedId != null && failure == null
        shortsLog.d { "[Short] phát tiếp theo vòng đời: được phép=$can (người dùng dừng=$userPaused, đã nạp=${loadedId != null}, lỗi=$failure)" }
        if (can) player.play()
    }

    /** Thử lại sau lỗi (lỗi chung, hoặc có mạng lại ở SV-14). Giữ nguyên vị trí đang dừng. */
    fun retry() {
        shortsLog.i { "[Short] thử lại sau lỗi $failure, vị trí=${player.currentPosition}ms" }
        failure = null
        failedItemId = null
        player.prepare()
        player.play()
        userPaused = false
    }

    /** Đọc lại vị trí, buffer và thời lượng; giao diện gọi theo nhịp ngắn. */
    fun refreshProgress() {
        positionMs = player.currentPosition
        bufferedPositionMs = player.bufferedPosition
        val duration = player.duration
        if (duration != C.TIME_UNSET && duration > 0L) durationMs = duration
    }

    fun release() {
        shortsLog.i { "[Short] nhả player ở vị trí=${player.currentPosition}ms" }
        player.removeListener(this)
        player.release()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        shortsLog.d { "[Short] đang phát=$isPlaying" }
        this.isPlaying = isPlaying
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        shortsLog.d {
            "[Short] trạng thái ${stateName(playbackState)} vị trí=${player.currentPosition}ms đệm=${player.bufferedPosition}ms " +
                "sau ${SystemClock.elapsedRealtime() - loadStartedAt}ms kể từ lúc nạp"
        }
        this.playbackState = playbackState
        if (playbackState == Player.STATE_READY) failure = null
        refreshProgress()
    }

    private fun stateName(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "?$state"
    }

    override fun onPlayerError(error: PlaybackException) {
        val classified = error.toFailure()
        val decoderName = error.failedDecoderName()
        shortsLog.e { "[Short] LỖI phát → $classified: ${error.describe()} bộ giải mã lỗi=$decoderName vị trí=${player.currentPosition}ms" }
        if (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED && decoderName != null) {
            val format = (error as? ExoPlaybackException)?.rendererFormat
            shortsLog.i { "[Decoder] ${decoders.describe(format)} chi tiết lỗi=${error.codecExceptionDetail()}" }
            if (retryWithOtherDecoder(decoderName, format)) return
        }
        failedItemId = player.currentMediaItem?.mediaId
        failure = classified
    }

    /**
     * Bộ giải mã [decoderName] chết giữa chừng (Dolby Vision 8.4 trên MediaTek): chặn nó rồi `prepare()` lại để Media3 chọn bộ tiếp theo,
     * giống `VideoPlayerController` của màn Xem video. False khi hết lượt thử hoặc không còn bộ nào khác (chặn bộ cuối làm Media3 bỏ
     * luôn track video và chỉ phát tiếng).
     */
    private fun retryWithOtherDecoder(decoderName: String, format: Format?): Boolean {
        if (decoderRetries >= ShortsConstants.MAX_DECODER_RETRIES ||
            !decoders.hasAlternative(format, decoderName) ||
            !decoders.block(decoderName)
        ) {
            return false
        }
        decoderRetries++
        shortsLog.w { "[Decoder] bộ $decoderName chết khi giải mã, chặn và phát lại bằng bộ khác (lần $decoderRetries)" }
        player.prepare()
        return true
    }

    override fun onTracksChanged(tracks: Tracks) {
        // Có track video mà không bộ giải mã nào nhận: ExoPlayer bỏ track và chỉ phát tiếng. Báo "không hỗ trợ" thay vì màn đen có tiếng (SV-14).
        if (tracks.containsType(C.TRACK_TYPE_VIDEO) && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO, true)) {
            shortsLog.e { "[Short] track video không có bộ giải mã nào nhận, dừng thay vì phát mỗi tiếng" }
            player.pause()
            failedItemId = player.currentMediaItem?.mediaId
            failure = PlayerFailure.Unsupported
        }
    }

    override fun onRenderedFirstFrame() {
        renderedItemId = player.currentMediaItem?.mediaId
        shortsLog.i {
            "[Short] đã vẽ khung hình đầu id=${renderedItemId?.takeLast(ID_LOG_LENGTH)} sau ${SystemClock.elapsedRealtime() - loadStartedAt}ms kể từ lúc nạp"
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        shortsLog.d { "[Short] kích thước video ${videoSize.width}x${videoSize.height} pixelRatio=${videoSize.pixelWidthHeightRatio}" }
        videoAspect = if (videoSize.width > 0 && videoSize.height > 0) {
            videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
        } else {
            0f
        }
    }

    private companion object {
        /** Id Graph dài; log chỉ ghi chừng này ký tự cuối (cùng quy ước với `:core:media`). */
        const val ID_LOG_LENGTH = 8
    }
}
