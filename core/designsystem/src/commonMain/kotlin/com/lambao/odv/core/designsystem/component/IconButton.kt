package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Màu của [ODVIconButton]: nền và màu icon. */
@Immutable
class ODVIconButtonColors(val container: Color, val content: Color)

/** Các bộ màu dựng sẵn (mục 4.1). Muốn bộ màu khác (ví dụ trên màn xem) thì tự dựng [ODVIconButtonColors]. */
object ODVIconButtonDefaults {
    @Composable
    fun standard() = ODVIconButtonColors(Color.Transparent, ODVTheme.colors.ink)

    @Composable
    fun tonal() = ODVIconButtonColors(ODVTheme.colors.surface2, ODVTheme.colors.ink)

    /** Đang chọn (ví dụ nút đổi dạng lưới). */
    @Composable
    fun selected() = ODVIconButtonColors(ODVTheme.colors.voltSoft, ODVTheme.colors.voltText)

    @Composable
    fun volt() = ODVIconButtonColors(ODVTheme.colors.volt, ODVTheme.colors.onVolt)
}

/** 48 × 48 tròn, icon 24. [contentDescription] bắt buộc vì nút không có chữ. */
@Composable
fun ODVIconButton(
    icon: ODVIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ODVIconButtonColors = ODVIconButtonDefaults.standard(),
    enabled: Boolean = true,
) {
    val shape = ODVTheme.shapes.full
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressScale = rememberPressScale(interaction)
    val theme = ODVTheme.colors
    Box(
        modifier = modifier
            .size(ODVSize.tapTarget)
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .pressScale(pressScale)
            .focusRing(focused, shape, theme.voltText, theme.surface)
            .background(colors.container, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ODVIcon(icon, contentDescription = contentDescription, tint = colors.content)
    }
}
