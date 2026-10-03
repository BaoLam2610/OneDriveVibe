package com.lambao.odv.core.domain.model

/** Kích thước thumbnail cần: ô vuông nhỏ (Thư viện, hàng danh sách) hoặc thẻ lưới Thư mục. */
enum class ThumbnailSize { Cell, Card }

/**
 * Khóa nhận diện một thumbnail để tải và cache (BN-02): theo id tệp và phiên bản nội dung [cTag], không theo tên hay
 * đường dẫn, nên đổi tên hay di chuyển vẫn dùng lại cache còn đổi nội dung thì bản cũ không được dùng nữa.
 * Giao diện chỉ truyền đối tượng này cho trình tải ảnh; việc gọi Graph nằm ở `:core:data`.
 */
data class ThumbnailSource(
    val itemId: String,
    val cTag: String?,
    val size: ThumbnailSize,
)

/** Thumbnail của tệp ảnh/video; null cho thư mục, PDF và tệp không hỗ trợ (giữ ô giữ chỗ theo loại). */
fun DriveItem.thumbnailSource(size: ThumbnailSize): ThumbnailSource? =
    if (!isFolder && (mediaKind == MediaKind.Image || mediaKind == MediaKind.Video)) ThumbnailSource(id, cTag, size) else null
