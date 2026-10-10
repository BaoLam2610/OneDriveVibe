package com.lambao.odv.core.domain.model

/**
 * Một video của tab Short (SV-01, SV-09): [item] là bản ghi video, [folderName] là tên thư mục chứa nó để hiện ở góc dưới. Null khi
 * video nằm ngay ở gốc hoặc thư mục cha chưa có trong Room; giao diện tự dùng nhãn gốc.
 */
data class ShortVideo(
    val item: DriveItem,
    val folderName: String?,
)
