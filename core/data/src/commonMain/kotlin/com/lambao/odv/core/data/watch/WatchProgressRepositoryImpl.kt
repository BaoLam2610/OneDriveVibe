package com.lambao.odv.core.data.watch

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.database.DatabaseCompactor
import com.lambao.odv.core.database.WatchProgressDao
import com.lambao.odv.core.database.WatchProgressEntity
import com.lambao.odv.core.domain.hook.AccountDataCleaner
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.ItemKey
import com.lambao.odv.core.domain.model.WatchProgress
import com.lambao.odv.core.domain.repository.WatchProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Vị trí xem dở của video (VD-12) trong Room, cùng cách làm với `ReadingProgressRepositoryImpl`. Cũng là [ConnectionResetter] (Ngắt kết nối
 * xóa hết, CD-05) và [AccountDataCleaner] (xóa tài khoản xóa vị trí xem của tài khoản đó, TA-08). Nằm riêng với `drive_item` nên đồng bộ
 * quét lại không xóa nó; `cTag` không khớp thì bị bỏ qua.
 */
internal class WatchProgressRepositoryImpl(
    private val dao: WatchProgressDao,
    private val compactor: DatabaseCompactor,
    private val dispatchers: DispatcherProvider,
) : WatchProgressRepository, ConnectionResetter, AccountDataCleaner {

    private val log = Logger.withTag("WatchProgress")

    override suspend fun get(itemId: String, cTag: String?): WatchProgress? = withContext(dispatchers.io) {
        val row = dao.get(itemId) ?: return@withContext null
        // Nội dung đã đổi (hoặc một bên không có cTag): vị trí cũ không còn đáng tin.
        if (row.cTag != cTag) return@withContext null
        row.toDomain()
    }

    override suspend fun save(progress: WatchProgress) = withContext(dispatchers.io) {
        dao.upsert(progress.toEntity())
    }

    override suspend fun clear(itemId: String) = withContext(dispatchers.io) {
        dao.delete(itemId)
    }

    /** Xóa tài khoản khỏi app (TA-08): xóa vị trí xem của mọi tệp có tiền tố `<drive id>|` ([ItemKey]). */
    override suspend fun purge(accountId: String) {
        withContext(dispatchers.io) { dao.deleteWithPrefix(ItemKey.of(accountId, "")) }
    }

    override suspend fun reset() {
        withContext(dispatchers.io) {
            dao.deleteAll()
            // DELETE chỉ đánh dấu trang trống nên id tệp còn trong odv.db và WAL; thu gọn để dữ liệu thật sự mất (cùng ReadingProgressRepositoryImpl).
            // Lỗi ở đây không được chặn các bản reset khác (hợp đồng ConnectionResetter), chỉ ghi tên loại ngoại lệ (CH-06).
            try {
                compactor.compact()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w { "Không thu gọn được odv.db sau khi xóa vị trí xem: ${e::class.simpleName}" }
            }
        }
    }
}

private fun WatchProgressEntity.toDomain() = WatchProgress(
    itemId = itemId,
    cTag = cTag,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
    finished = finished,
)

// Ghi đè cả dòng nên `dismissed` tự về false: xem tiếp thì mục hiện lại ở dải (DS-02).
private fun WatchProgress.toEntity() = WatchProgressEntity(
    itemId = itemId,
    cTag = cTag,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
    finished = finished,
    dismissed = false,
)
