package com.lambao.odv.core.designsystem.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

@Composable
internal expect fun rememberIconPainter(icon: ODVIcon): Painter

/**
 * Hiển thị icon ODV. Cỡ mặc định 24dp; dùng 18dp trong Chip/Tabs, 16dp ở dòng lỗi và Breadcrumb, 32 đến 36dp ở trạng thái trống.
 * [contentDescription] = null khi icon chỉ để trang trí (nhãn nằm ở thành phần cha).
 */
@Composable
fun ODVIcon(
    icon: ODVIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = ODVTheme.colors.ink,
    size: Dp = ODVSize.iconMd,
) {
    Icon(
        painter = rememberIconPainter(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
