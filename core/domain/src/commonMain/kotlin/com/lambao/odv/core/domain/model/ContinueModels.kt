package com.lambao.odv.core.domain.model

/**
 * Vị trí xem dở của một video (VD-12). [cTag] là phiên bản nội dung lúc lưu: tệp đổi nội dung thì vị trí cũ không còn đúng và bị bỏ qua khi
 * đọc. [finished] là video đã chạy hết, rời dải "Xem tiếp" (DS-02); dừng ở chỗ khác thì vẫn là xem dở.
 */
data class WatchProgress(
    val itemId: String,
    val cTag: String?,
    val positionMs: Long,
    val durationMs: Long,
    /** Epoch mili giây. */
    val updatedAt: Long,
    val finished: Boolean,
)

/**
 * Một mục của dải "Xem tiếp / Đọc tiếp" (DS-02): tệp kèm chỗ đã dừng. [parentId] là thư mục chứa tệp (`DriveItem` không mang trường này):
 * mở video từ dải thì danh sách phát là các video cùng thư mục đó (VD-10).
 */
sealed interface ContinueEntry {
    val item: DriveItem
    val parentId: String?

    /** Video xem dở: dừng ở [positionMs] trên tổng [durationMs]. */
    data class Video(
        override val item: DriveItem,
        override val parentId: String?,
        val positionMs: Long,
        val durationMs: Long,
    ) : ContinueEntry

    /** PDF đọc dở: đang ở [page] (đếm từ 0) trên tổng [pageCount] trang. */
    data class Pdf(
        override val item: DriveItem,
        override val parentId: String?,
        val page: Int,
        val pageCount: Int,
    ) : ContinueEntry
}
