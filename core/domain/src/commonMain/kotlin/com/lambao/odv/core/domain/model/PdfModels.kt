package com.lambao.odv.core.domain.model

/**
 * Một tệp PDF cho màn xem (PD-08): [item] là bản ghi trong Room, [folderName] là tên thư mục chứa nó. Null khi PDF nằm ngay ở gốc hoặc
 * thư mục cha chưa có trong Room; giao diện tự dùng nhãn gốc. Số trang không nằm ở đây vì chỉ biết sau khi `PdfRenderer` mở được tệp.
 */
data class PdfFile(
    val item: DriveItem,
    val folderName: String?,
)

/**
 * Trang đang đọc của một PDF (PD-05). [page] đếm từ 0. [cTag] là phiên bản nội dung lúc lưu: tệp đổi nội dung thì tiến độ cũ không còn
 * đúng và bị bỏ qua khi đọc. [finished] là đã đọc tới trang cuối, tệp rời dải "Đọc tiếp" (DS-02, Lát 10).
 */
data class ReadingProgress(
    val itemId: String,
    val cTag: String?,
    val page: Int,
    val pageCount: Int,
    /** Epoch mili giây. */
    val updatedAt: Long,
    val finished: Boolean,
)
