package com.lambao.odv.core.designsystem.component

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.lambao.odv.core.designsystem.findActivity
import java.util.WeakHashMap

/** Chế độ FLAG_SECURE toàn app. Chỉ công cụ debug đổi khỏi [ByDesign] (ADR-0012). */
enum class ODVSecureMode {
    /** Theo thiết kế: chỉ các màn gọi [ODVSecureWindow] (Kết nối, Khóa, nhập PIN, Cài đặt) chặn chụp màn hình. */
    ByDesign,

    /** Mọi Activity của app chặn chụp màn hình. Dialog/BottomSheet mở sau kế thừa cờ của Activity (SecureFlagPolicy.Inherit). */
    AlwaysOn,

    /** Không cửa sổ nào chặn, kể cả màn nhạy cảm. Chỉ để chụp/quay màn hình khi phát triển. */
    AlwaysOff,
}

/**
 * Nơi quyết định FLAG_SECURE của từng Window: đếm số màn đang yêu cầu chặn chụp màn hình trên cùng một Window. Khi chuyển
 * màn có animation, màn mới vào composition trước khi màn cũ rời đi; nếu không đếm thì màn cũ rời đi sẽ gỡ cờ đúng lúc
 * màn bảo mật mới đang hiện. Kết quả cuối cùng còn phụ thuộc [mode]. Chỉ gọi trên luồng chính.
 */
object ODVSecureWindowPolicy {
    private val counts = WeakHashMap<Window, Int>()

    var mode: ODVSecureMode = ODVSecureMode.ByDesign
        private set

    /** Đổi chế độ và áp dụng ngay cho mọi Window đã biết. */
    fun setMode(newMode: ODVSecureMode) {
        mode = newMode
        counts.keys.toList().forEach(::applyFlag)
    }

    /** Đăng ký một Window để chế độ [ODVSecureMode.AlwaysOn]/[ODVSecureMode.AlwaysOff] áp dụng cả khi nó không có màn nào yêu cầu. */
    fun track(window: Window) {
        if (window !in counts) counts[window] = 0
        applyFlag(window)
    }

    internal fun acquire(window: Window) {
        counts[window] = (counts[window] ?: 0) + 1
        applyFlag(window)
    }

    internal fun release(window: Window) {
        val count = (counts[window] ?: 0) - 1
        counts[window] = count.coerceAtLeast(0)
        applyFlag(window)
    }

    private fun applyFlag(window: Window) {
        val secure = when (mode) {
            ODVSecureMode.ByDesign -> (counts[window] ?: 0) > 0
            ODVSecureMode.AlwaysOn -> true
            ODVSecureMode.AlwaysOff -> false
        }
        if (secure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
actual fun ODVSecureWindow(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (enabled && window != null) {
            ODVSecureWindowPolicy.acquire(window)
            onDispose { ODVSecureWindowPolicy.release(window) }
        } else {
            onDispose { }
        }
    }
}
