package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.HomeListTab
import com.lambao.odv.core.domain.settings.BrowserPreferences

/** Ghi nhớ tab Thư mục hoặc Thư viện vừa dùng (DH-06). */
class SetLastListTabUseCase(
    private val preferences: BrowserPreferences,
) {
    suspend operator fun invoke(tab: HomeListTab) = preferences.setLastListTab(tab)
}
