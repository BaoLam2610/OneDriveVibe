package com.lambao.odv.feature.pdfviewer

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
 * System bar của màn xem PDF: nền luôn đen nên icon luôn sáng bất kể theme (thiet-ke-ui.md mục 1), và ẩn hẳn khi [hidden] (PD-06). Cùng
 * cách làm với `ODVViewerSystemBars` của `:feature:imageviewer` (hàm đó `internal` nên không dùng chung được; gom về `:core:designsystem` khi có
 * lát dọn dẹp). Rời màn thì trả nguyên màu icon và hiện lại bar.
 */
@Composable
internal fun ODVPdfSystemBars(hidden: Boolean) {
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

/** Giữ màn hình luôn sáng khi đang đọc (PD-06). */
@Composable
internal fun PdfKeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
