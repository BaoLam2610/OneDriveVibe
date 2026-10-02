package com.lambao.odv.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mọi trường đều tùy chọn vì Graph có thể bỏ thiếu (onedrive-graph-responses.md mục 4, điểm 9). */
@Serializable
data class DriveDto(
    val id: String,
    val driveType: String? = null,
    val quota: QuotaDto? = null,
)

/** Đơn vị byte; cần 64-bit. */
@Serializable
data class QuotaDto(
    val total: Long = 0,
    val used: Long = 0,
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
