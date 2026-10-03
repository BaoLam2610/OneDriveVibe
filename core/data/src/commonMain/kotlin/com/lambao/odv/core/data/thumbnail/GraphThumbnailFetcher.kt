package com.lambao.odv.core.data.thumbnail

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.data.drive.toCredentials
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.ThumbnailSource
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.network.GraphApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer

/** Số thumbnail tải song song tối đa: lưới Thư viện bật hàng chục ô cùng lúc, không nên mở hàng chục kết nối. */
private const val MAX_PARALLEL_DOWNLOADS = 6

/** Cỡ thumbnail Graph có sẵn, dùng khi cỡ tùy chỉnh bị từ chối (HTTP 400). */
private const val FALLBACK_SIZE = "medium"

private fun ThumbnailSize.graphSize(): String = when (this) {
    ThumbnailSize.Cell -> "c300x300_crop"
    ThumbnailSize.Card -> "c480x360_crop"
}

/**
 * Khóa cache của một thumbnail (BN-02): id tệp + `cTag` + cỡ. Đổi nội dung thì `cTag` đổi nên khóa đổi và bản cũ không
 * còn được dùng (nó nằm lại đến khi bị đẩy ra theo LRU, BN-01); đổi tên hay di chuyển không đổi khóa nên dùng lại cache.
 */
internal fun thumbnailCacheKey(source: ThumbnailSource): String =
    "thumb:${source.itemId}:${source.cTag ?: "-"}:${source.size.name}"

internal object ThumbnailKeyer : Keyer<ThumbnailSource> {
    override fun key(data: ThumbnailSource, options: Options): String = thumbnailCacheKey(data)
}

/** Không có thumbnail để hiện (tệp không có, offline và chưa cache...). Giao diện giữ ô giữ chỗ. Không mang nội dung nhạy cảm. */
internal class ThumbnailUnavailableException(error: AppError) : Exception("Thumbnail unavailable: ${error::class.simpleName}")

internal class GraphThumbnailFetcherFactory(
    private val api: GraphApi,
    private val configs: ConfigRepository,
) : Fetcher.Factory<ThumbnailSource> {

    private val gate = Semaphore(MAX_PARALLEL_DOWNLOADS)

    override fun create(data: ThumbnailSource, options: Options, imageLoader: ImageLoader): Fetcher =
        GraphThumbnailFetcher(data, api, configs, imageLoader.diskCache, options, gate)
}

/**
 * Tải thumbnail từ Graph cho Coil (BN-01 → BN-03). Coil không tự cache đĩa cho Fetcher tự viết nên lớp này đọc/ghi
 * [DiskCache] của ImageLoader (giới hạn dung lượng và xóa tệp lâu không dùng nhất do chính DiskCache lo, BN-01). Thumbnail
 * nhỏ nên tải lại từ đầu khi bị gián đoạn; việc tải tiếp phần dở dành cho ảnh gốc và PDF (BN-03, Lát 5 và 7).
 */
internal class GraphThumbnailFetcher(
    private val source: ThumbnailSource,
    private val api: GraphApi,
    private val configs: ConfigRepository,
    private val diskCache: DiskCache?,
    private val options: Options,
    private val gate: Semaphore,
) : Fetcher {

    private val key = thumbnailCacheKey(source)

    override suspend fun fetch(): FetchResult {
        diskCache?.openSnapshot(key)?.let { return it.toResult() }
        val bytes = gate.withPermit {
            // Một ô khác có thể vừa tải xong cùng thumbnail trong lúc chờ lượt.
            diskCache?.openSnapshot(key)?.let { return it.toResult() }
            download()
        }
        // Ngắt kết nối có thể chạy xong trong lúc đang tải: config đã bị xóa thì không ghi ảnh của tài khoản cũ vào cache (CD-05).
        if (configs.load() !is AppResult.Success) throw ThumbnailUnavailableException(AppError.AppLocked)
        return store(bytes) ?: SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = null,
            dataSource = DataSource.NETWORK,
        )
    }

    private suspend fun download(): ByteArray {
        val credentials = when (val loaded = configs.load()) {
            is AppResult.Success -> loaded.value.toCredentials()
            is AppResult.Failure -> throw ThumbnailUnavailableException(loaded.error)
        }
        var result = api.fetchThumbnail(credentials, source.itemId, source.size.graphSize())
        val error = (result as? AppResult.Failure)?.error
        if (error is AppError.Http && error.status == 400) {
            result = api.fetchThumbnail(credentials, source.itemId, FALLBACK_SIZE)
        }
        return when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> throw ThumbnailUnavailableException(result.error)
        }
    }

    /** Ghi [bytes] vào đĩa rồi mở lại làm nguồn; lỗi ghi thì bỏ qua cache, vẫn hiện ảnh vừa tải. */
    private fun store(bytes: ByteArray): FetchResult? {
        val cache = diskCache ?: return null
        val editor = cache.openEditor(key) ?: return null
        return try {
            cache.fileSystem.write(editor.data) { write(bytes) }
            editor.commitAndOpenSnapshot()?.toResult(DataSource.NETWORK)
        } catch (e: CancellationException) {
            runCatching { editor.abort() }
            throw e
        } catch (e: Exception) {
            runCatching { editor.abort() }
            null
        }
    }

    private fun DiskCache.Snapshot.toResult(dataSource: DataSource = DataSource.DISK): FetchResult {
        val cache = checkNotNull(diskCache)
        return SourceFetchResult(
            source = ImageSource(file = data, fileSystem = cache.fileSystem, diskCacheKey = key, closeable = this),
            mimeType = null,
            dataSource = dataSource,
        )
    }
}
