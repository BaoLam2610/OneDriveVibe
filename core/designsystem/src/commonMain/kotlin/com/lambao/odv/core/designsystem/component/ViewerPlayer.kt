package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

// Mọi component có tiền tố ODVViewer* (và ODVPlayState, ODVPlayerLabels) là thành phần riêng của ba màn xem (video, ảnh, PDF) theo mục 4.5, luôn nằm trên nền đen và dùng
// màu `media.*` cố định (không đổi theo theme). Chúng KHÔNG phải component dùng chung: component dùng chung không được biết
// khái niệm này (xem ODVMediaDefaults).

private fun Float.unit(): Float = if (isNaN()) 0f else coerceIn(0f, 1f)

/**
 * Thanh trên của màn xem (ViewerTopBar, mục 4.5): nút quay lại, tên tệp `body-strong` trắng một dòng, rồi các nút bên phải
 * (video: thông tin, khóa; ảnh: thông tin; PDF: kiểu đọc). Nút bên phải dùng [ODVMediaDefaults.iconButtonColors].
 *
 * @param withGradient dải gradient đen 70% → trong suốt cao 96 phía sau thanh (màn ảnh).
 */
@Composable
fun ODVViewerTopBar(
    title: String,
    backContentDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    withGradient: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (withGradient) {
                    Modifier
                        .height(96.dp)
                        .background(Brush.verticalGradient(listOf(ODVMediaColors.scrimStrong, Color.Transparent)))
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ODVIconButton(ODVIcon.ArrowLeft, backContentDescription, onBack, colors = ODVMediaDefaults.iconButtonColors())
            Text(
                title,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = ODVTheme.typography.bodyStrong,
                color = ODVMediaColors.onMedia,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            actions()
        }
    }
}

/**
 * Thanh tua (SeekBar, mục 4.5): vùng chạm cao 24, rãnh 4 tròn `media.track`, phần đã tải `media.buffer`, phần đã xem
 * `media.accent`, núm 16 `media.accent`. Chạm hoặc kéo để tua. Vùng chạm cao 24 đúng spec (nhỏ hơn 48 của mục 1.6); TalkBack điều chỉnh qua `setProgress`.
 *
 * @param position vị trí hiện tại trong 0..1; [buffered] phần đã tải trước trong 0..1.
 * @param contentDescription ví dụ "Vị trí phát"; [valueDescription] đọc bằng lời, ví dụ "12 phút 4 giây trên 1 giờ 26 phút".
 * @param onSeek nhận vị trí mới 0..1 (gọi liên tục khi kéo); [onSeekFinished] khi nhả tay.
 */
@Composable
fun ODVViewerSeekBar(
    position: Float,
    buffered: Float,
    onSeek: (Float) -> Unit,
    contentDescription: String,
    valueDescription: String,
    modifier: Modifier = Modifier,
    onSeekFinished: () -> Unit = {},
) {
    val seek by rememberUpdatedState(onSeek)
    val finished by rememberUpdatedState(onSeekFinished)
    val played = position.unit()
    val loaded = buffered.unit()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ODVSize.seekTouch)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = valueDescription
                progressBarRangeInfo = ProgressBarRangeInfo(played, 0f..1f)
                setProgress { target ->
                    seek(target.unit())
                    finished()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    seek((offset.x / size.width).unit())
                    finished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> seek((offset.x / size.width).unit()) },
                    onDragEnd = { finished() },
                    onDragCancel = { finished() },
                ) { change, _ -> seek((change.position.x / size.width).unit()) }
            },
    ) {
        val trackHeight = ODVSize.seekTrack.toPx()
        val top = (size.height - trackHeight) / 2f
        val radius = CornerRadius(trackHeight / 2f)
        drawRoundRect(ODVMediaColors.track, Offset(0f, top), Size(size.width, trackHeight), radius)
        drawRoundRect(ODVMediaColors.buffer, Offset(0f, top), Size(size.width * loaded, trackHeight), radius)
        drawRoundRect(ODVMediaColors.accent, Offset(0f, top), Size(size.width * played, trackHeight), radius)
        drawCircle(ODVMediaColors.accent, radius = ODVSize.seekThumb.toPx() / 2f, center = Offset(size.width * played, size.height / 2f))
    }
}

/** Thanh tiến độ mảnh khi ẩn điều khiển (MiniProgress, mục 4.5): cao 3, sát đáy khung video. */
@Composable
fun ODVViewerMiniProgress(
    position: Float,
    buffered: Float,
    modifier: Modifier = Modifier,
) {
    val played = position.unit()
    val loaded = buffered.unit()
    Canvas(modifier.fillMaxWidth().height(ODVSize.miniProgress)) {
        drawRect(ODVMediaColors.track)
        drawRect(ODVMediaColors.buffer, size = Size(size.width * loaded, size.height))
        drawRect(ODVMediaColors.accent, size = Size(size.width * played, size.height))
    }
}

/** Nút tốc độ (chữ `timecode` "1x", cao 48) trên thanh dưới. [contentDescription] ví dụ "Tốc độ phát 1x". */
@Composable
fun ODVViewerSpeedButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interaction)
    Box(
        modifier = modifier
            .height(ODVSize.tapTarget)
            .defaultMinSize(minWidth = ODVSize.tapTarget)
            .pressScale(pressScale)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp), style = ODVTheme.typography.timecode, color = ODVMediaColors.onMedia)
    }
}

/**
 * Thanh dưới của video (ViewerBottomBar, mục 4.5): lề trái 16, phải 8. Dòng trên là [seekBar]; dòng dưới là thời gian
 * (`timecode`, chiếm chỗ trống) rồi các nút [actions] (tốc độ, chế độ phát, khung hình, xoay màn hình). Không có nút toàn màn hình (VD-07).
 * Khoảng cách tới đáy do nơi gọi đặt (24 ở hướng dọc, 8 ở hướng ngang).
 */
@Composable
fun ODVViewerBottomBar(
    seekBar: @Composable () -> Unit,
    timeText: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp)) {
        Box(Modifier.padding(end = 8.dp)) { seekBar() }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(timeText, modifier = Modifier.weight(1f), style = ODVTheme.typography.timecode, color = ODVMediaColors.onMedia)
            actions()
        }
    }
}

/** Trạng thái nút giữa của video (mục 4.5). */
enum class ODVPlayState { Playing, Paused, Replay, Loading }

/** Nhãn TalkBack của [ODVViewerCenterControls]; do nơi gọi truyền để theo ngôn ngữ và bước tua trong Cài đặt (ví dụ "Tua lùi 10 giây"). */
@Immutable
class ODVPlayerLabels(
    val previous: String,
    val next: String,
    val rewind: String,
    val forward: String,
    val play: String,
    val pause: String,
    val replay: String,
    val loading: String,
)

/**
 * Nút giữa 72 tròn nền `media.accent`, icon 36 `media.on-accent`; là mảng volt duy nhất của màn xem.
 * Bốn trạng thái: pause (đang phát), play (đang dừng), replay (hết video), spinner (đang tải; nút bị khóa).
 */
@Composable
fun ODVViewerPlayButton(
    state: ODVPlayState,
    labels: ODVPlayerLabels,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = when (state) {
        ODVPlayState.Playing -> labels.pause
        ODVPlayState.Paused -> labels.play
        ODVPlayState.Replay -> labels.replay
        ODVPlayState.Loading -> labels.loading
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressScale = rememberPressScale(interaction)
    Box(
        modifier = modifier
            .size(ODVSize.playButton)
            .pressScale(pressScale)
            .focusRing(focused, CircleShape, ODVMediaColors.onMedia, ODVMediaColors.background)
            .background(ODVMediaColors.accent, CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = state != ODVPlayState.Loading, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ODVPlayState.Playing -> ODVIcon(ODVIcon.Pause, null, tint = ODVMediaColors.onAccent, size = 36.dp)
            ODVPlayState.Paused -> ODVIcon(ODVIcon.Play, null, tint = ODVMediaColors.onAccent, size = 36.dp)
            ODVPlayState.Replay -> ODVIcon(ODVIcon.Replay, null, tint = ODVMediaColors.onAccent, size = 36.dp)
            ODVPlayState.Loading -> ODVSpinner(size = 36.dp, color = ODVMediaColors.onAccent)
        }
    }
}

/**
 * Cụm điều khiển giữa (mục 4.5): hàng ngang căn giữa gồm trước, tua lùi, nút giữa, tua tới, sau; cách nhau 12 (hướng dọc),
 * 20 (hướng ngang, truyền [spacing]). Nút trước/sau mờ 0.38 và khóa khi ở đầu/cuối danh sách phát.
 */
@Composable
fun ODVViewerCenterControls(
    state: ODVPlayState,
    labels: ODVPlayerLabels,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
    hasPrevious: Boolean = true,
    hasNext: Boolean = true,
    spacing: Dp = 12.dp,
) {
    val colors = ODVMediaDefaults.iconButtonColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVIconButton(ODVIcon.Prev, labels.previous, onPrevious, colors = colors, enabled = hasPrevious)
        ODVIconButton(ODVIcon.Rewind, labels.rewind, onRewind, colors = colors)
        ODVViewerPlayButton(state, labels, onPlayPause)
        ODVIconButton(ODVIcon.Forward, labels.forward, onForward, colors = colors)
        ODVIconButton(ODVIcon.Next, labels.next, onNext, colors = colors, enabled = hasNext)
    }
}
