package com.lambao.odv.feature.settings

/** Hằng số của màn PIN trong Cài đặt (CD-03, CD-08, CD-09), cùng giá trị với màn Khóa (`LockConstants`). */
internal object PinFlowConstants {
    /** KH-06: cảnh báo số lần còn lại từ lần sai thứ 8 trở đi, tức khi còn ≤ 2 lần trong tổng 10. */
    const val WARN_WHEN_ATTEMPTS_LEFT = 2

    /** Nhịp cập nhật đồng hồ chờ nhập PIN (KH-02): mỗi tick hỏi lại thời gian còn lại thay vì tự trừ. */
    const val COOLDOWN_TICK_MS = 1_000L
}
