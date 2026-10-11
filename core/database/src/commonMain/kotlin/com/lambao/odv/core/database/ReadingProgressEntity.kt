package com.lambao.odv.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Trang đang đọc của một PDF (PD-05), một dòng cho mỗi tệp. Không có khóa ngoại tới `drive_item`: tiến độ không được mất khi đồng bộ
 * quét lại (xóa rồi ghi lại `drive_item`) và mục bị xóa trên OneDrive thì dòng mồ côi vô hại (nhỏ, và `cTag` không khớp thì bị bỏ qua).
 * `page` đếm từ 0. `cTag` là phiên bản nội dung lúc lưu.
 */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val itemId: String,
    val cTag: String?,
    val page: Int,
    val pageCount: Int,
    /** Epoch mili giây. */
    val updatedAt: Long,
    val finished: Boolean,
)
