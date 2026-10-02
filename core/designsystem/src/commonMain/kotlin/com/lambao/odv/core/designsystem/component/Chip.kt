package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Chip cao 36 (mục 4.1), hoạt động như nút bật/tắt. Chưa chọn: viền `line-strong`, có thể kèm chấm 8dp màu loại tệp
 * ([dotColor]) hoặc icon. Chọn: nền `volt-soft`, chữ và viền `volt-text`, icon check 18.
 *
 * @param selectedContentDescription nhãn TalkBack khi chọn, ví dụ "Ảnh, đang chọn". Để trống thì dùng [label].
 */
@Composable
fun ODVChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    icon: ODVIcon? = null,
    selectedContentDescription: String? = null,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.full
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressScale = rememberPressScale(interaction)
    val content = if (selected) colors.voltText else colors.ink

    Box(modifier = modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .height(ODVSize.chip)
                .pressScale(pressScale)
                // Chip thường nằm trên nền màn hình nên khe focus dùng `bg`.
                .focusRing(focused, shape, colors.voltText, colors.bg)
                .background(if (selected) colors.voltSoft else Color.Transparent, shape)
                .border(1.dp, if (selected) colors.voltText else colors.lineStrong, shape)
                .toggleable(
                    value = selected,
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onValueChange = { onClick() },
                )
                .semantics {
                    if (selected && selectedContentDescription != null) contentDescription = selectedContentDescription
                }
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                selected -> ODVIcon(ODVIcon.Check, contentDescription = null, tint = content, size = 18.dp)
                icon != null -> ODVIcon(icon, contentDescription = null, tint = content, size = 18.dp)
                dotColor != null -> Box(Modifier.size(8.dp).background(dotColor, CircleShape))
            }
            Text(label, style = ODVTheme.typography.buttonSm, color = content, maxLines = 1)
        }
    }
}
