package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.settings.SettingsPreferences

/** Lưu hoặc xóa (`null`) ngày hết hạn Client Secret (CD-06). Ngày hết hạn không phải bí mật nên lưu cùng cài đặt. */
class SetSecretExpiryUseCase(private val settings: SettingsPreferences) {
    suspend operator fun invoke(epochDay: Long?) = settings.setSecretExpiryEpochDay(epochDay)
}
