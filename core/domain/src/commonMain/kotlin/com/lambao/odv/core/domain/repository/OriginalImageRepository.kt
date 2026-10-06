package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.OriginalImageRef
import com.lambao.odv.core.domain.model.OriginalImageState
import kotlinx.coroutines.flow.Flow

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
