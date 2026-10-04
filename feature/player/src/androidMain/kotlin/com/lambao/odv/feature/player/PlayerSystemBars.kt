package com.lambao.odv.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * System bar của màn xem video: nền luôn đen nên icon luôn sáng bất kể theme (thiet-ke-ui.md mục 1), và ẩn hẳn khi [hidden]
 * (hướng ngang, VD-07; 6b). Vuốt từ mép thì bar hiện tạm thời rồi tự ẩn lại. Rời màn thì trả nguyên màu icon và hiện lại bar,
 * để màn Danh sách không bị ảnh hưởng. Cùng cách làm với màn xem ảnh; giữ bản riêng trong module vì module xem ảnh không lộ ra ngoài.
 */
@Composable
internal fun ODVPlayerSystemBars(hidden: Boolean) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }

    DisposableEffect(controller) {
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(hidden, controller) {
        if (hidden) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
