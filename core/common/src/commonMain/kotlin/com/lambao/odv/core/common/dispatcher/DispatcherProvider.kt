package com.lambao.odv.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Nguồn dispatcher, tiêm qua Koin thay vì gọi thẳng `Dispatchers.*` để dễ đổi khi sang iOS (MVP2).
 * Bản Android: [DefaultDispatcherProvider] trong androidMain.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
