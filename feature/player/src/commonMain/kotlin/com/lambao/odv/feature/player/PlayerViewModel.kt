package com.lambao.odv.feature.player

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.platform.NetworkMonitor
import com.lambao.odv.core.domain.settings.PlayerPreferences
import com.lambao.odv.core.domain.usecase.folder.GetFolderPathUseCase
import com.lambao.odv.core.domain.usecase.viewer.ObserveViewerItemsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn xem video (VD). Danh sách phát đọc từ Room theo [context] (ADR-0007): các video cùng thư mục hoặc cùng bộ lọc Thư
 * viện, đúng thứ tự người dùng thấy ở nơi mở (VD-10), tự cập nhật khi đồng bộ đổi dữ liệu. ViewModel không biết gì về
 * ExoPlayer: việc phát nằm ở lớp giao diện Android để `commonMain` không dính Media3 (ADR-0001).
 */
class PlayerViewModel(
    private val context: ViewerContext,
    private val startItemId: String,
    private val observeViewerItems: ObserveViewerItemsUseCase,
    private val getFolderPath: GetFolderPathUseCase,
    private val network: NetworkMonitor,
    private val prefs: PlayerPreferences,
) : BaseMviViewModel<PlayerState, PlayerIntent, PlayerEffect>(PlayerState(currentId = startItemId)) {

    private var closed = false
    private var infoJob: Job? = null

    /** Video được chạm đã xuất hiện trong danh sách phát chưa. Trước đó không chọn video thay thế (xem [observeVideos]). */
    private var startFound = false

    init {
        observeVideos()
        closeIfStartNeverAppears()
        observeNetwork()
        observePreferences()
    }

    override fun onIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.Previous -> step(-1)
            PlayerIntent.Next -> step(1)
            PlayerIntent.Advance -> advance()
            is PlayerIntent.VideoFailed -> skipFailed(intent.itemId)
            PlayerIntent.ClearFailed -> setState { if (failedIds.isEmpty()) this else copy(failedIds = emptySet()) }
            is PlayerIntent.SavePosition -> setState { copy(resumePositionMs = intent.positionMs, autoPlay = intent.playing) }
            is PlayerIntent.SetSpeed -> setState { copy(speed = intent.speed) }
            PlayerIntent.CyclePlayMode -> cyclePlayMode()
            PlayerIntent.CycleVideoFit -> cycleVideoFit()
            PlayerIntent.ShowInfo -> showInfo()
            PlayerIntent.HideInfo -> {
                infoJob?.cancel()
                setState { copy(info = null) }
            }
        }
    }

    /**
     * Chuyển video thủ công, không phụ thuộc chế độ phát (VD-10); video mới phát từ đầu. Chế độ Lặp danh sách thì quay vòng.
     * Chuyển tay là lựa chọn của người xem nên xóa danh sách video lỗi để họ có thể thử lại.
     */
    private fun step(delta: Int) {
        setState {
            val index = videos.indexOfFirst { it.id == currentId }
            val target = if (playMode == PlayMode.RepeatList && videos.size > 1) {
                videos[(index + delta).mod(videos.size)]
            } else {
                videos.getOrNull(index + delta)
            } ?: return@setState this
            copy(currentId = target.id, resumePositionMs = 0L, autoPlay = true, failedIds = emptySet())
        }
    }

    /** Video chạy hết và đếm ngược xong (VD-13): sang video kế tiếp theo chế độ phát. Không có thì giữ nguyên (đã ở cuối). */
    private fun advance() {
        setState {
            val target = nextInQueue ?: return@setState this
            if (target.id == currentId) return@setState this
            copy(currentId = target.id, resumePositionMs = 0L, autoPlay = true)
        }
    }

    /**
     * VD-15: video không phát được. Ở Tự phát tiếp và Lặp danh sách thì bỏ qua và chuyển sang video chưa lỗi kế tiếp; nếu mọi
     * video đều đã lỗi thì dừng ở thẻ lỗi (không có video nào để chuyển tới nên không lặp vô hạn). Chế độ khác chỉ ghi nhận.
     */
    private fun skipFailed(itemId: String) {
        setState {
            if (itemId != currentId) return@setState this
            val failed = failedIds + itemId
            val target = nextPlayable(failed)
            playerLog.w { "[VM] video lỗi id=${itemId.shortId()}, bỏ qua sang ${target?.id?.shortId() ?: "không còn video nào"}" }
            if (target == null) {
                copy(failedIds = failed)
            } else {
                copy(failedIds = failed, currentId = target.id, resumePositionMs = 0L, autoPlay = true)
            }
        }
    }

    private fun cyclePlayMode() {
        val next = currentState.playMode.next()
        playerLog.i { "[VM] chế độ phát → $next" }
        setState { copy(playMode = next, failedIds = emptySet()) }
        viewModelScope.launch { prefs.setPlayMode(next) }
    }

    private fun cycleVideoFit() {
        val next = currentState.videoFit.next()
        playerLog.i { "[VM] khung hình → $next" }
        setState { copy(videoFit = next) }
        viewModelScope.launch { prefs.setVideoFit(next) }
    }

    /** Mở bảng thông tin (VD-17): dòng từ Room hiện ngay, đường dẫn thư mục nạp thêm. Không gọi API nên dùng được khi offline. */
    private fun showInfo() {
        val item = currentState.current ?: return
        infoJob?.cancel()
        setState { copy(info = PlayerInfo(item)) }
        infoJob = viewModelScope.launch {
            try {
                val path = getFolderPath(item.id)
                setState {
                    val shown = info
                    if (shown != null && shown.item.id == item.id) copy(info = shown.copy(folderPath = path)) else this
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Lỗi đọc Room: bảng chỉ thiếu dòng thư mục, không đáng làm sập app (viewModelScope không có handler).
                playerLog.w { "[VM] không đọc được đường dẫn thư mục: ${e.javaClass.simpleName}" }
            }
        }
    }

    private fun observeVideos() {
        viewModelScope.launch {
            observeViewerItems(context, MediaKind.Video).collect { videos ->
                playerLog.d { "[VM] danh sách phát ${videos.size} video, ngữ cảnh=${context::class.simpleName}" }
                if (!startFound) {
                    // Chưa thấy video được chạm: quét lần đầu có thể chưa xong (TM-07, danh sách ở màn trước lấy từ API) nên Room chưa
                    // có thư mục, hoặc mới có một phần. Không đóng màn và không phát video khác; chờ nó xuất hiện (xem
                    // closeIfStartNeverAppears cho trường hợp không bao giờ có).
                    if (videos.none { it.id == startItemId }) return@collect
                    startFound = true
                }
                if (videos.isEmpty()) {
                    setState { copy(videos = emptyList(), isLoaded = true) }
                    if (!closed) {
                        closed = true
                        sendEffect(PlayerEffect.Close)
                    }
                    return@collect
                }
                setState {
                    if (videos.any { it.id == currentId }) {
                        copy(videos = videos, isLoaded = true)
                    } else {
                        // Video đang xem biến mất (bị xóa trên OneDrive qua đồng bộ, DS-06) hoặc id mở đầu không thuộc ngữ
                        // cảnh: chuyển sang video ở đúng vị trí đó (hoặc cuối danh sách) và phát từ đầu.
                        val oldIndex = this.videos.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
                        val replacement = videos.getOrNull(oldIndex) ?: videos.last()
                        copy(videos = videos, isLoaded = true, currentId = replacement.id, resumePositionMs = 0L, autoPlay = true)
                    }
                }
            }
        }
    }

    /** Hết [PlayerConstants.START_WAIT_MS] mà video được chạm vẫn không có trong Room (đã bị xóa, hoặc không thuộc ngữ cảnh): đóng màn. */
    private fun closeIfStartNeverAppears() {
        viewModelScope.launch {
            delay(PlayerConstants.START_WAIT_MS)
            if (!startFound && !closed) {
                playerLog.w { "[VM] không thấy video id=${startItemId.shortId()} trong danh sách phát sau ${PlayerConstants.START_WAIT_MS}ms, đóng màn" }
                closed = true
                sendEffect(PlayerEffect.Close)
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            network.isOnline.collect { online -> setState { copy(isOnline = online) } }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch { prefs.playMode.collect { mode -> setState { copy(playMode = mode) } } }
        viewModelScope.launch { prefs.videoFit.collect { fit -> setState { copy(videoFit = fit) } } }
    }
}
