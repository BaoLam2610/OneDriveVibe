package com.lambao.odv.core.domain.hook

/** Đọc chỉ-đọc các kho tùy chọn (DataStore) để công cụ debug hiển thị; không dùng ở luồng sản phẩm. */
interface PreferencesInspector {
    /** Tên kho → (khóa → giá trị dạng chữ), sắp theo khóa. Không ném ngoại lệ: kho đọc lỗi thì rỗng. */
    suspend fun dump(): Map<String, Map<String, String>>
}
