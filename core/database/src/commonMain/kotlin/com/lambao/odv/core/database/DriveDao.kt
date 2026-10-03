package com.lambao.odv.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Số tham số `?` tối đa cho một câu `IN (...)`; dưới giới hạn của SQLite để an toàn với drive lớn. */
private const val IN_CHUNK = 500

/**
 * Truy cập dữ liệu đồng bộ. Lớp trừu tượng (không phải interface) vì Room KMP chỉ cho `@Transaction` trên hàm `open` của
 * lớp trừu tượng; mỗi trang delta phải ghi nguyên tử cùng mốc `pendingNextLink` (DB-04).
 */
@Dao
abstract class DriveDao {

    @Query("SELECT * FROM drive_item WHERE parentId = :parentId")
    abstract fun observeChildren(parentId: String): Flow<List<DriveItemEntity>>

    @Query("SELECT * FROM sync_state WHERE id = 0")
    abstract fun observeSyncState(): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE id = 0")
    abstract suspend fun getSyncState(): SyncStateEntity?

    /**
     * Tìm theo [key] (đã hạ chữ thường, bỏ dấu) trong `nameKey`. Dùng `instr` thay `LIKE` để `%`, `_` và `\` trong từ
     * khóa không bị hiểu là ký tự đại diện. [kinds] là tên `MediaKind` được bật; thư mục luôn khớp.
     */
    @Query(
        "SELECT * FROM drive_item WHERE instr(nameKey, :key) > 0 " +
            "AND (isFolder = 1 OR mediaKind IN (:kinds)) " +
            "ORDER BY isFolder DESC, nameKey LIMIT :limit",
    )
    abstract suspend fun search(key: String, kinds: List<String>, limit: Int): List<DriveItemEntity>

    @Query("SELECT id, parentId, name FROM drive_item WHERE id IN (:ids)")
    abstract suspend fun refs(ids: List<String>): List<ItemRef>

    @Upsert
    abstract suspend fun upsertItems(items: List<DriveItemEntity>)

    @Upsert
    abstract suspend fun upsertSyncState(state: SyncStateEntity)

    @Query("DELETE FROM drive_item WHERE id IN (:ids)")
    abstract suspend fun deleteItems(ids: List<String>)

    @Query("SELECT id FROM drive_item WHERE parentId IN (:parentIds)")
    abstract suspend fun childIds(parentIds: List<String>): List<String>

    /** Xóa mục không được ghi trong lần quét đầy đủ [scanId] (đã biến mất trên OneDrive, DB-03, DS-06). */
    @Query("DELETE FROM drive_item WHERE scanId != :scanId")
    abstract suspend fun deleteNotInScan(scanId: Long)

    @Query("DELETE FROM drive_item")
    abstract suspend fun deleteAllItems()

    @Query("DELETE FROM sync_state")
    abstract suspend fun deleteSyncState()

    /**
     * Ghi một trang delta nguyên tử: thêm/sửa [upserts], xóa [deletedIds] cùng toàn bộ cây con của chúng (DS-06), rồi
     * lưu [state] (gồm `pendingNextLink` của trang kế tiếp). Hoặc trang được ghi đủ cùng mốc, hoặc không gì cả.
     *
     * Ở trang cuối của một lần quét đầy đủ, [purgeScanId] không null: dọn luôn mục không thuộc lần quét đó (DB-03) trong
     * cùng transaction với `deltaLink` mới, để không có lúc mốc đã lưu mà mục cũ chưa dọn. Các trang khác truyền null.
     */
    @Transaction
    open suspend fun applyPage(
        upserts: List<DriveItemEntity>,
        deletedIds: List<String>,
        state: SyncStateEntity,
        purgeScanId: Long?,
    ) {
        upserts.chunked(IN_CHUNK).forEach { upsertItems(it) }
        deleteSubtrees(deletedIds)
        if (purgeScanId != null) deleteNotInScan(purgeScanId)
        upsertSyncState(state)
    }

    /** Xóa mọi dữ liệu đồng bộ (ngắt kết nối, CD-05). */
    @Transaction
    open suspend fun clearAll() {
        deleteAllItems()
        deleteSyncState()
    }

    /** Xóa [rootIds] và mọi hậu duệ. Graph thường báo xóa từng con, nhưng không bảo đảm nên tự duyệt theo từng tầng. */
    open suspend fun deleteSubtrees(rootIds: List<String>) {
        if (rootIds.isEmpty()) return
        val all = LinkedHashSet<String>(rootIds)
        var frontier: List<String> = rootIds.distinct()
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<String>()
            for (chunk in frontier.chunked(IN_CHUNK)) {
                for (id in childIds(chunk)) if (all.add(id)) next += id
            }
            frontier = next
        }
        all.chunked(IN_CHUNK).forEach { deleteItems(it) }
    }
}
