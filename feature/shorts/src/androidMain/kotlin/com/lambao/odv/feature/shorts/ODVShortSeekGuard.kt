package com.lambao.odv.feature.shorts

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Cầu nối để thanh tua của Short "ăn" cả dải trên cùng của thanh điều hướng đáy (kiểm tay 2026-10-11, như YouTube): thanh tua nằm ngay trên
 * đường tiếp xúc với thanh đáy, mà Compose không chuyển chạm cho view con ngoài phạm vi view cha, nên chạm trúng chút dưới đường đó là rơi vào
 * một tab. Màn Chính phủ [ODVShortSeekGuardStrip] lên đầu thanh đáy khi đang ở tab Short; dải này chuyển cử chỉ cho thanh tua đang đăng ký ở
 * [target], nên bấm vào đó rồi kéo vẫn là tua, không bao giờ là chạm tab.
 *
 * Chỉ giữ tham chiếu, không giữ trạng thái: thanh tua (`ShortSeek`) đăng ký khi vào màn và hủy khi rời.
 */
class ODVShortSeekGuard {
    // State của Compose: dải chỉ được dựng (và chiếm chạm) khi có thanh tua đăng ký, nên video chưa nạp/trạng thái trống/lỗi thì chạm vào đầu
    // thanh đáy vẫn đổi được tab.
    internal var target: Target? by mutableStateOf(null)

    internal interface Target {
        fun onTouch(down: Boolean)
        fun onSeek(fraction: Float)
        fun onFinish()
    }
}

/** Màn Chính cung cấp cho cả vùng nội dung lẫn thanh đáy; null khi không có thì thanh tua chỉ dùng vùng chạm của nó. */
val LocalODVShortSeekGuard = staticCompositionLocalOf<ODVShortSeekGuard?> { null }

/**
 * Dải trong suốt chồng lên đầu thanh đáy, cao [ShortsConstants.SEEK_GUARD_DP]. Không vẽ gì. Khi có thanh tua đăng ký thì nhận luôn mọi cử chỉ
 * bắt đầu trong dải và chuyển thành tua theo vị trí ngang; chưa có thanh tua (video chưa nạp) thì để chạm đi tiếp xuống tab như thường.
 * Dải phải đặt **sau** thanh đáy trong cùng Box để nhận chạm trước.
 */
@Composable
fun ODVShortSeekGuardStrip(modifier: Modifier = Modifier) {
    val guard = LocalODVShortSeekGuard.current ?: return
    if (guard.target == null) return
    Box(
        modifier
            .fillMaxWidth()
            .height(ShortsConstants.SEEK_GUARD_DP.dp)
            .pointerInput(guard) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val target = guard.target ?: return@awaitEachGesture
                    shortsLog.d { "[Short][Seek] dải bảo vệ nhận cử chỉ trên thanh đáy" }
                    down.consume()
                    target.onTouch(true)
                    try {
                        target.onSeek((down.position.x / size.width).coerceIn(0f, 1f))
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            target.onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                            change.consume()
                        }
                    } finally {
                        // Cả khi cử chỉ bị hủy giữa chừng: chốt lần kéo rồi nhả cờ chạm.
                        target.onFinish()
                        target.onTouch(false)
                    }
                }
            },
    )
}
