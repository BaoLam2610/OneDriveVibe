package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Hiệu ứng rung ngang khi nhập sai (ví dụ sai mã PIN, KH-02): 5 nhịp 50ms (tổng 250ms = `duration.base`), biên độ 8dp.
 * Bỏ hẳn khi hệ thống bật Giảm hiệu ứng (mục 2.8). Dùng: `val shake = rememberODVShakeState()`,
 * `Modifier.odvShake(shake)`, và gọi `shake.shake()` trong một coroutine khi có lỗi.
 */
@Stable
class ODVShakeState internal constructor(
    private val amplitudePx: Float,
    private val reduceMotion: State<Boolean>,
) {
    internal val offsetX = Animatable(0f)

    suspend fun shake() {
        if (reduceMotion.value) return
        try {
            for (target in listOf(-amplitudePx, amplitudePx, -amplitudePx * 0.6f, amplitudePx * 0.6f, 0f)) {
                offsetX.animateTo(target, tween(50, easing = LinearEasing))
            }
        } finally {
            // Bị hủy giữa chừng (ví dụ người dùng gõ tiếp) thì vẫn phải về vị trí gốc, không để lệch.
            withContext(NonCancellable) { offsetX.snapTo(0f) }
        }
    }
}

@Composable
fun rememberODVShakeState(): ODVShakeState {
    val density = LocalDensity.current
    val reduceMotion = rememberUpdatedState(ODVTheme.motion.reduceMotion)
    return remember(density) { ODVShakeState(with(density) { 8.dp.toPx() }, reduceMotion) }
}

/** Áp độ lệch của [state] ở pha vẽ nên không làm recompose mỗi khung hình. */
fun Modifier.odvShake(state: ODVShakeState): Modifier = graphicsLayer { translationX = state.offsetX.value }

/**
 * Rung nhẹ (haptic), chỉ dùng ở hai nơi theo mục 2.8: bật/tắt khóa thao tác video (VD-08) và nhập sai PIN (KH-02).
 * Không dùng cho nơi khác.
 */
@Stable
class ODVHaptics internal constructor(private val feedback: HapticFeedback) {
    /** Nhập sai (PIN, KH-02). */
    fun reject() {
        feedback.performHapticFeedback(HapticFeedbackType.Reject)
    }

    /** Bật/tắt khóa thao tác video (VD-08). */
    fun toggle(on: Boolean) {
        feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
    }
}

@Composable
fun rememberODVHaptics(): ODVHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { ODVHaptics(feedback) }
}
