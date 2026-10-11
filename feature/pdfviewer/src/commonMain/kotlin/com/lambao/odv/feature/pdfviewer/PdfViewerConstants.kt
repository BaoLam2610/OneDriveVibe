package com.lambao.odv.feature.pdfviewer

/** Hằng số của màn xem PDF (PD-01 → PD-08). */
internal object PdfViewerConstants {
    /**
     * Chờ bao lâu sau lần lật trang cuối rồi mới ghi trang đang đọc xuống Room (PD-05): lật nhanh qua hàng chục trang không ghi
     * hàng chục lần. Rời màn hoặc xuống nền thì ghi ngay (`PdfViewerIntent.Persist`).
     */
    const val SAVE_DEBOUNCE_MS = 600L

    /** Thời gian hiện thông báo "Đang ở trang X" khi mở lại tệp (PD-05). */
    const val NOTICE_MS = 2_500L

    /** Khoảng cách giữa hai trang ở chế độ cuộn dọc, dp (thiet-ke-ui.md mục 4.5). */
    const val PAGE_GAP_DP = 12

    /**
     * Trần byte của một bitmap trang. Trang khổ lớn (bản vẽ A0) dựng đúng chiều rộng màn hình có thể rất cao; vượt trần thì thu nhỏ
     * theo tỉ lệ thay vì tràn bộ nhớ.
     */
    const val MAX_PAGE_BITMAP_BYTES = 24 * 1024 * 1024

    /**
     * Trần byte khi dựng lại trang ở độ phân giải cao lúc đang zoom (PD-03). Cao hơn [MAX_PAGE_BITMAP_BYTES] để chữ còn nét ở 2x đến 3x;
     * vượt trần thì thu nhỏ theo tỉ lệ nên 4x trên trang khổ lớn kém nét hơn zoom thật.
     */
    const val MAX_ZOOMED_PAGE_BITMAP_BYTES = 40 * 1024 * 1024

    /** Zoom tối đa (PD-03), bội số của cỡ vừa chiều rộng. */
    const val MAX_ZOOM = 4f

    /** Mức zoom khi chạm đúp từ trạng thái chưa zoom. */
    const val DOUBLE_TAP_ZOOM = 2.5f

    /** Chờ bao lâu sau khi zoom dừng rồi mới dựng lại trang ở độ phân giải cao hơn, để không dựng liên tục trong lúc ngón còn đang chụm. */
    const val ZOOM_SETTLE_MS = 250L

    /** Một bitmap trang không được vượt 1/giá trị này của heap tối đa của app, dù trần byte cao hơn (máy heap nhỏ). */
    const val PAGE_BITMAP_HEAP_DIVISOR = 10

    /** Tỉ lệ bộ nhớ tối đa của app dành cho cache bitmap các trang đã dựng (chia cho giá trị này). */
    const val PAGE_CACHE_MEMORY_DIVISOR = 8
}
