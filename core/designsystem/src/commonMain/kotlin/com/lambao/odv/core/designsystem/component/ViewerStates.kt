package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVOverlayColors
import com.lambao.odv.core.designsystem.theme.ODVRadius
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Nút trên màn xem: [filled] = nền `media.accent` chữ `media.on-accent` (hành động chính như "Tiếp tục"); mặc định là nút viền
 * 1dp `media.buffer` chữ trắng. Cao 48, padding 24, tròn. Khi [enabled] = false mờ 0.38 và không nhận chạm.
 */
@Composable
fun ODVViewerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = ODVTheme.shapes.full
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressScale = rememberPressScale(interaction)
    Box(
        modifier = modifier
            .height(ODVSize.button)
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .pressScale(pressScale)
            .focusRing(focused, shape, ODVMediaColors.accent, ODVMediaColors.background)
            .then(
                if (filled) Modifier.background(ODVMediaColors.accent, shape) else Modifier.border(1.dp, ODVMediaColors.buffer, shape),
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = ODVTheme.typography.button,
            color = if (filled) ODVMediaColors.onAccent else ODVMediaColors.onMedia,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Lỗi trên nền đen (MediaError, mục 4.5; VD-15, AN-07, PD-07): khối căn giữa, lề 32; vòng 72 nền `media.icon-well` icon 32 trắng;
 * tiêu đề `state-title`, nội dung `body` `media.on-media-muted`, dòng tên tệp `timecode-sm` `media.on-media-faint`.
 * Lỗi luôn có icon và chữ, không chỉ dựa vào màu.
 *
 * @param action nút phụ (viền), thường là [ODVViewerButton] "Quay lại"; ảnh không có nút (vuốt để sang ảnh khác).
 */
@Composable
fun ODVViewerError(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    fileLine: String? = null,
    icon: ODVIcon = ODVIcon.Alert,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(ODVSize.emptyIconWell).background(ODVMediaColors.iconWell, CircleShape), contentAlignment = Alignment.Center) {
            ODVIcon(icon, contentDescription = null, tint = ODVMediaColors.onMedia, size = 32.dp)
        }
        Text(
            title,
            modifier = Modifier.padding(top = 4.dp).semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            style = ODVTheme.typography.stateTitle,
            color = ODVMediaColors.onMedia,
            textAlign = TextAlign.Center,
        )
        if (body != null) Text(body, style = ODVTheme.typography.body, color = ODVMediaColors.onMediaMuted, textAlign = TextAlign.Center)
        if (fileLine != null) Text(fileLine, style = ODVTheme.typography.timecodeSm, color = ODVMediaColors.onMediaFaint, textAlign = TextAlign.Center)
        if (action != null) Box(Modifier.padding(top = 12.dp)) { action() }
    }
}

/**
 * Thẻ mất mạng (NetworkNotice, VD-16): lề 16, nền `media.card`, bo `md`, padding 16; tiêu đề kèm icon `cloud-off`, một đoạn
 * hướng dẫn, nút "Tiếp tục" volt mờ 0.38 tới khi có mạng ([actionEnabled]) kèm [hint] "Nút bật lại khi có mạng". Đọc ngay khi hiện.
 */
@Composable
fun ODVViewerNetworkNotice(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = false,
    hint: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ODVMediaColors.card, ODVTheme.shapes.md)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ODVIcon(ODVIcon.CloudOff, contentDescription = null, tint = ODVMediaColors.onMedia, size = 20.dp)
            Text(
                title,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                style = ODVTheme.typography.bodyStrong,
                color = ODVMediaColors.onMedia,
            )
        }
        Text(body, style = ODVTheme.typography.bodySm, color = ODVMediaColors.onMediaMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ODVViewerButton(actionLabel, onAction, filled = true, enabled = actionEnabled)
            if (hint != null) Text(hint, style = ODVTheme.typography.caption, color = ODVMediaColors.onMediaFaint, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Thẻ "Tiếp theo" của tự phát tiếp (VD-13, mục 4.5): nền `media.card`, bo `md`, padding 16; vòng đếm 56 (rãnh `media.track`,
 * cung `media.accent` đầy dần tuyến tính) có số giây `timecode` 18 ở giữa; [heading] ("Tiếp theo sau 5 giây") và tên video
 * sau; nút "Hủy" viền. Vòng đếm là thông tin nên vẫn chạy khi bật Giảm hiệu ứng.
 *
 * @param progress phần vòng đã đầy, 0..1; [secondsText] số giây còn lại.
 */
@Composable
fun ODVViewerNextUpCard(
    heading: String,
    title: String,
    secondsText: String,
    progress: Float,
    cancelLabel: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ODVMediaColors.card, ODVTheme.shapes.md)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(ODVSize.countdownRing), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(ODVMediaColors.track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(ODVMediaColors.accent, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(secondsText, style = ODVTheme.typography.timecode.copy(fontSize = 18.sp, lineHeight = 24.sp), color = ODVMediaColors.onMedia)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(heading, style = ODVTheme.typography.caption, color = ODVMediaColors.onMediaMuted, maxLines = 1)
            Text(title, style = ODVTheme.typography.bodySmStrong, color = ODVMediaColors.onMedia, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ODVViewerButton(cancelLabel, onCancel)
    }
}

/**
 * Bảng bên phải ở hướng ngang (mục 4.5): rộng 360, cao toàn màn, bo `lg` hai góc trái, có nút đóng; thay cho Bottom sheet
 * (thông tin tệp, bảng tốc độ). Là lớp phủ trong cùng cây composition: đặt ở gốc màn, phủ cả màn, scrim `media.scrim` phía sau;
 * chạm scrim để đóng. Màu theo [ODVTheme] hiện tại (dialog/sheet trong màn xem theo theme). Nơi gọi tự xử lý nút Back.
 */
@Composable
fun ODVSidePanel(
    title: String,
    closeContentDescription: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = ODVTheme.colors
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(ODVOverlayColors.scrim)
                // Chặn mọi chạm phía sau; chạm vào scrim thì đóng. TalkBack có hành động đóng tương đương.
                .pointerInput(Unit) { detectTapGestures { onDismissRequest() } }
                .semantics {
                    contentDescription = closeContentDescription
                    onClick(label = closeContentDescription) { onDismissRequest(); true }
                },
        )
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(ODVSize.sidePanelWidth)
                .background(colors.surface, RoundedCornerShape(topStart = ODVRadius.lg, bottomStart = ODVRadius.lg))
                // Nuốt chạm vào vùng trống của panel, nếu không sự kiện lọt xuống scrim và đóng panel.
                .pointerInput(Unit) { detectTapGestures { } }
                .semantics { paneTitle = title }
                .padding(start = 16.dp, top = 8.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = ODVTheme.typography.heading,
                    color = colors.ink,
                )
                ODVIconButton(ODVIcon.Close, closeContentDescription, onDismissRequest)
            }
            Box(Modifier.fillMaxWidth().padding(end = 16.dp)) { content() }
        }
    }
}
