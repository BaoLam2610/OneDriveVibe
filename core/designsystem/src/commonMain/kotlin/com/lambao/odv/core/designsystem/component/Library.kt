package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVElevation
import com.lambao.odv.core.designsystem.theme.ODVOverlayColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Tiêu đề nhóm theo ngày/tháng của tab Thư viện (DateHeader, mục 4.4): `body-strong`, padding 12/16/8, nền `bg`
 * (để dính trên cùng khi cuộn), số mục bên phải `body` `ink-muted`.
 */
@Composable
fun ODVDateHeader(
    title: String,
    count: String,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.semantics { heading() }, style = ODVTheme.typography.bodyStrong, color = colors.ink)
        Text(count, style = ODVTheme.typography.body, color = colors.inkMuted)
    }
}

/** Bo 4dp của ô lưới dày (mục 2.5: "ô Thư viện, 4dp trong lưới dày"). */
private val PhotoCellShape = RoundedCornerShape(4.dp)

/**
 * Ô lưới Thư viện (PhotoCell, mục 4.4): vuông, bo 4, thumbnail center-crop. Video có icon [kindIcon] 14 trắng ở góc trên trái
 * và huy hiệu [duration] gọn ở góc dưới phải.
 *
 * @param contentDescription nhãn TalkBack đủ thông tin, ví dụ "Video, mau-01.mp4, 1 giờ 26 phút, 974 MB, 23 tháng 5, 2026".
 */
@Composable
fun ODVPhotoCell(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    kindIcon: ODVIcon? = null,
    duration: String? = null,
    thumbnail: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(PhotoCellShape)
            .background(ODVTheme.colors.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            // Mô tả đã gồm thời lượng nên bỏ chữ của huy hiệu để TalkBack không đọc lặp.
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
    ) {
        thumbnail()
        if (kindIcon != null) {
            ODVIcon(kindIcon, contentDescription = null, tint = ODVOverlayColors.onScrim, size = 14.dp, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        }
        if (duration != null) {
            ODVDurationBadge(duration, Modifier.align(Alignment.BottomEnd).padding(4.dp), compact = true)
        }
    }
}

/** Ô khung chờ khi thumbnail chưa tải (TV-06): nền `surface-2`, không nhấp nháy, TalkBack bỏ qua. */
@Composable
fun ODVPhotoCellPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(ODVTheme.colors.surface2, PhotoCellShape)
            .clearAndSetSemantics {},
    )
}

/**
 * Cuộn nhanh theo thời gian (FastScroller, mục 4.4, TV-04): vùng chạm rộng 28 sát mép phải, rãnh 2dp `line`,
 * tay cầm 12 × 36 bo 6 nền `volt` viền 2dp `volt-text`. Khi kéo hiện bong bóng [bubbleText] (`inverse-surface`, bóng `shadow.lg`)
 * bên trái tay cầm. Chỉ nên hiện khi nội dung dài hơn khoảng 3 màn hình.
 *
 * Chiều cao do nơi gọi đặt (thường `fillMaxHeight()` trong một Box phủ danh sách).
 *
 * @param fraction vị trí hiện tại trong 0..1.
 * @param contentDescription ví dụ "Cuộn nhanh theo thời gian"; giá trị hiện tại đọc qua [bubbleText].
 */
@Composable
fun ODVFastScroller(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    bubbleText: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val currentOnChange by rememberUpdatedState(onFractionChange)
    var dragging by remember { mutableStateOf(false) }
    val value = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .width(ODVSize.fastScrollerTouch)
            .fillMaxHeight()
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = bubbleText
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress { target ->
                    currentOnChange(target.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(Unit) {
                val handle = ODVSize.fastScrollerHandleHeight.toPx()
                detectVerticalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    val travel = (size.height - handle).coerceAtLeast(1f)
                    currentOnChange(((change.position.y - handle / 2f) / travel).coerceIn(0f, 1f))
                }
            },
    ) {
        val handleTop = (maxHeight - ODVSize.fastScrollerHandleHeight) * value
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 13.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(colors.line, RoundedCornerShape(1.dp)),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp)
                .offset(y = handleTop)
                .size(ODVSize.fastScrollerHandleWidth, ODVSize.fastScrollerHandleHeight)
                .background(colors.volt, RoundedCornerShape(6.dp))
                .border(2.dp, colors.voltText, RoundedCornerShape(6.dp)),
        )
        if (dragging) {
            Text(
                bubbleText,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // Nằm bên trái tay cầm, tràn ra ngoài vùng chạm 28.
                    .wrapContentWidth(Alignment.End, unbounded = true)
                    .offset { IntOffset((-22).dp.roundToPx(), (handleTop + 2.dp).roundToPx()) }
                    .shadow(ODVElevation.lg, ODVTheme.shapes.full)
                    .background(colors.inverseSurface, ODVTheme.shapes.full)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                color = colors.inverseInk,
                maxLines = 1,
            )
        }
    }
}
