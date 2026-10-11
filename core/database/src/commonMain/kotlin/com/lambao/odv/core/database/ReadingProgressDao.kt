package com.lambao.odv.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

/** Truy cập tiến độ đọc PDF (PD-05). Tách khỏi [DriveDao]: dữ liệu của người dùng, không phải bản sao của OneDrive. */
@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId")
    suspend fun get(itemId: String): ReadingProgressEntity?

    @Upsert
    suspend fun upsert(entity: ReadingProgressEntity)

    /** Xóa toàn bộ (ngắt kết nối, CD-05). */
    @Query("DELETE FROM reading_progress")
    suspend fun deleteAll()
}
