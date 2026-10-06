package com.lambao.odv.core.domain.settings

/**
 * Tùy chọn bảo mật do người dùng đặt trong Cài đặt (Lát 9), tách khỏi `SecurityRepository` để repository không phải biết
 * nơi lưu cài đặt. Chỉ chứa **công tắc**; ngưỡng và luật nằm ở `PinPolicy` và `LockoutPolicy`.
 */
interface SecuritySettings {

    /** Đã bật "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08): đạt `PinPolicy.WIPE_AFTER_FAILURES` lần sai thì xóa như ngắt kết nối (KH-06). */
    suspend fun isWipeOnTooManyFailuresEnabled(): Boolean
}
