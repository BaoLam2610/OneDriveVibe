package com.lambao.odv.core.domain.model

import com.lambao.odv.core.common.error.AppError

/**
 * Khóa nhận diện một tệp tải về để cache (BN-02): id tệp và phiên bản nội dung [cTag]. Đổi nội dung thì bản cũ bị bỏ;
 * đổi tên hay di chuyển thì vẫn dùng lại. Dùng chung cho ảnh gốc và PDF.
 */
data class CachedFileRef(
    val itemId: String,
    val cTag: String?,
)

/** Trạng thái tải một tệp về cache (AN-01, PD-01): ảnh gốc và PDF dùng chung ba trạng thái này. */
sealed interface CachedFileState {
    /** Đang tải: [receivedBytes] đã có trên đĩa, [totalBytes] là dung lượng cả tệp nếu đã biết. */
    data class Downloading(val receivedBytes: Long, val totalBytes: Long?) : CachedFileState

    /** Đã đủ tệp trên đĩa tại [path] (đường dẫn tuyệt đối). Tệp chỉ xuất hiện ở đây sau khi tải đủ và đúng dung lượng (BN-03). */
    data class Ready(val path: String) : CachedFileState

    data class Failed(val error: AppError) : CachedFileState
}
