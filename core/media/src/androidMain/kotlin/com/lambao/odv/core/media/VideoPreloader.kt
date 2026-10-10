@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.core.media

import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Một video cần tải trước đầu tệp. [sizeBytes] chặn số byte tải (tệp nhỏ hơn mức tải trước thì chỉ tải chừng đó); [bitRateBps] (bit/giây, từ
 * Graph) để ước lượng số byte tương ứng vài giây đầu, null thì dùng mức mặc định.
 */
class PreloadTarget(
    val itemId: String,
    val cTag: String?,
    val sizeBytes: Long,
    val bitRateBps: Long?,
)

/**
 * Tải trước đoạn đầu của các video kế tiếp vào **cache video chung** ([VideoCache]) để vuốt sang là phát gần như ngay (SV-13, ADR-0026). Dùng
 * `CacheWriter` ghi qua đúng chuỗi nguồn dữ liệu và khóa cache ([videoCacheKey]) mà player dùng, nên khi player nạp video đó, phần đầu đã có sẵn
 * trong cache và link ký được lấy trước; phần tải trước tính vào trần cache video (BN-01) và phát lại được khi offline.
 *
 * Thay cho `DefaultPreloadManager` của ADR-0024 mục 1: manager đó đòi dựng player qua `DefaultPreloadManager.Builder` và đưa `MediaSource` theo từng
 * mục, buộc viết lại cách dựng player đang dùng chung với màn Xem video; `CacheWriter` đạt cùng mục đích tải trước (byte và link) mà không đụng tới player.
 * Chưa chuẩn bị track hay giải mã sẵn. Nơi gọi tự kiểm tra mạng trước khi gọi [update] (SV-13: không tải trước khi mất mạng).
 *
 * Link ký lấy qua [StreamUrlProvider] riêng của lớp này, nên [cancelAll] vừa dừng tải vừa xóa link (CH-03, khi app khóa). Mọi hàm gọi được từ bất kỳ luồng.
 */
class VideoPreloader(
    private val videoCache: VideoCache,
    getStreamUrl: GetStreamUrlUseCase,
    dispatchers: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val urls = StreamUrlProvider(getStreamUrl)
    private val lock = Any()

    /** Việc tải trước theo id video. Giữ cả việc đã xong để khỏi tải lại khi video vẫn là hàng xóm; việc lỗi thì tự gỡ để lần sau thử lại. */
    private val jobs = HashMap<String, Job>()

    /**
     * Đặt tập video cần tải trước là [targets], theo thứ tự ưu tiên: dừng việc của video không còn nằm trong đó, bắt đầu việc cho video mới.
     * Gọi lại với cùng tập thì không làm gì.
     */
    fun update(targets: List<PreloadTarget>) {
        synchronized(lock) {
            val keep = targets.mapTo(HashSet()) { it.itemId }
            val iterator = jobs.entries.iterator()
            while (iterator.hasNext()) {
                val (id, job) = iterator.next()
                if (id !in keep) {
                    mediaLog.d { "[Preload] bỏ id=${id.shortId()} (không còn là video kế cận)" }
                    job.cancel()
                    iterator.remove()
                }
            }
            for (target in targets) {
                if (target.itemId !in jobs) jobs[target.itemId] = start(target)
            }
        }
    }

    /**
     * Dừng mọi việc tải trước và xóa link ký đang giữ (mất mạng, app bị khóa, Màn chính bị bỏ; CH-03). Rời tab Short thì **không** gọi: việc đang
     * tải chạy nốt (tối đa 4 MB mỗi video). Một việc đang chờ lấy link lúc gọi vẫn có thể ghi lại một link vào bộ nhớ sau khi xóa (TTL 45 phút,
     * không ghi đĩa); chưa chặn vì cửa sổ hẹp, ghi ở review 2026-10-11.
     */
    fun cancelAll() {
        synchronized(lock) {
            if (jobs.isNotEmpty()) mediaLog.i { "[Preload] dừng ${jobs.size} việc tải trước" }
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }
        urls.clear()
    }

    private fun start(target: PreloadTarget): Job = scope.launch {
        val id = target.itemId.shortId()
        val length = preloadLength(target)
        val startedAt = SystemClock.elapsedRealtime()
        mediaLog.i { "[Preload] bắt đầu id=$id tải $length byte (tệp ${target.sizeBytes} byte, bitrate=${target.bitRateBps})" }
        try {
            val cache = videoCache.get()
            val http = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(MediaConstants.HTTP_TIMEOUT_MS)
                .setReadTimeoutMs(MediaConstants.HTTP_TIMEOUT_MS)
            val dataSource = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(StreamDataSourceFactory(http, urls))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource()
            val dataSpec = DataSpec.Builder()
                .setUri(streamUri(target.itemId))
                .setKey(videoCacheKey(target.itemId, target.cTag))
                .setPosition(0L)
                .setLength(length)
                .build()
            val writer = CacheWriter(dataSource, dataSpec, null, null)
            coroutineScope {
                // CacheWriter.cache() chặn luồng và không biết hủy coroutine: một coroutine canh gọi cancel() khi bị hủy để cache() thoát.
                // UNDISPATCHED: phải chạy tới awaitCancellation() ngay, nếu không job cha bị hủy trước khi coroutine này bắt đầu thì `finally` không
                // bao giờ chạy và cache() tiếp tục tải tới hết (review 2026-10-11).
                val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        writer.cancel()
                    }
                }
                try {
                    writer.cache()
                } finally {
                    watcher.cancel()
                }
            }
            mediaLog.i { "[Preload] xong id=$id sau ${SystemClock.elapsedRealtime() - startedAt}ms" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Bị hủy giữa chừng thì writer.cancel() làm cache() ném IOException: không phải lỗi thật. Chỉ ghi tên lớp lỗi (CH-06).
            if (currentCoroutineContext().isActive) {
                mediaLog.w { "[Preload] lỗi id=$id sau ${SystemClock.elapsedRealtime() - startedAt}ms: ${e.javaClass.simpleName}" }
                synchronized(lock) { jobs.remove(target.itemId) }
            }
        }
    }

    /** Số byte đầu cần tải: chừng [PRELOAD_SECONDS] giây theo bitrate, chặn trong [MIN_PRELOAD_BYTES]..[MAX_PRELOAD_BYTES] và không vượt kích thước tệp. */
    private fun preloadLength(target: PreloadTarget): Long {
        val estimated = target.bitRateBps?.takeIf { it > 0L }?.let { it / BITS_PER_BYTE * PRELOAD_SECONDS } ?: DEFAULT_PRELOAD_BYTES
        val limited = estimated.coerceIn(MIN_PRELOAD_BYTES, MAX_PRELOAD_BYTES)
        return if (target.sizeBytes > 0L) minOf(limited, target.sizeBytes) else limited
    }

    private companion object {
        /** ADR-0024 ước 3 đến 5 giây đầu, chốt khi thử máy. */
        const val PRELOAD_SECONDS = 4L
        const val BITS_PER_BYTE = 8L
        const val DEFAULT_PRELOAD_BYTES = 1_500_000L
        const val MIN_PRELOAD_BYTES = 512L * 1024
        const val MAX_PRELOAD_BYTES = 4L * 1024 * 1024
    }
}
