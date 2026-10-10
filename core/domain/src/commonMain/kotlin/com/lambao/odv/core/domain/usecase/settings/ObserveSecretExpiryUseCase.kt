package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow

/** Ngày hết hạn Client Secret đã đặt (epoch day), `null` nếu chưa đặt (CD-06). Hàng "Ngày hết hạn secret" ở Cài đặt đọc luồng này. */
class ObserveSecretExpiryUseCase(private val settings: SettingsPreferences) {
    operator fun invoke(): Flow<Long?> = settings.secretExpiryEpochDay
}
