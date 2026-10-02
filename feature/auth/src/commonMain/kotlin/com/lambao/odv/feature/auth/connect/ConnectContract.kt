package com.lambao.odv.feature.auth.connect

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.ConnectionConfigValidation
import com.lambao.odv.core.domain.model.DriveInfo

enum class ConnectField { TenantId, ClientId, ClientSecret, Upn }

/** Loại lỗi kết nối, ứng với bảng KN-09. UI đổi sang chuỗi theo ngôn ngữ; không hiển thị nguyên văn lỗi máy chủ. */
enum class ConnectFailureKind {
    TenantFormat,
    TenantNotFound,
    ClientNotFound,
    SecretInvalid,
    SecretExpired,
    AppDisabled,
    Forbidden,
    DriveNotFound,
    Network,
    Other,
}

/** [code] là mã để tra cứu (AADSTS..., mã Graph hoặc HTTP); không bao giờ chứa bí mật. */
data class ConnectFailure(val kind: ConnectFailureKind, val code: String?) {
    /** Lỗi do Client Secret: cho nút đi thẳng tới ô Client Secret (K5). */
    val isSecretProblem: Boolean
        get() = kind == ConnectFailureKind.SecretInvalid || kind == ConnectFailureKind.SecretExpired
}

/** Drive đã kiểm tra được, hiện ở sheet K4 trước khi sang bước bảo mật (KN-08). */
data class ConnectedDrive(val info: DriveInfo, val upn: String)

/**
 * State màn Kết nối. [toString] che toàn bộ vì có Client Secret (CH-06): `data class` mặc định sẽ in cả giá trị ô nhập.
 */
data class ConnectState(
    val tenantId: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val upn: String = "",
    /** Các ô đang hiện ký tự (KN-03). */
    val revealed: Set<ConnectField> = emptySet(),
    val isConnecting: Boolean = false,
    /** Dialog lỗi KN-09 nằm trong State: phải còn sau khi xoay màn hình hoặc đổi ngôn ngữ. */
    val failure: ConnectFailure? = null,
    /** Sheet KN-08. Cũng nằm trong State vì lý do trên. */
    val connected: ConnectedDrive? = null,
) {
    fun value(field: ConnectField): String = when (field) {
        ConnectField.TenantId -> tenantId
        ConnectField.ClientId -> clientId
        ConnectField.ClientSecret -> clientSecret
        ConnectField.Upn -> upn
    }

    private fun isValid(field: ConnectField): Boolean {
        val value = value(field)
        return when (field) {
            ConnectField.TenantId -> ConnectionConfigValidation.isValidTenantId(value)
            ConnectField.ClientId -> ConnectionConfigValidation.isValidClientId(value)
            ConnectField.ClientSecret -> ConnectionConfigValidation.isValidClientSecret(value)
            ConnectField.Upn -> ConnectionConfigValidation.isValidUpn(value)
        }
    }

    /** Lỗi định dạng hiện ngay dưới ô nhưng chỉ khi ô đã có chữ (KN-06); ô trống chỉ làm nút Kết nối tắt. */
    fun hasFormatError(field: ConnectField): Boolean = value(field).isNotEmpty() && !isValid(field)

    /** KN-06: nút Kết nối chỉ bật khi cả 4 trường hợp lệ. */
    val canConnect: Boolean
        get() = !isConnecting && ConnectField.entries.all { isValid(it) }

    fun toConfig() = ConnectionConfig(tenantId, clientId, clientSecret, upn)

    override fun toString(): String = "ConnectState(***)"
}

sealed interface ConnectIntent {
    data class FieldChanged(val field: ConnectField, val value: String) : ConnectIntent {
        override fun toString(): String = "FieldChanged($field)"
    }

    data class ToggleReveal(val field: ConnectField) : ConnectIntent

    /** App xuống nền hoặc rời màn: che lại mọi ô (KN-03). */
    data object HideAll : ConnectIntent
    data object Connect : ConnectIntent
    data object DismissFailure : ConnectIntent

    /** Vuốt đóng sheet thành công: quay lại form, config vẫn chưa được lưu. */
    data object DismissConnected : ConnectIntent

    /** Nút "Sửa Client Secret" trong dialog lỗi. */
    data object EditSecret : ConnectIntent

    /** Nút "Tiếp tục" ở sheet thành công. */
    data object Continue : ConnectIntent
}

sealed interface ConnectEffect {
    /** Đưa con trỏ vào ô. Mất effect này cũng không hại nên đi qua Effect. */
    data class FocusField(val field: ConnectField) : ConnectEffect
    data object NavigateToSecuritySetup : ConnectEffect
}

/**
 * Bảng KN-09 theo mã lỗi (onedrive-graph-api.md 1.4, 9). Mã AADSTS đến từ endpoint token; `403`/`404` đến từ Graph.
 * Lỗi lạ rơi vào [ConnectFailureKind.Other] kèm mã để người dùng tra cứu.
 */
internal fun AppError.toConnectFailure(): ConnectFailure = when (this) {
    AppError.Network, AppError.Timeout -> ConnectFailure(ConnectFailureKind.Network, null)
    is AppError.Http -> {
        val kind = when (code) {
            "AADSTS900023" -> ConnectFailureKind.TenantFormat
            "AADSTS90002" -> ConnectFailureKind.TenantNotFound
            "AADSTS700016" -> ConnectFailureKind.ClientNotFound
            "AADSTS7000215" -> ConnectFailureKind.SecretInvalid
            "AADSTS7000222" -> ConnectFailureKind.SecretExpired
            "AADSTS7000112" -> ConnectFailureKind.AppDisabled
            else -> when (status) {
                403 -> ConnectFailureKind.Forbidden
                404 -> ConnectFailureKind.DriveNotFound
                else -> ConnectFailureKind.Other
            }
        }
        ConnectFailure(kind, code ?: status.toString())
    }
    AppError.SecureStorage, is AppError.Unknown -> ConnectFailure(ConnectFailureKind.Other, null)
}
