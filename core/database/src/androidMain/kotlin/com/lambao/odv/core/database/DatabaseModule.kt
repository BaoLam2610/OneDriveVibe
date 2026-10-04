package com.lambao.odv.core.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val DATABASE_NAME = "odv.db"

/** Binding Koin của `:core:database` (bản Android). Ghép ở MainApplication. */
val databaseModule = module {
    single<OdvDatabase> { buildDatabase(androidContext()) }
    single<DriveDao> { get<OdvDatabase>().driveDao() }
}

private fun buildDatabase(context: Context): OdvDatabase {
    val appContext = context.applicationContext
    return Room.databaseBuilder<OdvDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
        // ADR-0007: BundledSQLiteDriver để cùng một bản SQLite trên mọi máy và sẵn sàng cho iOS (MVP2).
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // ADR-0015: trước khi phát hành không viết Migration; mỗi lần đổi schema chỉ cần TĂNG version ở OdvDatabase thì DB cũ
        // bị xóa và quét lại (dữ liệu chỉ là bản sao của OneDrive). Cờ này KHÔNG cứu được trường hợp đổi cột mà quên tăng
        // version (Room báo lỗi hash). Từ lần phát hành đầu tiên phải bỏ cờ này và viết Migration.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
