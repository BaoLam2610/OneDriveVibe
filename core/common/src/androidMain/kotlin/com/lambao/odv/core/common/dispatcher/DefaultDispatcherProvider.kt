package com.lambao.odv.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Đặt ở androidMain: mỗi nền tảng tự chọn dispatcher (Main trên Android cần kotlinx-coroutines-android). MVP2 viết bản iOS (ADR-0001).
class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
