package com.lambao.odv.tools.debug

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room3.executeSQL
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_NULL
import com.lambao.odv.core.database.OdvDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform

/** Tên tệp DB của Room (`DatabaseConstants.DATABASE_NAME` là `internal` của `:core:database` nên lặp lại giá trị ở đây). */
internal const val ROOM_DATABASE_NAME = "odv.db"

/** Số dòng mỗi trang ở màn chi tiết bảng. */
internal const val TABLE_PAGE_SIZE = 50

private const val BLOB_PREFIX = "<blob "

/** Cột bị che khi bật công tắc che: tên nghi là bí mật, hoặc chứa URL (downloadUrl có tempauth). */
internal fun isSensitiveColumn(name: String): Boolean = sensitiveKey.containsMatchIn(name) || name.contains("url", ignoreCase = true)

internal class RawRows(val columns: List<String>, val rows: List<List<String>>)

/**
 * Cổng chạy SQL thô trên một tệp DB. Có hai bản vì không được mở `odv.db` bằng SQLite hệ thống (hai bản SQLite cùng mở một tệp
 * làm hỏng DB, xem [StorageReader]): [RoomSqlSource] cho `odv.db`, [SystemSqlSource] cho các DB khác (vd. Media3).
 */
internal interface SqlSource {
    /** Chạy truy vấn đọc; ô NULL là "null", ô BLOB là "<blob N byte>", còn lại là chữ. */
    suspend fun query(sql: String, args: List<String> = emptyList()): RawRows

    suspend fun execute(sql: String)
}

internal fun sqlSourceFor(context: Context, dbName: String): SqlSource =
    if (dbName == ROOM_DATABASE_NAME) {
        RoomSqlSource(KoinPlatform.getKoin().get<OdvDatabase>())
    } else {
        SystemSqlSource(context.getDatabasePath(dbName).path)
    }

private class RoomSqlSource(private val db: OdvDatabase) : SqlSource {
    override suspend fun query(sql: String, args: List<String>): RawRows = db.useReaderConnection { connection ->
        connection.usePrepared(sql) { statement ->
            args.forEachIndexed { index, value -> statement.bindText(index + 1, value) }
            // Đọc tên cột trước khi step: sau khi hết dòng, một số driver không cho đọc metadata.
            val columns = statement.getColumnNames()
            val rows = mutableListOf<List<String>>()
            while (statement.step()) {
                rows += List(statement.getColumnCount()) { i ->
                    when (statement.getColumnType(i)) {
                        SQLITE_DATA_NULL -> "null"
                        SQLITE_DATA_BLOB -> "$BLOB_PREFIX${statement.getBlob(i).size} byte>"
                        else -> statement.getText(i)
                    }
                }
            }
            RawRows(columns, rows)
        }
    }

    // useWriterConnection tự làm mới InvalidationTracker nên các Flow của UI (Room) cập nhật sau khi xóa bảng.
    override suspend fun execute(sql: String) = db.useWriterConnection { it.executeSQL(sql) }
}

private class SystemSqlSource(private val path: String) : SqlSource {
    override suspend fun query(sql: String, args: List<String>): RawRows = withContext(Dispatchers.IO) {
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery(sql, args.toTypedArray()).use { cursor ->
                val rows = buildList {
                    while (cursor.moveToNext()) add(List(cursor.columnCount) { cell(cursor, it) })
                }
                RawRows(cursor.columnNames.toList(), rows)
            }
        }
    }

    override suspend fun execute(sql: String) = withContext(Dispatchers.IO) {
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE).use { it.execSQL(sql) }
    }

    private fun cell(cursor: Cursor, index: Int): String = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> "null"
        Cursor.FIELD_TYPE_BLOB -> "$BLOB_PREFIX${cursor.getBlob(index).size} byte>"
        else -> cursor.getString(index)
    }
}

internal class TableInfo(val name: String, val rowCount: Long)

/** Một trang kết quả: [total] là số dòng khớp từ khóa (hoặc cả bảng nếu không tìm), [columns] đã gồm cả cột bị che. */
internal class TablePage(val columns: List<String>, val rows: List<List<String>>, val total: Long) {
    val pageCount: Int get() = maxOf(1, ((total + TABLE_PAGE_SIZE - 1) / TABLE_PAGE_SIZE).toInt())
}

/** Liệt kê, phân trang, tìm kiếm và xóa bảng của một DB qua [SqlSource]. Mọi tên bảng/cột đều lấy từ chính DB và được đặt trong nháy kép. */
internal class DatabaseBrowser(private val source: SqlSource) {

    suspend fun tables(): List<TableInfo> {
        // room_master_table giữ mã băm schema: xóa nó thì lần mở sau Room báo hỏng, và nó không phải dữ liệu của app.
        val names = source.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
                "AND name <> 'room_master_table' ORDER BY name",
        ).rows.map { it[0] }
        return names.map { TableInfo(it, count(it)) }
    }

    /**
     * Trang [page] (từ 0) của [table]. [query] rỗng thì lấy cả bảng; khác rỗng thì lọc dòng có ô nào chứa [query] (không phân biệt
     * hoa thường). Khi [mask] bật, cột nhạy cảm bị che ("***") và không được tìm (nếu không, tìm kiếm là cách dò giá trị bị che).
     */
    suspend fun page(table: String, query: String, page: Int, mask: Boolean): TablePage {
        val quoted = quote(table)
        val columns = source.query("PRAGMA table_info($quoted)").rows.map { it[1] }
        val masked = columns.map { mask && isSensitiveColumn(it) }
        val searchable = columns.filterIndexed { index, _ -> !masked[index] }
        val where: String
        val args: List<String>
        if (query.isEmpty()) {
            where = ""
            args = emptyList()
        } else if (searchable.isEmpty()) {
            where = " WHERE 0"
            args = emptyList()
        } else {
            where = " WHERE " + searchable.joinToString(" OR ") { "CAST(${quote(it)} AS TEXT) LIKE ? ESCAPE '\\'" }
            args = List(searchable.size) { "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%" }
        }
        val total = source.query("SELECT COUNT(*) FROM $quoted$where", args).rows.first()[0].toLong()
        // Bảng WITHOUT ROWID không có cột rowid; thứ tự mặc định vẫn ổn định trong một lần đọc.
        val order = if (hasRowid(table)) " ORDER BY rowid" else ""
        val data = source.query("SELECT * FROM $quoted$where$order LIMIT $TABLE_PAGE_SIZE OFFSET ${page.toLong() * TABLE_PAGE_SIZE}", args)
        val rows = data.rows.map { row -> row.mapIndexed { index, cell -> if (masked.getOrElse(index) { false }) "***" else cell } }
        return TablePage(data.columns, rows, total)
    }

    suspend fun clear(table: String) = source.execute("DELETE FROM ${quote(table)}")

    private suspend fun count(table: String): Long = source.query("SELECT COUNT(*) FROM ${quote(table)}").rows.first()[0].toLong()

    private suspend fun hasRowid(table: String): Boolean {
        val sql = source.query("SELECT sql FROM sqlite_master WHERE type='table' AND name = ?", listOf(table)).rows.firstOrNull()?.get(0)
        return sql == null || !sql.contains("WITHOUT ROWID", ignoreCase = true)
    }

    private fun quote(name: String) = "\"" + name.replace("\"", "\"\"") + "\""
}
