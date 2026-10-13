package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.SecretExpiryPolicy
import com.lambao.odv.core.domain.platform.ForegroundSignal
import com.lambao.odv.core.domain.platform.UtcOffsetProvider
import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

/**
 * Thông báo secret sắp hết hạn (CD-06): có ngày hết hạn và còn không quá [SecretExpiryPolicy.WARN_DAYS] ngày (hoặc đã qua) thì
 * phát [SecretExpiryNotice], ngược lại `null`. Dùng chung cho Cài đặt và Danh sách (Q4). Tính lại mỗi khi ngày hết hạn đổi và mỗi
 * lần app vào foreground ([ForegroundSignal]): ViewModel của hai màn sống suốt Màn chính nên "mỗi lần màn quan sát lại" không đủ,
 * đổi ngày hệ thống hay để app qua đêm sẽ giữ số ngày cũ (review Lát 7, M2). Đang mở sẵn mà qua nửa đêm thì cập nhật ở lần
 * quay lại app kế tiếp, chấp nhận được với thông báo tính theo ngày.
 */
class ObserveSecretExpiryNoticeUseCase(
    private val settings: SettingsPreferences,
    private val utcOffset: UtcOffsetProvider,
    private val clock: Clock,
    private val foreground: ForegroundSignal,
) {
    operator fun invoke(): Flow<SecretExpiryNotice?> = combine(settings.secretExpiryEpochDay, foreground.entries) { expiry, _ ->
        if (expiry == null) return@combine null
        val today = SecretExpiryPolicy.epochDayOf(clock.now().toEpochMilliseconds(), utcOffset.currentOffsetMs())
        val left = SecretExpiryPolicy.daysLeft(expiry, today)
        if (left <= SecretExpiryPolicy.WARN_DAYS) SecretExpiryNotice(left) else null
    }
}
