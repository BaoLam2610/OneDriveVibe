package com.lambao.odv.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** Lựa chọn Giao diện trong Cài đặt: Theo hệ thống / Sáng / Tối. */
enum class ODVThemeMode { System, Light, Dark }

/** Font do nền tảng cung cấp (Android: `res/font`). */
@Composable
internal expect fun rememberODVFonts(): ODVFonts

/** Hệ thống đang bật Giảm hiệu ứng (Android: animator duration scale = 0). */
@Composable
internal expect fun rememberReduceMotion(): Boolean

/**
 * Đổi màu icon thanh trạng thái và thanh điều hướng của hệ thống theo theme của app ([darkTheme] = nền tối, icon sáng).
 * Cần vì `enableEdgeToEdge()` chỉ theo chế độ tối của hệ thống, không biết app đang ép Sáng/Tối trong Cài đặt.
 *
 * Chỉ [ODVTheme] ở gốc app gọi hàm này; không bọc `ODVTheme` lồng nhau trong Dialog hay màn xem. Màn xem video/ảnh/PDF (nền đen
 * luôn, icon sáng) cần cơ chế riêng có khôi phục khi thoát; làm khi dựng các màn đó.
 */
@Composable
internal expect fun ODVSystemBars(darkTheme: Boolean)

private val LocalColors = staticCompositionLocalOf<ODVColors> { error("Thiếu ODVTheme") }
private val LocalTypography = staticCompositionLocalOf<ODVTypography> { error("Thiếu ODVTheme") }
private val LocalMotion = staticCompositionLocalOf { ODVMotion(reduceMotion = false) }

/** Điểm truy cập token: `ODVTheme.colors.volt`, `ODVTheme.typography.body`... */
object ODVTheme {
    val colors: ODVColors
        @Composable @ReadOnlyComposable get() = LocalColors.current

    val typography: ODVTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current

    val motion: ODVMotion
        @Composable @ReadOnlyComposable get() = LocalMotion.current

    /** Màn xem luôn đen, không đổi theo theme. */
    val media: ODVMediaColors get() = ODVMediaColors

    val shapes: ODVShapes get() = ODVShapes
}

@Composable
fun ODVTheme(
    mode: ODVThemeMode = ODVThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ODVThemeMode.System -> isSystemInDarkTheme()
        ODVThemeMode.Light -> false
        ODVThemeMode.Dark -> true
    }
    ODVSystemBars(darkTheme = dark)
    val fonts = rememberODVFonts()
    val typography = remember(fonts) { ODVTypography(fonts) }
    val reduceMotion = rememberReduceMotion()
    val motion = remember(reduceMotion) { ODVMotion(reduceMotion) }
    // Đổi Sáng/Tối cross-fade 250ms (mục 2.8); Giảm hiệu ứng thì đổi ngay.
    val colors = animateODVColors(if (dark) DarkColors else LightColors, motion)

    val materialColors = remember(colors) { colors.toMaterialColorScheme() }
    val bodyStyle = remember(typography, colors) { typography.body.copy(color = colors.ink) }

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography,
        LocalMotion provides motion,
        // M3 lấy màu ripple từ LocalContentColor (mặc định đen); không đặt thì ripple gần như vô hình trên nền tối.
        LocalContentColor provides colors.ink,
    ) {
        // Material3 chỉ làm lớp nền cho các primitive dùng lại (ripple, chọn văn bản). Component của app tự vẽ theo token.
        // Không dùng dynamic color (quy ước dự án).
        MaterialTheme(colorScheme = materialColors) {
            ProvideTextStyle(bodyStyle, content)
        }
    }
}

private fun ODVColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = volt,
        onPrimary = onVolt,
        primaryContainer = voltSoft,
        onPrimaryContainer = voltText,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surface2,
        onSurfaceVariant = inkMuted,
        outline = lineStrong,
        outlineVariant = line,
        error = danger,
        errorContainer = dangerSoft,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseInk,
        inversePrimary = inverseAccent,
    )
}
