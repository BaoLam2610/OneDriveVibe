package com.lambao.odv.core.domain.model

/**
 * Kiểm tra định dạng 4 trường ở màn Kết nối (KN-06). Chỉ kiểm tra hình thức, không gọi mạng.
 * Nhận giá trị đã cắt khoảng trắng (KN-05).
 */
object ConnectionConfigValidation {

    private val guid = Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
    // Raw string để `\.` và `\s` giữ nguyên là escape của regex, không phải escape của chuỗi Kotlin.
    private val domain = Regex("""^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}${'$'}""")
    private val email = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+${'$'}""")

    /** GUID hoặc tên miền (vd. `xxx.onmicrosoft.com`). */
    fun isValidTenantId(value: String): Boolean = guid.matches(value) || domain.matches(value)

    fun isValidClientId(value: String): Boolean = guid.matches(value)

    fun isValidClientSecret(value: String): Boolean = value.isNotEmpty()

    fun isValidUpn(value: String): Boolean = email.matches(value)

    fun isValid(config: ConnectionConfig): Boolean =
        isValidTenantId(config.tenantId) &&
            isValidClientId(config.clientId) &&
            isValidClientSecret(config.clientSecret) &&
            isValidUpn(config.upn)
}
