package com.lambao.odv.core.designsystem.logo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Biến thể logo (mục 3.2). */
enum class ODVLogoVariant {
    /** Màu cố định, không tint. */
    Color,

    /** Một màu ink, dùng trên nền volt. */
    Ink,

    /** Một màu trắng, dùng trên media. */
    White,
}

@Composable
internal expect fun rememberLogoPainter(variant: ODVLogoVariant): Painter

/**
 * Cỡ chuẩn: nhỏ nhất 24dp; Splash 112dp; Kết nối 40dp; AppBar Danh sách 28dp; màn Khóa 48dp; vòng tải 36dp.
 */
@Composable
fun ODVLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    variant: ODVLogoVariant = ODVLogoVariant.Color,
    contentDescription: String? = null,
) {
    Image(
        painter = rememberLogoPainter(variant),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}
