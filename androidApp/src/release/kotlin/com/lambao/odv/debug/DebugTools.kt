package com.lambao.odv.debug

import android.app.Application
import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import org.koin.core.module.Module

/** Bản release của cổng công cụ debug (ADR-0012): không có nút bọ, không bộ ghi lưu lượng, không writer log nào. */
object DebugTools {
    val koinModules: List<Module> = emptyList()

    /** Kermit mặc định có writer ra Logcat: gỡ hết để bản release không ghi log (CH-06). */
    @Suppress("UNUSED_PARAMETER")
    fun install(application: Application) = Logger.setLogWriters(emptyList())

    @Composable
    fun Overlay() = Unit

    val topOverlay: (@Composable (asWindow: Boolean) -> Unit)? = null
}
