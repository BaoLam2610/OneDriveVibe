package com.lambao.odv.feature.player

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.ResumeBehavior
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.platform.NetworkMonitor
import com.lambao.odv.core.domain.settings.PlayerPreferences
import com.lambao.odv.core.domain.settings.SettingsPreferences
import com.lambao.odv.core.domain.usecase.folder.GetFolderPathUseCase
import com.lambao.odv.core.domain.usecase.viewer.ObserveViewerItemsUseCase
import com.lambao.odv.core.domain.usecase.watch.ClearWatchProgressUseCase
import com.lambao.odv.core.domain.usecase.watch.GetWatchProgressUseCase
import com.lambao.odv.core.domain.usecase.watch.SaveWatchProgressUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    private val getWatchProgress: GetWatchProgressUseCase,
    private val saveWatchProgress: SaveWatchProgressUseCase,
    private val clearWatchProgress: ClearWatchProgressUseCase,
    private val network: NetworkMonitor,
    private val prefs: PlayerPreferences,
    private val settings: SettingsPreferences,
) : BaseMviViewModel<PlayerState, PlayerIntent, PlayerEffect>(PlayerState(currentId = startItemId)) {

    private var closed = false
    private var infoJob: Job? = null

    /** Video được chạm đã xuất hiện trong danh sách phát chưa. Trước đó không chọn video thay thế (xem [observeVideos]). */
    private var startFound = false

    /** Khung hình cố định từ Cài đặt; null là "nhớ lần gần nhất" (VD-06). Chỉ khi null thì nút Khung hình mới ghi nhớ lựa chọn. */
    private var fixedFit: VideoFit? = null

    /** Người xem đã tự chọn tốc độ trong màn này: không để tốc độ mặc định (đọc bất đồng bộ) ghi đè lên. */
    private var speedTouched = false

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
            is PlayerIntent.ReportProgress -> reportProgress(intent)
            PlayerIntent.ResumeFromSaved -> setState {
                val prompt = resumePrompt ?: return@setState this
                copy(resumePrompt = null, resumeResolved = true, resumePositionMs = prompt.positionMs)
            }
            PlayerIntent.StartOver -> startOver()
            is PlayerIntent.SetSpeed -> {
                speedTouched = true
                setState { copy(speed = intent.speed) }
            }
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
        if (fixedFit == null) viewModelScope.launch { prefs.setVideoFit(next) }
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
                    val start = videos.firstOrNull { it.id == startItemId } ?: return@collect
                    startFound = true
                    resolveResume(start)
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

    /**
     * VD-12: quyết định video được chạm bắt đầu từ đâu, một lần khi nó xuất hiện trong danh sách (cần `cTag` để biết vị trí đã lưu còn
     * đúng). Mở từ dải "Xem tiếp" ([ViewerContext.Folder.resumeFromSaved]) thì vào thẳng vị trí đã dừng; còn lại theo cài đặt "Khi mở lại
     * video xem dở": hỏi trước (hộp thoại V3), luôn xem tiếp, hoặc luôn phát từ đầu (xóa vị trí đã lưu). Chưa từng xem dở, đã xem xong
     * (chạy hết video) hoặc không đọc được Room thì phát từ đầu như trước.
     */
    private fun resolveResume(start: DriveItem) {
        viewModelScope.launch {
            val saved = try {
                getWatchProgress(start.id, start.cTag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Lỗi đọc Room: chỉ mất tính năng xem tiếp, không đáng chặn việc xem video.
                playerLog.w { "[VM] không đọc được vị trí đã lưu: ${e.javaClass.simpleName}" }
                null
            }
            if (saved == null) {
                setState { copy(resumeResolved = true) }
                return@launch
            }
            val fromStrip = (context as? ViewerContext.Folder)?.resumeFromSaved == true
            val behavior = if (fromStrip) ResumeBehavior.Resume else settings.videoResumeBehavior.first()
            playerLog.i { "[VM] có vị trí đã lưu ${saved.positionMs}ms, cách xử lý=$behavior (từ dải=$fromStrip)" }
            when (behavior) {
                ResumeBehavior.Ask -> {
                    val percent = (saved.positionMs * 100L / saved.durationMs.coerceAtLeast(1L)).toInt().coerceIn(0, 100)
                    setState { copy(resumePrompt = ResumePrompt(saved.positionMs, percent)) }
                }
                ResumeBehavior.Resume -> setState { copy(resumeResolved = true, resumePositionMs = saved.positionMs) }
                ResumeBehavior.StartOver -> {
                    clearSaved(start.id)
                    setState { copy(resumeResolved = true) }
                }
            }
        }
    }

    /** "Xem từ đầu" ở hộp thoại V3: xóa vị trí đã lưu rồi phát từ đầu. */
    private fun startOver() {
        if (currentState.resumePrompt == null) return
        // Đóng hộp thoại ngay nhưng chỉ cho nạp video sau khi xóa xong: nếu không, lần báo vị trí đầu tiên có thể đến trước lệnh xóa rồi bị xóa mất.
        setState { copy(resumePrompt = null) }
        viewModelScope.launch {
            clearSaved(startItemId)
            setState { copy(resumeResolved = true) }
        }
    }

    private suspend fun clearSaved(itemId: String) {
        try {
            clearWatchProgress(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            playerLog.w { "[VM] không xóa được vị trí đã lưu: ${e.javaClass.simpleName}" }
        }
    }

    /**
     * Ghi vị trí xem (VD-12). Chưa quyết xong điểm bắt đầu (đang hỏi hoặc đang đọc cài đặt) thì người xem chưa xem gì nên bỏ qua, để không
     * ghi đè vị trí đã lưu. Ghi trong `NonCancellable`: lần ghi cuối khi rời màn xảy ra ngay lúc ViewModel sắp bị hủy.
     */
    private fun reportProgress(report: PlayerIntent.ReportProgress) {
        if (!currentState.resumeResolved) return
        val item = currentState.videos.firstOrNull { it.id == report.itemId } ?: return
        // `launch(NonCancellable)` chứ không phải `withContext` bên trong: lần ghi cuối khi rời màn có thể đến sau khi viewModelScope đã bị
        // hủy, lúc đó `launch` thường không chạy thân coroutine.
        viewModelScope.launch(NonCancellable) {
            try {
                saveWatchProgress(item.id, item.cTag, report.positionMs, report.durationMs)
            } catch (e: Exception) {
                // Không ghi được chỉ làm lần sau mở lại không có vị trí, không đáng làm sập màn xem.
                playerLog.w { "[VM] không ghi được vị trí xem: ${e.javaClass.simpleName}" }
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
        viewModelScope.launch {
            fixedFit = settings.defaultVideoFit.first()
            val fixed = fixedFit
            if (fixed != null) {
                setState { copy(videoFit = fixed) }
            } else {
                prefs.videoFit.collect { fit -> setState { copy(videoFit = fit) } }
            }
        }
        // Tốc độ mặc định áp dụng khi mở video (VD-05), trừ khi người xem đã chọn tốc độ khác trước khi đọc xong.
        viewModelScope.launch {
            val speed = settings.defaultSpeed.first()
            if (!speedTouched) setState { copy(speed = speed) }
        }
        viewModelScope.launch { settings.seekStepSeconds.collect { seconds -> setState { copy(seekStepSeconds = seconds) } } }
        viewModelScope.launch {
            val landscape = settings.openVideoLandscape.first()
            setState { copy(openInLandscape = landscape) }
        }
    }
}
