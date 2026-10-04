package com.lambao.odv.feature.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize

/**
 * Zoom tự do của video (VD-18): hai ngón để zoom từ 1x đến [PlayerConstants.MAX_ZOOM]x, khi lớn hơn 1x thì một ngón kéo để di chuyển.
 * Chạm đúp vẫn là tua, không dùng để zoom. Zoom về 1x khi chuyển video hoặc đổi hướng màn hình (nơi gọi gọi [reset]).
 *
 * [frameSize] là kích thước khung video (px) để chặn kéo lố ra ngoài mép video.
 */
@Stable
internal class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var frameSize by mutableStateOf(IntSize.Zero)

    /** Đang zoom lớn hơn 1x: tắt vuốt độ sáng/âm lượng (VD-04) và hiện nút "Đặt lại zoom". */
    val isZoomed: Boolean get() = scale > PlayerConstants.ZOOMED_THRESHOLD

    fun onTransform(zoomChange: Float, panChange: Offset) {
        val newScale = (scale * zoomChange).coerceIn(1f, PlayerConstants.MAX_ZOOM)
        scale = newScale
        offset = if (newScale <= 1f) Offset.Zero else clamp(offset + panChange, newScale)
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** Khi phóng s lần, mép ảnh lố ra mỗi bên (s - 1) / 2 kích thước khung: kéo tối đa chừng đó. */
    private fun clamp(value: Offset, scale: Float): Offset {
        val maxX = frameSize.width * (scale - 1f) / 2f
        val maxY = frameSize.height * (scale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }
}

/**
 * Nhận cử chỉ hai ngón zoom/kéo cho [state]. Tự nhận bằng `calculateZoom`/`calculatePan` thay cho `Modifier.transformable`
 * (`rememberTransformableState` đã deprecated, và `transformable` buộc phải nuốt cả cử chỉ một ngón).
 *
 * - Hai ngón trở lên: zoom và kéo ngay, và nuốt sự kiện để các bộ nhận cử chỉ khác (chạm, vuốt độ sáng) bỏ qua.
 * - Một ngón: chỉ là kéo khi đang zoom lớn hơn 1x và đã vượt ngưỡng trượt; ở 1x không đụng tới sự kiện nên chạm và vuốt dọc
 *   (độ sáng/âm lượng) vẫn đi tới các bộ nhận cử chỉ khác.
 *
 * [enabled] false (đang khóa thao tác) thì tắt.
 */
@Composable
internal fun Modifier.playerZoomable(state: ZoomState, enabled: Boolean): Modifier {
    // Luôn giữ modifier và kiểm tra cờ bên trong: đổi cấu trúc chuỗi modifier giữa cử chỉ làm Compose hủy cử chỉ đang chạy.
    val allowed by rememberUpdatedState(enabled)
    return pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var moved = Offset.Zero
            // Đã bắt đầu kéo/zoom trong cử chỉ này: giữ nguyên kể cả khi nhấc bớt một ngón.
            var active = false
            do {
                val event = awaitPointerEvent()
                if (!allowed) continue
                val pressed = event.changes.count { it.pressed }
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (pressed >= 2) {
                    active = true
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        state.onTransform(zoomChange, panChange)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } else if (state.isZoomed) {
                    moved += panChange
                    if (!active && moved.getDistance() > viewConfiguration.touchSlop) active = true
                    if (active) {
                        state.onTransform(1f, panChange)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
