package com.lambao.odv.core.data.original

import com.lambao.odv.core.data.file.ResumableFileStore
import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import com.lambao.odv.core.network.graph.GraphApi
import kotlinx.coroutines.flow.Flow

/**
 * Kho ảnh gốc (AN-01): chỉ biết cách tải một ảnh từ Graph; việc lưu tệp, tải tiếp, LRU và xóa sạch khi ngắt kết nối (CD-05)
 * nằm ở [ResumableFileStore] dùng chung với PDF. Cũng là [CacheStore] loại ảnh để Cài đặt đo, xóa và dọn theo giới hạn (CD-07).
 */
internal class FileOriginalImageRepository(
    private val api: GraphApi,
    private val store: ResumableFileStore,
) : OriginalImageRepository, ConnectionResetter by store, CacheStore {

    override val kind: CacheKind = CacheKind.Image

    override fun open(ref: CachedFileRef): Flow<CachedFileState> =
        store.open(ref) { offset, onStart, onBytes -> api.downloadContent(ref.itemId, offset, onStart, onBytes) }

    override suspend fun usedBytes(): Long = store.usedBytes()

    override suspend fun clear() = store.reset()

    override suspend fun trimTo(maxBytes: Long) = store.trimTo(maxBytes)
}
