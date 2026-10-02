package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVEasing
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVRadius
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

enum class ODVCheckState { Unchecked, Checked, Indeterminate }

/** Hàng Checkbox (mục 4.1): cao tối thiểu 48, nút 22 bo 6, cách chữ 12. */
@Composable
fun ODVCheckbox(
    label: String,
    state: ODVCheckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val box = RoundedCornerShape(ODVRadius.xs)
    val on = state != ODVCheckState.Unchecked
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ODVSize.tapTarget)
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .triStateToggleable(
                state = when (state) {
                    ODVCheckState.Unchecked -> ToggleableState.Off
                    ODVCheckState.Checked -> ToggleableState.On
                    ODVCheckState.Indeterminate -> ToggleableState.Indeterminate
                },
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ODVSize.radio)
                .background(if (on) colors.volt else colors.surface, box)
                .then(if (on) Modifier else Modifier.border(2.dp, colors.lineStrong, box)),
            contentAlignment = Alignment.Center,
        ) {
            if (on) {
                ODVIcon(
                    icon = if (state == ODVCheckState.Checked) ODVIcon.Check else ODVIcon.Minus,
                    contentDescription = null,
                    tint = colors.onVolt,
                    size = 16.dp,
                )
            }
        }
        Column {
            Text(label, style = type.body, color = colors.ink)
            if (description != null) Text(description, style = type.caption, color = colors.inkMuted)
        }
    }
}

/** Hàng Radio (mục 4.1). Chọn: vòng 2dp `volt-text` có chấm 10dp; chưa chọn: vòng `line-strong`. */
@Composable
fun ODVRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ODVSize.tapTarget)
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ODVSize.radio)
                .border(2.dp, if (selected) colors.voltText else colors.lineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(colors.voltText, CircleShape))
        }
        Column {
            Text(label, style = type.body, color = colors.ink)
            if (description != null) Text(description, style = type.caption, color = colors.inkMuted)
        }
    }
}

/**
 * Switch 52 × 32 (mục 4.1). Bật: nền `volt`, núm 24 `on-volt` bên phải. Tắt: nền `surface-2`, viền 2dp `line-strong`,
 * núm 16 `line-strong` bên trái. Núm trượt trong 150ms (bỏ hiệu ứng khi Giảm hiệu ứng). Vùng chạm mở rộng tới 48dp.
 *
 * @param onCheckedChange null thì Switch chỉ để hiển thị (không nhận chạm, không có semantics); dùng khi cả hàng đã là
 * một điều khiển bật/tắt, ví dụ `ODVSwitchRow`.
 * @param contentDescription nhãn TalkBack, thường trùng tên mục; null khi cha đã đặt nhãn.
 */
@Composable
fun ODVSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.full
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val duration = if (ODVTheme.motion.reduceMotion) 0 else ODVDuration.fast
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(duration, easing = ODVEasing.easeOut), label = "switchThumb")
    val thumbColor = if (checked) colors.onVolt else colors.lineStrong
    val interactive = onCheckedChange != null

    Box(
        modifier = modifier
            .then(if (interactive) Modifier.minimumInteractiveComponentSize() else Modifier)
            .size(ODVSize.switchWidth, ODVSize.switchHeight)
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .focusRing(focused, shape, colors.voltText, colors.surface)
            .background(if (checked) colors.volt else colors.surface2, shape)
            .then(if (checked) Modifier else Modifier.border(2.dp, colors.lineStrong, shape))
            // Núm vẽ ở pha vẽ để mỗi khung hình animation không làm recompose.
            // Theo HTML thiết kế: bật = núm 24 cách mép trái 24; tắt = núm 16 cách mép trái 10 (viền 2 + đệm 4 + lề 4).
            .drawBehind {
                val diameter = lerp(16.dp.toPx(), 24.dp.toPx(), progress)
                val left = lerp(10.dp.toPx(), 24.dp.toPx(), progress)
                drawCircle(thumbColor, radius = diameter / 2f, center = Offset(left + diameter / 2f, size.height / 2f))
            }
            .then(
                if (onCheckedChange != null) {
                    Modifier
                        .toggleable(
                            value = checked,
                            interactionSource = interaction,
                            indication = null,
                            enabled = enabled,
                            role = Role.Switch,
                            onValueChange = onCheckedChange,
                        )
                        .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    )
}
