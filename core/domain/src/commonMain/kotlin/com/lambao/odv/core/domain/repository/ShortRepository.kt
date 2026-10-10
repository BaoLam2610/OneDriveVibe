package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.ShortVideo
import kotlinx.coroutines.flow.Flow

/** Dữ liệu cho tab Short (mục 3.4.4): danh sách id video và chi tiết từng video, đọc từ Room nên dùng được offline (DB-05). */
interface ShortRepository {

    /**
     * Id mọi video trong drive (mọi thư mục) có thời lượng nhỏ hơn hoặc bằng [maxDurationMs] (SV-01); video không có thời lượng bị loại.
     * Theo thứ tự gốc cố định (không xáo: xáo là việc của tầng trình bày, SV-02) và tự phát lại khi đồng bộ đổi dữ liệu, nên video bị xóa
     * trên OneDrive biến mất khỏi danh sách (DS-06, SV-16). Chỉ trả id để không giữ cả bản ghi trong bộ nhớ (ADR-0024).
     */
    fun observeVideoIds(maxDurationMs: Long): Flow<List<String>>

    /** Chi tiết một video kèm tên thư mục chứa nó; null nếu không còn trong Room. */
    suspend fun getVideo(id: String): ShortVideo?
}
