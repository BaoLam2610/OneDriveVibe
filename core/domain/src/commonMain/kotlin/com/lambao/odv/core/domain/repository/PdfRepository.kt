package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.model.PdfFile
import kotlinx.coroutines.flow.Flow

/** Kho PDF trên máy (PD-01, BN-01 → BN-03) và thông tin tệp cho bảng thông tin (PD-08). Domain không dùng kiểu tệp nền tảng nên đường dẫn là chuỗi. */
interface PdfRepository {
    /**
     * Cung cấp tệp PDF của [ref] với cùng hợp đồng như `OriginalImageRepository.open`: có sẵn thì phát `Ready` ngay (không cần mạng), chưa
     * thì tải kèm `Downloading`, bị gián đoạn thì lần sau tải tiếp (BN-03), hủy collect thì dừng tải nhưng giữ phần đã có, lỗi phát `Failed`.
     */
    fun open(ref: CachedFileRef): Flow<CachedFileState>

    /** Xóa tệp (cả bản dở) của [ref] khỏi cache, để lần `open` sau tải lại từ đầu. Dùng khi tệp tải đủ nhưng hỏng, không mở được (PD-07). */
    suspend fun discard(ref: CachedFileRef)

    /** Bản ghi và thư mục chứa của PDF [itemId] từ Room (dùng được offline). Null nếu không còn trong Room (đã xóa trên OneDrive, DS-06). */
    suspend fun getFile(itemId: String): PdfFile?
}
