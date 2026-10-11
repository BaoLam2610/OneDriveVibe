package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.ReadingProgress

/** Trang đang đọc theo từng PDF (PD-05). Lưu trong Room nên mất khi Ngắt kết nối (CD-05). */
interface ReadingProgressRepository {
    /**
     * Tiến độ đã lưu của [itemId], hoặc null nếu chưa có hoặc đã lưu cho phiên bản khác của tệp (khác [cTag]): nội dung đổi thì số trang
     * cũ không còn đúng. [cTag] null (Graph không trả) thì chỉ khớp bản ghi cũng null.
     */
    suspend fun get(itemId: String, cTag: String?): ReadingProgress?

    /** Ghi đè tiến độ của `progress.itemId`. */
    suspend fun save(progress: ReadingProgress)
}
