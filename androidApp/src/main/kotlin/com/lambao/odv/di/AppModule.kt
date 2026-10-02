package com.lambao.odv.di

import com.lambao.odv.core.common.dispatcher.DefaultDispatcherProvider
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.ui.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding cấp app. Module của network, data, feature... khai báo riêng rồi ghép trong [com.lambao.odv.MainApplication]. */
val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    viewModelOf(::SplashViewModel)
}
