package com.lambao.odv.core.designsystem.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

private class ColorTransition(var from: ODVColors) {
    var to: ODVColors = from

    /** Màu đang hiển thị ở tiến độ [fraction] (1 = đã sang màu đích). */
    fun shown(fraction: Float): ODVColors = if (fraction >= 1f) to else from.lerp(to, fraction)
}

/**
 * Chuyển màu theme Sáng ↔ Tối bằng cross-fade 250ms (`duration.base`, mục 2.8, mục 6 "Đổi theme"). Nội suy từng màu nên
 * nội dung bên trong không bị dựng lại từ đầu (mất state), khác với Crossfade của cả cây. Bật Giảm hiệu ứng thì đổi ngay.
 * Nếu đổi tiếp giữa chừng thì bắt đầu từ màu đang hiển thị.
 */
@Composable
internal fun animateODVColors(target: ODVColors, motion: ODVMotion): ODVColors {
    val transition = remember { ColorTransition(target) }
    val fraction = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target !== transition.to) {
            transition.from = transition.shown(fraction.value)
            transition.to = target
            if (motion.reduceMotion) {
                fraction.snapTo(1f)
            } else {
                fraction.snapTo(0f)
                fraction.animateTo(1f, tween(ODVDuration.base, easing = ODVEasing.easeOut))
            }
        } else if (fraction.value < 1f) {
            // Đích không đổi nhưng animation trước bị hủy giữa chừng (đổi A→B→A trong một khung hình): chạy tiếp cho đủ.
            if (motion.reduceMotion) fraction.snapTo(1f) else fraction.animateTo(1f, tween(ODVDuration.base, easing = ODVEasing.easeOut))
        }
    }
    return transition.shown(fraction.value)
}
