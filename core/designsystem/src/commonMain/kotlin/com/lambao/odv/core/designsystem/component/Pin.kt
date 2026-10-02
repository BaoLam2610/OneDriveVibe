package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

enum class ODVPinDotsState {
    /** Chấm trống viền `line-strong`; chấm đã nhập nền `volt` viền 2dp `volt-text`. */
    Default,

    /** Cả dãy chấm màu `danger`. Luôn đi kèm icon và chữ ở [ODVPinMessage], không chỉ đổi màu. */
    Error,
}

/**
 * Dãy chấm PIN (mục 4.3): mỗi chấm 16dp, khe 16, cả dãy cao 24, căn giữa.
 *
 * @param filled số chấm đã nhập, trong 0..[length].
 * @param contentDescription mô tả cho TalkBack, ví dụ "Mã PIN, đã nhập 3 trên 6 số". Không để lộ giá trị PIN.
 */
@Composable
fun ODVPinDots(
    filled: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    state: ODVPinDotsState = ODVPinDotsState.Default,
) {
    val colors = ODVTheme.colors
    val count = filled.coerceIn(0, length)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                role = Role.Image
            },
        horizontalArrangement = Arrangement.spacedBy(ODVSize.pinDotGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(length) { index ->
            val on = index < count
            val fill: Color
            val stroke: Color
            when {
                state == ODVPinDotsState.Error -> { fill = colors.danger; stroke = colors.danger }
                on -> { fill = colors.volt; stroke = colors.voltText }
                else -> { fill = Color.Transparent; stroke = colors.lineStrong }
            }
            Box(
                Modifier
                    .size(ODVSize.pinDot)
                    .background(fill, CircleShape)
                    .border(2.dp, stroke, CircleShape),
            )
        }
    }
}

/**
 * Dòng thông báo dưới dãy chấm (mục 4.3): cao 20, chữ 13/20 đậm 500. Lỗi có icon `alert` 16 màu `danger`
 * và được đọc ngay (live assertive); trạng thái thường màu `ink-muted`, đọc lịch sự. Giữ thông báo ngắn (một dòng, quá dài sẽ cắt "…").
 * Live region chỉ đọc lại khi nội dung đổi: lỗi lặp lại nên đổi chữ (ví dụ "Còn 2 lần thử").
 */
@Composable
fun ODVPinMessage(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = ODVTheme.colors
    val tint = if (isError) colors.danger else colors.inkMuted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isError) ODVIcon(ODVIcon.Alert, contentDescription = null, tint = tint, size = 16.dp)
        Text(
            text,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite },
            style = ODVTheme.typography.caption.copy(lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Nút phụ ở góc trái dưới của bàn phím (mở bằng sinh trắc học). [label] là nhãn TalkBack, ví dụ "Mở bằng sinh trắc học". */
@Immutable
class ODVKeypadAction(val icon: ODVIcon, val label: String, val onClick: () -> Unit)

/**
 * Bàn phím số tự vẽ (mục 4.3, BM-07, KH-05): lưới 3 cột rộng tối đa 358, khe 12, phím cao 56 bo `md` nền `surface-2`.
 * Thứ tự cố định: 1 đến 9, rồi [nút phụ hoặc ô trống] [0] [xóa]. Phím đang nhấn: nền `volt-soft` và viền 2dp `volt-text`.
 * Phím nút phụ và phím xóa nền trong suốt khi không nhấn (theo HTML thiết kế, board 15). Không mở bàn phím hệ thống.
 * Cột rộng tối đa 358 và đặt sát trái; nơi gọi tự căn giữa khi cha rộng hơn.
 *
 * @param secondary có thì hiện ở góc trái dưới; null thì để ô trống (ẩn khi khóa tạm).
 * @param backspaceLabel nhãn TalkBack của phím xóa, ví dụ "Xóa số cuối".
 * @param enabled false: cả bàn phím mờ 0.38 và không nhận chạm (khóa tạm).
 */
@Composable
fun ODVKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    backspaceLabel: String,
    modifier: Modifier = Modifier,
    secondary: ODVKeypadAction? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .widthIn(max = ODVSize.keypadWidth)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else ODVOpacity.disabled),
        verticalArrangement = Arrangement.spacedBy(ODVSize.keypadGap),
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ODVSize.keypadGap)) {
                row.forEach { digit ->
                    DigitKey(digit, enabled, { onDigit(digit) }, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ODVSize.keypadGap)) {
            if (secondary != null) {
                IconKey(secondary.icon, secondary.label, enabled, secondary.onClick, Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f).height(ODVSize.keypadKeyHeight))
            }
            DigitKey(0, enabled, { onDigit(0) }, Modifier.weight(1f))
            IconKey(ODVIcon.Backspace, backspaceLabel, enabled, onBackspace, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DigitKey(digit: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = ODVTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = ODVTheme.shapes.md
    Box(
        modifier = modifier
            .height(ODVSize.keypadKeyHeight)
            .background(if (pressed) colors.voltSoft else colors.surface2, shape)
            .then(if (pressed) Modifier.border(2.dp, colors.voltText, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Số phím 26/32 đậm 600 (mục 4.3), không nằm trong 21 style chung nên lấy `body` rồi đổi cỡ để giữ họ chữ và căn dòng.
        Text(
            digit.toString(),
            style = ODVTheme.typography.body.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
            color = colors.ink,
        )
    }
}

@Composable
private fun IconKey(icon: ODVIcon, label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = ODVTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = ODVTheme.shapes.md
    Box(
        modifier = modifier
            .height(ODVSize.keypadKeyHeight)
            .background(if (pressed) colors.voltSoft else Color.Transparent, shape)
            .then(if (pressed) Modifier.border(2.dp, colors.voltText, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        ODVIcon(icon, contentDescription = null, tint = colors.ink)
    }
}
