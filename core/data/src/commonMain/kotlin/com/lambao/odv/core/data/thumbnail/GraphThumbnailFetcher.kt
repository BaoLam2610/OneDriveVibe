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
import com.lambao.odv.core.domain.repository.ThumbnailQuality
import com.lambao.odv.core.network.GraphApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer

/** Số thumbnail tải song song tối đa: lưới Thư viện bật hàng chục ô cùng lúc, không nên mở hàng chục kết nối. */
private const val MAX_PARALLEL_DOWNLOADS = 6

/** Cỡ thumbnail Graph có sẵn, dùng khi cỡ tùy chỉnh bị từ chối (HTTP 400). */
private const val FALLBACK_SIZE = "medium"

// Chất lượng trung bình có chủ ý: đủ rõ trên lưới mà nhẹ và nhanh (người dùng không cần cao). Ô Thư viện rộng khoảng
// 90dp (~250px ở xxhdpi) nên 240 hơi mềm nhưng chấp nhận được; thẻ Thư mục 2 cột rộng khoảng 160dp.
private const val MIN_SIDE = 60
private const val MAX_SIDE = 1600

private fun scaled(base: Int, scalePercent: Int): Int = (base * scalePercent / 100).coerceIn(MIN_SIDE, MAX_SIDE)

/** Cỡ tùy chỉnh của Graph (`c{rộng}x{cao}_crop`) sau khi nhân [scalePercent] với cỡ mặc định của từng loại ô. */
internal fun ThumbnailSize.graphSize(scalePercent: Int): String = when (this) {
    ThumbnailSize.Cell -> scaled(240, scalePercent).let { "c${it}x${it}_crop" }
    ThumbnailSize.Card -> "c${scaled(360, scalePercent)}x${scaled(270, scalePercent)}_crop"
}

/**
 * Khóa cache của một thumbnail (BN-02): id tệp + `cTag` + cỡ thực. Đổi nội dung thì `cTag` đổi nên khóa đổi và bản cũ không
 * còn được dùng (nó nằm lại đến khi bị đẩy ra theo LRU, BN-01); đổi tên hay di chuyển không đổi khóa nên dùng lại cache.
 * Có cỡ thực để đổi tỉ lệ chất lượng thì tải bản mới thay vì dùng nhầm bản cỡ cũ.
 */
internal fun thumbnailCacheKey(source: ThumbnailSource, scalePercent: Int): String =
    "thumb:${source.itemId}:${source.cTag ?: "-"}:${source.size.graphSize(scalePercent)}"

/**
 * Thế hệ của cache thumbnail, tăng mỗi lần cache bị xóa. Fetcher ghi nhớ giá trị lúc bắt đầu và bỏ ghi nếu nó đã đổi, nên
 * request đang bay (hoặc đang chờ lượt tải) khi ngắt kết nối không ghi ảnh cũ vào cache vừa dọn. Không phụ thuộc thứ tự
 * `reset()` và `wipe()` của `DisconnectUseCase`. Một cache cho cả tiến trình nên dùng object.
 */
internal object ThumbnailCacheGeneration {
    @Volatile
    var value: Int = 0
        private set

    fun advance() {
        value++
    }
}

/** Bản phát hành: luôn 100%, không có cách chỉnh. Bản debug ghi đè ở `DebugTools` (xem `ThumbnailQuality`). */
internal object FixedThumbnailQuality : ThumbnailQuality {
    override fun scalePercent(): Int = 100
}

internal class ThumbnailKeyer(private val quality: ThumbnailQuality) : Keyer<ThumbnailSource> {
    override fun key(data: ThumbnailSource, options: Options): String = thumbnailCacheKey(data, quality.scalePercent())
}

/** Không có thumbnail để hiện (tệp không có, offline và chưa cache...). Giao diện giữ ô giữ chỗ. Không mang nội dung nhạy cảm. */
internal class ThumbnailUnavailableException(error: AppError) : Exception("Thumbnail unavailable: ${error::class.simpleName}")

internal class GraphThumbnailFetcherFactory(
    private val api: GraphApi,
    private val configs: ConfigRepository,
    private val quality: ThumbnailQuality,
) : Fetcher.Factory<ThumbnailSource> {

    private val gate = Semaphore(MAX_PARALLEL_DOWNLOADS)

    override fun create(data: ThumbnailSource, options: Options, imageLoader: ImageLoader): Fetcher =
        GraphThumbnailFetcher(data, quality.scalePercent(), api, configs, imageLoader.diskCache, options, gate)
}

/**
 * Tải thumbnail từ Graph cho Coil (BN-01 → BN-03). Coil không tự cache đĩa cho Fetcher tự viết nên lớp này đọc/ghi
 * [DiskCache] của ImageLoader (giới hạn dung lượng và xóa tệp lâu không dùng nhất do chính DiskCache lo, BN-01). Thumbnail
 * nhỏ nên tải lại từ đầu khi bị gián đoạn; việc tải tiếp phần dở dành cho ảnh gốc và PDF (BN-03, Lát 5 và 7).
 */
internal class GraphThumbnailFetcher(
    private val source: ThumbnailSource,
    private val scalePercent: Int,
    private val api: GraphApi,
    private val configs: ConfigRepository,
    private val diskCache: DiskCache?,
    private val options: Options,
    private val gate: Semaphore,
) : Fetcher {

    private val key = thumbnailCacheKey(source, scalePercent)

    override suspend fun fetch(): FetchResult {
        val generation = ThumbnailCacheGeneration.value
        diskCache?.openSnapshot(key)?.let { return it.toResult() }
        val bytes = gate.withPermit {
            // Một ô khác có thể vừa tải xong cùng thumbnail trong lúc chờ lượt.
            diskCache?.openSnapshot(key)?.let { return it.toResult() }
            download()
        }
        // Cache vừa bị xóa (ngắt kết nối, xóa tay) trong lúc đang tải: bỏ ảnh này, không ghi lại ảnh của tài khoản cũ (CD-05).
        if (ThumbnailCacheGeneration.value != generation) throw ThumbnailUnavailableException(AppError.AppLocked)
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
        var result = api.fetchThumbnail(credentials, source.itemId, source.size.graphSize(scalePercent))
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
