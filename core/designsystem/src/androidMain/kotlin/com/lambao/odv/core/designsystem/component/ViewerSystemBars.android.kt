package com.lambao.odv.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lambao.odv.core.designsystem.findActivity

/**
 * System bar của các màn xem video, ảnh, PDF: nền luôn đen nên icon luôn sáng bất kể theme (thiet-ke-ui.md mục 1: màn xem luôn nền
 * đen), và ẩn hẳn khi [hidden] (VD-07, AN-04, PD-06). Vuốt từ mép thì bar hiện tạm thời rồi tự ẩn lại. Rời màn thì trả nguyên màu
 * icon và hiện lại bar, để màn Danh sách không bị ảnh hưởng. Gom từ ba bản giống hệt ở `:feature:player`, `:feature:imageviewer`,
 * `:feature:pdfviewer`; tab Short không ẩn bar nên giữ `ODVShortsSystemBars` riêng.
 */
@Composable
fun ODVViewerSystemBars(hidden: Boolean) {
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
