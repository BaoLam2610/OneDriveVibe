package com.lambao.odv.feature.pdfviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

// System bar của màn xem PDF dùng chung `ODVViewerSystemBars` ở `:core:designsystem`.

/** Giữ màn hình luôn sáng khi đang đọc (PD-06). */
@Composable
internal fun PdfKeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
