package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow

/** Thời lượng tối đa của video vào tab Short, phút (CD-13); Cài đặt hiện giá trị, tab Short lọc danh sách theo nó (SV-01). */
class ObserveShortMaxMinutesUseCase(
    private val settings: SettingsPreferences,
) {
    operator fun invoke(): Flow<Int> = settings.shortMaxMinutes
}
