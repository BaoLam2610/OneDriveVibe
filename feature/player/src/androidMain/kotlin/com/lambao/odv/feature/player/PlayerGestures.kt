package com.lambao.odv.feature.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job

/** Vùng chạm theo bề ngang khung cử chỉ (không phụ thuộc hướng màn hình): trái tua lùi, giữa tạm dừng/phát, phải tua tới. */
/** Ghi nhớ giữa hai lần chạm của một cặp chạm đúp; không đọc trong composition nên không cần là state. */
internal class TapMemo {
    /** Điều khiển đang hiện ngay trước lần chạm đầu: nếu có thì lần chạm thứ hai bị chặn, không tua. */
    var controlsWereVisible = false

    /** Hẹn giờ hiện điều khiển sau lần chạm đầu (khi đang ẩn); chạm đúp hoặc chạm tiếp thì hủy. */
    var pendingShow: Job? = null
}

/** Mức đang chỉnh trong một lần vuốt dọc (cộng dồn qua các lần [PlayerConstants.SWIPE_FULL_RANGE_RATIO]). */
internal class SwipeSession {
    var level = 0f
}

internal enum class TapZone { Left, Center, Right }

internal fun tapZoneOf(x: Float, width: Float): TapZone {
    val half = PlayerConstants.CENTER_ZONE_FRACTION / 2f
    return when {
        x < width * (0.5f - half) -> TapZone.Left
        x > width * (0.5f + half) -> TapZone.Right
        else -> TapZone.Center
    }
}

/**
 * Cử chỉ trên khung video (VD-01, VD-03, và giữ lâu để phát nhanh). Tự nhận cử chỉ thay cho `detectTapGestures` vì hàm đó chờ hết
 * ngưỡng chạm đúp (~300 ms) mới báo chạm một lần, làm điều khiển hiện chậm.
 *
 * - [onTap]: chạm một lần, báo **ngay lúc nhả tay** không chờ. Nơi gọi hiện/ẩn điều khiển tại đây.
 * - [onDoubleTap]: lần chạm thứ hai trong [PlayerConstants.DOUBLE_TAP_WINDOW_MS] sau lần đầu, cùng vùng. Thay cho [onTap] của lần thứ
 *   hai (lần đầu đã được báo qua [onTap]), nên nơi gọi phải hủy/hoàn tác tác dụng của lần chạm đầu nếu cần (ví dụ hẹn giờ hiện
 *   điều khiển).
 * - [onBoostStart] / [onBoostEnd]: giữ lâu rồi nhả để phát nhanh trong lúc giữ.
 *
 * Cả hai chạm trả vùng chạm ([TapZone]) và vị trí trong khung để vẽ sóng đúng chỗ.
 */
@Composable
internal fun Modifier.playerGestures(
    enabled: Boolean,
    onTap: (zone: TapZone, position: Offset) -> Unit,
    onDoubleTap: (zone: TapZone, position: Offset) -> Unit,
    onBoostStart: () -> Unit,
    onBoostEnd: () -> Unit,
): Modifier {
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val boostStart by rememberUpdatedState(onBoostStart)
    val boostEnd by rememberUpdatedState(onBoostEnd)
    // Không thêm/bớt modifier theo `enabled` (đổi cấu trúc chuỗi modifier hủy cử chỉ đang chạy của các bộ nhận khác): kiểm tra ở đầu mỗi cử chỉ.
    val active by rememberUpdatedState(enabled)
    return pointerInput(Unit) {
        // Thời điểm và vùng của lần chạm trước còn đang chờ ghép thành chạm đúp; -1 là không có.
        var lastTapUptime = -1L
        var lastTapZone = TapZone.Center
        awaitEachGesture {
            // requireUnconsumed: nút điều khiển đã nhận chạm của nó thì khung không được coi là chạm nền.
            val down = awaitFirstDown(requireUnconsumed = true)
            if (!active) {
                // Tắt (đang khóa thao tác): nút "Giữ để mở khóa" nằm trên khung này và không nuốt lần chạm xuống, nên không được để
                // nó bị coi là chạm/giữ lâu của khung (giữ lâu = phát nhanh 2x). Chờ nhả tay rồi bỏ.
                lastTapUptime = -1L
                waitForUpOrCancellation()
                return@awaitEachGesture
            }
            var up: PointerInputChange? = null
            val finishedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                up = waitForUpOrCancellation()
                true
            } ?: false

            if (!finishedBeforeLongPress) {
                lastTapUptime = -1L
                // Ngón đã trượt quá ngưỡng (đang vuốt dọc chậm chỉnh độ sáng/âm lượng, hoặc zoom) thì không phải giữ lâu: không bật 2x.
                val moved = currentEvent.changes.firstOrNull { it.id == down.id }
                if (moved == null || (moved.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    waitForUpOrCancellation()
                    return@awaitEachGesture
                }
                boostStart()
                // Chờ nhả tay (hoặc cử chỉ bị hủy) rồi trả tốc độ.
                waitForUpOrCancellation()
                boostEnd()
                return@awaitEachGesture
            }
            val release = up ?: return@awaitEachGesture
            // Kéo quá ngưỡng trượt thì không phải chạm.
            if ((release.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                lastTapUptime = -1L
                return@awaitEachGesture
            }
            release.consume()

            val zone = tapZoneOf(release.position.x, size.width.toFloat())
            val isSecondTap = lastTapUptime >= 0L &&
                release.uptimeMillis - lastTapUptime <= PlayerConstants.DOUBLE_TAP_WINDOW_MS &&
                zone == lastTapZone
            if (isSecondTap) {
                lastTapUptime = -1L
                doubleTap(zone, release.position)
            } else {
                lastTapUptime = release.uptimeMillis
                lastTapZone = zone
                tap(zone, release.position)
            }
        }
    }
}
