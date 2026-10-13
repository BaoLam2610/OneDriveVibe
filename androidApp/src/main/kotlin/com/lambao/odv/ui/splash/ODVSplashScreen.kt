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
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.theme.ODVTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Splash (S1, thiet-ke-ui.md mục 5.1): mark 112 căn giữa; dòng giới thiệu `caption` 500 màu `ink-muted`, cách đáy 56.
 * Đọc xem đã có config chưa rồi gọi [onConnectRequired] hoặc [onReady]; nơi gọi thay Splash bằng màn đó.
 */
@Composable
fun ODVSplashScreen(
    onConnectRequired: () -> Unit,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = koinViewModel(),
) {
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SplashEffect.NavigateToConnect -> onConnectRequired()
            SplashEffect.NavigateToHome -> onReady()
        }
    }

    ODVSplashContent(modifier)
}

/**
 * Hình của Splash (mark + dòng giới thiệu), không có logic. Dùng chung cho các khoảng chờ trước khi Màn chính sẵn sàng (chờ trạng thái khóa
 * ở `ODVNavDisplay`, chờ tab đầu ở `ODVHomeScreen`) để từ khung đầu tiên tới lúc có nội dung chỉ thấy MỘT hình, có logo, không xen
 * khoảng nền trơn (lỗi 2026-10-13: không thấy logo ở giao diện tối, chuyển sang Màn chính bị chớp và khựng).
 */
@Composable
fun ODVSplashContent(modifier: Modifier = Modifier) {
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
