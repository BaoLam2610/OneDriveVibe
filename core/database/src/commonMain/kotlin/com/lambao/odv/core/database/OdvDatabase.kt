package com.lambao.odv.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * CSDL Room KMP, nguồn dữ liệu duy nhất cho UI (ADR-0007). **Mỗi lần đổi schema (thêm/bỏ/đổi cột, bảng, index) phải tăng
 * `version`** (ADR-0015): Room so mã băm schema chứ không so số version, nên đổi cột mà quên tăng thì bản đã cài crash
 * "Room cannot verify the data integrity". Trước khi phát hành, tăng version là đủ: bản Android có
 * `fallbackToDestructiveMigration` nên DB cũ bị xóa và đồng bộ lại từ OneDrive. Sau khi phát hành phải viết `Migration`.
 *
 * Lịch sử version:
 * - 1: bảng `drive_item`, `sync_state` (Lát 3, 4).
 * - 2: `drive_item` thêm 5 cột facet video cho bảng thông tin (Lát 6, VD-17).
 */
@Database(
    entities = [DriveItemEntity::class, SyncStateEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(OdvDatabaseConstructor::class)
abstract class OdvDatabase : RoomDatabase() {
    abstract fun driveDao(): DriveDao
}

// Room (KSP) sinh bản `actual` cho từng target; không viết tay.
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object OdvDatabaseConstructor : RoomDatabaseConstructor<OdvDatabase> {
    override fun initialize(): OdvDatabase
}
