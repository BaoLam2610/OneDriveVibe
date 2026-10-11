package com.lambao.odv.core.data.pdf

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.data.drive.toDomain
import com.lambao.odv.core.data.file.ResumableFileStore
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PdfFile
import com.lambao.odv.core.domain.repository.PdfRepository
import com.lambao.odv.core.network.graph.GraphApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Kho PDF (PD-01): như `FileOriginalImageRepository`, chỉ biết cách tải một tệp từ Graph; lưu tệp, tải tiếp, LRU và xóa sạch khi
 * ngắt kết nối (CD-05) nằm ở [ResumableFileStore] dùng chung với ảnh gốc. Cũng là [CacheStore] loại PDF để Cài đặt đo, xóa và dọn
 * theo giới hạn (CD-07). Tệp PDF chưa mã hóa trên đĩa (cùng nhóm M3 của ảnh gốc, chờ ADR riêng).
 */
internal class FilePdfRepository(
    private val api: GraphApi,
    private val dao: DriveDao,
    private val store: ResumableFileStore,
    private val dispatchers: DispatcherProvider,
) : PdfRepository, ConnectionResetter by store, CacheStore {

    override val kind: CacheKind = CacheKind.Pdf

    override fun open(ref: CachedFileRef): Flow<CachedFileState> =
        store.open(ref) { offset, onStart, onBytes -> api.downloadContent(ref.itemId, offset, onStart, onBytes) }

    override suspend fun getFile(itemId: String): PdfFile? = withContext(dispatchers.io) {
        val entity = dao.getItem(itemId) ?: return@withContext null
        if (entity.mediaKind != MediaKind.Pdf.name) return@withContext null
        val folderName = entity.parentId?.let { parent -> dao.refs(listOf(parent)).firstOrNull()?.name }
        PdfFile(item = entity.toDomain(), folderName = folderName)
    }

    override suspend fun usedBytes(): Long = store.usedBytes()

    override suspend fun clear() = store.reset()

    override suspend fun trimTo(maxBytes: Long) = store.trimTo(maxBytes)
}
