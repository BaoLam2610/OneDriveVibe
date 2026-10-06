package com.lambao.odv.core.network

import com.lambao.odv.core.network.auth.TokenProvider
import com.lambao.odv.core.network.graph.GraphApi
import com.lambao.odv.core.network.http.createDownloadHttpClient
import com.lambao.odv.core.network.http.createHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Binding Koin của `:core:network`. Ghép ở MainApplication (qua `coreModules` của `:core:data`). */
val networkModule = module {
    single { createHttpClient() }
    // HttpTrafficRecorder chỉ có ở bản debug (module :tools:debug); bản release không có nên recorder = null.
    single { TokenProvider(get(), getOrNull()) }
    // Client không bearer cho URL đã ký (thumbnail). Có qualifier để không lẫn với client Graph ở trên.
    single(named(HttpConstants.DOWNLOAD_CLIENT)) { createDownloadHttpClient() }
    // GraphCredentialsSource do :core:data cài đặt (đọc config đã lưu); network chỉ biết interface, nên đây là phụ thuộc runtime
    // qua Koin chứ không phải phụ thuộc module.
    single { GraphApi(get(), get(), getOrNull(), get(named(HttpConstants.DOWNLOAD_CLIENT)), get()) }
}
