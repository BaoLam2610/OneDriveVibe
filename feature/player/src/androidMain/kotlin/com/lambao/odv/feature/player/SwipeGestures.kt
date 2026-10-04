package com.lambao.odv.feature.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Thứ đang chỉnh bằng vuốt dọc: nửa trái là độ sáng, nửa phải là âm lượng (VD-04). */
internal enum class SwipeKind { Brightness, Volume }

/**
 * Vuốt dọc nửa trái để chỉnh độ sáng, nửa phải để chỉnh âm lượng (VD-04). Tắt hẳn khi [enabled] là false (đang zoom lớn hơn 1x,
 * VD-18, hoặc đang khóa thao tác, VD-08).
 *
 * Bỏ qua cử chỉ bắt đầu sát hai mép, sát đỉnh và sát đáy ([PlayerConstants.SWIPE_EDGE_DEAD_ZONE_DP], [PlayerConstants.SWIPE_TOP_DEAD_ZONE_DP],
 * [PlayerConstants.SWIPE_BOTTOM_DEAD_ZONE_DP]): mép là cử chỉ Back/điều hướng của hệ thống, đỉnh là vuốt kéo thanh trạng thái xuống khi
 * system bar đang ẩn, đáy là thanh tua và cử chỉ về Home.
 *
 * [onDelta] nhận phần thay đổi của mức (cộng dồn do nơi gọi): vuốt hết chiều cao khung là đổi [PlayerConstants.SWIPE_FULL_RANGE_RATIO]
 * toàn dải.
 */
@Composable
internal fun Modifier.playerSwipeGestures(
    enabled: Boolean,
    onStart: (SwipeKind) -> Unit,
    onDelta: (SwipeKind, Float) -> Unit,
    onEnd: () -> Unit,
): Modifier {
    val start by rememberUpdatedState(onStart)
    val delta by rememberUpdatedState(onDelta)
    val end by rememberUpdatedState(onEnd)
    // Không được thêm/bớt modifier theo `enabled`: đổi cấu trúc chuỗi modifier làm Compose gán lại các bộ nhận cử chỉ khác (zoom) và hủy
    // cử chỉ đang chạy giữa chừng (bug "zoom bị khựng" khi vừa vượt 1x). Luôn giữ modifier, kiểm tra cờ ở đầu mỗi lần vuốt.
    val active by rememberUpdatedState(enabled)
    return pointerInput(Unit) {
        var kind = SwipeKind.Brightness
        var ignored = false
        detectVerticalDragGestures(
            onDragStart = { offset ->
                val edge = PlayerConstants.SWIPE_EDGE_DEAD_ZONE_DP.dp.toPx()
                val bottom = PlayerConstants.SWIPE_BOTTOM_DEAD_ZONE_DP.dp.toPx()
                val top = PlayerConstants.SWIPE_TOP_DEAD_ZONE_DP.dp.toPx()
                ignored = !active || offset.x < edge || offset.x > size.width - edge || offset.y < top || offset.y > size.height - bottom
                kind = if (offset.x < size.width / 2f) SwipeKind.Brightness else SwipeKind.Volume
                if (!ignored) start(kind)
            },
            onDragEnd = { if (!ignored) end() },
            onDragCancel = { if (!ignored) end() },
            onVerticalDrag = { change, dragAmount ->
                if (!ignored) {
                    change.consume()
                    // Vuốt lên (dragAmount âm) là tăng.
                    delta(kind, -dragAmount / size.height * PlayerConstants.SWIPE_FULL_RANGE_RATIO)
                }
            },
        )
    }
}
