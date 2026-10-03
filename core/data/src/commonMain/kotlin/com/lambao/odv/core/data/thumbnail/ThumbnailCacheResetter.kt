package com.lambao.odv.core.data.thumbnail

import co.touchlab.kermit.Logger
import coil3.ImageLoader
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.repository.ConnectionResetter
import com.lambao.odv.core.domain.repository.ThumbnailCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Xóa cache thumbnail (đĩa và bộ nhớ) khi ngắt kết nối (CD-05, KH-03, KH-06): thumbnail là ảnh của người dùng nên không
 * được còn lại sau khi xóa dữ liệu. Đăng ký ở Koin như một [ConnectionResetter] để `DisconnectUseCase` tự gọi.
 * Nuốt lỗi và chỉ ghi log để không chặn các bản cài khác (hợp đồng của [ConnectionResetter]).
 */
internal class ThumbnailCacheResetter(
    private val imageLoader: ImageLoader,
    private val dispatchers: DispatcherProvider,
) : ConnectionResetter, ThumbnailCache {

    private val log = Logger.withTag("ThumbnailCache")

    override suspend fun reset() = clear()

    override suspend fun clear() {
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
