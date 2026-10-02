package com.lambao.odv.tools.debug

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlin.math.roundToInt

/** Vị trí nút bọ trong phiên, để chuyển màn không làm nút nhảy về chỗ cũ. Mất khi tiến trình chết. */
private object BugButtonPosition {
    var offset: Offset? = null
}

/**
 * Nút nổi hình con bọ cánh cứng (ADR-0012): kéo thả để khỏi che nội dung, chạm để mở [DebugActivity].
 * Đặt chồng lên mọi màn của app; vùng chạm 48dp, chỉ chiếm đúng ô nút nên không chặn thao tác bên dưới.
 */
@Composable
fun ODVDebugBugButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = ODVTheme.colors
    val density = LocalDensity.current
    val sizePx = with(density) { 48.dp.toPx() }

    BoxWithConstraints(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val maxX = (constraints.maxWidth - sizePx).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - sizePx).coerceAtLeast(0f)
        var position by remember {
            mutableStateOf(BugButtonPosition.offset ?: Offset(maxX, with(density) { 160.dp.toPx() }.coerceAtMost(maxY)))
        }
        Box(
            Modifier
                // Kẹp lại mỗi lần đo: xoay màn hình hay chia đôi màn hình có thể làm vị trí đã lưu nằm ngoài vùng nhìn thấy.
                .offset { IntOffset(position.x.coerceIn(0f, maxX).roundToInt(), position.y.coerceIn(0f, maxY).roundToInt()) }
                .size(48.dp)
                .shadow(6.dp, CircleShape)
                .background(colors.volt, CircleShape)
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        position = Offset(
                            (position.x + drag.x).coerceIn(0f, maxX),
                            (position.y + drag.y).coerceIn(0f, maxY),
                        )
                        BugButtonPosition.offset = position
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { context.startActivity(Intent(context, DebugActivity::class.java)) })
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_debug_bug), contentDescription = "Debug", tint = colors.onVolt)
        }
    }
}
