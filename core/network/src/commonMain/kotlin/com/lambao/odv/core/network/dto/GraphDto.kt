package com.lambao.odv.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chỉ dùng để xác nhận drive tồn tại (KN-08). Không có `quota`: app không đọc, không lưu, không hiển thị dung lượng
 * (KN-12), nên `used > total` (`state = exceeded`) không ảnh hưởng gì.
 */
@Serializable
data class DriveDto(
    val id: String,
    val driveType: String? = null,
)

@Serializable
data class DriveItemDto(
    val id: String,
    /** Mục đã xóa trong delta chỉ có `id` và `deleted`, không có tên; do đó có mặc định. */
    val name: String = "",
    val size: Long? = null,
    val folder: FolderFacetDto? = null,
    val file: FileFacetDto? = null,
    val video: VideoFacetDto? = null,
    /** Có với sổ tay OneNote (`package.type = oneNote`): là thư mục nhưng không duyệt được. */
    @SerialName("package") val packageFacet: PackageFacetDto? = null,
    // Các trường dưới đây chỉ có ở delta (Lát 3, ADR-0007); `listChildren` dùng `$select` hẹp nên chúng là null.
    /** Có (kể cả rỗng) khi mục đã bị xóa trên OneDrive: delta chỉ gửi `id` và `deleted` (DS-06). */
    val deleted: DeletedFacetDto? = null,
    /** Chỉ có ở thư mục gốc của drive; dùng để biết id gốc, không lưu như một mục. */
    val root: RootFacetDto? = null,
    val parentReference: ParentReferenceDto? = null,
    val cTag: String? = null,
    /** ISO 8601 UTC. Ngày sửa trên OneDrive (TM-05). */
    val lastModifiedDateTime: String? = null,
    /** Ngày tải lên OneDrive (TV-02, mức cuối). */
    val createdDateTime: String? = null,
    val fileSystemInfo: FileSystemInfoDto? = null,
    val photo: PhotoFacetDto? = null,
    /** Kích thước ảnh. Không nằm trong `DELTA_SELECT`: chỉ lấy theo yêu cầu khi mở bảng thông tin (AN-05). */
    val image: ImageFacetDto? = null,
)

@Serializable
data class DeletedFacetDto(val state: String? = null)

@Serializable
class RootFacetDto

@Serializable
data class ParentReferenceDto(val id: String? = null)

/** Ngày của tệp gốc trên máy người dùng (TV-02, mức giữa). */
@Serializable
data class FileSystemInfoDto(val createdDateTime: String? = null)

/** Ngày chụp (TV-02, mức đầu) và thiết bị chụp (AN-05, chỉ đọc khi mở bảng thông tin). */
@Serializable
data class PhotoFacetDto(
    val takenDateTime: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
)

/** Chiều rộng, cao của ảnh tính bằng pixel (AN-05). */
@Serializable
data class ImageFacetDto(val width: Int? = null, val height: Int? = null)

@Serializable
data class FolderFacetDto(val childCount: Int? = null)

@Serializable
data class FileFacetDto(val mimeType: String? = null)

/** `duration` tính bằng mili giây. */
@Serializable
data class VideoFacetDto(val duration: Long? = null)

@Serializable
data class PackageFacetDto(val type: String? = null)

@Serializable
internal data class ChildrenPageDto(
    val value: List<DriveItemDto> = emptyList(),
    @SerialName("@odata.nextLink") val nextLink: String? = null,
)

/**
 * Một trang của `delta`: còn trang thì có [nextLink], trang cuối có [deltaLink] (mốc cho lần đồng bộ sau, DB-02).
 */
@Serializable
data class DeltaPageDto(
    val value: List<DriveItemDto> = emptyList(),
    @SerialName("@odata.nextLink") val nextLink: String? = null,
    @SerialName("@odata.deltaLink") val deltaLink: String? = null,
)

@Serializable
internal data class GraphErrorEnvelopeDto(val error: GraphErrorBodyDto? = null)

@Serializable
internal data class GraphErrorBodyDto(val code: String? = null)
