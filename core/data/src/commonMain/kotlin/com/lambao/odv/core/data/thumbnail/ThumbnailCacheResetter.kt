package com.lambao.odv.core.data.thumbnail

import co.touchlab.kermit.Logger
import coil3.ImageLoader
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.hook.ThumbnailCache
import com.lambao.odv.core.domain.model.CacheKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Xóa cache thumbnail (đĩa và bộ nhớ) khi ngắt kết nối (CD-05, KH-03, KH-06): thumbnail là ảnh của người dùng nên không
 * được còn lại sau khi xóa dữ liệu. Đăng ký ở Koin như một [ConnectionResetter] để `DisconnectUseCase` tự gọi. Cũng là [CacheStore] loại thumbnail để Cài đặt đo, xóa và dọn theo giới hạn (CD-07).
 * Nuốt lỗi và chỉ ghi log để không chặn các bản cài khác (hợp đồng của [ConnectionResetter]).
 */
internal class ThumbnailCacheResetter(
    private val imageLoader: ImageLoader,
    private val dispatchers: DispatcherProvider,
) : ConnectionResetter, ThumbnailCache, CacheStore {

    override val kind: CacheKind = CacheKind.Thumbnail

    override suspend fun usedBytes(): Long = imageLoader.diskCache?.size ?: 0L

    /**
     * Coil không dọn chọn lọc được và chỉ nhận trần lúc tạo `DiskCache`, nên giảm trần thì xóa hết thumbnail nếu đang vượt (thumbnail tải
     * lại khi cuộn tới). Trần mới của Coil có hiệu lực từ lần mở app sau; giữa chừng cache có thể lớn hơn trần mới một chút khi tải thêm.
     */
    override suspend fun trimTo(maxBytes: Long) {
        if (usedBytes() > maxBytes) clear()
    }

    private val log = Logger.withTag("ThumbnailCache")

    override suspend fun reset() = clear()

    override suspend fun clear() {
        // Trước khi xóa: tải đang bay thấy thế hệ đổi và không ghi lại ảnh cũ sau khi cache đã dọn.
        ThumbnailCacheGeneration.advance()
        try {
            imageLoader.memoryCache?.clear()
            withContext(dispatchers.io) { imageLoader.diskCache?.clear() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Không xóa được cache thumbnail" }
        }
    }
}
