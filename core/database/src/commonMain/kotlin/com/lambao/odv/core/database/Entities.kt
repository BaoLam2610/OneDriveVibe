package com.lambao.odv.core.database

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Một mục của drive (thư mục hoặc tệp) đã đồng bộ về máy (DB-01). Chỉ lưu thứ UI cần; không lưu quota (KN-12).
 *
 * `parentId` là id cha theo Graph. Mục ngay dưới gốc có `parentId` bằng [SyncStateEntity.rootId]; không có khóa ngoại vì
 * delta có thể gửi con trước cha. `nameKey` là tên đã hạ chữ thường và bỏ dấu (tìm kiếm DS-03, sắp xếp theo tên TM-05).
 * `scanId` là số lần quét đầy đủ đã ghi mục này: dùng để dọn mục không còn tồn tại sau khi quét lại vì `410` (DB-03).
 */
@Entity(
    tableName = "drive_item",
    indices = [Index("parentId"), Index("nameKey")],
)
data class DriveItemEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val nameKey: String,
    val sizeBytes: Long,
    val isFolder: Boolean,
    /** Tên của `MediaKind` (`Image`, `Video`, `Pdf`); null cho thư mục và tệp không hỗ trợ. */
    val mediaKind: String?,
    val childCount: Int?,
    val durationMs: Long?,
    /** Epoch mili giây. */
    val modifiedAt: Long?,
    val cTag: String?,
    /** Epoch mili giây; ngày chụp (TV-02). */
    val takenAt: Long?,
    /** Epoch mili giây; ngày tạo tệp gốc, không có thì ngày tải lên (TV-02). */
    val createdAt: Long?,
    val scanId: Long,
)

/**
 * Trạng thái đồng bộ, đúng một dòng (`id = 0`).
 *
 * - `deltaLink`: mốc để lấy thay đổi lần sau (DB-02). Null cho tới khi quét đầy đủ xong lần đầu, và bị xóa khi gặp `410`.
 * - `pendingNextLink`: trang đang quét dở, ghi cùng transaction với dữ liệu của trang trước nên tắt app lúc nào cũng
 *   tiếp tục được (DB-04). Null khi không có lần quét nào dở.
 * - `scanId`: tăng mỗi lần bắt đầu quét đầy đủ từ `/root/delta`.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val rootId: String? = null,
    val deltaLink: String? = null,
    val pendingNextLink: String? = null,
    val scanId: Long = 0,
    val scannedCount: Int = 0,
    val initialSyncDone: Boolean = false,
    /** Epoch mili giây. */
    val lastSyncedAt: Long? = null,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

/** Kết quả rút gọn để dựng đường dẫn cha cho kết quả tìm kiếm mà không kéo cả hàng. */
data class ItemRef(
    val id: String,
    val parentId: String?,
    val name: String,
)
