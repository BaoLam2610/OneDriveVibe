package com.lambao.odv.tools.debug

import android.content.Context
import kotlinx.coroutines.CancellationException
import java.io.File

internal class PrefsFile(val name: String, val entries: List<Pair<String, String>>)

internal class DatabaseDump(val name: String, val sizeBytes: Long, val tables: List<TableInfo>, val error: String?)

internal class StorageSnapshot(
    val preferences: List<PrefsFile>,
    /** Kho DataStore đọc qua [DebugHooks.dumpPreferences] (tệp protobuf, không đọc trực tiếp được). */
    val dataStore: List<PrefsFile>,
    val databases: List<DatabaseDump>,
    /** Tên tệp trong kho bí mật mã hóa. Chỉ liệt kê tên, không bao giờ giải mã. */
    val secretFiles: List<String>,
)

internal val sensitiveKey = Regex("secret|token|pin|password|passwd|credential|key", RegexOption.IGNORE_CASE)

/**
 * Đọc dữ liệu cục bộ của app cho tab "Lưu trữ" (ADR-0012): SharedPreferences, danh sách bảng của các DB và tên các tệp trong kho
 * bí mật. Nội dung bảng xem ở màn chi tiết bảng ([DatabaseBrowser]). Không giải mã kho bí mật; giá trị prefs có khóa nghi là bí
 * mật bị che.
 */
internal object StorageReader {

    suspend fun read(context: Context): StorageSnapshot {
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
        val dataStore = DebugHooks.dumpPreferences?.invoke().orEmpty().entries.sortedBy { it.key }.map { (name, entries) ->
            PrefsFile(name, entries.entries.map { (key, value) -> key to if (sensitiveKey.containsMatchIn(key)) "***" else value })
        }
        val secretFiles = File(context.filesDir, "secrets").list().orEmpty().sorted()
        val databases = context.databaseList()
            // `.lck`: tệp khóa liên tiến trình của Room bundled, không phải DB (mở nó sẽ hiện lại bảng của odv.db).
            .filterNot { it.endsWith("-journal") || it.endsWith("-wal") || it.endsWith("-shm") || it.endsWith(".lck") }
            .sorted()
            .map { dump(context, it) }
        return StorageSnapshot(preferences, dataStore, databases, secretFiles)
    }

    // `odv.db` đi qua kết nối của chính Room, các DB khác (Media3...) qua SQLite hệ thống: xem SqlSource. Không bao giờ mở `odv.db`
    // bằng SQLite hệ thống (hỏng DB, "file is not a database", Nhật ký 2026-10-04).
    private suspend fun dump(context: Context, name: String): DatabaseDump {
        val size = context.getDatabasePath(name).length()
        return try {
            DatabaseDump(name, size, DatabaseBrowser(sqlSourceFor(context, name)).tables(), error = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DatabaseDump(name, size, emptyList(), error = e::class.simpleName)
        }
    }
}
