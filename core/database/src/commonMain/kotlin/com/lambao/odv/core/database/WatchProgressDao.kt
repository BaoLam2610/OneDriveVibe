package com.lambao.odv.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

/** Truy cập vị trí xem dở của video (VD-12). Tách khỏi [DriveDao]: dữ liệu của người dùng, không phải bản sao của OneDrive. */
@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress WHERE itemId = :itemId")
    suspend fun get(itemId: String): WatchProgressEntity?

    @Upsert
    suspend fun upsert(entity: WatchProgressEntity)

    /** Xóa vị trí của một video ("Xem từ đầu", VD-12). */
    @Query("DELETE FROM watch_progress WHERE itemId = :itemId")
    suspend fun delete(itemId: String)

    /** Ẩn khỏi dải Xem tiếp nhưng giữ vị trí (DS-02). */
    @Query("UPDATE watch_progress SET dismissed = 1 WHERE itemId = :itemId")
    suspend fun dismiss(itemId: String)

    /** Xóa toàn bộ (ngắt kết nối, CD-05). */
    @Query("DELETE FROM watch_progress")
    suspend fun deleteAll()

    /** Xóa vị trí xem của một tài khoản (TA-08); cùng cách so tiền tố với `ReadingProgressDao.deleteWithPrefix`. */
    @Query("DELETE FROM watch_progress WHERE substr(itemId, 1, length(:prefix)) = :prefix")
    suspend fun deleteWithPrefix(prefix: String)
}
