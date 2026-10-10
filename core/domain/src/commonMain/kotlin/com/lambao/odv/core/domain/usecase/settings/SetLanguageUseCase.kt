package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.AppLanguage
import com.lambao.odv.core.domain.platform.AppLocaleController

/** Đổi ngôn ngữ app (CD-10): áp dụng ngay, không khởi động lại app. Gọi trên luồng chính. */
class SetLanguageUseCase(
    private val locale: AppLocaleController,
) {
    operator fun invoke(language: AppLanguage) = locale.set(language)
}
