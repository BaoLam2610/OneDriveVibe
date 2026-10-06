package com.lambao.odv.core.domain.hook

/** Cache thumbnail trên máy (BN-01). Chỉ để công cụ gọi xóa tay; ngắt kết nối xóa qua [ConnectionResetter]. */
interface ThumbnailCache {
    /** Xóa toàn bộ thumbnail đã lưu (đĩa và bộ nhớ). Không ném ngoại lệ: lỗi chỉ được ghi log. */
    suspend fun clear()
}

/**
 * Tỉ lệ chất lượng thumbnail, phần trăm so với cỡ mặc định (100 = mặc định). Bản phát hành luôn 100; bản debug cho chỉnh
 * từ màn Debug (50 → 150) để so chất lượng và dung lượng. Đọc lại mỗi lần tải, nên đổi giá trị có hiệu lực từ lần tải kế
 * tiếp (khóa cache có cỡ thực nên thumbnail cỡ cũ không bị dùng lại sai).
 */
interface ThumbnailQuality {
    fun scalePercent(): Int
}
