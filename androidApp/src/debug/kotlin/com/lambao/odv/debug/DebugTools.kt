package com.lambao.odv.debug

import androidx.compose.runtime.Composable
import com.lambao.odv.tools.debug.DebugLogging
import com.lambao.odv.tools.debug.ODVDebugBugButton
import com.lambao.odv.tools.debug.debugModule
import org.koin.core.module.Module

/**
 * Cổng vào công cụ debug (ADR-0012). Bản debug nối `:tools:debug`; bản release (src/release) cùng API nhưng rỗng, nên
 * code ở src/main không bao giờ nhắc tới `:tools:debug` và APK release không chứa nó.
 */
object DebugTools {
    /** Module Koin cài bộ ghi lưu lượng API. */
    val koinModules: List<Module> = listOf(debugModule)

    /** Gọi trước `startKoin`: ghi log ra Logcat và vào màn Debug. */
    fun installLogging() = DebugLogging.install()

    /** Nút bọ nổi, đặt chồng lên mọi màn. */
    @Composable
    fun Overlay() = ODVDebugBugButton()
}
