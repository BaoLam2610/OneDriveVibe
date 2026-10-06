package com.lambao.odv.core.domain.model

import kotlin.math.min

/**
 * Luật chống đoán PIN (KH-02): từ lần sai thứ [FREE_ATTEMPTS] chờ [BASE_PENALTY_MS], mỗi lần sai tiếp theo gấp đôi, trần
 * [MAX_PENALTY_MS]. Là luật nghiệp vụ nên nằm ở domain; nơi lưu bộ đếm bền (`LockoutStore`) chỉ gọi hàm này.
 */
object LockoutPolicy {
    const val FREE_ATTEMPTS = 5
    const val BASE_PENALTY_MS = 30_000L
    const val MAX_PENALTY_MS = 60 * 60 * 1000L

    /** Chặn số mũ để không tràn: 30 giây × 2^7 đã vượt trần 1 giờ. */
    private const val MAX_DOUBLINGS = 7

    /** Thời gian bị khóa nhập sau [failures] lần sai liên tiếp, mili giây; 0 nếu chưa bị khóa. */
    fun penaltyMs(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0
        val doublings = min(failures - FREE_ATTEMPTS, MAX_DOUBLINGS)
        return min(BASE_PENALTY_MS shl doublings, MAX_PENALTY_MS)
    }
}
