package com.lambao.odv.core.designsystem.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.lambao.odv.core.designsystem.R
import com.lambao.odv.core.designsystem.findActivity

@Composable
internal actual fun ODVSystemBars(darkTheme: Boolean) {
    val window = LocalContext.current.findActivity()?.window
    val view = LocalView.current
    if (window != null) {
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            // Nền sáng thì icon tối và ngược lại.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
internal actual fun rememberODVFonts(): ODVFonts = remember {
    ODVFonts(
        sans = FontFamily(
            Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
            Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
            Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
            Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
            Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
        ),
        mono = FontFamily(
            Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
        ),
    )
}

@Composable
internal actual fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduceMotion by remember(resolver) { mutableStateOf(readReduceMotion(resolver)) }
    // Theo dõi cài đặt để bật/tắt "Giảm hiệu ứng" khi app đang chạy có hiệu lực ngay.
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = readReduceMotion(resolver)
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduceMotion
}

private fun readReduceMotion(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
