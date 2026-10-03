package com.lambao.odv.core.domain.model

/** Một thư mục trên đường dẫn: [id] để mở lại đúng thư mục đó, [name] để hiển thị. */
data class FolderRef(val id: String, val name: String)

/**
 * Một kết quả tìm kiếm (DS-03). [parentPath] là các thư mục cha từ cấp dưới gốc xuống thư mục chứa mục; rỗng nếu mục
 * nằm ngay ở gốc. UI tự nối tên thành dòng đường dẫn theo ngôn ngữ; chạm một thư mục kết quả thì mở bằng chính đường dẫn này.
 */
data class SearchResult(
    val item: DriveItem,
    val parentPath: List<FolderRef>,
)
