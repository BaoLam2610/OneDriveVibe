package com.lambao.odv.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lambao.odv.R
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Splash (S1, thiet-ke-ui.md mục 5.1): mark 112 căn giữa; dòng giới thiệu `caption` 500 màu `ink-muted`, cách đáy 56.
 * Lát 0 dừng ở màn này (kiểm tay đổi ngôn ngữ qua dòng giới thiệu). Lát 1 thêm điều hướng tiếp theo luồng khởi động.
 */
@Composable
fun ODVSplashScreen(modifier: Modifier = Modifier) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Box(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        ODVLogo(Modifier.align(Alignment.Center), size = 112.dp)
        Text(
            stringResource(R.string.splash_tagline),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 56.dp),
            style = type.caption.copy(fontWeight = FontWeight.Medium),
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
