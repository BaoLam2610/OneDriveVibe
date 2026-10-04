@file:OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.VideoStreamRepository
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Địa chỉ mà ExoPlayer thấy: `odv-video://item/{itemId}`, không phải link thật. Nhờ vậy link ký (đổi mỗi giờ, chứa `tempauth`)
 * không bao giờ lọt vào khóa cache hay bị ghi lại; [StreamDataSource] đổi sang link thật ngay lúc mở kết nối.
 */
internal fun streamUri(itemId: String): Uri =
    Uri.Builder().scheme(PlayerConstants.STREAM_SCHEME).authority(PlayerConstants.STREAM_AUTHORITY).appendPath(itemId).build()

/** Video không còn trên OneDrive. Là `FileNotFoundException` nên ExoPlayer không thử lại vô ích. */
internal class VideoRemovedException : FileNotFoundException("video removed")

/**
 * Không lấy được link phát (mất mạng, bị khóa PIN...). [error] giữ loại lỗi để phân loại ở [toFailure]; không đưa chi tiết
 * lỗi vào message vì có thể chứa URL (CH-06).
 */
internal class StreamUrlException(val error: AppError) : IOException("stream url unavailable")

/**
 * Giữ link phát trong bộ nhớ theo từng video và lấy link mới khi cần. Mỗi phiên phát có một bản riêng, nên link ký chết
 * theo màn xem (không sống tiếp khi app bị khóa PIN, CH-03) và không bao giờ ghi ra đĩa. Không ghi link vào log.
 */
internal class StreamUrlProvider(
    private val streams: VideoStreamRepository,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private class Entry(val url: String, val fetchedAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    /**
     * Link cho [itemId]; [refresh] bỏ qua bản đang giữ. Chặn luồng gọi: ExoPlayer mở DataSource trên luồng tải riêng của
     * nó nên chờ ở đây không đứng giao diện.
     */
    fun urlFor(itemId: String, refresh: Boolean): String {
        val id = itemId.shortId()
        if (!refresh) {
            entries[itemId]?.takeIf { now() - it.fetchedAt < PlayerConstants.URL_TTL_MS }?.let {
                playerLog.d { "[Url] dùng lại link đã giữ id=$id tuổi=${(now() - it.fetchedAt) / 1000}s" }
                return it.url
            }
        }
        playerLog.i { "[Url] lấy link id=$id refresh=$refresh" }
        val startedAt = now()
        return when (val result = runBlocking { streams.streamUrl(itemId) }) {
            is AppResult.Success -> {
                playerLog.i { "[Url] có link id=$id sau ${now() - startedAt}ms (độ dài ${result.value.length})" }
                result.value.also { entries[itemId] = Entry(it, now()) }
            }
            is AppResult.Failure -> {
                playerLog.w { "[Url] lỗi lấy link id=$id sau ${now() - startedAt}ms: ${result.error.describe()}" }
                throw result.error.toIoException()
            }
        }
    }

    private fun AppError.toIoException(): IOException =
        if (this is AppError.Http && status in PlayerConstants.REMOVED_STATUSES) VideoRemovedException() else StreamUrlException(this)
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
internal class StreamDataSource(
    private val upstream: DataSource,
    private val urls: StreamUrlProvider,
) : DataSource {

    private var openedUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        val itemId = dataSpec.uri.lastPathSegment ?: run {
            playerLog.e { "[Source] địa chỉ ảo không có id: scheme=${dataSpec.uri.scheme}" }
            throw VideoRemovedException()
        }
        val id = itemId.shortId()
        playerLog.d { "[Source] mở id=$id vị trí=${dataSpec.position} độ dài=${dataSpec.length}" }
        val length = try {
            openWith(dataSpec, itemId, refresh = false)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            playerLog.w { "[Source] id=$id HTTP ${e.responseCode}" }
            if (e.responseCode !in PlayerConstants.EXPIRED_STATUSES) throw e
            playerLog.i { "[Source] id=$id link hết hạn (HTTP ${e.responseCode}), lấy link mới và mở lại" }
            runCatching { upstream.close() }
            openWith(dataSpec, itemId, refresh = true)
        } catch (e: IOException) {
            // Lỗi mạng/lấy link: chỉ ghi tên lớp, không ghi message (có thể chứa URL).
            playerLog.w { "[Source] id=$id mở thất bại: ${e.javaClass.simpleName}" }
            throw e
        }
        playerLog.d { "[Source] id=$id đã mở, độ dài còn lại=$length" }
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

internal class StreamDataSourceFactory(
    private val http: DataSource.Factory,
    private val urls: StreamUrlProvider,
) : DataSource.Factory {
    override fun createDataSource(): DataSource = StreamDataSource(http.createDataSource(), urls)
}
