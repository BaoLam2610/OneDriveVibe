package com.lambao.odv.core.domain.model

/**
 * Config kết nối: đúng 4 trường (đặc tả mục 1). Giá trị đã được cắt khoảng trắng đầu/cuối (KN-05).
 *
 * [toString] che toàn bộ để config không lọt vào log hay thông báo lỗi (CH-06); đừng bỏ override này.
 */
data class ConnectionConfig(
    val tenantId: String,
    val clientId: String,
    val clientSecret: String,
    val upn: String,
) {
    override fun toString(): String = "ConnectionConfig(***)"
}
