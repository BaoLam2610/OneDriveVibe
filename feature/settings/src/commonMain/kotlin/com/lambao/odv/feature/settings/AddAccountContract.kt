package com.lambao.odv.feature.settings

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.ConnectionConfigValidation

/** Loại lỗi khi thêm tài khoản (TA-07), ứng với bảng KN-09 phần liên quan tới một UPN. UI đổi sang chuỗi theo ngôn ngữ (CD-10). */
enum class AddAccountFailureKind { SecretInvalid, SecretExpired, AppDisabled, Forbidden, DriveNotFound, Network, SaveFailed, Other }

/** [code] để người dùng tra cứu (mã Graph, AADSTS hoặc HTTP); không bao giờ chứa bí mật. */
data class AddAccountFailure(val kind: AddAccountFailureKind, val code: String?) {
    /** Lỗi do Client Secret (TK-04): cho nút đi thẳng tới cập nhật Client Secret, như K5. */
    val isSecretProblem: Boolean
        get() = kind == AddAccountFailureKind.SecretInvalid || kind == AddAccountFailureKind.SecretExpired
}

/**
 * State màn Thêm tài khoản (TA-07, AC4 đến AC7). [accounts] để báo trùng ngay khi gõ (AC5). [duplicateFromServer] là trùng phát hiện
 * sau khi gọi Graph (hai UPN khác nhau trỏ cùng một drive, TA-01). [toString] che toàn bộ vì UPN là dữ liệu cá nhân (CH-06).
 */
data class AddAccountState(
    val upn: String = "",
    val accounts: List<Account> = emptyList(),
    /** AC6: đang kiểm tra truy cập; khóa trường và nút, không có Hủy. */
    val isChecking: Boolean = false,
    /** Dialog lỗi AC7 nằm trong State: phải còn sau khi xoay màn hình hoặc đổi ngôn ngữ. */
    val failure: AddAccountFailure? = null,
    val duplicateFromServer: Boolean = false,
    /**
     * Đã thêm thành công, màn đang đóng. Lúc này danh sách [accounts] đã có chính UPN vừa nhập nên [isDuplicate] sẽ đúng: phải bỏ qua để ô nhập
     * không nháy viền đỏ trước khi màn đóng.
     */
    val added: Boolean = false,
) {
    /**
     * Trùng UPN (không phân biệt hoa thường) với một tài khoản đã có (TA-01). Không tính trong lúc đang kiểm tra hay sau khi đã thêm: việc thêm
     * cập nhật danh sách ngay trước khi màn đóng, và UPN vừa thêm khi đó trùng với chính nó.
     */
    val isDuplicate: Boolean
        get() = !added && !isChecking && (duplicateFromServer || accounts.any { it.upn.equals(upn.trim(), ignoreCase = true) })

    /** Sai định dạng email: chỉ báo khi đã có chữ (KN-06); ô trống chỉ làm nút tắt. */
    val hasFormatError: Boolean get() = upn.isNotEmpty() && !ConnectionConfigValidation.isValidUpn(upn)

    val canSubmit: Boolean get() = !isChecking && !added && ConnectionConfigValidation.isValidUpn(upn) && !isDuplicate

    override fun toString(): String = "AddAccountState(***)"
}

sealed interface AddAccountIntent {
    data class UpnChanged(val value: String) : AddAccountIntent {
        override fun toString(): String = "UpnChanged(***)"
    }

    /** Nút "Thêm tài khoản". */
    data object Submit : AddAccountIntent

    data object DismissFailure : AddAccountIntent

    /** Nút "Sửa Client Secret" trong dialog lỗi secret (TK-04): đóng dialog và đi tới cập nhật Client Secret. */
    data object FixSecret : AddAccountIntent
}

sealed interface AddAccountEffect {
    /** Đã thêm và đã chuyển sang tài khoản mới (TA-07): đóng màn về nơi đã mở. */
    data object Added : AddAccountEffect

    /** Mở cập nhật Client Secret (CD-04); [requiresPin] là bảo vệ đang bật nên phải qua P1 trước. App đóng màn này rồi đẩy màn tương ứng. */
    data class OpenSecretFix(val requiresPin: Boolean) : AddAccountEffect
}

/** Bảng lỗi theo mã (KN-09). Lỗi secret sai hoặc hết hạn là lỗi chung của app (TK-04), không phải của riêng tài khoản này. */
internal fun AppError.toAddAccountFailure(): AddAccountFailure = when (this) {
    AppError.Network, AppError.Timeout -> AddAccountFailure(AddAccountFailureKind.Network, null)
    is AppError.Http -> {
        val kind = when (code) {
            "AADSTS7000215" -> AddAccountFailureKind.SecretInvalid
            "AADSTS7000222" -> AddAccountFailureKind.SecretExpired
            "AADSTS7000112" -> AddAccountFailureKind.AppDisabled
            else -> when (status) {
                403 -> AddAccountFailureKind.Forbidden
                404 -> AddAccountFailureKind.DriveNotFound
                else -> AddAccountFailureKind.Other
            }
        }
        AddAccountFailure(kind, code ?: status.toString())
    }
    AppError.SecureStorage -> AddAccountFailure(AddAccountFailureKind.SaveFailed, null)
    // AppLocked được AddAccountViewModel xử lý riêng (không hiện dialog); nhánh này chỉ để `when` đủ nhánh.
    AppError.AppLocked, is AppError.Unknown -> AddAccountFailure(AddAccountFailureKind.Other, null)
}
