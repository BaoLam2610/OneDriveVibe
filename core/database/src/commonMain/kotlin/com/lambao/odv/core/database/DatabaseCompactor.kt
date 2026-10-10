package com.lambao.odv.core.database

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection

/**
 * Thu gọn tệp `odv.db` sau khi xóa dữ liệu (CD-05, KH-03, KH-06). `DELETE` chỉ đánh dấu trang là trống, nên tên tệp và tên thư
 * mục cũ vẫn nằm trong tệp (và trong WAL) tới khi bị ghi đè. `VACUUM` ghi lại toàn bộ tệp, `wal_checkpoint(TRUNCATE)` đưa WAL về 0 byte.
 *
 * Không đóng Room để xóa hẳn tệp: `OdvDatabase` là singleton đang có Flow của UI thu thập, đóng rồi mở lại dễ hỏng hơn mà không
 * an toàn hơn. `useWriterConnection` không mở transaction (VACUUM không chạy được trong transaction).
 */
class DatabaseCompactor(
    private val db: OdvDatabase,
) {
    /** Ném ngoại lệ nếu SQLite báo lỗi; bên gọi tự quyết định có nuốt không. */
    suspend fun compact() {
        db.useWriterConnection { connection ->
            connection.executeSQL("VACUUM")
            connection.executeSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        }
    }
}
