package com.lambao.odv.feature.auth.security

/** Bước của màn Thiết lập bảo mật. */
enum class SetupStep {
    /** B1, B2, B3: nhập mã PIN mới. */
    Create,

    /** B4, B5: nhập lại để xác nhận. */
    Confirm,

    /** B8: đang mã hóa lại config (Argon2id mất khoảng nửa giây, BM-04). */
    Finishing,

    /**
     * B6: PIN đã khớp và config đã mã hóa bằng PIN; hỏi có bật mở khóa bằng sinh trắc học không (BM-02). Chỉ hiện khi máy hỗ
     * trợ. Đóng app ở bước này vẫn an toàn vì config đã ở chế độ PIN.
     */
    OfferBiometric,
}

enum class SetupError {
    /** BM-06: PIN dễ đoán. */
    Weak,

    /** Hai lần nhập không khớp (BM-02). */
    Mismatch,
}

/**
 * State màn Thiết lập bảo mật (BM-01 → BM-08). Không chứa PIN: các chữ số nằm trong ViewModel (CharArray, xóa ngay khi
 * dùng xong, CH-02); State chỉ có số chấm đã nhập.
 */
data class SetupUiState(
    val step: SetupStep = SetupStep.Create,
    val entered: Int = 0,
    val error: SetupError? = null,

    /** Không mã hóa lại được config (BM-04): config giữ nguyên ở chế độ thiết bị. Dialog nằm trong State để còn sau khi xoay màn hình. */
    val saveFailed: Boolean = false,

    /** Đã bật bảo vệ xong: vào Danh sách. Là State chứ không phải Effect để không mất khi xoay màn hình. */
    val isDone: Boolean = false,

    /** Đang hiện hộp thoại sinh trắc học của hệ thống (B6): khóa hai nút trong lúc đó. */
    val isEnrollingBiometric: Boolean = false,
)

sealed interface SetupIntent {
    data class Digit(val digit: Int) : SetupIntent
    data object Backspace : SetupIntent

    /** Back từ bước nhập lại về bước đặt PIN (PIN đã nhập bị bỏ). Back ở bước đặt PIN do màn xử lý (quay lại KN-13, BM-08). */
    data object BackToCreate : SetupIntent
    data object DismissSaveFailure : SetupIntent

    /** B6, nút "Bật": hiện hộp thoại sinh trắc học rồi bọc khóa. */
    data object EnableBiometric : SetupIntent

    /** B6, nút "Để sau" (và Back): vào Danh sách, bật lại ở Cài đặt (CD-02). */
    data object SkipBiometric : SetupIntent
}

sealed interface SetupEffect {
    /** Rung ngang dãy chấm và rung nhẹ khi PIN yếu hoặc không khớp. Mất effect này cũng không hại. */
    data object Shake : SetupEffect
}
