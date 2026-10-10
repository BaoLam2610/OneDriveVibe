package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.AppLanguage
import com.lambao.odv.core.domain.platform.AppLocaleController

/** Ngôn ngữ app đang chọn (CD-10). */
class GetLanguageUseCase(
    private val locale: AppLocaleController,
) {
    operator fun invoke(): AppLanguage = locale.current()
}
