package com.lambao.odv.feature.auth.lock

/** Lý do dòng lỗi dưới dãy chấm PIN đang hiện. UI đổi sang chuỗi theo ngôn ngữ (ADR-0011). */
enum class LockError {
    /** PIN sai (KH-02). */
    WrongPin,

    /** Lỗi lưu trữ (khóa Keystore mất, tệp hỏng): không phải PIN sai, không tính vào bộ đếm. */
    StorageFailed,
}

/** Màn Khóa kết thúc theo hướng nào. Nằm trong State chứ không phải Effect: mất khi xoay màn hình sẽ kẹt ở màn Khóa. */
enum class LockCompletion {
    /** Nhập đúng PIN: bỏ màn Khóa, thấy lại màn đang xem. */
    Unlocked,

    /** Đã xóa dữ liệu (Quên PIN KH-03 hoặc sai quá nhiều KH-06): về màn Kết nối. */
    Disconnected,
}

/** Bước của luồng Quên mã PIN (KH-03): hai hộp thoại liên tiếp (thiet-ke-ui.md L7, L8). */
enum class ForgotStep { None, Warn, Confirm }

/**
 * State màn Khóa (KH-01 → KH-06). Không chứa PIN: các chữ số nằm trong ViewModel (CharArray, xóa ngay khi dùng xong), State
 * chỉ có số chấm đã nhập.
 */
data class LockUiState(
    /** Số chấm đã nhập, 0..6. */
    val entered: Int = 0,
    val error: LockError? = null,

    /** Còn bị khóa nhập bao lâu (KH-02), 0 nếu không. [cooldownTotalMs] là tổng thời gian của đợt phạt này, cho thanh tiến độ. */
    val cooldownRemainingMs: Long = 0,
    val cooldownTotalMs: Long = 0,

    /** KH-06: số lần thử còn lại trước khi xóa dữ liệu, chỉ có từ lần sai thứ 8 khi bật CD-08. */
    val attemptsBeforeWipe: Int? = null,

    /** Đã bật sinh trắc học và còn dùng được: hiện phím sinh trắc học ở góc bàn phím (KH-01). Ẩn khi bị khóa tạm (KH-02). */
    val biometricEnabled: Boolean = false,

    /** Đang chạy Argon2id hoặc hiện hộp thoại sinh trắc học: khóa bàn phím để không nhập chồng. */
    val isBusy: Boolean = false,
    val forgot: ForgotStep = ForgotStep.None,
    val completion: LockCompletion? = null,
) {
    val isCoolingDown: Boolean get() = cooldownRemainingMs > 0
}

sealed interface LockIntent {
    data class Digit(val digit: Int) : LockIntent
    data object Backspace : LockIntent

    /** Phím sinh trắc học (L2, KH-01). */
    data object UseBiometric : LockIntent

    /** Nút "Quên mã PIN" (L7). */
    data object ForgotClicked : LockIntent

    /** Từ cảnh báo (L7) sang xác nhận cuối (L8). */
    data object ForgotContinue : LockIntent
    data object ForgotDismiss : LockIntent

    /** Xác nhận cuối (L8): xóa dữ liệu và ngắt kết nối. */
    data object ForgotConfirm : LockIntent
}

sealed interface LockEffect {
    /** Rung ngang dãy chấm và rung nhẹ khi PIN sai (KH-02). Mất effect này cũng không hại. */
    data object Shake : LockEffect
}
