package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Viên thuốc nhãn trên màn xem (mục 4.5): nền `media.pill`, tròn, padding 6/14 (nổi bật: 8/18, chữ 14 đậm 600).
 * Dùng cho "Cắt đầy" (nổi bật), "2,4x" ([mono]), "72 / 310" ([mono]), "250%", "Đang tải ảnh gốc" (kèm [icon]).
 *
 * @param live đọc ngay khi hiện (nhãn đổi khung hình/chế độ phát, VD-06, VD-20).
 */
@Composable
fun ODVViewerPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ODVIcon? = null,
    mono: Boolean = false,
    prominent: Boolean = false,
    live: Boolean = false,
) {
    val type = ODVTheme.typography
    val style = when {
        prominent -> type.bodySm.copy(fontWeight = FontWeight.SemiBold)
        mono -> type.timecode
        else -> type.meta
    }
    Row(
        modifier = modifier
            .background(ODVMediaColors.pill, ODVTheme.shapes.full)
            .border(1.dp, ODVMediaColors.onMedia.copy(alpha = 0.13f), ODVTheme.shapes.full)
            .padding(horizontal = if (prominent) 18.dp else 14.dp, vertical = if (prominent) 8.dp else 6.dp)
            .then(if (live) Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) ODVIcon(icon, contentDescription = null, tint = ODVMediaColors.onMedia, size = 14.dp)
        Text(text, style = style, color = ODVMediaColors.onMedia, maxLines = 1)
    }
}

/** Nút "Đặt lại zoom" (VD-18, mục 4.5): cao 44, padding 20, viền 1dp `media.buffer`, nền `media.pill`, chữ 14 đậm 600. */
@Composable
fun ODVViewerResetZoomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = ODVTheme.shapes.full
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressScale = rememberPressScale(interaction)
    Box(
        modifier = modifier
            .height(44.dp)
            .pressScale(pressScale)
            .focusRing(focused, shape, ODVMediaColors.accent, ODVMediaColors.background)
            .background(ODVMediaColors.pill, shape)
            .border(1.dp, ODVMediaColors.buffer, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ODVTheme.typography.bodySm.copy(fontWeight = FontWeight.SemiBold), color = ODVMediaColors.onMedia, maxLines = 1)
    }
}

/**
 * HUD vuốt (mục 4.5, VD-04): khung 56 × 200 bo 28 nền `media.pill`, phần trăm `timecode-sm` ở trên, rãnh dọc 6 (`media.track`) với
 * mức hiện tại `media.accent`, icon `sun` (nửa trái) hoặc `volume` (nửa phải) ở dưới. Luôn có số phần trăm bằng chữ.
 *
 * @param level mức 0..1; [contentDescription] ví dụ "Độ sáng 40%".
 */
@Composable
fun ODVViewerHud(
    level: Float,
    icon: ODVIcon,
    percentText: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val fraction = if (level.isNaN()) 0f else level.coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .size(ODVSize.hudWidth, ODVSize.hudHeight)
            .background(ODVMediaColors.pill, RoundedCornerShape(28.dp))
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(percentText, style = ODVTheme.typography.timecodeSm, color = ODVMediaColors.onMedia, maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .width(6.dp)
                .background(ODVMediaColors.track, RoundedCornerShape(3.dp)),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(ODVMediaColors.accent, RoundedCornerShape(3.dp)),
            )
        }
        ODVIcon(icon, contentDescription = null, tint = ODVMediaColors.onMedia)
    }
}

/**
 * Gợn chạm đúp (mục 4.5, VD-03): nửa elip nền `media.ripple` phủ nửa trái hoặc nửa phải khung video, icon `rewind`/`forward` 32
 * kèm nhãn 14 đậm 600 ("-10 giây", "+20 giây"); chạm liên tiếp thì đổi [label] (cộng dồn) chứ không tạo gợn mới.
 * Nơi gọi đặt component vào đúng nửa khung (ví dụ `Modifier.fillMaxHeight().fillMaxWidth(0.5f)`).
 * [onRightSide] là phía vật lý của màn hình; hình dạng theo chiều bố cục nên MVP1 (chưa hỗ trợ RTL) không bị ảnh hưởng.
 */
@Composable
fun ODVViewerDoubleTapRipple(
    label: String,
    onRightSide: Boolean,
    modifier: Modifier = Modifier,
) {
    val big = 999.dp
    val shape = if (onRightSide) RoundedCornerShape(topStart = big, bottomStart = big) else RoundedCornerShape(topEnd = big, bottomEnd = big)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ODVMediaColors.ripple, shape)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ODVIcon(if (onRightSide) ODVIcon.Forward else ODVIcon.Rewind, contentDescription = null, tint = ODVMediaColors.onMedia, size = 32.dp)
            Text(label, style = ODVTheme.typography.bodySm.copy(fontWeight = FontWeight.SemiBold), color = ODVMediaColors.onMedia)
        }
    }
}

/**
 * Nút mở khóa thao tác (LockHold, mục 4.5, VD-08): nút tròn 52 nền `media.ripple` có icon `unlock` 26, nằm trong vòng 72
 * (rãnh `media.track`, cung `media.accent` chạy theo thời gian giữ). Giữ đủ `duration.lock-hold` (1 giây) thì gọi [onUnlocked];
 * thả sớm thì vòng về 0. Có thêm hành động trợ năng: chạm đúp trong TalkBack mở khóa ngay.
 *
 * @param hint dòng chữ bên dưới, ví dụ "Giữ để mở khóa"; [subHint] dòng phụ 13, ví dụ "Mọi thao tác chạm khác đang bị chặn".
 */
@Composable
fun ODVViewerLockHoldButton(
    contentDescription: String,
    hint: String,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    subHint: String? = null,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val unlocked by rememberUpdatedState(onUnlocked)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(ODVSize.lockHoldRing)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                    onClick(label = contentDescription) { unlocked(); true }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val hold: Job = scope.launch {
                            progress.animateTo(1f, tween(ODVDuration.lockHold, easing = LinearEasing))
                            unlocked()
                        }
                        waitForUpOrCancellation()
                        hold.cancel()
                        scope.launch { progress.snapTo(0f) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(ODVMediaColors.track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(ODVMediaColors.accent, -90f, 360f * progress.value, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Box(
                Modifier.size(ODVSize.lockHoldButton).background(ODVMediaColors.ripple, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ODVIcon(ODVIcon.Unlock, contentDescription = null, tint = ODVMediaColors.onMedia, size = 26.dp)
            }
        }
        Text(hint, style = ODVTheme.typography.bodySm.copy(fontWeight = FontWeight.SemiBold), color = ODVMediaColors.onMedia, textAlign = TextAlign.Center)
        if (subHint != null) Text(subHint, style = ODVTheme.typography.caption, color = ODVMediaColors.onMediaMuted, textAlign = TextAlign.Center)
    }
}

/** Đang tải trên nền đen (BufferSpinner): spinner 36 `media.accent` và chữ 13 trắng bên dưới, cách 10. */
@Composable
fun ODVViewerBufferSpinner(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ODVSpinner(size = 36.dp, color = ODVMediaColors.accent, trackColor = ODVMediaColors.track)
        Text(label, style = ODVTheme.typography.caption, color = ODVMediaColors.onMedia)
    }
}

/**
 * Tiến độ tải PDF (DownloadProgress, PD-01): thanh 6 `media` rồi dòng "45%" và "12,4 / 27,6 MB" mono 13.
 * [progress] trong 0..1. Nút Hủy do nơi gọi đặt bên dưới.
 */
@Composable
fun ODVViewerDownloadProgress(
    progress: Float,
    percentText: String,
    sizeText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ODVProgressBar(progress, height = 6.dp, colors = ODVMediaDefaults.progressColors())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(percentText, style = ODVTheme.typography.timecode, color = ODVMediaColors.onMediaMuted)
            Text(sizeText, style = ODVTheme.typography.timecode, color = ODVMediaColors.onMediaMuted)
        }
    }
}
