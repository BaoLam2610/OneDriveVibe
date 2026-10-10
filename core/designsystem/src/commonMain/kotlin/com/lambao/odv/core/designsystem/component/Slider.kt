package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVEasing
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Slider (ODV Foundations › Slider và tỉ lệ chia, Cài đặt › Bộ nhớ đệm): rãnh cao 8 bo 4 màu `surface-2`, phần đã có tô [accent]
 * (màu của loại; mặc định `volt-text`), núm `volt` viền 2dp `volt-text`.
 *
 * - Mặc định núm 24dp; **đang kéo** núm 32dp kèm vòng 6dp `volt-soft`. Không dùng bong bóng nổi vì ngón tay che mất: giá trị đang
 *   kéo hiện ở hàng chứa slider (xem [ODVSliderRow]).
 * - Vùng chạm cao 48dp (do Material3 Slider bảo đảm); hai đầu rãnh lùi 12dp (nửa núm, do Material3 Slider tự làm) để núm không tràn khỏi hàng. Vòng 6dp khi kéo tràn ra ngoài Slider tới 10dp ở hai đầu: cha cần đệm ngang ≥ 10dp và không cắt (clip) sát mép.
 * - Focus: focus ring quanh núm. Vô hiệu: độ mờ 0.38.
 *
 * Dựng trên `Slider` của Material3 (cử chỉ, TalkBack vuốt lên/xuống) nhưng vẽ lại rãnh và núm bằng token; bỏ vạch chia vì thiết kế
 * không có. [steps] vẫn giữ để slider nguyên bước (vd. 1 đến 10 GB thì `steps = 8`).
 *
 * @param contentDescription tên cho trình đọc màn hình, vd. "Ảnh".
 * @param stateDescription lời đọc giá trị, vd. "Ảnh 30%, 614,4 MB"; null thì Slider tự đọc số thô.
 * @param interactionSource truyền vào nếu hàng cha cần biết trạng thái kéo (xem [ODVSliderRow]).
 */
@Composable
fun ODVSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    accent: Color = ODVTheme.colors.voltText,
    stateDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    // Núm to dần 24 → 32dp khi kéo (150ms); Giảm hiệu ứng thì đổi ngay (mục 2.8).
    val grow by animateFloatAsState(
        targetValue = if (enabled && (dragged || pressed)) 1f else 0f,
        animationSpec = if (ODVTheme.motion.reduceMotion) snap() else tween(ODVDuration.fast, easing = ODVEasing.easeOut),
        label = "sliderThumb",
    )
    val rangeSize = valueRange.endInclusive - valueRange.start

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else ODVOpacity.disabled }
            .semantics {
                this.contentDescription = contentDescription
                if (stateDescription != null) this.stateDescription = stateDescription
            },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(),
        thumb = {
            // Kích thước bố cục luôn 24dp để rãnh không xê dịch khi kéo; phần phình ra (32dp + vòng 6dp) chỉ vẽ tràn ngoài.
            Box(
                Modifier
                    .size(24.dp)
                    .focusRing(focused, CircleShape, colors.voltText, colors.surface)
                    .drawBehind {
                        val border = 2.dp.toPx()
                        val radius = 12.dp.toPx() + 4.dp.toPx() * grow
                        if (grow > 0f) drawCircle(colors.voltSoft.copy(alpha = grow), radius = radius + 6.dp.toPx() * grow)
                        drawCircle(colors.volt, radius = radius)
                        drawCircle(colors.voltText, radius = radius - border / 2, style = Stroke(border))
                    },
            )
        },
        track = { state ->
            val fraction = if (rangeSize > 0f) ((state.value - valueRange.start) / rangeSize).coerceIn(0f, 1f) else 0f
            val shape = RoundedCornerShape(4.dp)
            // Material3 Slider đã tự lùi rãnh nửa bề rộng núm (12dp) mỗi đầu, nên tâm núm chạy đúng trong đoạn này và phần tô
            // kết thúc đúng tâm núm. Không thêm padding ở đây (sẽ lệch núm 12dp).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(colors.surface2, shape),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .widthIn(min = 8.dp)
                        .background(accent, shape),
                )
            }
        },
    )
}

/**
 * Hàng slider của một loại (Cài đặt › Bộ nhớ đệm › Tỉ lệ chia): ô màu 10 bo 3 + tên (15, đậm vừa) ở trái, viên giá trị ở phải
 * (`code` 13: phần trăm đậm vừa, dung lượng mờ 75%), slider bên dưới tô cùng [color].
 *
 * Khi đang kéo, viên giá trị đổi sang nền `volt-soft`, chữ `volt-text`. [valueText] là phần trăm ("45%"), [detailText] là dung lượng
 * ("921,6 MB"); [stateDescription] là lời đọc đầy đủ cho TalkBack ("Video 45%, 921,6 MB").
 */
@Composable
fun ODVSliderRow(
    label: String,
    color: Color,
    valueText: String,
    detailText: String,
    stateDescription: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val active = enabled && (dragged || pressed)

    Column(modifier.fillMaxWidth()) {
        // Hàng chữ chỉ để nhìn: TalkBack đọc qua slider (đã có tên + stateDescription) nên không đọc lặp.
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                Text(label, style = type.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            }
            // Viên đệm 6dp mỗi bên nhưng đẩy ra 6dp để chữ vẫn thẳng mép phải với phần còn lại của sheet.
            Row(
                modifier = Modifier
                    .offset(x = 6.dp)
                    .background(if (active) colors.voltSoft else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val chipColor = if (active) colors.voltText else colors.ink
                val chipStyle = type.code.copy(lineHeight = type.timecodeSm.lineHeight)
                Text(valueText, style = chipStyle.copy(fontWeight = FontWeight.Medium), color = chipColor, maxLines = 1)
                Text(detailText, style = chipStyle, color = chipColor.copy(alpha = 0.75f), maxLines = 1)
            }
        }
        ODVSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            contentDescription = label,
            stateDescription = stateDescription,
            enabled = enabled,
            accent = color,
            interactionSource = interactionSource,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/** Một đoạn của [ODVSplitBar]: [weight] là tỉ lệ (phần trăm), [color] là màu của loại. */
@Immutable
class ODVSplitSegment(val weight: Int, val color: Color)

/**
 * SplitBar (Foundations › Thanh tỉ lệ nhiều đoạn): cao 12, các đoạn cách 2dp, mỗi đoạn bo 3 tô màu của loại; bên phải là tổng kèm dấu
 * check `success`. Tổng luôn 100% nên không có trạng thái lỗi. Đoạn 0% được bỏ (`weight(0f)` ném lỗi).
 *
 * @param barContentDescription vd. "Tỉ lệ chia: Thumbnail 10%, Ảnh 30%, Video 45%, PDF 15%".
 * @param totalText vd. "100%".
 */
@Composable
fun ODVSplitBar(
    segments: List<ODVSplitSegment>,
    totalText: String,
    barContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .semantics { contentDescription = barContentDescription },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            segments.filter { it.weight > 0 }.forEach { segment ->
                Box(
                    Modifier
                        .weight(segment.weight.toFloat())
                        .fillMaxHeight()
                        .background(segment.color, RoundedCornerShape(3.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ODVIcon(ODVIcon.Check, contentDescription = null, tint = colors.success, size = 16.dp)
            Text(totalText, style = ODVTheme.typography.caption.copy(fontWeight = FontWeight.Medium, lineHeight = 16.sp), color = colors.success)
        }
    }
}
