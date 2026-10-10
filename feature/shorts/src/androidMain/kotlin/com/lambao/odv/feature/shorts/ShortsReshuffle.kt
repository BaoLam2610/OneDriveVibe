package com.lambao.odv.feature.shorts

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Cử chỉ kéo xuống để xáo lại (SV-03, SH6). Chỉ bắt đầu khi [canPull] (đang ở video đầu) và Pager không còn gì để cuộn lùi: phần kéo xuống mà
 * Pager không dùng tới ([onPostScroll]) mở đầu [pull]. Thả tay khi đã quá [thresholdPx] thì gọi [onTrigger]. Ở video khác video đầu, vuốt xuống
 * là về video trước (SV-04) do Pager tự xử lý nên không bao giờ vào đây.
 *
 * **Từ lúc bắt đầu kéo cho tới khi thả tay, lớp này sở hữu cử chỉ** (`owning`): [onPreScroll] nuốt mọi chuyển động của ngón tay, cả xuống lẫn
 * ngược lên, nên Pager không bao giờ nhận được gì để cuộn. Trước đây phần chuyển động còn lại (lệch dưới 1px, hoặc ngón tay nới lên) lọt xuống
 * Pager, nó cuộn một chút và vòng kéo bị hủy dù chưa thả tay (log 2026-10-11: `consumed=0.57px, đã kéo=432px`). Ngón tay đi ngược lên quá điểm bắt
 * đầu chỉ thu vòng về 0 chứ không chuyển cho Pager; muốn sang video kế tiếp phải thả tay rồi vuốt lên lần khác.
 *
 * Quy ước tên: hàm cập nhật là `applyPull`, không phải `setPull`, vì `var pull ... private set` đã sinh `setPull(F)V` ("Platform declaration clash").
 */
@Stable
internal class ReshuffleConnection(
    private val thresholdPx: Float,
    private val canPull: () -> Boolean,
    private val onTrigger: () -> Unit,
) : NestedScrollConnection {

    /** Quãng đã kéo (px), để vẽ vòng xáo lại trượt xuống theo ngón tay. */
    var pull by mutableFloatStateOf(0f)
        private set

    /** 0..1 theo ngưỡng; 1 nghĩa là thả ra sẽ xáo lại. */
    val progress: Float get() = (pull / thresholdPx).coerceIn(0f, 1f)

    /** Đã báo "đạt ngưỡng" trong lần kéo này chưa, để log mỗi lần kéo đúng một dòng khi vượt và một dòng khi tụt xuống dưới ngưỡng. */
    private var reachedThreshold = false

    private val maxPull: Float get() = thresholdPx * MAX_PULL_FACTOR

    /**
     * Lần chạm hiện tại đã thuộc về cử chỉ xáo lại (từ lúc bắt đầu kéo xuống cho tới khi thả tay). Khác với [pull] > 0: ngón tay có thể nới
     * ngược lên làm [pull] về 0 mà người dùng vẫn đang trong lần kéo đó, và khi ấy cú vuốt lên **không** được hiểu là sang video kế tiếp
     * (kiểm tay 2026-10-11, mục 3: kéo xuống rồi kéo lên bị xung đột với vuốt lên để sang video dưới).
     */
    private var owning = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || !owning) return Offset.Zero
        // Đang trong lần kéo xáo lại: nuốt hết chuyển động (xuống, nới lên, và cả đi lên quá điểm bắt đầu) để Pager không cuộn gì.
        applyPull((pull + available.y).coerceIn(0f, maxPull))
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || owning || available.y <= 0f || !canPull()) return Offset.Zero
        shortsLog.d { "[Short][Pull] bắt đầu kéo xuống ở video đầu (còn ${available.y}px sau khi Pager cuộn ${consumed.y}px)" }
        owning = true
        applyPull(available.y.coerceAtMost(maxPull))
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!owning) return Velocity.Zero
        val fire = pull >= thresholdPx
        shortsLog.i {
            "[Short][Pull] thả tay: đã kéo=${pull}px ngưỡng=${thresholdPx}px vận tốc=${available.y}px/s → ${if (fire) "xáo lại" else "hủy"}"
        }
        finishGesture()
        if (fire) onTrigger()
        // Nuốt vận tốc của cú thả: nếu không Pager sẽ fling theo và có thể trượt sang video kế tiếp.
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // Phòng khi cử chỉ kết thúc mà onPreFling không tới (bị hủy): đừng để lần chạm sau bị nuốt oan.
        if (owning) {
            shortsLog.w { "[Short][Pull] cử chỉ kết thúc mà chưa qua onPreFling, nhả quyền sở hữu" }
            finishGesture()
        }
        return Velocity.Zero
    }

    private fun finishGesture() {
        owning = false
        reachedThreshold = false
        applyPull(0f)
    }

    private fun applyPull(value: Float) {
        pull = value.coerceAtLeast(0f)
        val reached = pull >= thresholdPx
        if (reached != reachedThreshold) {
            reachedThreshold = reached
            shortsLog.d { "[Short][Pull] ${if (reached) "đạt" else "tụt xuống dưới"} ngưỡng xáo lại (${pull}px / ${thresholdPx}px)" }
        }
    }

    private companion object {
        /** Vòng không kéo xa quá chừng này lần ngưỡng cho khỏi trôi khỏi màn. */
        const val MAX_PULL_FACTOR = 1.5f
    }
}
