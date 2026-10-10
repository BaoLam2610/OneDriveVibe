package com.lambao.odv.di

import com.lambao.odv.core.common.dispatcher.DefaultDispatcherProvider
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.platform.AppLocaleController
import com.lambao.odv.core.domain.platform.BiometricAuthenticator
import com.lambao.odv.core.domain.platform.NetworkMonitor
import com.lambao.odv.core.domain.platform.UtcOffsetProvider
import com.lambao.odv.time.AndroidUtcOffsetProvider
import com.lambao.odv.locale.AndroidAppLocaleController
import com.lambao.odv.network.AndroidNetworkMonitor
import com.lambao.odv.security.AndroidBiometricAuthenticator
import com.lambao.odv.security.CurrentActivityHolder
import com.lambao.odv.ui.home.HomeViewModel
import com.lambao.odv.ui.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

/** Binding cấp app. Module của network, data, feature... khai báo riêng rồi ghép trong [com.lambao.odv.MainApplication]. */
val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    // Sinh trắc học cần Activity để hiện BiometricPrompt nên cài đặt nằm ở app, không ở :core:data (ADR-0014).
    single { CurrentActivityHolder() }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticator(androidContext(), get(), get(), get()) }
    // DS-05: cần Context (ConnectivityManager) nên cài đặt nằm ở app, như BiometricAuthenticator.
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    // TV-01: nhóm ảnh theo ngày địa phương, cần múi giờ của máy (java.util.TimeZone không dùng được ở commonMain).
    single<UtcOffsetProvider> { AndroidUtcOffsetProvider() }
    // CD-10: ngôn ngữ app qua AppCompatDelegate (cần AppCompat nên cài đặt nằm ở app); cũng là ConnectionResetter để Ngắt kết nối
    // đưa về "Theo hệ thống" (CD-05). DisconnectUseCase gom mọi ConnectionResetter bằng getAll().
    single { AndroidAppLocaleController(get()) } binds arrayOf(AppLocaleController::class, ConnectionResetter::class)
    viewModelOf(::SplashViewModel)
    viewModelOf(::HomeViewModel)
}
