package com.lambao.odv.debug

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import org.koin.core.module.Module

/** Bản release của cổng công cụ debug (ADR-0012): không có nút bọ, không bộ ghi lưu lượng, không writer log nào. */
object DebugTools {
    val koinModules: List<Module> = emptyList()

    /** Kermit mặc định có writer ra Logcat: gỡ hết để bản release không ghi log (CH-06). */
    fun install() = Logger.setLogWriters(emptyList())

    @Composable
    fun Overlay() = Unit
}
