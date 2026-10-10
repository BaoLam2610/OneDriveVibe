package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.SecretExpiryPolicy
import com.lambao.odv.core.domain.platform.UtcOffsetProvider
import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Thông báo secret sắp hết hạn (CD-06): có ngày hết hạn và còn không quá [SecretExpiryPolicy.WARN_DAYS] ngày (hoặc đã qua) thì
 * phát [SecretExpiryNotice], ngược lại `null`. Dùng chung cho Cài đặt và Danh sách (Q4). Tính lại mỗi khi ngày hết hạn đổi và mỗi
 * lần màn quan sát lại, nên đủ chính xác theo ngày mà không cần bộ hẹn giờ.
 */
class ObserveSecretExpiryNoticeUseCase(
    private val settings: SettingsPreferences,
    private val utcOffset: UtcOffsetProvider,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<SecretExpiryNotice?> = settings.secretExpiryEpochDay.map { expiry ->
        if (expiry == null) return@map null
        val today = SecretExpiryPolicy.epochDayOf(clock.now().toEpochMilliseconds(), utcOffset.currentOffsetMs())
        val left = SecretExpiryPolicy.daysLeft(expiry, today)
        if (left <= SecretExpiryPolicy.WARN_DAYS) SecretExpiryNotice(left) else null
    }
}
