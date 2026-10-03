package com.lambao.odv.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * CSDL Room KMP, nguồn dữ liệu duy nhất cho UI (ADR-0007). `version = 1` giữ nguyên đến khi phát hành: đổi cột trong
 * lúc dev thì bản đã cài tự tạo lại (xem `fallbackToDestructiveMigration` ở bản Android) và đồng bộ lại từ OneDrive.
 */
@Database(
    entities = [DriveItemEntity::class, SyncStateEntity::class],
    version = 1,
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
