package com.lambao.odv.feature.settings

import com.lambao.odv.core.common.error.AppError

/** Lý do cập nhật Client Secret thất bại (S3). UI đổi sang chuỗi; secret cũ luôn được giữ (CD-04). */
enum class SecretFailure { Invalid, Expired, Network, SaveFailed, Other }

/**
 * State màn Cập nhật Client Secret (CD-04, S1 đến S3). [toString] che toàn bộ vì có Client Secret mới đang nhập (CH-06): `data class`
 * mặc định sẽ in cả giá trị ô nhập.
 */
data class UpdateSecretState(
    val secret: String = "",
    val revealed: Boolean = false,
    /** S2: đang kiểm tra kết nối bằng secret mới; loading toàn màn hình, không có nút Hủy. */
    val isChecking: Boolean = false,
    val failure: SecretFailure? = null,
) {
    val canSubmit: Boolean get() = secret.isNotEmpty() && !isChecking

    override fun toString(): String = "UpdateSecretState(***)"
}

sealed interface UpdateSecretIntent {
    data class SecretChanged(val value: String) : UpdateSecretIntent {
        override fun toString(): String = "SecretChanged(***)"
    }

    data object ToggleReveal : UpdateSecretIntent

    /** Nút "Kiểm tra và lưu". */
    data object Submit : UpdateSecretIntent
}

sealed interface UpdateSecretEffect {
    /** Đã lưu secret mới: đóng form về Cài đặt (Cài đặt hiện Snackbar S5). */
    data object Saved : UpdateSecretEffect
}

/** Bảng lỗi theo mã (giống KN-09 nhưng chỉ phần liên quan tới secret). Mã lạ vào [SecretFailure.Other]. */
internal fun AppError.toSecretFailure(): SecretFailure = when (this) {
    AppError.Network, AppError.Timeout -> SecretFailure.Network
    is AppError.Http -> when (code) {
        "AADSTS7000215" -> SecretFailure.Invalid
        "AADSTS7000222" -> SecretFailure.Expired
        else -> SecretFailure.Other
    }
    AppError.SecureStorage -> SecretFailure.SaveFailed
    AppError.AppLocked, is AppError.Unknown -> SecretFailure.Other
}
