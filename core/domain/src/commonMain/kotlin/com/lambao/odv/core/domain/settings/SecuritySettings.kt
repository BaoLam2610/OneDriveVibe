package com.lambao.odv.core.domain.settings

import com.lambao.odv.core.domain.model.AutoLockDelay
import kotlinx.coroutines.flow.Flow

/**
 * Tùy chọn bảo mật do người dùng đặt trong Cài đặt (Lát 7c), tách khỏi `SecurityRepository` để repository không phải biết nơi
 * lưu cài đặt. Chỉ chứa **công tắc và lựa chọn**; ngưỡng và luật nằm ở `PinPolicy` và `LockoutPolicy`. Bị xóa khi Ngắt kết nối (CD-05).
 */
interface SecuritySettings {

    /** Đã bật "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08): đạt `PinPolicy.WIPE_AFTER_FAILURES` lần sai thì xóa như ngắt kết nối (KH-06). */
    suspend fun isWipeOnTooManyFailuresEnabled(): Boolean

    /** Cùng giá trị với [isWipeOnTooManyFailuresEnabled], dạng luồng cho màn Cài đặt. Mặc định tắt. */
    val wipeOnTooManyFailures: Flow<Boolean>

    /** Bật tùy chọn phải đã xác nhận PIN (CD-08): nơi gọi đảm bảo, không kiểm tra ở đây. Tắt thì không cần PIN. */
    suspend fun setWipeOnTooManyFailures(enabled: Boolean)

    /** Thời gian chờ trước khi tự khóa khi rời app; mặc định [AutoLockDelay.OneMinute]. */
    val autoLockDelay: Flow<AutoLockDelay>

    suspend fun setAutoLockDelay(delay: AutoLockDelay)

    /**
     * "Bảo vệ màn hình" (Cài đặt › Bảo mật): bật thì mọi màn của app đặt FLAG_SECURE (chặn chụp và quay màn hình, ẩn nội dung ở danh
     * sách app gần đây). Tắt (mặc định) thì chỉ các màn nhạy cảm (Kết nối, Khóa, nhập PIN, Cài đặt) chặn như thiết kế. Độc lập với
     * việc có bật PIN hay không.
     */
    val screenProtection: Flow<Boolean>

    suspend fun setScreenProtection(enabled: Boolean)
}
