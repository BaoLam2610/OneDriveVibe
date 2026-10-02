package com.lambao.odv.core.network

import org.koin.dsl.module

/** Binding Koin của `:core:network`. Ghép ở MainApplication. */
val networkModule = module {
    single { createHttpClient() }
    single { TokenProvider(get()) }
    single { GraphApi(get(), get()) }
}
