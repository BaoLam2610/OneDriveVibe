package com.lambao.odv.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVViewerDoubleTapRipple
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlin.math.hypot

/** Biểu tượng giữa khung sau khi chạm đúp ở vùng giữa: [playing] là trạng thái *sau* khi bấm; [tick] đổi mỗi lần để chạy lại. */
internal data class CenterFeedback(val playing: Boolean, val tick: Int)

/**
 * Vẽ [PlayerConstants.WAVE_RING_COUNT] vòng tròn mảnh nở dần từ [origin] tới [maxRadius] và mờ đi; vòng sau trễ hơn vòng trước
 * [PlayerConstants.WAVE_RING_DELAY] (tỉ lệ thời gian). [progress] là tiến độ chung 0..1.
 */
private fun DrawScope.drawWaves(progress: Float, origin: Offset, maxRadius: Float) {
    val delay = PlayerConstants.WAVE_RING_DELAY
    val strokeWidth = 1.5.dp.toPx()
    repeat(PlayerConstants.WAVE_RING_COUNT) { ring ->
        val p = ((progress - ring * delay) / (1f - ring * delay)).coerceIn(0f, 1f)
        if (p > 0f && p < 1f) {
            val fade = 1f - p
            drawCircle(ODVMediaColors.onMedia.copy(alpha = 0.10f * fade), radius = maxRadius * p, center = origin)
            drawCircle(ODVMediaColors.onMedia.copy(alpha = 0.35f * fade), radius = maxRadius * p, center = origin, style = Stroke(strokeWidth))
        }
    }
}

/**
 * Vùng ripple chạm đúp tua (nửa trái hoặc nửa phải **khung video**, thiet-ke-ui.md mục 4.5) kèm sóng lan: các vòng tròn mảnh tỏa
 * ra từ mép giữa khung về phía mép ngoài theo hướng tua, bị cắt đúng theo hình nửa elip của ripple nên sóng nằm gọn trong video.
 * Mỗi lần chạm ([tick] đổi) sóng chạy lại; nhãn "+20 giây" do [ODVViewerDoubleTapRipple] vẽ. Bật Giảm hiệu ứng thì bỏ sóng.
 * Đặt trong ô chiếm nửa khung (bề ngang một nửa) để kích thước khớp.
 */
@Composable
internal fun SeekRipple(label: String, rightSide: Boolean, tick: Int, modifier: Modifier = Modifier) {
    val reduce = ODVTheme.motion.reduceMotion
    val progress = remember { Animatable(0f) }
    LaunchedEffect(tick) {
        if (!reduce) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(PlayerConstants.WAVE_DURATION_MS, easing = LinearOutSlowInEasing))
        }
    }
    val big = 999.dp
    val shape = if (rightSide) RoundedCornerShape(topStart = big, bottomStart = big) else RoundedCornerShape(topEnd = big, bottomEnd = big)
    Box(modifier.fillMaxSize().clip(shape)) {
        ODVViewerDoubleTapRipple(label = label, onRightSide = rightSide)
        if (!reduce) {
            Canvas(Modifier.fillMaxSize()) {
                // Gốc sóng ở giữa mép trong (sát tâm khung), sóng lan ra tới góc xa nhất của ô.
                val origin = Offset(if (rightSide) 0f else size.width, size.height / 2f)
                drawWaves(progress.value, origin, maxRadius = hypot(size.width, size.height / 2f))
            }
        }
    }
}

/**
 * Biểu tượng Phát/Tạm dừng giữa khung sau khi chạm đúp ở vùng giữa: nền viên thuốc phóng nhẹ rồi mờ dần, kèm sóng tròn tỏa ra
 * quanh nó (cùng kiểu với [SeekRipple], nằm trong video). Bật Giảm hiệu ứng thì bỏ phóng và sóng, vẫn mờ dần. [onFinished] báo khi
 * xong để nơi gọi gỡ.
 */
@Composable
internal fun CenterFeedbackIcon(feedback: CenterFeedback, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = ODVTheme.motion.reduceMotion
    val progress = remember { Animatable(0f) }
    LaunchedEffect(feedback.tick) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PlayerConstants.CENTER_FEEDBACK_MS, easing = LinearOutSlowInEasing))
        onFinished()
    }
    val p = progress.value
    Box(modifier.size(ODVSize.playButton * 2.6f), contentAlignment = Alignment.Center) {
        if (!reduce) {
            Canvas(Modifier.fillMaxSize()) { drawWaves(p, center, maxRadius = size.minDimension / 2f) }
        }
        Box(
            Modifier
                .size(ODVSize.playButton)
                .scale(if (reduce) 1f else 0.85f + 0.25f * p)
                .alpha(1f - p)
                .background(ODVMediaColors.pill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ODVIcon(
                if (feedback.playing) ODVIcon.Play else ODVIcon.Pause,
                contentDescription = null,
                tint = ODVMediaColors.onMedia,
                size = 36.dp,
            )
        }
    }
}
