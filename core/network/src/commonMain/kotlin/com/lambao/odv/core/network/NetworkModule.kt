package com.lambao.odv.core.network

import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Binding Koin của `:core:network`. Ghép ở MainApplication. */
val networkModule = module {
    single { createHttpClient() }
    // HttpTrafficRecorder chỉ có ở bản debug (module :tools:debug); bản release không có nên recorder = null.
    single { TokenProvider(get(), getOrNull()) }
    // Client không bearer cho URL đã ký (thumbnail). Có qualifier để không lẫn với client Graph ở trên.
    single(named(DOWNLOAD_CLIENT)) { createDownloadHttpClient() }
    single { GraphApi(get(), get(), getOrNull(), get(named(DOWNLOAD_CLIENT))) }
}

private const val DOWNLOAD_CLIENT = "download"
