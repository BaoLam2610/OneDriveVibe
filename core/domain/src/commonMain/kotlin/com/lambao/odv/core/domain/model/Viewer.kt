package com.lambao.odv.core.domain.model

/**
 * Ngữ cảnh mở một tệp trong màn xem: quyết định danh sách mục để vuốt trước/sau (AN-03). Chỉ mang tham số tối thiểu để
 * route điều hướng lưu được qua process death; màn xem tự nạp danh sách từ Room theo ngữ cảnh này.
 */
sealed interface ViewerContext {
    /**
     * Mở từ tab Thư mục (kể cả từ kết quả tìm): các tệp cùng thư mục [folderId] (null = gốc), theo [sort] đang chọn.
     * [resumeFromSaved] là mở từ dải "Xem tiếp / Đọc tiếp" (DS-02, Lát 10): video vào thẳng vị trí đã dừng, không xét cài đặt "Khi mở
     * lại video xem dở" (VD-12). Chỉ màn xem video đọc cờ này; để trong ngữ cảnh vì ngữ cảnh đã đi hết chuỗi điều hướng và route.
     */
    data class Folder(val folderId: String?, val sort: SortOrder, val resumeFromSaved: Boolean = false) : ViewerContext

    /**
     * Mở từ tab Thư viện: các mục thuộc cùng bộ lọc [filter] (TV-03). [query] khác null là mở từ kết quả tìm (TV-07): màn xem chỉ vuốt trong
     * các mục có tên khớp từ khóa đó, cùng thứ tự với lưới kết quả. Mặc định null nên route lưu từ phiên bản trước vẫn khôi phục được.
     */
    data class Library(val filter: LibraryFilter, val query: String? = null) : ViewerContext
}

/**
 * Thông tin ảnh cho bảng thông tin (AN-05), lấy theo yêu cầu từ Graph. Trường nào không có dữ liệu thì null và UI ẩn dòng.
 * Tên, dung lượng, đường dẫn thư mục và ngày đã có trong Room nên không nằm ở đây.
 */
data class ImageInfo(
    val width: Int? = null,
    val height: Int? = null,
    /** Hãng và model máy ảnh đã nối thành một chuỗi, ví dụ "Hãng Model". */
    val camera: String? = null,
    /** Ngày chụp, epoch mili giây. */
    val takenAt: Long? = null,
)
