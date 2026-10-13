package com.lambao.odv.core.database

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow

/**
 * Truy cập dữ liệu đồng bộ. Lớp trừu tượng (không phải interface) vì Room KMP chỉ cho `@Transaction` trên hàm `open` của
 * lớp trừu tượng; mỗi trang delta phải ghi nguyên tử cùng mốc `pendingNextLink` (DB-04).
 *
 * Room 3 không tự nhận `PagingSource` làm kiểu trả về như Room 2: phải đăng ký [PagingSourceDaoReturnTypeConverter] của
 * `room3-paging` (cho [pagedLibrary]); thiếu thì KSP báo "Not sure how to convert the query result".
 */
@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class DriveDao {

    @Query("SELECT * FROM drive_item WHERE parentId = :parentId")
    abstract fun observeChildren(parentId: String): Flow<List<DriveItemEntity>>

    /**
     * Ảnh và video của Thư viện, mới nhất trước (TV-01, TV-02), theo trang. [kinds] là tên `MediaKind`; [accountIds] là các tài khoản
     * trong phạm vi xem (TA-05). Room tự làm mất hiệu lực nguồn khi bảng đổi nên Pager nạp lại khi đồng bộ ghi thêm (DB-05).
     */
    @Query("SELECT * FROM drive_item WHERE mediaKind IN (:kinds) AND accountId IN (:accountIds) ORDER BY sortDate DESC, id")
    abstract fun pagedLibrary(kinds: List<String>, accountIds: List<String>): PagingSource<Int, DriveItemEntity>

    /**
     * Số mục theo ngày, mới nhất trước. `dayNumber` dùng phép chia số nguyên của SQLite, khớp `dayNumberOf` ở domain
     * (cùng [utcOffsetMs]) để tiêu đề nhóm và vị trí cuộn nhanh khớp từng mục.
     */
    @Query(
        "SELECT (sortDate + :utcOffsetMs) / 86400000 AS dayNumber, COUNT(*) AS count, " +
            "SUM(CASE WHEN mediaKind = 'Video' THEN 1 ELSE 0 END) AS videoCount FROM drive_item " +
            "WHERE mediaKind IN (:kinds) AND accountId IN (:accountIds) GROUP BY dayNumber ORDER BY dayNumber DESC",
    )
    abstract fun libraryDays(kinds: List<String>, accountIds: List<String>, utcOffsetMs: Long): Flow<List<DayCount>>

    /**
     * Toàn bộ mục thuộc [kinds] theo thứ tự của Thư viện, không phân trang: màn xem ảnh cần chỉ số theo vị trí để vuốt
     * trước/sau (AN-03). Cùng thứ tự với [pagedLibrary] nên ảnh ở đâu trên lưới thì ở đúng vị trí đó khi vuốt.
     */
    @Query("SELECT * FROM drive_item WHERE mediaKind IN (:kinds) AND accountId IN (:accountIds) ORDER BY sortDate DESC, id")
    abstract fun observeLibraryItems(kinds: List<String>, accountIds: List<String>): Flow<List<DriveItemEntity>>

    /**
     * Tìm trong Thư viện (TV-07): cùng điều kiện và thứ tự với [pagedLibrary] cộng thêm khớp tên. [key] đã hạ chữ thường, bỏ dấu (`searchKey`);
     * dùng `instr` thay `LIKE` như [search] để `%`, `_`, `\` trong từ khóa không thành ký tự đại diện. Thư mục không xuất hiện vì `mediaKind`
     * chỉ là Image hoặc Video. Theo trang nên không giới hạn số kết quả.
     */
    @Query(
        "SELECT * FROM drive_item WHERE mediaKind IN (:kinds) AND accountId IN (:accountIds) AND instr(nameKey, :key) > 0 " +
            "ORDER BY sortDate DESC, id",
    )
    abstract fun pagedLibrarySearch(key: String, kinds: List<String>, accountIds: List<String>): PagingSource<Int, DriveItemEntity>

    /**
     * Toàn bộ kết quả của [pagedLibrarySearch], không phân trang, cùng thứ tự: màn xem mở từ kết quả tìm vuốt đúng danh sách này (TV-07,
     * AN-03). Phải giữ hai truy vấn này đồng bộ, nếu không thứ tự vuốt lệch với lưới.
     */
    @Query(
        "SELECT * FROM drive_item WHERE mediaKind IN (:kinds) AND accountId IN (:accountIds) AND instr(nameKey, :key) > 0 " +
            "ORDER BY sortDate DESC, id",
    )
    abstract fun observeLibrarySearchItems(key: String, kinds: List<String>, accountIds: List<String>): Flow<List<DriveItemEntity>>

    /** Trạng thái đồng bộ của mọi tài khoản (một dòng mỗi tài khoản, DB-01). */
    @Query("SELECT * FROM sync_state")
    abstract fun observeSyncStates(): Flow<List<SyncStateEntity>>

    @Query("SELECT * FROM sync_state WHERE accountId = :accountId")
    abstract fun observeSyncState(accountId: String): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE accountId = :accountId")
    abstract suspend fun getSyncState(accountId: String): SyncStateEntity?

    @Query("SELECT * FROM sync_state")
    abstract suspend fun getSyncStates(): List<SyncStateEntity>

    /** Mọi tài khoản còn dữ liệu đồng bộ (mục hoặc trạng thái). Dùng để tìm dữ liệu mồ côi của tài khoản đã bị gỡ khỏi config (TA-08). */
    @Query("SELECT accountId FROM drive_item UNION SELECT accountId FROM sync_state")
    abstract suspend fun getAccountIdsWithData(): List<String>

    /**
     * Tìm theo [key] (đã hạ chữ thường, bỏ dấu) trong `nameKey`. Dùng `instr` thay `LIKE` để `%`, `_` và `\` trong từ
     * khóa không bị hiểu là ký tự đại diện. [kinds] là tên `MediaKind` được bật; thư mục luôn khớp. [accountIds] là các tài khoản trong
     * phạm vi xem (TA-05).
     */
    @Query(
        "SELECT * FROM drive_item WHERE instr(nameKey, :key) > 0 " +
            "AND (isFolder = 1 OR mediaKind IN (:kinds)) AND accountId IN (:accountIds) " +
            "ORDER BY isFolder DESC, nameKey LIMIT :limit",
    )
    abstract suspend fun search(key: String, kinds: List<String>, accountIds: List<String>, limit: Int): List<DriveItemEntity>

    @Query("SELECT id, parentId, name FROM drive_item WHERE id IN (:ids)")
    abstract suspend fun refs(ids: List<String>): List<ItemRef>

    /**
     * Id mọi video của [accountIds] có thời lượng nhỏ hơn hoặc bằng [maxDurationMs] (SV-01, tab Short). Video không có thời lượng bị loại. Sắp theo id để
     * thứ tự gốc cố định: xáo bằng seed (SV-02) cho cùng kết quả khi khôi phục. Chỉ đọc id, không nạp cả bản ghi. Dùng chỉ mục
     * `(mediaKind, sortDate)`, không cần đổi schema.
     */
    @Query(
        "SELECT id FROM drive_item WHERE mediaKind = 'Video' AND durationMs IS NOT NULL AND durationMs <= :maxDurationMs " +
            "AND accountId IN (:accountIds) ORDER BY id",
    )
    abstract fun observeShortVideoIds(maxDurationMs: Long, accountIds: List<String>): Flow<List<String>>

    @Query("SELECT * FROM drive_item WHERE id = :id")
    abstract suspend fun getItem(id: String): DriveItemEntity?

    @Upsert
    abstract suspend fun upsertItems(items: List<DriveItemEntity>)

    @Upsert
    abstract suspend fun upsertSyncState(state: SyncStateEntity)

    @Query("DELETE FROM drive_item WHERE id IN (:ids)")
    abstract suspend fun deleteItems(ids: List<String>)

    @Query("SELECT id FROM drive_item WHERE parentId IN (:parentIds)")
    abstract suspend fun childIds(parentIds: List<String>): List<String>

    /**
     * Xóa mục **của [accountId]** không được ghi trong lần quét đầy đủ [scanId] (đã biến mất trên OneDrive, DB-03, DS-06). `scanId` là
     * của từng tài khoản nên phải lọc theo tài khoản: không thì lần quét của tài khoản này xóa sạch mục của tài khoản kia.
     */
    @Query("DELETE FROM drive_item WHERE accountId = :accountId AND scanId != :scanId")
    abstract suspend fun deleteNotInScan(accountId: String, scanId: Long)

    @Query("DELETE FROM drive_item")
    abstract suspend fun deleteAllItems()

    @Query("DELETE FROM sync_state")
    abstract suspend fun deleteSyncState()

    @Query("DELETE FROM drive_item WHERE accountId = :accountId")
    abstract suspend fun deleteItemsOf(accountId: String)

    @Query("DELETE FROM sync_state WHERE accountId = :accountId")
    abstract suspend fun deleteSyncStateOf(accountId: String)

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
        upserts.chunked(DatabaseConstants.IN_CHUNK).forEach { upsertItems(it) }
        deleteSubtrees(deletedIds)
        if (purgeScanId != null) deleteNotInScan(state.accountId, purgeScanId)
        upsertSyncState(state)
    }

    /** Xóa mọi dữ liệu đồng bộ (ngắt kết nối, CD-05). */
    @Transaction
    open suspend fun clearAll() {
        deleteAllItems()
        deleteSyncState()
    }

    /** Xóa dữ liệu đồng bộ của một tài khoản (xóa tài khoản khỏi app, TA-08): mục và trạng thái đồng bộ, cùng một transaction. */
    @Transaction
    open suspend fun clearAccount(accountId: String) {
        deleteItemsOf(accountId)
        deleteSyncStateOf(accountId)
    }

    /** Xóa [rootIds] và mọi hậu duệ. Graph thường báo xóa từng con, nhưng không bảo đảm nên tự duyệt theo từng tầng. */
    open suspend fun deleteSubtrees(rootIds: List<String>) {
        if (rootIds.isEmpty()) return
        val all = LinkedHashSet<String>(rootIds)
        var frontier: List<String> = rootIds.distinct()
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<String>()
            for (chunk in frontier.chunked(DatabaseConstants.IN_CHUNK)) {
                for (id in childIds(chunk)) if (all.add(id)) next += id
            }
            frontier = next
        }
        all.chunked(DatabaseConstants.IN_CHUNK).forEach { deleteItems(it) }
    }
}
