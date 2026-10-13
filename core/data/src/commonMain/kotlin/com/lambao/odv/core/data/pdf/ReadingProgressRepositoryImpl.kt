package com.lambao.odv.core.data.pdf

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.database.DatabaseCompactor
import com.lambao.odv.core.database.ReadingProgressDao
import com.lambao.odv.core.database.ReadingProgressEntity
import com.lambao.odv.core.domain.hook.AccountDataCleaner
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.ItemKey
import com.lambao.odv.core.domain.model.ReadingProgress
import com.lambao.odv.core.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Trang đang đọc của PDF (PD-05) trong Room. Cũng là [ConnectionResetter]: Ngắt kết nối xóa hết (CD-05). Tiến độ nằm riêng với
 * `drive_item` nên đồng bộ quét lại không xóa nó; mục đã biến mất trên OneDrive thì dòng mồ côi vô hại và `cTag` không khớp thì bị bỏ qua.
 */
internal class ReadingProgressRepositoryImpl(
    private val dao: ReadingProgressDao,
    private val compactor: DatabaseCompactor,
    private val dispatchers: DispatcherProvider,
) : ReadingProgressRepository, ConnectionResetter, AccountDataCleaner {

    private val log = Logger.withTag("ReadingProgress")

    override suspend fun get(itemId: String, cTag: String?): ReadingProgress? = withContext(dispatchers.io) {
        val row = dao.get(itemId) ?: return@withContext null
        // Nội dung đã đổi (hoặc một bên không có cTag): số trang cũ không còn đáng tin.
        if (row.cTag != cTag) return@withContext null
        row.toDomain()
    }

    override suspend fun save(progress: ReadingProgress) = withContext(dispatchers.io) {
        dao.upsert(progress.toEntity())
    }

    /** Xóa tài khoản khỏi app (TA-08): xóa tiến độ đọc của mọi tệp có tiền tố `<drive id>|` ([ItemKey]). */
    override suspend fun purge(accountId: String) {
        withContext(dispatchers.io) { dao.deleteWithPrefix(ItemKey.of(accountId, "")) }
    }

    override suspend fun reset() {
        withContext(dispatchers.io) {
            dao.deleteAll()
            // DELETE chỉ đánh dấu trang trống nên id tệp còn trong odv.db và WAL; thu gọn để dữ liệu thật sự mất (cùng SyncCoordinator.reset).
            // Lỗi ở đây không được chặn các bản reset khác (hợp đồng ConnectionResetter), chỉ ghi tên loại ngoại lệ (CH-06).
            try {
                compactor.compact()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w { "Không thu gọn được odv.db sau khi xóa tiến độ đọc: ${e::class.simpleName}" }
            }
        }
    }
}

private fun ReadingProgressEntity.toDomain() = ReadingProgress(
    itemId = itemId,
    cTag = cTag,
    page = page,
    pageCount = pageCount,
    updatedAt = updatedAt,
    finished = finished,
)

// Ghi đè cả dòng nên `dismissed` tự về false: đọc tiếp thì mục hiện lại ở dải Đọc tiếp (DS-02).
private fun ReadingProgress.toEntity() = ReadingProgressEntity(
    itemId = itemId,
    cTag = cTag,
    page = page,
    pageCount = pageCount,
    updatedAt = updatedAt,
    finished = finished,
    dismissed = false,
)
