package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Kiểu nút (mục 4.1). Mỗi màn chỉ nên có một nút primary. */
enum class ODVButtonStyle { Primary, Secondary, Ghost, Tonal, Danger, DangerSolid }

/** Md cao 48, Sm cao 36 (vùng chạm vẫn ≥ 48dp). */
enum class ODVButtonSize { Md, Sm }

/**
 * @param loading spinner thay icon và khóa nút (trạng thái "đang chạy"); không làm mờ nút.
 * @param loadingDescription mô tả cho trình đọc màn hình khi [loading], ví dụ "Đang kết nối".
 */
@Composable
fun ODVButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ODVButtonStyle = ODVButtonStyle.Primary,
    size: ODVButtonSize = ODVButtonSize.Md,
    icon: ODVIcon? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingDescription: String? = null,
    fullWidth: Boolean = false,
) {
    val colors = ODVTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val pressScale = rememberPressScale(interaction)
    val shape = ODVTheme.shapes.full
    val md = size == ODVButtonSize.Md

    val container: Color
    val content: Color
    val pressedContainer: Color
    var border: Color? = null
    when (style) {
        ODVButtonStyle.Primary -> { container = colors.volt; content = colors.onVolt; pressedContainer = colors.voltPressed }
        ODVButtonStyle.Secondary -> { container = Color.Transparent; content = colors.ink; pressedContainer = colors.surface2; border = colors.lineStrong }
        ODVButtonStyle.Ghost -> { container = Color.Transparent; content = colors.voltText; pressedContainer = colors.voltSoft }
        ODVButtonStyle.Tonal -> { container = colors.surface2; content = colors.ink; pressedContainer = colors.line }
        ODVButtonStyle.Danger -> { container = colors.dangerSoft; content = colors.danger; pressedContainer = colors.surface2 }
        // Chữ trắng ở theme sáng; theme tối dùng on-volt (#101217) như spec.
        ODVButtonStyle.DangerSolid -> { container = colors.danger; content = if (colors.isDark) colors.onVolt else Color.White; pressedContainer = colors.danger }
    }
    val background = if (pressed && enabled && !loading) pressedContainer else container
    // Ghost cỡ Sm padding 12 theo HTML thiết kế (board 10); spec văn bản chỉ ghi 16 cho ghost nói chung.
    val horizontalPadding = when {
        style == ODVButtonStyle.Ghost -> if (md) 16.dp else 12.dp
        md -> 24.dp
        else -> 16.dp
    }

    Row(
        modifier = modifier
            .then(if (md) Modifier else Modifier.minimumInteractiveComponentSize())
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(if (md) ODVSize.button else ODVSize.buttonSm)
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .pressScale(pressScale)
            .focusRing(focused, shape, colors.voltText, colors.surface)
            .background(background, shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (loading && loadingDescription != null) Modifier.semantics { stateDescription = loadingDescription } else Modifier)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            ODVSpinner(size = 20.dp, color = content)
        } else if (icon != null) {
            ODVIcon(icon, contentDescription = null, tint = content, size = if (md) 20.dp else 18.dp)
        }
        Text(
            text = text,
            style = if (md) ODVTheme.typography.button else ODVTheme.typography.buttonSm,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
