package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Nút con mắt trong ô che ký tự. [contentDescription] bắt buộc: "Hiện ký tự" khi đang che, "Ẩn ký tự" khi đang hiện.
 */
@Immutable
class ODVMaskToggle(val onToggle: () -> Unit, val contentDescription: String)

/**
 * Ô nhập (mục 4.1): nhãn luôn hiện phía trên, khung cao 52, lỗi nằm ngay dưới ô.
 *
 * Nhãn, dòng trợ giúp và dòng lỗi nằm trong phần trang trí của ô nhập nên TalkBack đọc chung với ô (nhãn gắn với ô).
 *
 * @param masked che ký tự (Tenant ID, Client ID, Client Secret). Có [maskToggle] thì hiện nút con mắt.
 * @param mono dùng JetBrains Mono (Client ID khi hiện ký tự).
 * @param errorText có giá trị thì ô ở trạng thái lỗi và thay cho [helperText].
 */
@Composable
fun ODVTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    mono: Boolean = false,
    masked: Boolean = false,
    maskToggle: ODVMaskToggle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val shape = ODVTheme.shapes.sm
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hasError = errorText != null

    val borderWidth = if (focused || hasError) 2.dp else 1.dp
    val borderColor = when {
        !enabled -> colors.line
        hasError -> colors.danger
        focused -> colors.voltText
        else -> colors.lineStrong
    }
    val textStyle = (if (mono) type.body.copy(fontFamily = type.code.fontFamily) else type.body).copy(color = colors.ink)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else ODVOpacity.fieldDisabled),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.voltText),
        keyboardOptions = keyboardOptions,
        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
        interactionSource = interaction,
        decorationBox = { innerTextField ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = type.captionStrong, color = colors.ink)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ODVSize.field)
                        .background(if (enabled) colors.surface else colors.surface2, shape)
                        .border(borderWidth, borderColor, shape)
                        .padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = textStyle, color = colors.inkFaint, maxLines = 1)
                        }
                        innerTextField()
                    }
                    if (maskToggle != null) {
                        ODVIconButton(
                            icon = if (masked) ODVIcon.Eye else ODVIcon.EyeOff,
                            contentDescription = maskToggle.contentDescription,
                            onClick = maskToggle.onToggle,
                            enabled = enabled,
                        )
                    } else {
                        Box(Modifier.width(14.dp))
                    }
                }
                if (errorText != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ODVIcon(ODVIcon.Alert, contentDescription = null, tint = colors.danger, size = 16.dp, modifier = Modifier.padding(top = 1.dp))
                        // liveRegion đặt trên chính dòng chữ để TalkBack đọc ngay khi lỗi xuất hiện.
                        Text(
                            errorText,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                            style = type.caption,
                            color = colors.danger,
                        )
                    }
                } else if (helperText != null) {
                    Text(helperText, style = type.caption, color = colors.inkMuted)
                }
            }
        },
    )
}
