package com.lambao.odv.core.data.drive

import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveInfo
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.mediaKindOf
import com.lambao.odv.core.network.GraphCredentials
import com.lambao.odv.core.network.dto.DriveDto
import com.lambao.odv.core.network.dto.DriveItemDto

internal fun ConnectionConfig.toCredentials() = GraphCredentials(tenantId, clientId, clientSecret, upn)

internal fun DriveDto.toDomain() = DriveInfo(
    id = id,
    driveType = driveType,
    usedBytes = quota?.used ?: 0,
    totalBytes = quota?.total ?: 0,
)

/**
 * Null cho mục không duyệt được: sổ tay OneNote (`package`) có `folder` nhưng không phải thư mục thật.
 * Tệp không hỗ trợ vẫn được giữ (mediaKind = null) để tầng trên quyết định ẩn hay hiện mờ (TM-03).
 */
internal fun DriveItemDto.toDomain(): DriveItem? {
    if (packageFacet != null) return null
    val isFolder = folder != null
    return DriveItem(
        id = id,
        name = name,
        sizeBytes = size ?: 0,
        isFolder = isFolder,
        mediaKind = if (isFolder) null else mediaKindOf(name, file?.mimeType),
        childCount = folder?.childCount,
        durationMs = video?.duration,
    )
}
