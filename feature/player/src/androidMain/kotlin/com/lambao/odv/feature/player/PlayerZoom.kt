package com.lambao.odv.feature.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/**
 * Zoom tự do của video (VD-18): hai ngón để zoom từ 1x đến [PlayerConstants.MAX_ZOOM]x, khi lớn hơn 1x thì một ngón kéo để di chuyển.
 * Chạm đúp vẫn là tua, không dùng để zoom. Zoom về 1x khi chuyển video hoặc đổi hướng màn hình (nơi gọi gọi [reset]).
 *
 * Video được phóng quanh tâm vùng hiển thị ([viewportSize], cả màn hình) chứ không bị cắt ở mép khung video: ở Vừa khung hướng dọc,
 * khung 16:9 chỉ là bố cục lúc 1x, khi zoom video tràn ra toàn màn. [contentSize] là hình chữ nhật video thật (px) để chặn kéo lố
 * vào vùng viền đen: theo chiều nào video đã phóng còn nhỏ hơn màn thì giữ ở giữa, lớn hơn thì kéo tới đúng mép video.
 */
@Stable
internal class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    private var viewportSize by mutableStateOf(IntSize.Zero)
    private var contentSize by mutableStateOf(IntSize.Zero)

    /** Đang zoom lớn hơn 1x: tắt vuốt độ sáng/âm lượng (VD-04) và hiện nút "Đặt lại zoom". */
    val isZoomed: Boolean get() = scale > PlayerConstants.ZOOMED_THRESHOLD

    fun updateViewport(size: IntSize) {
        viewportSize = size
        offset = clamp(offset, scale)
    }

    fun updateContent(size: IntSize) {
        contentSize = size
        offset = clamp(offset, scale)
    }

    /**
     * [centroid] là điểm giữa các ngón trong toạ độ của vùng hiển thị: điểm video dưới nó đứng yên khi zoom (như trình xem ảnh).
     * Gốc phóng của lớp đồ họa là tâm vùng hiển thị, nên đổi centroid sang toạ độ tính từ tâm.
     */
    fun onTransform(zoomChange: Float, panChange: Offset, centroid: Offset) {
        val newScale = (scale * zoomChange).coerceIn(1f, PlayerConstants.MAX_ZOOM)
        if (newScale <= 1f) {
            scale = 1f
            offset = Offset.Zero
            return
        }
        val ratio = newScale / scale
        val c = centroid - Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        scale = newScale
        // [c] là điểm giữa sau khi kéo; điểm video dưới điểm giữa trước khi kéo là c - pan, nên phần kéo cũng bị nhân theo tỉ lệ zoom.
        offset = clamp(c - (c - offset) * ratio + panChange * ratio, newScale)
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /**
     * Video đã phóng rộng `content * s`, vùng hiển thị rộng `viewport`: mép lố ra mỗi bên `(content * s - viewport) / 2`,
     * kéo tối đa chừng đó (không âm: video còn nhỏ hơn màn thì không kéo). Chưa đo được video thì lấy cả vùng hiển thị.
     */
    private fun clamp(value: Offset, scale: Float): Offset {
        if (scale <= 1f) return Offset.Zero
        val contentW = if (contentSize.width > 0) contentSize.width else viewportSize.width
        val contentH = if (contentSize.height > 0) contentSize.height else viewportSize.height
        val maxX = max(0f, (contentW * scale - viewportSize.width) / 2f)
        val maxY = max(0f, (contentH * scale - viewportSize.height) / 2f)
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
                // Chưa có ngón nào thì centroid là Unspecified; lấy tâm vùng chạm (0 sau khi trừ tâm) cho an toàn.
                val centroid = event.calculateCentroid().takeIf { it.isSpecified } ?: Offset(size.width / 2f, size.height / 2f)
                if (pressed >= 2) {
                    active = true
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        state.onTransform(zoomChange, panChange, centroid)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } else if (state.isZoomed) {
                    moved += panChange
                    if (!active && moved.getDistance() > viewConfiguration.touchSlop) active = true
                    if (active) {
                        state.onTransform(1f, panChange, centroid)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
