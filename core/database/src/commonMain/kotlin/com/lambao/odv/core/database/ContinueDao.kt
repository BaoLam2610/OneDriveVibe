package com.lambao.odv.core.database

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Một mục của dải Xem tiếp / Đọc tiếp (DS-02): tệp kèm tiến độ. Video thì [progressValue] là vị trí (ms) và [progressTotal] là thời lượng
 * (ms); PDF thì là trang (từ 0) và tổng số trang. Phân biệt bằng `item.mediaKind`.
 */
data class ContinueRow(
    @Embedded val item: DriveItemEntity,
    val progressValue: Long,
    val progressTotal: Long,
    /** Epoch mili giây; lần xem gần nhất, dùng để xếp. */
    val progressAt: Long,
)

/** Truy vấn gộp tiến độ xem video và đọc PDF cho dải Xem tiếp / Đọc tiếp (DS-02). Chỉ đọc. */
@Dao
interface ContinueDao {

    /**
     * Tối đa [limit] ứng viên (repository còn cắt "mỗi tài khoản tối đa 10") chưa xem/đọc xong và chưa bị ẩn, mới nhất trước. Nối với `drive_item` nên mục đã xóa trên OneDrive tự biến mất
     * (DS-06), `cTag` khác (tệp đổi nội dung) thì bỏ qua như khi mở lại; lọc theo loại tệp đang bật ([kinds]) và phạm vi xem ([accountIds]).
     * `IS` thay `=` để hai cTag cùng null vẫn khớp.
     */
    @Query(
        """
        SELECT d.*, w.positionMs AS progressValue, w.durationMs AS progressTotal, w.updatedAt AS progressAt
        FROM watch_progress w JOIN drive_item d ON d.id = w.itemId
        WHERE w.finished = 0 AND w.dismissed = 0 AND d.cTag IS w.cTag
            AND d.mediaKind IN (:kinds) AND d.accountId IN (:accountIds)
        UNION ALL
        SELECT d.*, r.page, r.pageCount, r.updatedAt
        FROM reading_progress r JOIN drive_item d ON d.id = r.itemId
        WHERE r.finished = 0 AND r.dismissed = 0 AND d.cTag IS r.cTag
            AND d.mediaKind IN (:kinds) AND d.accountId IN (:accountIds)
        ORDER BY progressAt DESC
        LIMIT :limit
        """,
    )
    fun observe(kinds: List<String>, accountIds: List<String>, limit: Int): Flow<List<ContinueRow>>
}
