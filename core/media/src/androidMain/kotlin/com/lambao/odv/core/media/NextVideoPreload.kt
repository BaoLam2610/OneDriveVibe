@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.core.media

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.PreloadStatus
import androidx.media3.exoplayer.source.preload.PreloadException
import androidx.media3.exoplayer.source.preload.PreloadManagerListener
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import com.lambao.odv.core.domain.hook.NextVideoPreloadSwitch

/**
 * Chuẩn bị sẵn trong **RAM** đầu video kế tiếp (+1) của tab Short bằng `DefaultPreloadManager` (ADR-0028): nguồn đã mở, track đã chọn, vài giây mẫu đã tách,
 * nên khi vuốt sang player bỏ được các bước mở nguồn và đọc phần mô tả tệp. Bổ sung cho [VideoPreloader] (ADR-0026), không thay: [VideoPreloader] vẫn ghi byte
 * đầu của video ±1 vào cache đĩa (phát lại được khi offline, tính vào trần cache); lớp này chỉ thêm tầng RAM cho video +1.
 *
 * Manager **không được bật chế độ cache** (`Builder.setCache`, xem `ExoPlayerFactory.createWithPreload`): khi gỡ mục nó xóa cả video khỏi cache dùng chung.
 * Dữ liệu trong RAM mất khi gỡ mục hoặc [reset], không ảnh hưởng cache đĩa.
 *
 * Mọi hàm phải gọi trên luồng chính (luồng tạo manager). Dựng bởi `ExoPlayerFactory.createWithPreload`, người dùng giữ nó cùng vòng đời với player và gọi
 * [release] **trước** `player.release()`.
 */
class NextVideoPreload internal constructor(private val switch: NextVideoPreloadSwitch) {

    private var manager: DefaultPreloadManager? = null

    /** Giá trị công tắc debug ở lần đọc trước, để chỉ ghi log khi nó đổi. Bản phát hành luôn bật. */
    private var lastEnabled = true

    // Chỉ số trang đang xem và trạng thái mạng; control đọc hai giá trị này mỗi lần manager đánh giá lại (invalidate).
    @Volatile private var currentIndex = 0

    @Volatile private var online = true

    /** Mục đang giữ trong manager, theo id video. */
    private val managed = HashMap<String, MediaItem>()

    /**
     * Mục player đã lấy nguồn của nó ([takeSource]). Nguồn đã qua tay player không dùng lại được cho lần phát khác, nên mục nằm ở đây thì
     * [takeSource] trả null và [update] dựng lại mục mới khi nó lại thành video kế tiếp (vuốt ngược).
     */
    private val consumed = HashSet<String>()

    /** Mục tải trước lỗi: [takeSource] trả null để player tự mở như bình thường thay vì nhận một nguồn hỏng. */
    private val failed = HashSet<String>()
    private val startedAt = HashMap<String, Long>()

    /** Mục manager đã báo hoàn tất tải trước ([PreloadManagerListener.onCompleted]); để log biết nguồn đã sẵn sàng đầy đủ hay còn dở lúc player lấy. */
    private val completed = HashSet<String>()

    /** Thứ hạng (chỉ số trang) lúc mục được thêm; [control] so với trang đang xem nên mục cũ sau khi xáo lại có thứ hạng lệch và phải dựng lại. */
    private val ranks = HashMap<String, Int>()

    /** Chỉ mục có thứ hạng đúng bằng trang đang xem + 1 mới được tải; mất mạng thì không mục nào (SV-13). */
    internal val control = TargetPreloadStatusControl<Int, PreloadStatus> { rank ->
        if (online && rank == currentIndex + 1) {
            PreloadStatus.specifiedRangeLoaded(0L, MediaConstants.NEXT_PRELOAD_MS)
        } else {
            PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
        }
    }

    internal fun attach(manager: DefaultPreloadManager) {
        this.manager = manager
        manager.addListener(object : PreloadManagerListener {
            override fun onCompleted(mediaItem: MediaItem) {
                val id = mediaItem.mediaId
                completed += id
                val now = SystemClock.elapsedRealtime()
                mediaLog.i { "[Preload+1] xong id=${id.shortId()} sau ${now - (startedAt[id] ?: now)}ms" }
            }

            override fun onError(exception: PreloadException) {
                // Chỉ ghi tên lớp lỗi (CH-06). Không gỡ mục ngay trong callback của manager; đánh dấu để player tự mở.
                val id = exception.mediaItem.mediaId
                failed += id
                mediaLog.w { "[Preload+1] lỗi id=${id.shortId()}: ${exception.cause?.javaClass?.simpleName ?: exception.javaClass.simpleName}" }
            }
        })
    }

    /**
     * Đặt cửa sổ tải trước: người dùng đang ở trang [currentIndex] (video [currentId]) và video kế tiếp là [next] (null nếu hết danh sách). Giữ
     * tối đa hai mục: video đang xem (nguồn có thể đang được player dùng) và [next]; mục khác bị gỡ. Gọi sau khi video đang xem đã lên hình để tải trước
     * không giành băng thông với nó.
     */
    fun update(currentIndex: Int, currentId: String, next: PreloadTarget?) {
        val manager = manager ?: return
        // Công tắc debug (chỉ bản debug đổi được): tắt thì gỡ hết khỏi RAM và không chuẩn bị gì, player mở bằng MediaItem thường để so A/B.
        if (!syncEnabled()) {
            reset()
            return
        }
        this.currentIndex = currentIndex
        val nextId = next?.itemId
        val iterator = managed.entries.iterator()
        while (iterator.hasNext()) {
            val (id, item) = iterator.next()
            if (id != currentId && id != nextId) {
                manager.remove(item)
                forget(id)
                iterator.remove()
            }
        }
        // Dựng lại mục của video kế tiếp khi nguồn cũ không dùng được: đã qua tay player (vuốt ngược) hoặc thứ hạng lệch (danh sách vừa xáo lại).
        if (nextId != null && (nextId in consumed || ranks[nextId].let { it != null && it != currentIndex + 1 })) {
            managed.remove(nextId)?.let { manager.remove(it) }
            forget(nextId)
        }
        if (next != null && nextId !in managed) {
            val item = streamMediaItem(next.itemId, next.cTag)
            managed[next.itemId] = item
            ranks[next.itemId] = currentIndex + 1
            startedAt[next.itemId] = SystemClock.elapsedRealtime()
            manager.add(item, currentIndex + 1)
            mediaLog.i { "[Preload+1] bắt đầu id=${next.itemId.shortId()} (cửa sổ ${managed.size} mục)" }
        }
        manager.setCurrentPlayingIndex(currentIndex)
        manager.invalidate()
    }

    /**
     * Nguồn đã tải trước của [itemId] để đưa vào `player.setMediaSource`, hoặc null nếu không có (chưa tải, đã dùng, lỗi) và player phải mở bằng
     * `setMediaItem` như thường. Trả mỗi nguồn **một lần**.
     */
    fun takeSource(itemId: String): MediaSource? {
        val manager = manager ?: return null
        if (!syncEnabled()) return null
        if (itemId in consumed || itemId in failed) return null
        val item = managed[itemId] ?: return null
        val source = manager.getMediaSource(item) ?: return null
        consumed += itemId
        // sẵn=true: manager đã tải xong đủ khoảng NEXT_PRELOAD_MS; false: player lấy nguồn khi còn đang chuẩn bị dở. chuẩnBị = thời gian từ lúc thêm mục.
        val prepared = SystemClock.elapsedRealtime() - (startedAt[itemId] ?: SystemClock.elapsedRealtime())
        mediaLog.i { "[Preload+1] player dùng nguồn tải trước id=${itemId.shortId()} sẵn=${itemId in completed} chuẩnBị=${prepared}ms" }
        return source
    }

    /** Đọc công tắc và ghi log khi nó đổi. */
    private fun syncEnabled(): Boolean {
        val enabled = switch.isEnabled()
        if (enabled != lastEnabled) {
            lastEnabled = enabled
            mediaLog.i { "[Preload+1] công tắc debug: ${if (enabled) "BẬT" else "TẮT"} nguồn tải trước vào RAM" }
        }
        return enabled
    }

    /** Mất mạng hoặc có lại (SV-13): mất mạng thì manager dừng và giải phóng phần đang tải trong RAM, có lại thì lần [update] kế tiếp tải tiếp. */
    fun setOnline(online: Boolean) {
        if (this.online == online) return
        this.online = online
        manager?.invalidate()
    }

    /** Gỡ hết (app bị khóa, CH-03; xáo lại danh sách): không mục nào còn trong RAM. */
    fun reset() {
        if (managed.isNotEmpty()) mediaLog.i { "[Preload+1] xóa ${managed.size} mục" }
        manager?.reset()
        managed.clear()
        consumed.clear()
        failed.clear()
        startedAt.clear()
        ranks.clear()
        completed.clear()
    }

    /** Giải phóng manager. Gọi trước `player.release()`; sau đó không dùng lại. */
    fun release() {
        reset()
        manager?.release()
        manager = null
    }

    private fun forget(id: String) {
        consumed.remove(id)
        failed.remove(id)
        startedAt.remove(id)
        ranks.remove(id)
        completed.remove(id)
    }
}
