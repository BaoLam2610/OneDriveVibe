package com.lambao.odv.core.domain.model

/** Thông tin drive dùng cho bước kiểm tra kết nối (KN-07, KN-08). Đơn vị byte. */
data class DriveInfo(
    val id: String,
    /** Thường là `business`. Null nếu máy chủ không trả. */
    val driveType: String?,
    val usedBytes: Long,
    val totalBytes: Long,
)
