package com.lambao.odv.core.network.auth

/**
 * Bốn giá trị cần để lấy token và gọi `/users/{upn}/drive` (ADR-0005). Lớp riêng của `:core:network` vì module này
 * không phụ thuộc `:core:domain`; `:core:data` đổi từ ConnectionConfig sang đây.
 *
 * [toString] che toàn bộ để secret không lọt vào log (CH-06).
 */
data class GraphCredentials(
    val tenantId: String,
    val clientId: String,
    val clientSecret: String,
    val upn: String,
) {
    override fun toString(): String = "GraphCredentials(***)"
}
