package com.lambao.odv.core.domain.model

/**
 * Một tài khoản OneDrive for Business đã thêm vào app (đặc tả mục 3.9, ADR-0030). Mọi tài khoản dùng chung Tenant ID, Client ID,
 * Client Secret và một access token; chỉ khác UPN.
 *
 * [id] là **drive id** do Graph trả (`GET /users/{upn}/drive`), không phải UPN: đổi UPN không làm mất dữ liệu đã đồng bộ và khóa
 * cache không chứa email. [displayName] và [email] lấy từ `owner.user` của drive (TA-02); thiếu thì dùng [of].
 *
 * [toString] che toàn bộ vì UPN và email là dữ liệu cá nhân (CH-06): đừng bỏ override này.
 */
data class Account(
    val id: String,
    val upn: String,
    val displayName: String,
    val email: String,
) {
    override fun toString(): String = "Account(***)"

    companion object {
        /**
         * Dựng tài khoản từ response của drive. Thiếu tên hiển thị thì dùng phần trước `@` của [upn]; thiếu email thì dùng [upn]
         * (TA-02). Giá trị chỉ có khoảng trắng coi như thiếu.
         */
        fun of(driveId: String, upn: String, displayName: String?, email: String?): Account = Account(
            id = driveId,
            upn = upn,
            displayName = displayName?.trim().takeUnless { it.isNullOrEmpty() } ?: upn.substringBefore('@'),
            email = email?.trim().takeUnless { it.isNullOrEmpty() } ?: upn,
        )
    }
}

/**
 * Phạm vi xem đang chọn (TA-04): dữ liệu của một tài khoản hoặc của mọi tài khoản gộp lại. [All] chỉ có nghĩa khi app có từ 2
 * tài khoản; `AccountRepository.observeScope` luôn trả phạm vi đã được đưa về giá trị hợp lệ.
 */
sealed interface AccountScope {
    data object All : AccountScope
    data class One(val accountId: String) : AccountScope
}

/**
 * Config đã lưu: phần dùng chung và danh sách tài khoản (TA-01). Khác [ConnectionConfig] là dữ liệu người dùng **nhập** ở màn
 * Kết nối (4 trường). [accounts] giữ thứ tự thêm; tài khoản đầu là tài khoản nhập lúc kết nối (KN-08).
 *
 * [toString] che toàn bộ vì có Client Secret (CH-06).
 */
data class ConnectionProfile(
    val tenantId: String,
    val clientId: String,
    val clientSecret: String,
    val accounts: List<Account>,
) {
    override fun toString(): String = "ConnectionProfile(***)"

    /** Phần dùng để kiểm tra kết nối một UPN khác (TA-07), cùng Tenant ID, Client ID, Client Secret. */
    fun configFor(upn: String) = ConnectionConfig(tenantId, clientId, clientSecret, upn)
}

/**
 * Id của mục trong CSDL là `<drive id>|<id Graph>` (ADR-0030): id Graph chỉ duy nhất trong một drive, nên hai tài khoản có thể có
 * hai mục trùng id. Dùng chuỗi có tiền tố làm id **mờ** xuyên mọi tầng (route, ViewModel, cache, link phát chia sẻ, tiến độ đọc)
 * giúp các nơi đó không phải biết về tài khoản, và khóa cache theo id tự có tài khoản (BN-02).
 *
 * Dấu `|` không xuất hiện trong drive id (`b!…`, base64 url) lẫn id mục (chữ và số).
 */
object ItemKey {
    private const val SEPARATOR = '|'

    fun of(accountId: String, graphId: String): String = "$accountId$SEPARATOR$graphId"

    /** Drive id của tài khoản chứa mục; null nếu [key] không có tiền tố (id thô). */
    fun accountIdOf(key: String): String? = key.substringBefore(SEPARATOR, missingDelimiterValue = "").ifEmpty { null }

    /** Id Graph thô để gọi API; [key] không có tiền tố thì trả nguyên. */
    fun graphIdOf(key: String): String = key.substringAfter(SEPARATOR, missingDelimiterValue = key)
}
