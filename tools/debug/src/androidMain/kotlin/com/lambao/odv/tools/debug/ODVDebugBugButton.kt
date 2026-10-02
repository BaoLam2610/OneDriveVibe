package com.lambao.odv.tools.debug

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlin.math.roundToInt

/**
 * Vị trí nút bọ, dùng chung cho bản vẽ trong Activity và bản vẽ trong cửa sổ riêng trên Dialog/BottomSheet. Là state của
 * Compose nên cả hai bản cập nhật cùng lúc. Mất khi tiến trình chết. Toạ độ theo cửa sổ Activity (px).
 */
private object BugButtonPosition {
    var offset by mutableStateOf<Offset?>(null)
}

private const val BUTTON_DP = 48

private fun open(context: Context) {
    context.startActivity(Intent(context, DebugActivity::class.java))
}

/**
 * Nút nổi hình con bọ cánh cứng (ADR-0012): chạm để mở [DebugActivity].
 *
 * - [asWindow] = false: vẽ trong Activity, kéo thả được. Đặt ở gốc của app.
 * - [asWindow] = true: được `ODVDialog`/`ODVBottomSheet` gọi ngay sau khi mở cửa sổ của chúng (qua `LocalODVTopOverlay`).
 *   Dialog và sheet là cửa sổ riêng nên nút vẽ trong Activity bị đè; bản này tự mở một Popup, cửa sổ mở sau nên nằm
 *   trên. Popup không focus nên chạm ra ngoài nút vẫn xuyên xuống Dialog/sheet. Bản trong cửa sổ chỉ chạm được, không
 *   kéo (cửa sổ tự di chuyển theo ngón tay làm cử chỉ kéo bị giật); muốn dời nút thì kéo ở màn thường.
 */
@Composable
fun ODVDebugBugButton(asWindow: Boolean = false, modifier: Modifier = Modifier) {
    if (asWindow) BugButtonInWindow() else BugButtonInActivity(modifier)
}

@Composable
private fun BugButtonInActivity(modifier: Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { BUTTON_DP.dp.toPx() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxX = (constraints.maxWidth - sizePx).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - sizePx).coerceAtLeast(0f)
        val initial = Offset(maxX, with(density) { 160.dp.toPx() }.coerceAtMost(maxY))
        val position = (BugButtonPosition.offset ?: initial).let { Offset(it.x.coerceIn(0f, maxX), it.y.coerceIn(0f, maxY)) }
        BugButtonBody(
            Modifier
                // Kẹp lại mỗi lần đo: xoay màn hình hay chia đôi màn hình có thể làm vị trí đã lưu nằm ngoài vùng nhìn thấy.
                .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val current = BugButtonPosition.offset ?: initial
                        BugButtonPosition.offset = Offset(
                            (current.x + drag.x).coerceIn(0f, maxX),
                            (current.y + drag.y).coerceIn(0f, maxY),
                        )
                    }
                }
                .pointerInput(Unit) { detectTapGestures(onTap = { open(context) }) },
        )
    }
}

@Composable
private fun BugButtonInWindow() {
    val context = LocalContext.current
    val density = LocalDensity.current
    // View của cửa sổ chứa Popup này (cửa sổ Dialog/sheet): dùng để đổi toạ độ màn hình sang toạ độ trong cửa sổ đó.
    val hostView = LocalView.current
    val sizePx = with(density) { BUTTON_DP.dp.toPx() }
    val fallbackY = with(density) { 160.dp.toPx() }
    val saved = BugButtonPosition.offset
    // Tạo lại provider khi vị trí đổi để Popup tự dời theo (Popup đọc lại positionProvider mỗi lần recomposition).
    val provider = remember(saved, sizePx, fallbackY, hostView) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                // Vị trí lưu theo toạ độ màn hình (cửa sổ Activity phủ kín màn hình). Popup là cửa sổ con nên toạ độ trả về
                // tính từ góc cửa sổ chứa nó (Dialog/sheet), phải trừ đi gốc của cửa sổ đó trên màn hình.
                val metrics = hostView.resources.displayMetrics
                val maxX = (metrics.widthPixels - sizePx).coerceAtLeast(0f)
                val maxY = (metrics.heightPixels - sizePx).coerceAtLeast(0f)
                val desired = saved ?: Offset(maxX, fallbackY.coerceAtMost(maxY))
                val origin = IntArray(2).also { hostView.getLocationOnScreen(it) }
                return IntOffset(
                    desired.x.coerceIn(0f, maxX).roundToInt() - origin[0],
                    desired.y.coerceIn(0f, maxY).roundToInt() - origin[1],
                )
            }
        }
    }
    Popup(
        popupPositionProvider = provider,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        BugButtonBody(Modifier.pointerInput(Unit) { detectTapGestures(onTap = { open(context) }) })
    }
}

@Composable
private fun BugButtonBody(modifier: Modifier) {
    val colors = ODVTheme.colors
    Box(
        modifier
            .size(BUTTON_DP.dp)
            .shadow(6.dp, CircleShape)
            .background(colors.volt, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_debug_bug), contentDescription = "Debug", tint = colors.onVolt)
    }
}
