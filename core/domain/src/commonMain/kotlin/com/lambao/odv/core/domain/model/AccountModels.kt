package com.lambao.odv.core.domain.model

import com.lambao.odv.core.common.error.AppError

/**
 * Trạng thái của một tài khoản cho hàng tài khoản ở Cài đặt (AC9): thời điểm đồng bộ gần nhất, đang quét lần đầu (kèm số mục), và
 * có lỗi hay không (TA-10). Không chứa Client Secret.
 */
data class AccountStatus(
    val account: Account,
    val sync: SyncState,
) {
    /** Tài khoản không truy cập được (Graph `403`/`404`, TA-10). Lỗi mạng và app khóa không tính. */
    val inaccessible: Boolean
        get() = (sync.error as? AppError.Http)?.status.let { it == 403 || it == 404 }
}

/** Kết quả thêm tài khoản (TA-07): trùng là kết quả riêng vì UI hiện dòng lỗi ngay dưới trường, không phải hộp thoại lỗi mạng. */
sealed interface AddAccountOutcome {
    data class Added(val account: Account) : AddAccountOutcome

    /** Đã có tài khoản này: trùng UPN (không phân biệt hoa thường) hoặc cùng drive id (TA-01). */
    data object AlreadyAdded : AddAccountOutcome

    data class Failed(val error: AppError) : AddAccountOutcome
}

/** Kết quả xóa tài khoản khỏi app (TA-08). */
sealed interface RemoveAccountOutcome {
    data object Removed : RemoveAccountOutcome

    /** Tài khoản đang là phạm vi xem, hoặc là tài khoản duy nhất: không xóa (TA-08). */
    data object NotAllowed : RemoveAccountOutcome

    data class Failed(val error: AppError) : RemoveAccountOutcome
}
