package com.lambao.odv.core.database

internal object DatabaseConstants {
    /** Tên tệp CSDL Room. **Không được đổi giá trị:** đổi là người dùng mất dữ liệu đã đồng bộ. */
    const val DATABASE_NAME = "odv.db"

    /** Số tham số `?` tối đa cho một câu `IN (...)`; dưới giới hạn của SQLite để an toàn với drive lớn. */
    const val IN_CHUNK = 500
}
