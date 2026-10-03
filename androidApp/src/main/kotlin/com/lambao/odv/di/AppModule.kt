package com.lambao.odv.di

import com.lambao.odv.core.common.dispatcher.DefaultDispatcherProvider
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.repository.BiometricAuthenticator
import com.lambao.odv.security.AndroidBiometricAuthenticator
import com.lambao.odv.security.CurrentActivityHolder
import com.lambao.odv.ui.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding cấp app. Module của network, data, feature... khai báo riêng rồi ghép trong [com.lambao.odv.MainApplication]. */
val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    // Sinh trắc học cần Activity để hiện BiometricPrompt nên cài đặt nằm ở app, không ở :core:data (ADR-0014).
    single { CurrentActivityHolder() }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticator(androidContext(), get(), get(), get()) }
    viewModelOf(::SplashViewModel)
}
