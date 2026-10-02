package com.lambao.odv.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween

/**
 * Cross-fade/đổi độ mờ (mục 2.8). Khi hệ thống bật Giảm hiệu ứng thì rút xuống tối đa `duration.fast` (150ms) thay vì bỏ hẳn,
 * vì cross-fade ngắn là cách thay thế được phép.
 */
fun <T> ODVMotion.fade(durationMs: Int = ODVDuration.base): FiniteAnimationSpec<T> =
    tween(if (reduceMotion) minOf(durationMs, ODVDuration.fast) else durationMs, easing = ODVEasing.easeOut)

/**
 * Animation có dịch chuyển/co giãn/trượt (scale, translate, mở rộng thumbnail; mục 2.8). Khi bật Giảm hiệu ứng thì hiện ngay,
 * không animation. Không dùng cho thanh tiến độ, vòng đếm ngược, spinner vì đó là thông tin.
 */
fun <T> ODVMotion.transform(durationMs: Int = ODVDuration.base): FiniteAnimationSpec<T> =
    if (reduceMotion) snap() else tween(durationMs, easing = ODVEasing.easeOut)
