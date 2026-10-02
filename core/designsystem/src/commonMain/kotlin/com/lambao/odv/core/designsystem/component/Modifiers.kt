package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVEasing
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Hệ số co khi nhấn: về 98% trong 150ms (mục 6). Giữ 1 khi hệ thống bật Giảm hiệu ứng. */
@Composable
internal fun rememberPressScale(interactionSource: InteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = ODVTheme.motion.reduceMotion
    return animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) ODVOpacity.pressedScale else 1f,
        animationSpec = tween(ODVDuration.fast, easing = ODVEasing.easeOut),
        label = "pressScale",
    )
}

/** Áp [scale] ở pha vẽ, nên mỗi khung hình animation không làm recompose. */
internal fun Modifier.pressScale(scale: State<Float>): Modifier = graphicsLayer {
    scaleX = scale.value
    scaleY = scale.value
}

/**
 * Focus ring (mục 2.7): khe 2dp màu nền rồi vòng 2dp [ringColor], vẽ ngoài khung điều khiển.
 * Chỉ hiện khi [focused] (focus bàn phím / D-pad), không bao giờ ẩn khi đang focus.
 *
 * [gapColor] theo token `focus-ring` là `surface` ở cả hai theme; truyền màu khác nếu điều khiển nằm trên nền khác.
 */
internal fun Modifier.focusRing(
    focused: Boolean,
    shape: Shape,
    ringColor: Color,
    gapColor: Color,
): Modifier = if (!focused) this else drawBehind {
    val width = 2.dp.toPx()
    fun ring(distance: Float, color: Color) {
        val outline = shape.createOutline(
            Size(size.width + distance * 2, size.height + distance * 2),
            layoutDirection,
            this,
        )
        translate(-distance, -distance) { drawOutline(outline, color, style = Stroke(width)) }
    }
    ring(3.dp.toPx(), ringColor)
    ring(1.dp.toPx(), gapColor)
}
