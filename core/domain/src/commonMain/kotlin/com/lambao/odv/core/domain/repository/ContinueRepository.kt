package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.ContinueEntry
import com.lambao.odv.core.domain.model.MediaKind
import kotlinx.coroutines.flow.Flow

/** Dải "Xem tiếp / Đọc tiếp" (DS-02): gộp video xem dở và PDF đọc dở. Đọc từ Room nên dùng được offline. */
interface ContinueRepository {
    /**
     * Mỗi tài khoản tối đa 10 mục (nhiều tài khoản thì tổng tối đa 20) chưa xem/đọc xong và chưa bị ẩn, lần xem gần nhất trước, thuộc các tài khoản trong phạm vi xem (TA-05) và loại tệp
     * trong [enabledKinds] (CD-01). Tự phát lại khi tiến độ, phạm vi hoặc dữ liệu đồng bộ đổi (mục bị xóa trên OneDrive tự biến mất, DS-06).
     */
    fun observe(enabledKinds: Set<MediaKind>): Flow<List<ContinueEntry>>

    /** Ẩn [itemId] khỏi dải (nút X) nhưng giữ vị trí hoặc trang đã lưu; xem hoặc đọc tiếp thì mục hiện lại. */
    suspend fun dismiss(itemId: String)
}
