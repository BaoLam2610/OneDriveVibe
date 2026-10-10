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

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (canPull() && available.y > 0f) {
            pull = (pull + available.y).coerceAtMost(thresholdPx * MAX_PULL_FACTOR)
            return Offset(0f, available.y)
        }
        // Pager đang tự cuộn (người dùng đổi hướng): hủy lần kéo.
        if (consumed.y != 0f) pull = 0f
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val fire = pull >= thresholdPx
        pull = 0f
        if (fire) onTrigger()
        return Velocity.Zero
    }

    private companion object {
        /** Vòng không kéo xa quá chừng này lần ngưỡng cho khỏi trôi khỏi màn. */
        const val MAX_PULL_FACTOR = 1.5f
    }
}
