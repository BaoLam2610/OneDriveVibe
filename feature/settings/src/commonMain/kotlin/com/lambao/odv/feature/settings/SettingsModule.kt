package com.lambao.odv.feature.settings

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding Koin của `:feature:settings`. Ghép ở MainApplication. */
val settingsModule = module {
    singleOf(::SecretUpdateEvents)
    // 23 tham số, vượt giới hạn 22 của viewModelOf nên khai báo tường minh bằng tên.
    viewModel {
        SettingsViewModel(
            observeThemeMode = get(),
            observeConnectionInfo = get(),
            observeProtection = get(),
            observeSecretExpiry = get(),
            observeSecretExpiryNotice = get(),
            setSecretExpiry = get(),
            disconnect = get(),
            secretUpdateEvents = get(),
            getLanguage = get(),
            setThemeMode = get(),
            setLanguage = get(),
            toggleFileKind = get(),
            getBiometricStatus = get(),
            observeCacheBudget = get(),
            getCacheUsage = get(),
            clearCache = get(),
            setCacheLimit = get(),
            setCacheShares = get(),
            enableBiometric = get(),
            disableBiometric = get(),
            settings = get(),
            security = get(),
            player = get(),
        )
    }
    viewModelOf(::UpdateSecretViewModel)
    // Màn PIN nhận mục đích qua tham số (route chỉ mang tên PinPurpose).
    viewModel { params ->
        PinFlowViewModel(
            purpose = params.get(),
            verifyPin = get(),
            changePin = get(),
            disableProtection = get(),
            getLockStatus = get(),
            getLockoutRemaining = get(),
            security = get(),
            observeLockState = get(),
        )
    }
}
