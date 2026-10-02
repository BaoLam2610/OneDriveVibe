package com.lambao.odv.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

/**
 * Đếm số màn đang yêu cầu chặn chụp màn hình trên cùng một Window. Khi chuyển màn có animation, màn mới vào composition
 * trước khi màn cũ rời đi; nếu không đếm thì màn cũ rời đi sẽ gỡ cờ đúng lúc màn bảo mật mới đang hiện. Chỉ gọi trên luồng chính.
 */
private object SecureWindowRegistry {
    private val counts = WeakHashMap<Window, Int>()

    fun acquire(window: Window) {
        val count = counts[window] ?: 0
        counts[window] = count + 1
        if (count == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun release(window: Window) {
        val count = (counts[window] ?: 0) - 1
        if (count <= 0) {
            counts.remove(window)
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            counts[window] = count
        }
    }
}

@Composable
actual fun ODVSecureWindow(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (enabled && window != null) {
            SecureWindowRegistry.acquire(window)
            onDispose { SecureWindowRegistry.release(window) }
        } else {
            onDispose { }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
