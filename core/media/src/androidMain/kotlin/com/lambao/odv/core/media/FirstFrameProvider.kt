package com.lambao.odv.core.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Lấy **khung hình đầu thật** của một video để làm poster khi vuốt tới (tab Short): ảnh khớp đúng khung hình đầu nên khi player vẽ khung thật không
 * còn nháy, và trang không cần hiện spinner (kiểm tay 2026-10-11, thay cho thiết kế hai player). Thumbnail của OneDrive không dùng được vì nó không
 * phải khung hình đầu.
 *
 * Dùng `MediaMetadataRetriever` đọc qua link ký lấy từ [StreamUrlProvider] riêng của lớp này. Bộ giải mã chỉ được mượn vài chục đến vài trăm ms cho
 * một khung rồi trả ngay (không giữ phiên giải mã thứ hai suốt lúc xem như phương án hai player), và chỉ chạy một việc mỗi lần. Máy không có bộ giải mã
 * cho định dạng đó (vd. HEVC 10-bit trên Helio G99) thì trả null và giao diện giữ cách cũ (nền đen, spinner sau một lúc). Ảnh trả về đã xoay đúng
 * hướng hiển thị. Chỉ giữ trong bộ nhớ, không ghi đĩa; [clear] xóa link ký khi app bị khóa (CH-03).
 */
class FirstFrameProvider(
    getStreamUrl: GetStreamUrlUseCase,
    private val dispatchers: DispatcherProvider,
) {
    private val urls = StreamUrlProvider(getStreamUrl)
    private val gate = Semaphore(1)

    /**
     * Khung hình đầu của [itemId], thu nhỏ để rộng không quá [maxWidthPx]; null nếu lấy không được. Chặn luồng I/O nên một lần gọi đang chạy
     * không hủy được giữa chừng: người gọi hủy coroutine thì chỉ bỏ kết quả. Ném lại [CancellationException].
     */
    suspend fun firstFrame(itemId: String, maxWidthPx: Int): Bitmap? = gate.withPermit {
        withContext(dispatchers.io) {
            val id = itemId.shortId()
            val startedAt = SystemClock.elapsedRealtime()
            val retriever = MediaMetadataRetriever()
            try {
                val url = urls.urlFor(itemId, refresh = false)
                // Bị hủy (app khóa) trong lúc lấy link: không mở kết nối, và xóa lại link vừa được nhớ để CH-03 không bị hở.
                if (!currentCoroutineContext().isActive) {
                    urls.clear()
                    throw CancellationException()
                }
                retriever.setDataSource(url, HashMap())
                val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame == null) {
                    mediaLog.w { "[Poster] id=$id không lấy được khung hình đầu sau ${SystemClock.elapsedRealtime() - startedAt}ms" }
                    return@withContext null
                }
                val scaled = frame.scaledToWidth(maxWidthPx)
                mediaLog.i {
                    "[Poster] id=$id có khung hình đầu ${scaled.width}x${scaled.height} sau ${SystemClock.elapsedRealtime() - startedAt}ms"
                }
                scaled
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Chỉ ghi tên lớp lỗi: message của ngoại lệ mạng có thể chứa link ký (CH-06).
                mediaLog.w { "[Poster] id=$id lỗi sau ${SystemClock.elapsedRealtime() - startedAt}ms: ${e.javaClass.simpleName}" }
                null
            } finally {
                retriever.release()
            }
        }
    }

    /** Xóa link ký đang giữ (app bị khóa, CH-03). */
    fun clear() {
        urls.clear()
    }

    private fun Bitmap.scaledToWidth(maxWidthPx: Int): Bitmap {
        if (width <= maxWidthPx) return this
        val height = (height.toLong() * maxWidthPx / width).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(this, maxWidthPx, height, true)
        if (scaled !== this) recycle()
        return scaled
    }
}
