package com.lambao.odv.core.domain.model

/** Loại media app hỗ trợ (đặc tả mục 5.1). */
enum class MediaKind { Image, Video, Pdf }

/**
 * Một mục trong drive: thư mục hoặc tệp. Lát 1 chỉ cần các trường để duyệt cây; ngày sửa, cTag... thêm khi có Room (Lát 3).
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
