package com.lambao.odv.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Màn trống nền `bg` cho route chưa làm (Lát 0). Xóa khi mọi route đã có màn thật. */
@Composable
fun ODVPlaceholderScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(ODVTheme.colors.bg))
}
