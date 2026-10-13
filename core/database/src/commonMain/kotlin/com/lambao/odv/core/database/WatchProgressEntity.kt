package com.lambao.odv.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Vị trí xem dở của một video (VD-12), một dòng cho mỗi tệp. Cùng cách làm với [ReadingProgressEntity]: không có khóa ngoại tới
 * `drive_item` để đồng bộ quét lại không xóa nó, mục đã bị xóa trên OneDrive thì dòng mồ côi vô hại (truy vấn dải Xem tiếp nối với
 * `drive_item` nên không hiện nó) và `cTag` không khớp thì bị bỏ qua.
 *
 * [finished] là video đã chạy hết (rời dải Xem tiếp). [dismissed] là người dùng bấm X ở dải: vị trí vẫn giữ để hỏi xem tiếp, mỗi lần lưu
 * lại thì ghi đè cả dòng nên [dismissed] tự về false.
 */
@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val itemId: String,
    val cTag: String?,
    val positionMs: Long,
    val durationMs: Long,
    /** Epoch mili giây. */
    val updatedAt: Long,
    val finished: Boolean,
    val dismissed: Boolean,
)
