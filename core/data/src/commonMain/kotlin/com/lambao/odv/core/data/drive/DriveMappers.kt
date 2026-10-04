package com.lambao.odv.core.data.drive

import com.lambao.odv.core.database.DriveItemEntity
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ImageInfo
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.VideoMeta
import com.lambao.odv.core.domain.model.libraryDateOf
import com.lambao.odv.core.domain.model.mediaKindOf
import com.lambao.odv.core.network.GraphCredentials
import com.lambao.odv.core.network.dto.DriveItemDto
import kotlin.time.Instant

internal fun ConnectionConfig.toCredentials() = GraphCredentials(tenantId, clientId, clientSecret, upn)

/** Ngày Graph trả là ISO 8601 UTC; giá trị lạ hoặc thiếu thì null thay vì làm hỏng cả trang đồng bộ. */
private fun parseInstantMs(value: String?): Long? =
    value?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

/**
 * Ánh xạ sang hàng Room cho delta (DB-01); null với mục không duyệt được (sổ tay OneNote, như [toDomain]).
 * Gọi sau khi đã loại mục `deleted` và `root`. [scanId] là lần quét đầy đủ đang chạy.
 */
internal fun DriveItemDto.toEntity(scanId: Long): DriveItemEntity? {
    if (packageFacet != null) return null
    val isFolder = folder != null
    val createdAt = parseInstantMs(fileSystemInfo?.createdDateTime) ?: parseInstantMs(createdDateTime)
    val takenAt = parseInstantMs(photo?.takenDateTime)
    val modifiedAt = parseInstantMs(lastModifiedDateTime)
    return DriveItemEntity(
        id = id,
        parentId = parentReference?.id,
        name = name,
        nameKey = searchKey(name),
        sizeBytes = size ?: 0,
        isFolder = isFolder,
        mediaKind = if (isFolder) null else mediaKindOf(name, file?.mimeType)?.name,
        childCount = folder?.childCount,
        durationMs = video?.duration,
        videoWidth = video?.width,
        videoHeight = video?.height,
        videoFrameRate = video?.frameRate,
        videoBitRate = video?.bitRate,
        videoFourCc = video?.fourCC,
        modifiedAt = modifiedAt,
        cTag = cTag,
        takenAt = takenAt,
        createdAt = createdAt,
        sortDate = libraryDateOf(takenAt, createdAt, modifiedAt),
        scanId = scanId,
    )
}

/** Thông tin ảnh cho bảng thông tin (AN-05). Hãng và model nối thành một chuỗi; thiếu cả hai thì null để ẩn dòng. */
internal fun DriveItemDto.toImageInfo() = ImageInfo(
    width = image?.width,
    height = image?.height,
    camera = listOfNotNull(photo?.cameraMake, photo?.cameraModel)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifEmpty { null },
    takenAt = parseInstantMs(photo?.takenDateTime),
)

internal fun DriveItemEntity.toDomain() = DriveItem(
    id = id,
    name = name,
    sizeBytes = sizeBytes,
    isFolder = isFolder,
    mediaKind = mediaKind?.let { stored -> MediaKind.entries.firstOrNull { it.name == stored } },
    childCount = childCount,
    durationMs = durationMs,
    modifiedAt = modifiedAt,
    takenAt = takenAt,
    createdAt = createdAt,
    cTag = cTag,
    video = videoMetaOf(videoWidth, videoHeight, videoFrameRate, videoBitRate, videoFourCc),
)

/** Gom các cột video thành [VideoMeta]; null khi không có cột nào (mục không phải video, hoặc Graph không trả facet). */
private fun videoMetaOf(width: Int?, height: Int?, frameRate: Double?, bitRate: Long?, fourCc: String?): VideoMeta? =
    if (width == null && height == null && frameRate == null && bitRate == null && fourCc == null) {
        null
    } else {
        VideoMeta(width, height, frameRate, bitRate, fourCc)
    }

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
        video = video?.let { videoMetaOf(it.width, it.height, it.frameRate, it.bitRate, it.fourCC) },
    )
}
