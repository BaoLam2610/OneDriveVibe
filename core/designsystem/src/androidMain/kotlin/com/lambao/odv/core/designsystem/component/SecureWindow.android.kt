package com.lambao.odv.core.designsystem.component

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.lambao.odv.core.designsystem.findActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.WeakHashMap

/**
 * Nơi quyết định FLAG_SECURE của từng Window: đếm số màn đang yêu cầu chặn chụp màn hình trên cùng một Window. Khi chuyển
 * màn có animation, màn mới vào composition trước khi màn cũ rời đi; nếu không đếm thì màn cũ rời đi sẽ gỡ cờ đúng lúc
 * màn bảo mật mới đang hiện. Chỉ gọi trên luồng chính.
 */
object ODVSecureWindowPolicy {
    private val counts = WeakHashMap<Window, Int>()

    private val _appWide = MutableStateFlow(false)

    /**
     * "Bảo vệ màn hình" đang áp dụng toàn app (Cài đặt › Bảo mật, hoặc PIN bật trên Android 12 trở xuống, ADR-0014). Là trạng thái của
     * cả process chứ không của một màn, nên Window nào được [track] sau cũng nhận (kể cả `DebugActivity`).
     */
    val appWide: StateFlow<Boolean> = _appWide.asStateFlow()

    /** Đặt "Bảo vệ màn hình" toàn app và áp dụng ngay cho mọi Window đã biết. Gọi trên luồng chính. */
    fun setAppWide(enabled: Boolean) {
        if (_appWide.value == enabled) return
        _appWide.value = enabled
        counts.keys.toList().forEach(::applyFlag)
    }

    /**
     * Đăng ký một Window để [appWide] áp dụng cả khi nó không có màn nào
     * yêu cầu. `MainActivity` gọi ở mọi bản dựng; công cụ debug gọi cho mọi Activity còn lại (bản debug).
     */
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
        // Một nguồn sự thật: cài đặt "Bảo vệ màn hình" của người dùng (appWide), hoặc có màn nhạy cảm đang yêu cầu. Công cụ debug không ghi đè.
        val secure = _appWide.value || (counts[window] ?: 0) > 0
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
