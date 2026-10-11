package com.lambao.odv.feature.pdfviewer

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.lambao.odv.core.designsystem.theme.ODVDuration
import kotlin.math.abs

/**
 * Zoom của vùng đọc PDF (PD-03): hai ngón để zoom từ 1x đến [PdfViewerConstants.MAX_ZOOM]x, chạm đúp để vào/ra zoom, một ngón kéo khi đã
 * zoom. 1x là vừa chiều rộng màn hình (mặc định).
 *
 * Lớp đồ họa phóng quanh **góc trên-trái** của vùng đọc (`TransformOrigin(0, 0)`): điểm nội dung `c` hiện ở `scale * c + translation`.
 * Dịch chuyển bị chặn để vùng nhìn thấy luôn nằm trong vùng đọc: `translation` thuộc `[viewport - viewport * scale, 0]`. Theo chiều dọc
 * ở chế độ cuộn, vùng đọc chỉ là một "cửa sổ" của danh sách dài, nên phần kéo vượt mép cửa sổ được trả về ([applyTransform]) để nơi gọi
 * chuyển thành cuộn danh sách.
 *
 * Mọi hàm đổi trạng thái đặt tên `apply*`/`reset`, không dùng `setX` (tránh trùng tên với bộ set của thuộc tính `private set`, sự cố
 * "Platform declaration clash" ở `ShortsReshuffle.kt`).
 */
@Stable
internal class PdfZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var translationX by mutableFloatStateOf(0f)
        private set
    var translationY by mutableFloatStateOf(0f)
        private set

    // Chỉ đọc trong xử lý cử chỉ (không phải state): được cập nhật từ SideEffect của nơi gọi.
    private var viewportWidth = 0f
    private var viewportHeight = 0f

    /** Đang zoom lớn hơn 1x: tắt lật trang ở chế độ lật ngang và cho một ngón kéo. */
    val isZoomed: Boolean get() = scale > ZOOMED_THRESHOLD

    fun updateViewport(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
    }

    /**
     * Zoom [zoomChange] lần quanh [centroid] (toạ độ vùng đọc, điểm nội dung dưới nó đứng yên) rồi kéo thêm [pan]. Trả về phần kéo **dọc**
     * không áp được vì đã chạm mép cửa sổ (px màn hình, dương là ngón đi xuống, nghĩa là muốn xem phía trên), để nơi gọi cuộn danh sách.
     */
    fun applyTransform(centroid: Offset, pan: Offset, zoomChange: Float): Float {
        val newScale = (scale * zoomChange).coerceIn(1f, PdfViewerConstants.MAX_ZOOM)
        val ratio = newScale / scale
        scale = newScale
        // Zoom trước: điểm dưới centroid đứng yên, rồi chặn vào cửa sổ.
        val zoomedX = clampX(centroid.x - (centroid.x - translationX) * ratio)
        val zoomedY = clampY(centroid.y - (centroid.y - translationY) * ratio)
        // Kéo sau: phần dọc vượt mép là phần trả về, tách khỏi phần do zoom làm lệch.
        val wantedY = zoomedY + pan.y
        translationX = clampX(zoomedX + pan.x)
        translationY = clampY(wantedY)
        return wantedY - translationY
    }

    /** Zoom mượt tới [target] quanh [centroid]; dùng cho chạm đúp. */
    suspend fun animateTo(target: Float, centroid: Offset) {
        animate(initialValue = scale, targetValue = target, animationSpec = tween(ODVDuration.fast)) { value, _ ->
            applyTransform(centroid, Offset.Zero, value / scale)
        }
    }

    /** Chạm đúp: đang zoom thì về 1x, chưa thì zoom tới [PdfViewerConstants.DOUBLE_TAP_ZOOM] quanh điểm chạm. */
    suspend fun toggleAt(point: Offset) {
        animateTo(if (isZoomed) 1f else PdfViewerConstants.DOUBLE_TAP_ZOOM, point)
    }

    fun reset() {
        scale = 1f
        translationX = 0f
        translationY = 0f
    }

    private fun clampX(value: Float): Float = value.coerceIn(viewportWidth - viewportWidth * scale, 0f)

    private fun clampY(value: Float): Float = value.coerceIn(viewportHeight - viewportHeight * scale, 0f)

    private companion object {
        /** Dưới ngưỡng này coi như chưa zoom (sai số làm tròn của cử chỉ). */
        const val ZOOMED_THRESHOLD = 1.01f
    }
}

/**
 * Nhận cử chỉ zoom/kéo cho vùng đọc. Nhận ở `PointerEventPass.Initial` để giành quyền trước danh sách và Pager con: khi đã nhận cử chỉ thì
 * nuốt sự kiện, con thấy `isConsumed` và bỏ qua. Chỉ nhận khi (a) hai ngón trở lên, hoặc (b) một ngón kéo quá ngưỡng trượt lúc đang
 * zoom ([isZoomed]); một ngón ở 1x không bị đụng tới nên danh sách cuộn bình thường và chạm vẫn tới bộ nhận chạm.
 *
 * [onEnd] nhận vận tốc (px/giây) nếu cử chỉ là một ngón đã nhận, để nơi gọi tiếp tục quán tính; null nếu không có quán tính.
 */
internal suspend fun PointerInputScope.detectPdfZoomGestures(
    isZoomed: () -> Boolean,
    onStart: () -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoomChange: Float) -> Unit,
    onEnd: (velocity: Offset?) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onStart()
        val tracker = VelocityTracker()
        val slop = viewConfiguration.touchSlop
        var zoom = 1f
        var pan = Offset.Zero
        var claimed = false
        var multiTouch = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2) multiTouch = true
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            // Chưa có ngón nào thì centroid là Unspecified; lấy tâm vùng chạm cho an toàn.
            val centroid = event.calculateCentroid().takeIf { it.isSpecified } ?: Offset(size.width / 2f, size.height / 2f)
            if (!claimed) {
                zoom *= zoomChange
                pan += panChange
                val zoomMotion = abs(1f - zoom) * event.calculateCentroidSize(useCurrent = false)
                claimed = zoomMotion > slop || (pan.getDistance() > slop && (pressed >= 2 || isZoomed()))
                // Vừa vượt ngưỡng: áp phần đã tích lũy một lần để nội dung không bị giật lúc bắt đầu.
                if (claimed) onTransform(centroid, pan, zoom)
            } else if (zoomChange != 1f || panChange != Offset.Zero) {
                onTransform(centroid, panChange, zoomChange)
            }
            if (claimed) {
                event.changes.firstOrNull { it.pressed }?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        val velocity = if (claimed && !multiTouch) tracker.calculateVelocity().let { Offset(it.x, it.y) } else null
        onEnd(velocity)
    }
}

/**
 * Quán tính cuộn dọc sau khi nhả tay lúc đang zoom: phần ngón kéo trên màn hình quy về px của danh sách bằng cách chia cho [zoom] scale.
 * Dừng khi danh sách chạm mép. Nơi gọi hủy khi có cử chỉ mới.
 */
internal suspend fun flingList(listState: LazyListState, velocityY: Float, zoom: PdfZoomState) {
    var last = 0f
    AnimationState(initialValue = 0f, initialVelocity = velocityY).animateDecay(exponentialDecay()) {
        val delta = value - last
        last = value
        // Ngón đi xuống (delta dương) thì danh sách cuộn lùi (âm).
        val consumed = listState.dispatchRawDelta(-delta / zoom.scale)
        if (delta != 0f && consumed == 0f) cancelAnimation()
    }
}
