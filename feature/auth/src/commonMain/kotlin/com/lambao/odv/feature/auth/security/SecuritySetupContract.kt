package com.lambao.odv.feature.auth.security

/**
 * Lát 1 chỉ có nhánh bảo mật TẮT (BM-03, BM-04). Công tắc "Bảo vệ ứng dụng" hiện ở trạng thái tắt và chưa bật được;
 * Lát 2 thêm nhánh PIN (BM-01, BM-02, BM-05 → BM-07), đặt mặc định BẬT (BM-01) và cho phép bật tắt.
 */
data class SecuritySetupState(
    val isSaving: Boolean = false,
    /** Dialog lỗi lưu nằm trong State: phải còn sau khi xoay màn hình. */
    val saveFailed: Boolean = false,
)

sealed interface SecuritySetupIntent {
    /** Nút "Hoàn tất" (BM-04). */
    data object Complete : SecuritySetupIntent
    data object DismissSaveFailure : SecuritySetupIntent
}

sealed interface SecuritySetupEffect {
    /** Đã lưu config: vào Danh sách. */
    data object NavigateToHome : SecuritySetupEffect

    /** Không còn config chờ lưu (tiến trình bị thu hồi giữa chừng): quay về Kết nối. */
    data object NavigateToConnect : SecuritySetupEffect
}
