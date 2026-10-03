package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.error.AppError
import kotlinx.coroutines.flow.Flow

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

/**
 * Kho ảnh gốc trên máy (AN-01, BN-01 → BN-03), tách khỏi cache thumbnail vì thumbnail cố ý chất lượng thấp. Domain không
 * dùng kiểu tệp của nền tảng nên đường dẫn là chuỗi.
 */
interface OriginalImageRepository {
    /**
     * Cung cấp ảnh gốc của [ref]: có sẵn trong cache thì phát [OriginalImageState.Ready] ngay (không cần mạng); chưa thì tải,
     * phát [OriginalImageState.Downloading] theo tiến trình rồi [OriginalImageState.Ready]. Bị gián đoạn thì lần sau tải tiếp
     * phần còn thiếu (BN-03). Hủy collect thì dừng tải nhưng giữ phần đã có. Lỗi phát [OriginalImageState.Failed] (flow
     * không ném ngoại lệ).
     */
    fun open(ref: OriginalImageRef): Flow<OriginalImageState>
}
