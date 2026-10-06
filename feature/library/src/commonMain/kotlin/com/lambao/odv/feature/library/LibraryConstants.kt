package com.lambao.odv.feature.library

/** Hằng số của tab Thư viện (TV-01 → TV-06). Hằng số bố cục (số cột, khoảng cách, độ cao) ở lại cạnh composable dùng nó. */
internal object LibraryConstants {
    /** Giãn cách tối thiểu giữa hai lần cập nhật số mục theo ngày: mỗi trang delta làm Room phát lại, không cần vẽ lại từng lần. */
    const val DAYS_MIN_INTERVAL_MS = 300L
}
