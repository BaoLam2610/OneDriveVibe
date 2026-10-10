package com.lambao.odv.feature.settings

/** Bước của màn nhập PIN trong Cài đặt: P1 nhập PIN hiện tại, P2 nhập PIN mới, P3 nhập lại (CD-09). */
enum class PinStep { Current, New, ConfirmNew }

enum class PinError {
    /** PIN hiện tại sai (còn thử được). */
    Wrong,

    /** BM-06: PIN mới dễ đoán. */
    Weak,

    /** Hai lần nhập PIN mới không khớp. */
    Mismatch,

    /** Lỗi lưu trữ (khóa Keystore mất, tệp hỏng): không phải PIN sai và không tính vào bộ đếm. */
    StorageFailed,
}

/** Kết thúc màn PIN. Là State chứ không phải Effect để không mất khi xoay màn hình. */
enum class PinCompletion {
    /** Xong việc: quay về Cài đặt. */
    Done,

    /** Nhập sai quá nhiều và tùy chọn CD-08 đang bật: dữ liệu đã bị xóa như ngắt kết nối, về màn Kết nối (KH-06). */
    Disconnected,
}

/**
 * State màn PIN (P1 → P3, thiet-ke-ui.md mục 5.4). Không chứa PIN: các chữ số nằm trong [PinFlowViewModel] (CharArray, xóa ngay khi
 * dùng xong, CH-02); State chỉ có số chấm đã nhập.
 */
data class PinFlowState(
    val purpose: PinPurpose,
    val step: PinStep = PinStep.Current,
    val entered: Int = 0,
    val error: PinError? = null,
    /** Số lần sai nữa thì xóa dữ liệu, chỉ có khi gần ngưỡng (KH-06, CD-08). */
    val attemptsBeforeWipe: Int? = null,
    /** Thời gian còn bị khóa nhập vì sai nhiều lần (KH-02); 0 là không bị khóa. */
    val cooldownRemainingMs: Long = 0L,
    val isBusy: Boolean = false,
    /** D1: PIN hiện tại đã đúng, đang chờ xác nhận tắt bảo vệ (CD-03). */
    val confirmDisable: Boolean = false,
    val completion: PinCompletion? = null,
) {
    val isCoolingDown: Boolean get() = cooldownRemainingMs > 0L
}

sealed interface PinFlowIntent {
    data class Digit(val digit: Int) : PinFlowIntent

    data object Backspace : PinFlowIntent

    /** D1, nút "Tắt bảo vệ". */
    data object ConfirmDisable : PinFlowIntent

    /** D1, nút "Hủy" hoặc bấm ra ngoài: giữ bảo vệ, thoát màn. */
    data object CancelDisable : PinFlowIntent
}

sealed interface PinFlowEffect {
    /** Rung ngang dãy chấm và rung nhẹ khi PIN sai, yếu hoặc không khớp (KH-02). Mất effect này cũng không hại. */
    data object Shake : PinFlowEffect
}
