package com.lambao.odv.feature.pdfviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Kết quả mở tệp PDF đã tải về bằng `PdfRenderer` (PD-07: có mật khẩu hoặc hỏng thì [Failed]). */
internal sealed interface PdfOpenState {
    data object Loading : PdfOpenState
    data object Failed : PdfOpenState
    data class Opened(val document: PdfDocument) : PdfOpenState
}

/**
 * Một tệp PDF đang mở bằng `PdfRenderer` của hệ thống (không thêm thư viện, tech-stack.md). `PdfRenderer` chỉ cho mở **một trang tại một
 * lúc** nên mọi lệnh dựng đi qua [lock] và chạy tuần tự trên luồng I/O; đợi khóa là điểm hủy được, nên cuộn nhanh thì các trang đã trôi qua
 * bị hủy trước khi kịp dựng. Bitmap các trang đã dựng nằm trong [LruCache] giới hạn theo byte.
 *
 * Tỉ lệ cao/rộng của mỗi trang chỉ biết khi trang đó được mở nên các trang chưa dựng dùng tỉ lệ của trang đầu ([aspectOrDefault]); tệp có
 * trang khác khổ thì danh sách cuộn dọc nhích nhẹ khi trang thật được dựng (giới hạn đã biết của bản này, quét trước mọi trang là quá nặng
 * với tệp hàng nghìn trang).
 */
internal class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    val pageCount: Int = renderer.pageCount

    private data class Key(val index: Int, val maxWidth: Int, val maxHeight: Int)

    private val lock = Mutex()
    private var closed = false
    private val aspects = ConcurrentHashMap<Int, Float>()

    @Volatile
    private var defaultAspect = A4_ASPECT

    private val cache = object : LruCache<Key, Bitmap>(cacheBytes()) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.byteCount
    }

    /** Cao / rộng của trang [index]: tỉ lệ thật nếu trang đã được mở, không thì tỉ lệ trang đầu. */
    fun aspectOrDefault(index: Int): Float = aspects[index] ?: defaultAspect

    /** Bitmap đã dựng sẵn (đồng bộ, không dựng mới), hoặc null. */
    fun cached(index: Int, maxWidthPx: Int, maxHeightPx: Int?): Bitmap? = cache.get(Key(index, maxWidthPx, maxHeightPx ?: 0))

    /**
     * Dựng trang [index] vừa trong [maxWidthPx] × [maxHeightPx] (null là chỉ giới hạn chiều rộng, dùng cho cuộn dọc). Trả null nếu tệp đã
     * đóng, hết bộ nhớ hoặc `PdfRenderer` báo lỗi; trang đó để trắng. Có thể hủy khi đang đợi khóa.
     */
    suspend fun render(
        index: Int,
        maxWidthPx: Int,
        maxHeightPx: Int?,
        maxBytes: Int = PdfViewerConstants.MAX_PAGE_BITMAP_BYTES,
    ): Bitmap? {
        if (index !in 0 until pageCount || maxWidthPx <= 0) return null
        val key = Key(index, maxWidthPx, maxHeightPx ?: 0)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            lock.withLock {
                if (closed) return@withLock null
                cache.get(key)?.let { return@withLock it }
                try {
                    renderer.openPage(index).use { page ->
                        if (page.width <= 0 || page.height <= 0) return@use null
                        val aspect = page.height.toFloat() / page.width
                        aspects[index] = aspect
                        var width = maxWidthPx
                        if (maxHeightPx != null) width = minOf(width, (maxHeightPx / aspect).toInt())
                        width = width.coerceAtLeast(1)
                        var height = (width * aspect).roundToInt().coerceAtLeast(1)
                        val bytes = width.toLong() * height * BYTES_PER_PIXEL
                        // Trần còn bị chặn theo heap của máy: đang zoom có thể giữ vài trang cùng lúc nên heap nhỏ không chịu nổi 40 MB mỗi trang.
                        val limit = minOf(maxBytes.toLong(), Runtime.getRuntime().maxMemory() / PdfViewerConstants.PAGE_BITMAP_HEAP_DIVISOR)
                        if (bytes > limit) {
                            // Trang khổ rất lớn hoặc đang zoom: thu nhỏ theo tỉ lệ để bitmap không vượt trần.
                            val scale = sqrt(limit.toDouble() / bytes)
                            width = (width * scale).toInt().coerceAtLeast(1)
                            height = (height * scale).toInt().coerceAtLeast(1)
                        }
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // PdfRenderer chỉ vẽ nội dung, nền trang trong suốt nên tô trắng trước.
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cache.put(key, bitmap)
                        bitmap
                    }
                } catch (e: OutOfMemoryError) {
                    cache.evictAll()
                    null
                } catch (e: Exception) {
                    // Trang lỗi chỉ để trắng; không có suspend trong khối này nên không nuốt nhầm CancellationException.
                    null
                }
            }
        }
    }

    /**
     * Đóng tệp sau khi lệnh dựng đang chạy (nếu có) xong. Gọi từ chỗ không suspend được (rời composition), nên chạy trên scope riêng; sau khi
     * đóng, [render] trả null.
     */
    fun closeAsync() {
        closeScope.launch {
            lock.withLock {
                if (closed) return@withLock
                closed = true
                runCatching { renderer.close() }
                runCatching { descriptor.close() }
            }
            cache.evictAll()
        }
    }

    private fun measureFirstPage() {
        renderer.openPage(0).use { page ->
            if (page.width > 0 && page.height > 0) {
                val aspect = page.height.toFloat() / page.width
                defaultAspect = aspect
                aspects[0] = aspect
            }
        }
    }

    companion object {
        private const val A4_ASPECT = 1.4142f
        private const val BYTES_PER_PIXEL = 4
        private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun cacheBytes(): Int =
            (Runtime.getRuntime().maxMemory() / PdfViewerConstants.PAGE_CACHE_MEMORY_DIVISOR)
                .coerceIn(PdfViewerConstants.MAX_ZOOMED_PAGE_BITMAP_BYTES.toLong(), Int.MAX_VALUE.toLong()).toInt()

        /** Chặn luồng, gọi trên luồng I/O. Null nếu không mở được (có mật khẩu, hỏng, không có trang, PD-07). */
        fun open(path: String): PdfDocument? {
            var descriptor: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            return try {
                descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(descriptor)
                if (renderer.pageCount <= 0) {
                    cleanUp(renderer, descriptor)
                    return null
                }
                PdfDocument(descriptor, renderer).also { it.measureFirstPage() }
            } catch (e: Exception) {
                // IOException (hỏng), SecurityException (có mật khẩu), IllegalArgumentException... đều là "không mở được" (PD-07).
                cleanUp(renderer, descriptor)
                null
            }
        }

        private fun cleanUp(renderer: PdfRenderer?, descriptor: ParcelFileDescriptor?) {
            runCatching { renderer?.close() }
            runCatching { descriptor?.close() }
        }
    }
}

/**
 * Mở tệp [path] khi sẵn sàng và đóng khi rời composition. Mở chạy trên luồng I/O; nếu composition bị hủy giữa chừng thì đóng ngay tệp vừa mở
 * để không rò descriptor.
 */
@Composable
internal fun rememberPdfDocument(path: String): PdfOpenState {
    val state by produceState<PdfOpenState>(PdfOpenState.Loading, path) {
        val document = withContext(Dispatchers.IO) { PdfDocument.open(path) }
        try {
            ensureActive()
        } catch (e: Exception) {
            document?.closeAsync()
            throw e
        }
        value = if (document != null) PdfOpenState.Opened(document) else PdfOpenState.Failed
        awaitDispose { document?.closeAsync() }
    }
    return state
}
