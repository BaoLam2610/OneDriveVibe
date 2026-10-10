package com.lambao.odv.feature.shorts

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * System bar của tab Short: nền luôn đen nên icon thanh trạng thái và thanh điều hướng luôn sáng bất kể theme (SV-10, thiet-ke-ui.md mục 1).
 * Rời tab thì trả nguyên màu icon để các tab khác không bị ảnh hưởng. Video vẽ tràn dưới thanh trạng thái vì màn không đệm inset trên.
 */
@Composable
internal fun ODVShortsSystemBars() {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    DisposableEffect(controller) {
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        // Thanh điều hướng hệ thống nằm dưới thanh đáy N4 nền đen nên cũng dùng icon sáng.
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
