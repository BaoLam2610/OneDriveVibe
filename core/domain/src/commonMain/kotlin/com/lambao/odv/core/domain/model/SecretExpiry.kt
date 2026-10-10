package com.lambao.odv.core.domain.model

/**
 * Nhắc hết hạn Client Secret (CD-06). Ngày hết hạn do người dùng tự nhập (không bắt buộc), lưu dạng số ngày kể từ 1970-01-01
 * theo giờ địa phương (epoch day) để không phụ thuộc thư viện ngày giờ ở domain.
 */
object SecretExpiryPolicy {
    /** Nhắc từ 14 ngày trước ngày hết hạn (CD-06). */
    const val WARN_DAYS = 14

    /** Số ngày còn lại tính từ [todayEpochDay] đến [expiryEpochDay]; âm nếu đã qua. */
    fun daysLeft(expiryEpochDay: Long, todayEpochDay: Long): Int =
        (expiryEpochDay - todayEpochDay).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    /** Epoch day của thời điểm [nowMs] theo giờ địa phương lệch [utcOffsetMs] so với UTC. */
    fun epochDayOf(nowMs: Long, utcOffsetMs: Long): Long = (nowMs + utcOffsetMs).floorDiv(MILLIS_PER_DAY)

    private const val MILLIS_PER_DAY = 86_400_000L
}

/**
 * Thông báo secret sắp hết hạn hoặc đã hết hạn, hiện ở Cài đặt và Danh sách (CD-06). [daysLeft] bằng 0 là hết hạn trong hôm nay,
 * âm là đã qua.
 */
data class SecretExpiryNotice(val daysLeft: Int) {
    val isExpired: Boolean get() = daysLeft < 0
}
