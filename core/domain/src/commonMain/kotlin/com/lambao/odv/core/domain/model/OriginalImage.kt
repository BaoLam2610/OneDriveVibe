package com.lambao.odv.core.domain.model

import com.lambao.odv.core.common.error.AppError

/**
 * Khóa nhận diện ảnh gốc để tải và cache (BN-02): id tệp và phiên bản nội dung [cTag]. Đổi nội dung thì bản cũ bị bỏ;
 * đổi tên hay di chuyển thì vẫn dùng lại.
 */
data class OriginalImageRef(
    val itemId: String,
    val cTag: String?,
)

/** Trạng thái tải ảnh gốc (AN-01). */
sealed interface OriginalImageState {
    /** Đang tải: [receivedBytes] đã có trên đĩa, [totalBytes] là dung lượng cả tệp nếu đã biết. */
    data class Downloading(val receivedBytes: Long, val totalBytes: Long?) : OriginalImageState

    /** Đã đủ tệp trên đĩa tại [path] (đường dẫn tuyệt đối). Tệp chỉ xuất hiện ở đây sau khi tải đủ và đúng dung lượng (BN-03). */
    data class Ready(val path: String) : OriginalImageState

    data class Failed(val error: AppError) : OriginalImageState
}
