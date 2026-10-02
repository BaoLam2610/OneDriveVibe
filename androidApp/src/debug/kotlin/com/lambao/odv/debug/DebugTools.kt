package com.lambao.odv.debug

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Composable
import com.lambao.odv.gallery.FoundationsGalleryActivity
import com.lambao.odv.tools.debug.DebugAction
import com.lambao.odv.tools.debug.DebugActions
import com.lambao.odv.tools.debug.DebugLogging
import com.lambao.odv.tools.debug.DebugSettings
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

    /**
     * Gọi trước `startKoin`: ghi log ra Logcat và vào màn Debug, nạp cài đặt debug (FLAG_SECURE toàn app, che log API),
     * đăng ký công cụ riêng của app. Chỉ có MỘT icon launcher: Foundations gallery mở từ tab "Khác" của màn Debug.
     */
    fun install(application: Application) {
        DebugLogging.install()
        DebugSettings.install(application)
        DebugActions.register(
            DebugAction(
                title = "Foundations gallery",
                description = "Token, icon và component của design system để so với canvas thiết kế",
                onClick = { context -> context.startActivity(Intent(context, FoundationsGalleryActivity::class.java)) },
            ),
        )
    }

    /** Nút bọ nổi vẽ trong Activity, đặt ở gốc của app. */
    @Composable
    fun Overlay() = ODVDebugBugButton(asWindow = false)

    /**
     * Cung cấp cho `LocalODVTopOverlay`: Dialog và BottomSheet gọi nó để vẽ lại nút bọ trong cửa sổ riêng nằm trên
     * chúng (ADR-0012). Null ở bản release.
     */
    val topOverlay: (@Composable (asWindow: Boolean) -> Unit)? = { asWindow -> ODVDebugBugButton(asWindow = asWindow) }
}
