package com.lambao.odv.feature.shorts

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.domain.platform.NetworkMonitor
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.media.ExoPlayerFactory
import com.lambao.odv.core.media.FirstFrameProvider
import com.lambao.odv.core.media.NextVideoPreload
import com.lambao.odv.core.media.PreloadTarget
import com.lambao.odv.core.media.SharedStreamUrls
import com.lambao.odv.core.media.VideoCache
import com.lambao.odv.core.media.VideoPreloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Cửa sổ tải trước quanh trang đang xem: chỉ số và id của video đang xem, video kế tiếp và video trước (null nếu không có). Video kế tiếp được tải cả
 * vào cache đĩa lẫn RAM (ADR-0028), video trước chỉ vào cache đĩa (ADR-0026).
 */
internal class PreloadWindow(
    val currentIndex: Int,
    val currentId: String,
    val next: ShortVideo?,
    val previous: ShortVideo?,
)

/**
 * Giữ [ShortPlayerController] suốt vòng đời Màn chính (ADR-0024 mục 6): rời tab Short chỉ tạm dừng, ExoPlayer và bộ giải mã chỉ được giải
 * phóng khi Màn chính bị bỏ (Ngắt kết nối, thoát app), nên quay lại tab thì phát tiếp đúng vị trí (SV-11). Là ViewModel riêng ở androidMain
 * vì `ShortsViewModel` nằm ở commonMain và không được biết tới kiểu của Media3. Player được dựng ngay khi tab Short mở lần đầu.
 */
internal class ShortPlayerHolder(
    private val factory: ExoPlayerFactory,
    private val videoCache: VideoCache,
    private val preloader: VideoPreloader,
    private val firstFrames: FirstFrameProvider,
    private val sharedUrls: SharedStreamUrls,
    private val network: NetworkMonitor,
    observeLockState: ObserveLockStateUseCase,
) : ViewModel() {

    /** Null cho tới khi dựng xong (mở cache lần đầu có thể chờ đĩa ngắn). */
    var controller by mutableStateOf<ShortPlayerController?>(null)
        private set

    /** Tải trước video kế tiếp vào RAM (ADR-0028); do cùng player dựng ra nên cùng vòng đời với [controller]. */
    private var nextPreload: NextVideoPreload? = null

    /**
     * Khung hình đầu thật của các video kế cận, theo id (poster, 2026-10-11). Là state của Compose nên trang tự dựng lại khi ảnh về. Chỉ trong bộ
     * nhớ, tối đa [ShortsConstants.POSTER_MAX_COUNT] ảnh (cũ nhất bị bỏ trước), xóa hết khi app bị khóa.
     */
    private val posters = mutableStateMapOf<String, ImageBitmap>()
    private val posterOrder = ArrayDeque<String>()
    private val posterJobs = HashMap<String, Job>()

    /** Poster của [itemId], hoặc null nếu chưa lấy được (giao diện giữ nền đen, spinner sau một lúc). */
    fun posterOf(itemId: String): ImageBitmap? = posters[itemId]

    init {
        // viewModelScope chạy trên luồng chính: ExoPlayer phải được tạo và dùng trên cùng một luồng.
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            shortsLog.i { "[Short][Holder] dựng player" }
            try {
                val handle = factory.createWithPreload()
                nextPreload = handle.nextPreload?.also { it.setOnline(network.isOnline.value) }
                controller = ShortPlayerController(handle, videoCache).also { it.isOnline = network.isOnline.value }
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
                nextPreload?.setOnline(online)
                if (!online) preloader.cancelAll()
            }
        }
        // CH-03: app bị khóa thì dừng tải trước và player, xóa link ký (link cho phép tải tệp mà không cần token).
        viewModelScope.launch {
            observeLockState().collect { state ->
                if (state == LockState.Locked) {
                    shortsLog.i { "[Short][Holder] app bị khóa" }
                    preloader.cancelAll()
                    nextPreload?.reset()
                    clearPosters()
                    controller?.onAppLocked()
                    // Sau cùng, khi mọi người dùng đã dừng: xóa link ký chung và hủy việc lấy link đang chạy một lần (CH-03, ADR-0029).
                    sharedUrls.clear()
                }
            }
        }
    }

    /**
     * Tải trước đầu các video kế cận [window] (SV-13), theo thứ tự ưu tiên (kế tiếp trước, rồi video trước). Cả hai vào cache đĩa chung
     * ([VideoPreloader], ADR-0026); video kế tiếp còn được chuẩn bị sẵn trong RAM ([NextVideoPreload], ADR-0028). Không tải khi mất mạng. Gọi sau khi video
     * đang xem đã lên hình, để tải trước không giành băng thông với nó.
     */
    fun preload(window: PreloadWindow) {
        if (!network.isOnline.value) {
            shortsLog.d { "[Short][Holder] bỏ qua tải trước: không có mạng" }
            return
        }
        val neighbours = listOfNotNull(window.next, window.previous)
        preloader.update(neighbours.map { it.toTarget() })
        nextPreload?.update(window.currentIndex, window.currentId, window.next?.toTarget())
        requestPosters(neighbours.map { it.item.id })
    }

    private fun ShortVideo.toTarget() = PreloadTarget(item.id, item.cTag, item.sizeBytes, item.video?.bitRate)

    /** Lấy khung hình đầu của các video [ids] chưa có poster, lần lượt từng cái theo thứ tự ưu tiên (xem [FirstFrameProvider]). */
    private fun requestPosters(ids: List<String>) {
        for (id in ids) {
            if (id in posters || id in posterJobs) continue
            val job = viewModelScope.launch {
                val bitmap = try {
                    firstFrames.firstFrame(id, ShortsConstants.POSTER_MAX_WIDTH_PX)
                } finally {
                    // Xóa theo cả job: sau clearPosters() có thể đã có job mới cùng id, không được xóa nhầm nó.
                    posterJobs.remove(id, coroutineContext[Job])
                }
                if (bitmap != null) {
                    posters[id] = bitmap.asImageBitmap()
                    posterOrder.addLast(id)
                    while (posterOrder.size > ShortsConstants.POSTER_MAX_COUNT) {
                        val oldest = posterOrder.removeFirst()
                        posters.remove(oldest)
                        shortsLog.d { "[Short][Holder] bỏ poster cũ id=${oldest.takeLast(ID_LOG_LENGTH)}" }
                    }
                    shortsLog.d { "[Short][Holder] có poster id=${id.takeLast(ID_LOG_LENGTH)} (đang giữ ${posters.size})" }
                }
            }
            posterJobs[id] = job
        }
    }

    private fun clearPosters() {
        posterJobs.values.forEach { it.cancel() }
        posterJobs.clear()
        posters.clear()
        posterOrder.clear()
    }

    private companion object {
        /** Id Graph dài; log chỉ ghi chừng này ký tự cuối (CH-06). */
        const val ID_LOG_LENGTH = 8
    }

    override fun onCleared() {
        shortsLog.i { "[Short][Holder] giải phóng player (Màn chính bị bỏ)" }
        preloader.cancelAll()
        clearPosters()
        // Manager trước player: nguồn tải trước có thể còn đang được player dùng (ADR-0028).
        nextPreload?.release()
        nextPreload = null
        controller?.release()
        controller = null
        sharedUrls.clear()
    }
}
