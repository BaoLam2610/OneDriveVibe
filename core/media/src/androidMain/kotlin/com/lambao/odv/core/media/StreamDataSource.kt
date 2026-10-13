@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.core.media

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Địa chỉ mà ExoPlayer thấy: `odv-video://item/{itemId}`, không phải link thật. Nhờ vậy link ký (đổi mỗi giờ, chứa `tempauth`)
 * không bao giờ lọt vào khóa cache hay bị ghi lại; [StreamDataSource] đổi sang link thật ngay lúc mở kết nối.
 */
fun streamUri(itemId: String): Uri =
    Uri.Builder().scheme(MediaConstants.STREAM_SCHEME).authority(MediaConstants.STREAM_AUTHORITY).appendPath(itemId).build()

/**
 * `MediaItem` của một video: địa chỉ ảo kèm khóa cache theo id và `cTag` (BN-02). **Một nơi dựng duy nhất** cho player và `DefaultPreloadManager`
 * (ADR-0028): manager tra nguồn đã tải trước theo `MediaItem.equals`, nên hai bên dựng khác nhau một trường là tải trước vô dụng.
 */
fun streamMediaItem(itemId: String, cTag: String?): MediaItem =
    MediaItem.Builder()
        .setMediaId(itemId)
        .setUri(streamUri(itemId))
        .setCustomCacheKey(videoCacheKey(itemId, cTag))
        .build()

/** Video không còn trên OneDrive. Là `FileNotFoundException` nên ExoPlayer không thử lại vô ích. */
class VideoRemovedException : FileNotFoundException("video removed")

/**
 * Không lấy được link phát (mất mạng, bị khóa PIN...). [error] giữ loại lỗi để phân loại ở [toFailure]; không đưa chi tiết
 * lỗi vào message vì có thể chứa URL (CH-06).
 */
class StreamUrlException(val error: AppError) : IOException("stream url unavailable")

/**
 * Giữ link phát trong bộ nhớ theo từng video và lấy link mới khi cần; không bao giờ ghi link ra đĩa và không ghi link vào log.
 *
 * Màn Xem video: mỗi phiên phát có một bản riêng nên link ký chết theo màn xem. Tab Short: một bản dùng chung ([SharedStreamUrls], ADR-0029) cho bộ
 * tải trước, player cùng manager và poster, xóa một lần khi app bị khóa (CH-03).
 *
 * **Một yêu cầu cho một id** (ADR-0029): nhiều người cùng cần link của một video lúc chưa có thì chỉ một lần gọi Graph chạy, những người còn lại chờ
 * chung kết quả. Việc lấy link chạy trong scope riêng của provider nên người chờ hết hạn hay bị ngắt chỉ bỏ chờ, không hủy việc của người khác.
 * [clear] hủy mọi việc đang chạy và tăng `epoch`, nên một việc chạy dở lúc khóa app không thể ghi lại link sau khi đã xóa.
 */
class StreamUrlProvider(
    private val getStreamUrl: GetStreamUrlUseCase,
    dispatchers: DispatcherProvider,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private class Entry(val url: String, val fetchedAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    /** Việc lấy link đang chạy theo id; người đến sau cùng id thì chờ chung. */
    private val inflight = ConcurrentHashMap<String, Deferred<AppResult<String>>>()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    /** Tăng mỗi lần [clear]; việc lấy link chỉ ghi link nếu `epoch` vẫn như lúc nó bắt đầu. Ghi và xóa cùng khóa [lock] nên không có khe hở giữa kiểm tra và ghi. */
    private val epoch = AtomicInteger(0)
    private val lock = Any()

    /**
     * Quên mọi link đang giữ và hủy các việc lấy link đang chạy. Gọi khi app bị khóa PIN (CH-03): link ký cho phép tải tệp mà không cần token nên
     * không để nó nằm lại trong bộ nhớ sau khi khóa; lần mở kết nối kế tiếp sẽ tự lấy link mới. Người đang chờ một việc bị hủy nhận
     * [StreamUrlException] với [AppError.AppLocked].
     */
    fun clear() {
        val pending: List<Deferred<AppResult<String>>>
        synchronized(lock) {
            epoch.incrementAndGet()
            if (entries.isNotEmpty() || inflight.isNotEmpty()) {
                mediaLog.i { "[Url] xóa ${entries.size} link đã giữ, hủy ${inflight.size} việc đang lấy link" }
            }
            entries.clear()
            pending = inflight.values.toList()
            inflight.clear()
        }
        pending.forEach { it.cancel() }
    }

    /**
     * Link cho [itemId]; [refresh] bỏ qua bản đang giữ. Chặn luồng gọi: ExoPlayer mở DataSource trên luồng tải riêng của
     * nó nên chờ ở đây không đứng giao diện.
     */
    fun urlFor(itemId: String, refresh: Boolean): String {
        val id = itemId.shortId()
        if (!refresh) {
            entries[itemId]?.takeIf { now() - it.fetchedAt < MediaConstants.URL_TTL_MS }?.let {
                mediaLog.d { "[Url] dùng lại link đã giữ id=$id tuổi=${(now() - it.fetchedAt) / 1000}s" }
                return it.url
            }
        }
        val deferred = startOrJoin(itemId, id, refresh)
        // Mỗi người chờ có hạn riêng: Graph bị throttle thì retry có thể chờ rất lâu và giữ luồng tải của ExoPlayer. Hết hạn thì coi như mất mạng
        // (AppError.Timeout → PlayerFailure.Network, có nút Tiếp tục) thay vì treo; việc lấy link vẫn chạy tiếp cho người khác.
        val result = try {
            runBlocking { withTimeoutOrNull(MediaConstants.URL_FETCH_TIMEOUT_MS) { deferred.await() } } ?: AppResult.Failure(AppError.Timeout)
        } catch (e: CancellationException) {
            // Chỉ có clear() (app bị khóa) hủy việc lấy link; ngắt luồng của ExoPlayer ném InterruptedException chứ không vào đây.
            AppResult.Failure(AppError.AppLocked)
        }
        return when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> throw result.error.toIoException()
        }
    }

    /**
     * Việc lấy link của [itemId] đang chạy thì tham gia, chưa có thì tạo mới. [refresh] (VD-14, link cũ bị từ chối) cũng tham gia việc đang chạy:
     * việc đó luôn gọi Graph nên cho ra link mới.
     */
    private fun startOrJoin(itemId: String, id: String, refresh: Boolean): Deferred<AppResult<String>> {
        var created = false
        val deferred = inflight.computeIfAbsent(itemId) {
            created = true
            val startEpoch = epoch.get()
            val job = scope.async(start = CoroutineStart.LAZY) { fetch(itemId, id, refresh, startEpoch) }
            job.invokeOnCompletion { inflight.remove(itemId, job) }
            job
        }
        if (created) deferred.start() else mediaLog.d { "[Url] gộp yêu cầu id=$id vào việc đang lấy link" }
        return deferred
    }

    /** Một lần gọi Graph. Có hạn riêng để không treo mãi khi không còn ai chờ. Ghi link chỉ khi chưa có [clear] nào từ lúc bắt đầu ([epoch]). */
    private suspend fun fetch(itemId: String, id: String, refresh: Boolean, startEpoch: Int): AppResult<String> {
        mediaLog.i { "[Url] lấy link id=$id refresh=$refresh" }
        val startedAt = now()
        val result = withTimeoutOrNull(MediaConstants.URL_FETCH_TIMEOUT_MS) { getStreamUrl(itemId) } ?: AppResult.Failure(AppError.Timeout)
        when (result) {
            is AppResult.Success -> {
                mediaLog.i { "[Url] có link id=$id sau ${now() - startedAt}ms (độ dài ${result.value.length})" }
                synchronized(lock) {
                    if (epoch.get() == startEpoch) entries[itemId] = Entry(result.value, now())
                }
            }
            is AppResult.Failure -> mediaLog.w { "[Url] lỗi lấy link id=$id sau ${now() - startedAt}ms: ${result.error.describe()}" }
        }
        return result
    }

    private fun AppError.toIoException(): IOException =
        if (this is AppError.Http && status in MediaConstants.REMOVED_STATUSES) VideoRemovedException() else StreamUrlException(this)
}

/** Mô tả [AppError] cho log mà không lộ chi tiết nhạy cảm: [AppError.Unknown] chỉ ghi tên lớp của nguyên nhân. */
internal fun AppError.describe(): String = when (this) {
    is AppError.Unknown -> "Unknown(${cause?.javaClass?.simpleName})"
    else -> toString()
}

/**
 * Đổi `odv-video://item/{id}` thành link thật mỗi lần mở kết nối rồi mở bằng [upstream] (HTTP thường, **không** gửi
 * `Authorization`: link đã ký sẵn, bearer không được rời Graph). Gặp 401/403 thì coi link hết hạn, lấy link mới và mở lại
 * một lần (VD-14): ExoPlayer không biết gì, không dựng lại player, không mất vị trí.
 *
 * [getUri] cố ý trả địa chỉ ảo chứ không phải link thật: `CacheDataSource` ghi lại địa chỉ chuyển hướng vào chỉ mục cache
 * trên đĩa, mà link thật chứa `tempauth` nên không được để nó nằm lại đó.
 */
class StreamDataSource(
    private val upstream: DataSource,
    private val urls: StreamUrlProvider,
) : DataSource {

    private var openedUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        val itemId = dataSpec.uri.lastPathSegment ?: run {
            mediaLog.e { "[Source] địa chỉ ảo không có id: scheme=${dataSpec.uri.scheme}" }
            throw VideoRemovedException()
        }
        val id = itemId.shortId()
        mediaLog.d { "[Source] mở id=$id vị trí=${dataSpec.position} độ dài=${dataSpec.length}" }
        val length = try {
            openWith(dataSpec, itemId, refresh = false)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            mediaLog.w { "[Source] id=$id HTTP ${e.responseCode}" }
            if (e.responseCode !in MediaConstants.EXPIRED_STATUSES) throw e
            mediaLog.i { "[Source] id=$id link hết hạn (HTTP ${e.responseCode}), lấy link mới và mở lại" }
            runCatching { upstream.close() }
            openWith(dataSpec, itemId, refresh = true)
        } catch (e: IOException) {
            // Lỗi mạng/lấy link: chỉ ghi tên lớp, không ghi message (có thể chứa URL).
            mediaLog.w { "[Source] id=$id mở thất bại: ${e.javaClass.simpleName}" }
            throw e
        }
        mediaLog.d { "[Source] id=$id đã mở, độ dài còn lại=$length" }
        openedUri = dataSpec.uri
        return length
    }

    private fun openWith(dataSpec: DataSpec, itemId: String, refresh: Boolean): Long =
        upstream.open(dataSpec.withUri(Uri.parse(urls.urlFor(itemId, refresh))))

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = openedUri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        try {
            upstream.close()
        } finally {
            openedUri = null
        }
    }
}

class StreamDataSourceFactory(
    private val http: DataSource.Factory,
    private val urls: StreamUrlProvider,
) : DataSource.Factory {
    override fun createDataSource(): DataSource = StreamDataSource(http.createDataSource(), urls)
}
