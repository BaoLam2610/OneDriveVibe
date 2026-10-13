package com.lambao.odv.core.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.SystemClock
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Lấy **khung hình đầu thật** của một video để làm poster khi vuốt tới (tab Short): ảnh khớp đúng khung hình đầu nên khi player vẽ khung thật không
 * còn nháy, và trang không cần hiện spinner (kiểm tay 2026-10-11, thay cho thiết kế hai player). Thumbnail của OneDrive không dùng được vì nó không
 * phải khung hình đầu.
 *
 * Dùng `MediaMetadataRetriever` đọc qua link ký lấy từ [SharedStreamUrls] dùng chung của tab Short (ADR-0029). Bộ giải mã chỉ được mượn vài chục đến vài trăm ms cho
 * một khung rồi trả ngay (không giữ phiên giải mã thứ hai suốt lúc xem như phương án hai player), và chỉ chạy một việc mỗi lần. Máy không có bộ giải mã
 * cho định dạng đó (vd. HEVC 10-bit trên Helio G99) thì trả null và giao diện giữ cách cũ (nền đen, spinner sau một lúc). Ảnh trả về đã xoay đúng
 * hướng hiển thị. Chỉ giữ trong bộ nhớ, không ghi đĩa; link ký được xóa chung khi app bị khóa (CH-03, `ShortPlayerHolder`).
 */
class FirstFrameProvider(
    sharedUrls: SharedStreamUrls,
    private val dispatchers: DispatcherProvider,
) {
    private val urls = sharedUrls.provider
    private val gate = Semaphore(1)

    // Chạy các việc chặn luồng của MediaMetadataRetriever tách khỏi coroutine người gọi (xem firstFrame). Singleton của Koin nên sống cùng app.
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    /**
     * Khung hình đầu của [itemId], thu nhỏ để rộng không quá [maxWidthPx]; null nếu lấy không được. Chặn luồng I/O nên một lần gọi đang chạy
     * không hủy được giữa chừng: người gọi hủy coroutine thì chỉ bỏ kết quả. Ném lại [CancellationException].
     *
     * Có hạn [FRAME_TIMEOUT_MS]: `setDataSource` qua mạng không có timeout riêng, một request treo sẽ giữ chỗ của hàng đợi (chỉ một việc mỗi
     * lần) và chặn mọi poster phía sau. Việc chặn luồng chạy trong coroutine riêng ([scope]) và ở đây chỉ `await` nó có hạn: `withContext` bị hủy
     * vẫn phải chờ khối đang chặn kết thúc mới quay về nên không dùng được. Hết hạn thì hàng đợi chạy tiếp ngay (kết quả null, giao diện giữ
     * nền đen và spinner như khi không có poster); retriever được nhả trên luồng I/O chứ không trên luồng gọi, vì nếu luồng kia còn kẹt trong
     * `setDataSource` thì `release()` có thể phải chờ.
     */
    suspend fun firstFrame(itemId: String, maxWidthPx: Int): Bitmap? = gate.withPermit {
        val id = itemId.shortId()
        val startedAt = SystemClock.elapsedRealtime()
        val retriever = MediaMetadataRetriever()
        val work = scope.async { loadFrame(retriever, itemId, id, startedAt, maxWidthPx) }
        try {
            val frame = withTimeoutOrNull(FRAME_TIMEOUT_MS) { work.await() }
            if (frame == null && SystemClock.elapsedRealtime() - startedAt >= FRAME_TIMEOUT_MS) {
                mediaLog.w { "[Poster] id=$id quá hạn ${FRAME_TIMEOUT_MS}ms, bỏ qua" }
            }
            frame
        } finally {
            // Xong, hết hạn hay bị hủy (app khóa) đều dừng việc còn dang dở rồi nhả retriever. Kết quả tới muộn thì bị bỏ (GC thu bitmap).
            work.cancel()
            scope.launch { runCatching { retriever.release() } }
        }
    }

    /** Phần chặn luồng I/O của [firstFrame], chạy trong [scope]; [retriever] do nơi gọi nhả. */
    private suspend fun loadFrame(
        retriever: MediaMetadataRetriever,
        itemId: String,
        id: String,
        startedAt: Long,
        maxWidthPx: Int,
    ): Bitmap? =
            try {
                val url = urls.urlFor(itemId, refresh = false)
                // Bị hủy (app khóa) trong lúc lấy link: không mở kết nối. Link không bị ghi lại sau khi xóa vì StreamUrlProvider.clear() đã đổi epoch
                // (ADR-0029); tuyệt đối không gọi urls.clear() ở đây vì link giờ dùng chung với player và bộ tải trước.
                currentCoroutineContext().ensureActive()
                retriever.setDataSource(url, HashMap())
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                // Khung giải mã đầy đủ chiếm cả chục MB (4K ≈ 33 MB ARGB): quá cỡ thì bỏ poster thay vì liều với bộ nhớ.
                // Không đọc được kích thước thì không biết khung lớn cỡ nào: cũng bỏ qua cho an toàn.
                if (width <= 0 || height <= 0 || width.toLong() * height > MAX_SOURCE_PIXELS) {
                    mediaLog.w { "[Poster] id=$id bỏ qua: video quá lớn hoặc không rõ kích thước (${width}x$height)" }
                    return null
                }
                // API 27+ cho giải mã thẳng ra cỡ nhỏ (không dựng khung đầy đủ rồi mới thu nhỏ). Video xoay 90/270 độ thì chưa rõ nền tảng áp
                // kích thước trước hay sau khi xoay nên dùng đường cũ, đã có trần ở trên.
                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && width > maxWidthPx && rotation % 180 == 0) {
                    val scaledHeight = (height.toLong() * maxWidthPx / width).toInt().coerceAtLeast(1)
                    retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxWidthPx, scaledHeight)
                } else {
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (frame == null) {
                    mediaLog.w { "[Poster] id=$id không lấy được khung hình đầu sau ${SystemClock.elapsedRealtime() - startedAt}ms" }
                    return null
                }
                val scaled = frame.scaledToWidth(maxWidthPx)
                mediaLog.i {
                    "[Poster] id=$id có khung hình đầu ${scaled.width}x${scaled.height} sau ${SystemClock.elapsedRealtime() - startedAt}ms"
                }
                scaled
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                // Poster chỉ là phần thêm: thiếu bộ nhớ thì bỏ, không để làm sập app.
                mediaLog.w { "[Poster] id=$id hết bộ nhớ khi lấy khung hình đầu, bỏ qua" }
                null
            } catch (e: Exception) {
                // Chỉ ghi tên lớp lỗi: message của ngoại lệ mạng có thể chứa link ký (CH-06).
                mediaLog.w { "[Poster] id=$id lỗi sau ${SystemClock.elapsedRealtime() - startedAt}ms: ${e.javaClass.simpleName}" }
                null
            }

    private companion object {
        /** Trên cỡ này (hơi hơn 4K) thì không lấy poster. */
        const val MAX_SOURCE_PIXELS = 4096L * 2304L

        /** Quá hạn này thì bỏ poster của video đó (mở kết nối + giải mã một khung thường dưới vài giây). */
        const val FRAME_TIMEOUT_MS = 10_000L
    }

    private fun Bitmap.scaledToWidth(maxWidthPx: Int): Bitmap {
        if (width <= maxWidthPx) return this
        val height = (height.toLong() * maxWidthPx / width).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(this, maxWidthPx, height, true)
        if (scaled !== this) recycle()
        return scaled
    }
}
