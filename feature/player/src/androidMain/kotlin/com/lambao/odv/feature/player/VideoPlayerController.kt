@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.domain.model.DriveItem

/**
 * Cầu nối giữa ExoPlayer và Compose: giữ trạng thái phát dưới dạng state của Compose để giao diện đọc trực tiếp. Mọi hàm gọi
 * trên luồng chính. Do composable sở hữu (tạo khi vào màn, [release] khi rời hoặc khi màn Khóa che lên) chứ không do ViewModel,
 * để ExoPlayer, bộ giải mã và kết nối mạng không còn sống khi người dùng không thấy màn xem.
 */
@Stable
internal class VideoPlayerController(
    val player: ExoPlayer,
    private val videoCache: VideoCache,
    private val decoders: VideoDecoders,
) : Player.Listener {

    /** Số lần đã chặn bộ giải mã chết và phát lại cho video đang nạp (giới hạn [PlayerConstants.MAX_DECODER_RETRIES]). */
    private var decoderRetries = 0

    /** Đang thật sự phát (không tính lúc đệm hay tạm dừng). */
    var isPlaying by mutableStateOf(false)
        private set

    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    /** Vị trí đã tải trước (buffer), mili giây. */
    var bufferedPositionMs by mutableLongStateOf(0L)
        private set

    /** 0 khi chưa biết. Trước khi player đọc được thì dùng thời lượng Room (Graph) để thanh tua có sẵn. */
    var durationMs by mutableLongStateOf(0L)
        private set

    var speed by mutableFloatStateOf(1f)
        private set

    /** Tỉ lệ khung hình thật của video (rộng/cao); 0 khi chưa biết. */
    var videoAspect by mutableFloatStateOf(0f)
        private set

    /** Đã vẽ khung hình đầu tiên chưa; trước đó giao diện phủ thumbnail lên để không để màn đen (cải tiến Lát 6). */
    var renderedItemId by mutableStateOf<String?>(null)
        private set

    /**
     * Video [itemId] đã vẽ khung hình đầu chưa. So theo id chứ không dùng cờ true/false: ngay sau khi chuyển video, [load] chưa kịp
     * chạy nên cờ cũ còn true, khung hình cuối của video trước (TextureView giữ lại) lộ ra phía sau thumbnail.
     */
    fun hasRenderedFirstFrame(itemId: String): Boolean = renderedItemId == itemId

    /** Khác null khi phát lỗi (VD-15, VD-16). */
    var failure by mutableStateOf<PlayerFailure?>(null)
        private set

    /**
     * Id video mà [failure] thuộc về. Khi chuyển video, `current.id` đổi trước khi [load] kịp xóa lỗi cũ; giao diện so id này để
     * không coi lỗi của video trước là lỗi của video mới (làm video mới bị bỏ qua oan, VD-15).
     */
    var failedItemId by mutableStateOf<String?>(null)
        private set

    init {
        player.addListener(this)
        // Ghi bộ giải mã video thật sự được dùng: cho biết video chạy bằng phần cứng của máy hay bằng FFmpeg dự phòng (ADR-0018).
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                playerLog.i { "[Decoder] video dùng bộ giải mã $decoderName (khởi tạo ${initializationDurationMs}ms)" }
            }

            // Đo độ giật: số khung rớt trong từng khoảng, để so decoder FFmpeg giữa các bản Media3/NextLib (ADR-0018).
            override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
                playerLog.w { "[Decoder] rớt $droppedFrames khung trong ${elapsedMs}ms vị trí=${player.currentPosition}ms" }
            }
        })
    }

    /** Trạng thái cho nút giữa (mục 4.5). IDLE mà không lỗi nghĩa là chưa chuẩn bị xong nên coi là đang tải. */
    val playState: ODVPlayState
        get() = when {
            playbackState == Player.STATE_ENDED -> ODVPlayState.Replay
            playbackState == Player.STATE_BUFFERING || (playbackState == Player.STATE_IDLE && failure == null) -> ODVPlayState.Loading
            isPlaying -> ODVPlayState.Playing
            else -> ODVPlayState.Paused
        }

    /**
     * Nạp [item] để phát từ [startPositionMs]. Địa chỉ ảo kèm khóa cache theo id và `cTag` (BN-02): link ký thật chỉ được lấy lúc
     * mở kết nối ở [StreamDataSource]. [autoPlay] false cho trường hợp khôi phục sau khi bị gỡ (video nằm chờ ở trạng thái tạm dừng).
     */
    suspend fun load(item: DriveItem, startPositionMs: Long, autoPlay: Boolean, speed: Float) {
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
        val key = videoCacheKey(item.id, item.cTag)
        playerLog.i {
            "[Player] nạp id=${item.id.shortId()} bắt đầu=${startPositionMs}ms tự phát=$autoPlay tốc độ=$speed " +
                "dung lượng=${item.sizeBytes} thời lượng(Room)=${item.durationMs}ms cTag=${if (item.cTag == null) "không có" else "có"}"
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(streamUri(item.id))
            .setCustomCacheKey(key)
            .build()
        player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
        player.setPlaybackSpeed(speed)
        player.playWhenReady = autoPlay
        player.prepare()
        videoCache.dropStaleVersions(item.id, key)
    }

    /** Tiếp tục sau lỗi (VD-16: có mạng lại; lỗi chung: thử lại). Giữ nguyên vị trí đang dừng. */
    fun retry() {
        playerLog.i { "[Player] thử lại sau lỗi ${failure}, vị trí=${player.currentPosition}ms" }
        failure = null
        player.prepare()
        player.play()
    }

    /** Nút giữa: hết video thì phát lại từ đầu (VD-21), còn lại đổi qua lại phát/tạm dừng. */
    /** Phát lại từ đầu (VD-21, Lặp một video VD-13): không hỏi "Xem tiếp từ mm:ss?". */
    fun replay() {
        player.seekTo(0L)
        player.play()
    }

    fun togglePlay(): Boolean = when {
        playbackState == Player.STATE_ENDED -> {
            replay()
            true
        }
        player.playWhenReady -> {
            player.pause()
            false
        }
        else -> {
            player.play()
            true
        }
    }

    /** Không đặt tên `setSpeed`: trùng chữ ký JVM với setter của property [speed] (đã gặp lỗi build). */
    fun applySpeed(value: Float) {
        player.setPlaybackSpeed(value)
    }

    /** Tua tương đối [deltaMs] (chạm đúp, nút tua), chặn trong khoảng 0 đến hết video. */
    fun seekBy(deltaMs: Long) {
        val limit = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        seekTo((player.currentPosition + deltaMs).coerceIn(0L, limit))
    }

    fun seekTo(targetMs: Long) {
        player.seekTo(targetMs.coerceAtLeast(0L))
        refreshProgress()
    }

    /** Đọc lại vị trí, buffer và thời lượng; giao diện gọi theo nhịp ngắn khi đang hiển thị. */
    fun refreshProgress() {
        positionMs = player.currentPosition
        bufferedPositionMs = player.bufferedPosition
        val duration = player.duration
        if (duration != C.TIME_UNSET && duration > 0L) durationMs = duration
    }

    fun release() {
        playerLog.i { "[Player] nhả player ở vị trí=${player.currentPosition}ms" }
        player.removeListener(this)
        player.release()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playerLog.d { "[Player] đang phát=$isPlaying" }
        this.isPlaying = isPlaying
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        playerLog.d { "[Player] trạng thái ${stateName(playbackState)} vị trí=${player.currentPosition}ms đệm=${player.bufferedPosition}ms" }
        this.playbackState = playbackState
        if (playbackState == Player.STATE_READY) failure = null
        refreshProgress()
    }

    override fun onPlayerError(error: PlaybackException) {
        val classified = error.toFailure()
        val decoderName = error.failedDecoderName()
        playerLog.e { "[Player] LỖI phát → $classified: ${error.describe()} bộ giải mã lỗi=$decoderName vị trí=${player.currentPosition}ms" }
        if (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED && decoderName != null) {
            val format = (error as? ExoPlaybackException)?.rendererFormat
            playerLog.i { "[Decoder] ${decoders.describe(format)} chi tiết lỗi=${error.codecExceptionDetail()}" }
            if (retryWithOtherDecoder(decoderName, format)) return
        }
        failedItemId = player.currentMediaItem?.mediaId
        failure = classified
    }

    /**
     * Bộ giải mã [decoderName] chết giữa chừng (Dolby Vision 8.4 trên MediaTek): chặn nó rồi `prepare()` lại để Media3 chọn bộ tiếp theo,
     * giữ nguyên vị trí và ý định phát. Không đặt [failure] nên giao diện vẫn ở trạng thái đang tải thay vì nháy thẻ lỗi. False khi hết
     * lượt thử hoặc bộ đó đã bị chặn rồi (thử lại cũng chỉ lặp lỗi), lúc đó lỗi được báo như bình thường.
     */
    private fun retryWithOtherDecoder(decoderName: String, format: Format?): Boolean {
        // Không chặn bộ cuối cùng: không còn bộ nào thì Media3 bỏ track video và phát mỗi tiếng thay vì báo lỗi.
        if (decoderRetries >= PlayerConstants.MAX_DECODER_RETRIES ||
            !decoders.hasAlternative(format, decoderName) ||
            !decoders.block(decoderName)
        ) {
            return false
        }
        decoderRetries++
        playerLog.w { "[Decoder] bộ $decoderName chết khi giải mã, chặn và phát lại bằng bộ khác (lần $decoderRetries) vị trí=${player.currentPosition}ms" }
        player.prepare()
        return true
    }

    private fun stateName(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "?$state"
    }

    override fun onTracksChanged(tracks: Tracks) {
        // Có track video mà không bộ giải mã nào nhận: ExoPlayer bỏ track đó và vẫn phát tiếng, người xem chỉ thấy màn đen có tiếng. Báo lỗi
        // và dừng để hiện thẻ "không hỗ trợ" như các lỗi giải mã khác.
        if (tracks.containsType(C.TRACK_TYPE_VIDEO) && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO, true)) {
            playerLog.e { "[Player] track video không có bộ giải mã nào nhận, dừng thay vì phát mỗi tiếng" }
            player.pause()
            failedItemId = player.currentMediaItem?.mediaId
            failure = PlayerFailure.Unsupported
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        speed = playbackParameters.speed
    }

    override fun onRenderedFirstFrame() {
        playerLog.i { "[Player] đã vẽ khung hình đầu tiên" }
        renderedItemId = player.currentMediaItem?.mediaId
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        playerLog.d { "[Player] kích thước video ${videoSize.width}x${videoSize.height} pixelRatio=${videoSize.pixelWidthHeightRatio}" }
        videoAspect = if (videoSize.width > 0 && videoSize.height > 0) {
            videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
        } else {
            0f
        }
    }
}
