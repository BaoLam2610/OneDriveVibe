package com.lambao.odv.core.domain.model

/** Loại media app hỗ trợ (đặc tả mục 5.1). */
enum class MediaKind { Image, Video, Pdf }

/**
 * Một mục trong drive: thư mục hoặc tệp. Các trường có giá trị mặc định (ngày, cTag) chỉ có khi mục đến từ Room
 * (đồng bộ delta, Lát 3); mục lấy trực tiếp từ API để duyệt (TM-07) không có chúng.
 */
data class DriveItem(
    val id: String,
    val name: String,
    /** Byte. Tệp: dung lượng tệp; thư mục: tổng bên trong. */
    val sizeBytes: Long,
    val isFolder: Boolean,
    /** Loại media nếu là tệp được hỗ trợ; null cho thư mục và tệp không hỗ trợ (TM-03). */
    val mediaKind: MediaKind?,
    /** Số mục con trực tiếp của thư mục (null nếu máy chủ không trả hoặc là tệp). */
    val childCount: Int?,
    /** Thời lượng video, mili giây. */
    val durationMs: Long?,
    /** Ngày sửa trên OneDrive, epoch mili giây; null nếu máy chủ không trả (TM-05). */
    val modifiedAt: Long? = null,
    /** Ngày chụp, epoch mili giây (TV-02, mức đầu). */
    val takenAt: Long? = null,
    /** Ngày tạo tệp gốc rồi tới ngày tải lên, epoch mili giây (TV-02, hai mức sau). */
    val createdAt: Long? = null,
    /** Dấu nhận biết nội dung đổi, để làm mới thumbnail và cache (BN-02, Lát 4). */
    val cTag: String? = null,
    /** Thông tin kỹ thuật của video (VD-17); chỉ có khi mục đến từ Room hoặc API và Graph có trả. */
    val video: VideoMeta? = null,
)

/** Độ phân giải, tốc độ khung hình, bitrate và codec của video cho bảng thông tin (VD-17). Trường nào không có thì null và UI ẩn dòng. */
data class VideoMeta(
    val width: Int? = null,
    val height: Int? = null,
    /** Khung hình/giây, có thể lẻ (28,83). */
    val frameRate: Double? = null,
    /** Bit/giây. */
    val bitRate: Long? = null,
    /** Mã codec Graph trả, ví dụ `H264`, `AV01`. */
    val fourCc: String? = null,
)

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
private val videoExtensions = setOf("mp4", "m4v", "mkv", "webm", "mov", "3gp")

/**
 * Nhận diện loại media: ưu tiên `mimeType`, không có thì theo đuôi tên tệp (đặc tả mục 5.1).
 * `application/octet-stream` coi như không có thông tin nên rơi về đuôi tên tệp.
 */
fun mediaKindOf(name: String, mimeType: String?): MediaKind? {
    val mime = mimeType?.lowercase()?.takeUnless { it == "application/octet-stream" }
    if (mime != null) {
        when {
            mime.startsWith("image/") -> return MediaKind.Image
            mime.startsWith("video/") -> return MediaKind.Video
            mime == "application/pdf" -> return MediaKind.Pdf
        }
    }
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when {
        extension in imageExtensions -> MediaKind.Image
        extension in videoExtensions -> MediaKind.Video
        extension == "pdf" -> MediaKind.Pdf
        else -> null
    }
}
