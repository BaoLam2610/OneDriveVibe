package com.lambao.odv.tools.debug

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

class PrefsFile(val name: String, val entries: List<Pair<String, String>>)

class TableDump(val name: String, val rowCount: Long, val columns: List<String>, val rows: List<List<String>>)

class DatabaseDump(val name: String, val sizeBytes: Long, val tables: List<TableDump>, val error: String?)

class StorageSnapshot(
    val preferences: List<PrefsFile>,
    /** Tên tệp DataStore (protobuf, không đọc được dạng chữ). */
    val dataStoreFiles: List<String>,
    val databases: List<DatabaseDump>,
    /** Tên tệp trong kho bí mật mã hóa. Chỉ liệt kê tên, không bao giờ giải mã. */
    val secretFiles: List<String>,
)

private const val MAX_ROWS = 50
private val sensitiveKey = Regex("secret|token|pin|password|passwd|credential|key", RegexOption.IGNORE_CASE)

/**
 * Đọc chỉ-đọc dữ liệu cục bộ của app cho tab "Lưu trữ" (ADR-0012): SharedPreferences, cơ sở dữ liệu SQLite (Room) và
 * tên các tệp trong kho bí mật. Không giải mã kho bí mật; giá trị prefs có khóa nghi là bí mật bị che.
 */
/** Tên tệp DB của Room trong `:core:database` (`DATABASE_NAME`); `:tools:debug` không phụ thuộc module đó nên lặp lại hằng số. */
private const val ROOM_DATABASE_PREFIX = "odv.db"

internal object StorageReader {

    fun read(context: Context): StorageSnapshot {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val preferences = prefsDir.listFiles { file -> file.extension == "xml" }.orEmpty()
            .sortedBy { it.name }
            .map { file ->
                val name = file.nameWithoutExtension
                val all = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
                PrefsFile(
                    name = name,
                    entries = all.entries.sortedBy { it.key }.map { (key, value) ->
                        key to if (sensitiveKey.containsMatchIn(key)) "***" else value.toString()
                    },
                )
            }
        val dataStoreFiles = File(context.filesDir, "datastore").list().orEmpty().sorted()
        val secretFiles = File(context.filesDir, "secrets").list().orEmpty().sorted()
        val databases = context.databaseList()
            .filterNot { it.endsWith("-journal") || it.endsWith("-wal") || it.endsWith("-shm") }
            .sorted()
            .map { dump(context, it) }
        return StorageSnapshot(preferences, dataStoreFiles, databases, secretFiles)
    }

    private fun dump(context: Context, name: String): DatabaseDump {
        val file = context.getDatabasePath(name)
        // Không mở DB của Room (`odv.db`) bằng SQLite của hệ thống: Room dùng `BundledSQLiteDriver` (bản SQLite riêng trong app), mà
        // hai bản SQLite cùng mở một tệp trong một tiến trình là lỗi hỏng DB đã biết (khóa tệp POSIX bị nhả khi bản kia đóng kết nối,
        // https://sqlite.org/howtocorrupt.html mục 2.2). Mở tab này rồi quét/đồng bộ đã gây "file is not a database" và crash
        // `libsqliteJni`. Chỉ báo dung lượng tệp.
        if (name.startsWith(ROOM_DATABASE_PREFIX)) {
            return DatabaseDump(name, file.length(), emptyList(), error = "đã tắt (mở bằng SQLite hệ thống làm hỏng DB của Room)")
        }
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val tables = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' ORDER BY name",
                    null,
                ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
                DatabaseDump(name, file.length(), tables.map { table(db, it) }, error = null)
            }
        } catch (e: Exception) {
            DatabaseDump(name, file.length(), emptyList(), error = e::class.simpleName)
        }
    }

    private fun table(db: SQLiteDatabase, table: String): TableDump {
        // Tên bảng lấy từ sqlite_master, không phải đầu vào người dùng; vẫn đặt trong dấu nháy kép.
        val quoted = "\"" + table.replace("\"", "\"\"") + "\""
        val count = db.rawQuery("SELECT COUNT(*) FROM $quoted", null).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        return db.rawQuery("SELECT * FROM $quoted LIMIT $MAX_ROWS", null).use { cursor ->
            val columns = cursor.columnNames.toList()
            // Che cả cột có vẻ chứa URL: Lát 3 sẽ lưu downloadUrl (có tempauth) trong Room.
            val masked = columns.map { sensitiveKey.containsMatchIn(it) || it.contains("url", ignoreCase = true) }
            val rows = buildList {
                while (cursor.moveToNext()) add(columns.indices.map { if (masked[it]) "***" else cell(cursor, it) })
            }
            TableDump(table, count, columns, rows)
        }
    }

    private fun cell(cursor: Cursor, index: Int): String = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> "null"
        Cursor.FIELD_TYPE_BLOB -> "<blob ${cursor.getBlob(index).size} byte>"
        else -> cursor.getString(index)
    }
}
