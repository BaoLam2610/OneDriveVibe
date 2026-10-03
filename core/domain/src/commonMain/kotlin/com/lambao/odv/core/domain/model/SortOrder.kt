package com.lambao.odv.core.domain.model

/** Trường sắp xếp của tab Thư mục (TM-05). */
enum class SortField { Name, Modified, Size }

enum class SortDirection { Ascending, Descending }

/**
 * Cách sắp xếp trong một thư mục (TM-05). Thư mục luôn đứng trước tệp bất kể [field] và [direction] (TM-02).
 * Mặc định là Tên, A đến Z.
 */
data class SortOrder(
    val field: SortField = SortField.Name,
    val direction: SortDirection = SortDirection.Ascending,
)

/**
 * Sắp xếp danh sách mục của một thư mục theo [order]: thư mục trước tệp (TM-02), rồi theo trường đã chọn (TM-05); hòa thì
 * theo tên để thứ tự ổn định. Dùng cho danh sách lấy trực tiếp từ API (TM-07); danh sách từ Room do repository tự sắp xếp.
 */
fun List<DriveItem>.sortedFor(order: SortOrder): List<DriveItem> {
    val byField: Comparator<DriveItem> = when (order.field) {
        SortField.Name -> compareBy { it.name.lowercase() }
        SortField.Modified -> compareBy { it.modifiedAt ?: 0L }
        SortField.Size -> compareBy { it.sizeBytes }
    }
    val directed = if (order.direction == SortDirection.Descending) byField.reversed() else byField
    return sortedWith(compareByDescending<DriveItem> { it.isFolder }.then(directed).thenBy { it.name.lowercase() })
}
