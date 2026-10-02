package com.lambao.odv.core.network

import org.koin.dsl.module

/** Binding Koin của `:core:network`. Ghép ở MainApplication. */
val networkModule = module {
    single { createHttpClient() }
    // HttpTrafficRecorder chỉ có ở bản debug (module :tools:debug); bản release không có nên recorder = null.
    single { TokenProvider(get(), getOrNull()) }
    single { GraphApi(get(), get(), getOrNull()) }
}
