package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.WatchProgress

/** Vị trí xem dở theo từng video (VD-12). Lưu trong Room nên mất khi Ngắt kết nối (CD-05). */
interface WatchProgressRepository {
    /**
     * Vị trí đã lưu của [itemId], hoặc null nếu chưa có hoặc đã lưu cho phiên bản khác của tệp (khác [cTag]). [cTag] null (Graph không trả)
     * thì chỉ khớp bản ghi cũng null. Trả cả bản ghi đã `finished`; người gọi tự quyết.
     */
    suspend fun get(itemId: String, cTag: String?): WatchProgress?

    /** Ghi đè vị trí của `progress.itemId`; cũng đưa mục trở lại dải Xem tiếp nếu trước đó bị ẩn. */
    suspend fun save(progress: WatchProgress)

    /** Xóa vị trí của [itemId] ("Xem từ đầu" hoặc cài đặt "Luôn phát từ đầu", VD-12). */
    suspend fun clear(itemId: String)
}
