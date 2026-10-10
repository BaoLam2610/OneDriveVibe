package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow

/** Giao diện Sáng/Tối người dùng chọn; app shell dùng để dựng theme, màn Cài đặt dùng để hiện giá trị. */
class ObserveThemeModeUseCase(
    private val settings: SettingsPreferences,
) {
    operator fun invoke(): Flow<ThemeMode> = settings.themeMode
}
