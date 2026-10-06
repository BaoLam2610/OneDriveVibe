package com.lambao.odv.core.domain.platform

/**
 * Độ lệch múi giờ hiện tại của máy so với UTC, mili giây (+7 giờ ở Việt Nam = 25.200.000). Thư viện nhóm ảnh theo ngày
 * theo giờ địa phương (TV-01) nên cần biết độ lệch này; `commonMain` không được dùng `java.util.TimeZone`, nên bản Android
 * nằm ở `:androidApp` như [NetworkMonitor]. Một độ lệch cố định cho mọi mục, không tính giờ mùa hè của từng ngày: sai
 * lệch tối đa một giờ ở ranh giới ngày, và tiêu đề nhóm vẫn khớp với các mục trong nhóm vì cùng dùng một giá trị.
 */
interface UtcOffsetProvider {
    fun currentOffsetMs(): Long
}
