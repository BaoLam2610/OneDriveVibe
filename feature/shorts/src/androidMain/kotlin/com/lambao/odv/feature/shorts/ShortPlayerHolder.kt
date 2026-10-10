package com.lambao.odv.feature.shorts

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.domain.platform.NetworkMonitor
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.media.ExoPlayerFactory
import com.lambao.odv.core.media.PreloadTarget
import com.lambao.odv.core.media.VideoCache
import com.lambao.odv.core.media.VideoPreloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Giữ [ShortPlayerController] suốt vòng đời Màn chính (ADR-0024 mục 6): rời tab Short chỉ tạm dừng, ExoPlayer và bộ giải mã chỉ được giải
 * phóng khi Màn chính bị bỏ (Ngắt kết nối, thoát app), nên quay lại tab thì phát tiếp đúng vị trí (SV-11). Là ViewModel riêng ở androidMain
 * vì `ShortsViewModel` nằm ở commonMain và không được biết tới kiểu của Media3. Player được dựng ngay khi tab Short mở lần đầu.
 */
internal class ShortPlayerHolder(
    private val factory: ExoPlayerFactory,
    private val videoCache: VideoCache,
    private val preloader: VideoPreloader,
    private val network: NetworkMonitor,
    observeLockState: ObserveLockStateUseCase,
) : ViewModel() {

    /** Null cho tới khi dựng xong (mở cache lần đầu có thể chờ đĩa ngắn). */
    var controller by mutableStateOf<ShortPlayerController?>(null)
        private set

    init {
        // viewModelScope chạy trên luồng chính: ExoPlayer phải được tạo và dùng trên cùng một luồng.
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            shortsLog.i { "[Short][Holder] dựng player" }
            try {
                controller = ShortPlayerController(factory.create(), videoCache).also { it.isOnline = network.isOnline.value }
                shortsLog.i { "[Short][Holder] player sẵn sàng sau ${SystemClock.elapsedRealtime() - startedAt}ms" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Không dựng được player: tab Short ở trạng thái đang tải. Chỉ ghi tên lớp lỗi (CH-06).
                shortsLog.e { "[Short][Holder] KHÔNG dựng được player: ${e.javaClass.simpleName}" }
            }
        }
        // SV-13, SV-14: mất mạng thì dừng tải trước; thẻ mất mạng chỉ cho bấm Tiếp tục khi có mạng.
        viewModelScope.launch {
            network.isOnline.collect { online ->
                shortsLog.i { "[Short][Holder] mạng: ${if (online) "có" else "mất"}" }
                controller?.isOnline = online
                if (!online) preloader.cancelAll()
            }
        }
        // CH-03: app bị khóa thì dừng tải trước và player, xóa link ký (link cho phép tải tệp mà không cần token).
        viewModelScope.launch {
            observeLockState().collect { state ->
                if (state == LockState.Locked) {
                    shortsLog.i { "[Short][Holder] app bị khóa" }
                    preloader.cancelAll()
                    controller?.onAppLocked()
                }
            }
        }
    }

    /**
     * Tải trước đầu các video kế cận [videos] vào cache chung (SV-13), theo thứ tự ưu tiên. Không tải khi mất mạng. Gọi sau khi video đang xem
     * đã lên hình, để tải trước không giành băng thông với nó.
     */
    fun preload(videos: List<ShortVideo>) {
        if (!network.isOnline.value) {
            shortsLog.d { "[Short][Holder] bỏ qua tải trước: không có mạng" }
            return
        }
        preloader.update(videos.map { PreloadTarget(it.item.id, it.item.cTag, it.item.sizeBytes, it.item.video?.bitRate) })
    }

    override fun onCleared() {
        shortsLog.i { "[Short][Holder] giải phóng player (Màn chính bị bỏ)" }
        preloader.cancelAll()
        controller?.release()
        controller = null
    }
}
