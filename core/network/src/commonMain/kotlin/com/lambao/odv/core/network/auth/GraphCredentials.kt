package com.lambao.odv.core.network.auth

/**
 * Ba giá trị dùng chung cho mọi tài khoản để lấy token (ADR-0005, ADR-0030). UPN không nằm ở đây: mỗi lời gọi Graph nhận UPN của
 * tài khoản sở hữu dữ liệu để đi qua `/users/{upn}/drive`, còn token (Client Credentials) cấp cho cả ứng dụng. Lớp riêng của
 * `:core:network` vì module này không phụ thuộc `:core:domain`; `:core:data` đổi từ config sang đây.
 *
 * [toString] che toàn bộ để secret không lọt vào log (CH-06).
 */
data class GraphCredentials(
    val tenantId: String,
    val clientId: String,
    val clientSecret: String,
) {
    override fun toString(): String = "GraphCredentials(***)"
}
