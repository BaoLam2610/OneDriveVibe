package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.HomeListTab
import com.lambao.odv.core.domain.settings.BrowserPreferences
import kotlinx.coroutines.flow.Flow

/** Tab Thư mục hoặc Thư viện dùng gần nhất; Màn chính đọc một lần lúc mở để chọn tab đầu tiên (DH-06). */
class ObserveLastListTabUseCase(
    private val preferences: BrowserPreferences,
) {
    operator fun invoke(): Flow<HomeListTab> = preferences.lastListTab
}
