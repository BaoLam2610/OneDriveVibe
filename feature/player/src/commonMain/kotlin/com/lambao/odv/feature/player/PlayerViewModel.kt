package com.lambao.odv.feature.player

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.launch

/**
 * Màn xem video (VD). Danh sách phát đọc từ Room theo [context] (ADR-0007): các video cùng thư mục hoặc cùng bộ lọc Thư
 * viện, đúng thứ tự người dùng thấy ở nơi mở (VD-10), tự cập nhật khi đồng bộ đổi dữ liệu. ViewModel không biết gì về
 * ExoPlayer: việc phát nằm ở lớp giao diện Android để `commonMain` không dính Media3 (ADR-0001).
 */
class PlayerViewModel(
    private val context: ViewerContext,
    startItemId: String,
    private val drives: DriveRepository,
    private val network: NetworkMonitor,
) : BaseMviViewModel<PlayerState, PlayerIntent, PlayerEffect>(PlayerState(currentId = startItemId)) {

    private var closed = false

    init {
        observeVideos()
        observeNetwork()
    }

    override fun onIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.Previous -> step(-1)
            PlayerIntent.Next -> step(1)
            is PlayerIntent.SavePosition -> setState { copy(resumePositionMs = intent.positionMs, autoPlay = intent.playing) }
            is PlayerIntent.SetSpeed -> setState { copy(speed = intent.speed) }
        }
    }

    /** Chuyển video thủ công, không phụ thuộc chế độ phát (VD-10); video mới phát từ đầu. */
    private fun step(delta: Int) {
        setState {
            val target = videos.getOrNull(videos.indexOfFirst { it.id == currentId } + delta) ?: return@setState this
            copy(currentId = target.id, resumePositionMs = 0L, autoPlay = true)
        }
    }

    private fun observeVideos() {
        viewModelScope.launch {
            drives.observeViewerItems(context, MediaKind.Video).collect { videos ->
                playerLog.d { "[VM] danh sách phát ${videos.size} video, ngữ cảnh=${context::class.simpleName}" }
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

    private fun observeNetwork() {
        viewModelScope.launch {
            network.isOnline.collect { online -> setState { copy(isOnline = online) } }
        }
    }
}
