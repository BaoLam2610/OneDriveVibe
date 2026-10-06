package com.lambao.odv.feature.imageviewer

/** Hằng số của màn xem ảnh (AN-01 → AN-07). */
internal object ImageViewerConstants {
    /**
     * Zoom tối đa (AN-02), bội số của cỡ vừa khung. Telephoto tính `zoomFraction` (0..1) từ mức này nên phần trăm hiện ở viên
     * thuốc cũng quy theo nó.
     */
    const val MAX_ZOOM = 4f

    /** Dưới ngưỡng này coi như chưa zoom (sai số làm tròn của cử chỉ), để không hiện viên thuốc phần trăm vô nghĩa. */
    const val ZOOMED_THRESHOLD = 0.02f
}
