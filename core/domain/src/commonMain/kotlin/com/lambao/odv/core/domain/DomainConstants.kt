package com.lambao.odv.core.domain

/**
 * Hằng số dùng chung của tầng domain. Các luật có tên riêng giữ cạnh luật đó: `PinPolicy` (độ dài PIN, ngưỡng xóa dữ liệu CD-08)
 * và `LockoutPolicy` (thời gian chờ KH-02).
 */
object DomainConstants {
    /** Số kết quả tìm kiếm tối đa (DS-03). */
    const val SEARCH_LIMIT = 100

    /** Số mili giây của một ngày: nhóm ảnh theo ngày ở Thư viện (TV-01), dùng chung cho SQL, domain và giao diện. */
    const val MS_PER_DAY = 86_400_000L
}
