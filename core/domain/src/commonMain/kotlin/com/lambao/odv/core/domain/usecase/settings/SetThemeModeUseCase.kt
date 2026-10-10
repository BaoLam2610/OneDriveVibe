package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.settings.SettingsPreferences

/** Đổi giao diện Sáng/Tối; áp dụng ngay (theme đọc từ [ObserveThemeModeUseCase]). */
class SetThemeModeUseCase(
    private val settings: SettingsPreferences,
) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}
