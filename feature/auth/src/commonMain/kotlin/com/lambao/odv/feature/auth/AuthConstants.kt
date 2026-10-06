package com.lambao.odv.feature.auth

/** Hằng số của màn Khóa (KH-01 → KH-06). */
internal object LockConstants {
    /** KH-06: cảnh báo số lần còn lại từ lần sai thứ 8 trở đi, tức khi còn ≤ 2 lần trong tổng 10. */
    const val WARN_WHEN_ATTEMPTS_LEFT = 2

    /** Nhịp cập nhật đồng hồ chờ nhập PIN (KH-02): mỗi tick hỏi lại thời gian còn lại thay vì tự trừ. */
    const val COOLDOWN_TICK_MS = 1_000L
}
