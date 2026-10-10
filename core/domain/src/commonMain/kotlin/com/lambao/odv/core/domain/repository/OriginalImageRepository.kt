package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import kotlinx.coroutines.flow.Flow

/**
 * Kho ảnh gốc trên máy (AN-01, BN-01 → BN-03), tách khỏi cache thumbnail vì thumbnail cố ý chất lượng thấp. Domain không
 * dùng kiểu tệp của nền tảng nên đường dẫn là chuỗi.
 */
interface OriginalImageRepository {
    /**
     * Cung cấp ảnh gốc của [ref]: có sẵn trong cache thì phát [CachedFileState.Ready] ngay (không cần mạng); chưa thì tải,
     * phát [CachedFileState.Downloading] theo tiến trình rồi [CachedFileState.Ready]. Bị gián đoạn thì lần sau tải tiếp
     * phần còn thiếu (BN-03). Hủy collect thì dừng tải nhưng giữ phần đã có. Lỗi phát [CachedFileState.Failed] (flow
     * không ném ngoại lệ).
     */
    fun open(ref: CachedFileRef): Flow<CachedFileState>
}
