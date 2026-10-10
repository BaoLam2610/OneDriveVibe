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
 * Cử chỉ kéo xuống để xáo lại (SV-03, SH6): chỉ khi [canPull] (đang ở video đầu và Pager không cuộn lùi được nữa). Phần kéo mà Pager không
 * dùng tới được cộng dồn vào [pull]; thả tay khi đã quá [thresholdPx] thì gọi [onTrigger]. Ở video khác video đầu, vuốt xuống là về video
 * trước (SV-04) do Pager tự xử lý nên không bao giờ vào đây.
 *
 * Khi đang giữ vòng kéo mà ngón tay đi **ngược lên** (người dùng nới tay hoặc rung tay), phần đi lên được trừ vào [pull] trước ([onPreScroll])
 * chứ không chuyển cho Pager. Trước đây Pager nhận phần đi lên đó như một cú vuốt sang video kế tiếp: vòng kéo tự mất và màn nhảy sang
 * video khác dù chưa thả tay (lỗi 2026-10-11).
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

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || pull <= 0f || available.y >= 0f) return Offset.Zero
        // Ngón tay đi ngược lên khi vòng đang kéo: thu vòng lại, đừng để Pager cuộn sang video kế tiếp.
        val used = maxOf(available.y, -pull)
        applyPull(pull + used)
        return Offset(0f, used)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (canPull() && available.y > 0f) {
            if (pull <= 0f) shortsLog.d { "[Short][Pull] bắt đầu kéo xuống ở video đầu" }
            applyPull((pull + available.y).coerceAtMost(thresholdPx * MAX_PULL_FACTOR))
            return Offset(0f, available.y)
        }
        // Pager tự cuộn (không phải do nới tay đã xử lý ở trên): hủy lần kéo, ghi log để biết khi nào xảy ra.
        if (consumed.y != 0f && pull > 0f) {
            shortsLog.w { "[Short][Pull] Pager tự cuộn nên hủy lần kéo (consumed=${consumed.y}px, đã kéo=${pull}px)" }
            applyPull(0f)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val fire = pull >= thresholdPx
        shortsLog.i {
            "[Short][Pull] thả tay: đã kéo=${pull}px ngưỡng=${thresholdPx}px vận tốc=${available.y}px/s → ${if (fire) "xáo lại" else "hủy"}"
        }
        applyPull(0f)
        reachedThreshold = false
        if (fire) onTrigger()
        return Velocity.Zero
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
