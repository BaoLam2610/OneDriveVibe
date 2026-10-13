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
 * - 3: bảng `reading_progress` lưu trang đang đọc của PDF (Lát 9a, PD-05).
 * - 4: nhiều tài khoản (Lát T, ADR-0030): `drive_item` thêm `accountId` (id có tiền tố `<drive id>|`, chỉ mục `accountId`);
 *   `sync_state` đổi khóa chính từ `id` (một dòng) sang `accountId` (một dòng mỗi tài khoản). Chưa phát hành nên DB cũ bị xóa và
 *   đồng bộ lại (`fallbackToDestructiveMigration`); từ bản phát hành đầu phải viết Migration.
 * - 5: Xem tiếp / Đọc tiếp (Lát 10, DS-02, VD-12): bảng `watch_progress` lưu vị trí xem dở của video; `reading_progress` thêm cột
 *   `dismissed` (bấm X ở dải). Chưa phát hành nên DB cũ bị xóa và đồng bộ lại; từ bản phát hành đầu phải viết Migration.
 */
@Database(
    entities = [DriveItemEntity::class, SyncStateEntity::class, ReadingProgressEntity::class, WatchProgressEntity::class],
    version = 5,
    exportSchema = true,
)
@ConstructedBy(OdvDatabaseConstructor::class)
abstract class OdvDatabase : RoomDatabase() {
    abstract fun driveDao(): DriveDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun continueDao(): ContinueDao
}

// Room (KSP) sinh bản `actual` cho từng target; không viết tay.
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object OdvDatabaseConstructor : RoomDatabaseConstructor<OdvDatabase> {
    override fun initialize(): OdvDatabase
}
