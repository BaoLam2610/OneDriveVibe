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
    val name: String,
    val size: Long? = null,
    val folder: FolderFacetDto? = null,
    val file: FileFacetDto? = null,
    val video: VideoFacetDto? = null,
    /** Có với sổ tay OneNote (`package.type = oneNote`): là thư mục nhưng không duyệt được. */
    @SerialName("package") val packageFacet: PackageFacetDto? = null,
)

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

@Serializable
internal data class GraphErrorEnvelopeDto(val error: GraphErrorBodyDto? = null)

@Serializable
internal data class GraphErrorBodyDto(val code: String? = null)
