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

    /** Ẩn khỏi dải Đọc tiếp nhưng giữ trang đang đọc (DS-02). */
    @Query("UPDATE reading_progress SET dismissed = 1 WHERE itemId = :itemId")
    suspend fun dismiss(itemId: String)

    /** Xóa toàn bộ (ngắt kết nối, CD-05). */
    @Query("DELETE FROM reading_progress")
    suspend fun deleteAll()

    /**
     * Xóa tiến độ của một tài khoản (TA-08): `itemId` có tiền tố `<drive id>|` ([prefix]). Dùng `substr` thay `LIKE` vì `_` trong drive id
     * (base64 url) là ký tự đại diện của `LIKE`.
     */
    @Query("DELETE FROM reading_progress WHERE substr(itemId, 1, length(:prefix)) = :prefix")
    suspend fun deleteWithPrefix(prefix: String)
}
