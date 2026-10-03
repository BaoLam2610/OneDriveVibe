package com.lambao.odv.core.database

import android.content.Context
import androidx.room.Room
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
        // Schema giữ version = 1 đến khi phát hành (ADR-0007) nên không viết Migration. Dữ liệu chỉ là bản sao của
        // OneDrive nên mất cũng chỉ tốn một lần quét lại; đổi cột lúc dev không làm app crash.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
