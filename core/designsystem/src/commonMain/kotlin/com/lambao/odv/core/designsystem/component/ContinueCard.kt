package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVElevation
import com.lambao.odv.core.designsystem.theme.ODVOverlayColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Tiêu đề nhóm (SectionHeader, mục 4.4): kiểu `heading`, ví dụ "Xem tiếp", "Đọc tiếp". [trailing] là chữ phụ bên phải (số mục).
 */
@Composable
fun ODVSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val colors = ODVTheme.colors
    Row(modifier.fillMaxWidth()) {
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = ODVTheme.typography.heading,
            color = colors.ink,
        )
        if (trailing != null) Text(trailing, style = ODVTheme.typography.body, color = colors.inkMuted)
    }
}

/**
 * ContinueCard của dải Xem tiếp / Đọc tiếp (mục 4.4): rộng 232, nền `surface`, bo `md`, bóng `shadow.sm`;
 * thumbnail cao 130 kèm thanh tiến độ 4 sát đáy; chữ padding 10/12 (tên `body-sm-strong`, phụ `meta`).
 * Nút X "Xóa khỏi dải": vùng chạm 40, hình tròn 28 nền mờ, icon 16 trắng, nằm ngoài vùng bấm của thẻ nên TalkBack thấy hai hành động riêng.
 *
 * @param dismissContentDescription ví dụ "Xóa khỏi Xem tiếp".
 */
@Composable
fun ODVContinueCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    dismissContentDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    progressColors: ODVProgressColors = ODVThumbnailDefaults.imageProgressColors(),
    duration: String? = null,
    thumbnail: @Composable BoxScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val shape = ODVTheme.shapes.md
    Box(
        modifier = modifier
            .width(ODVSize.continueCardWidth)
            .shadow(ODVElevation.sm, shape)
            .background(colors.surface, shape)
            .clip(shape),
    ) {
        Column(Modifier.clickable(role = Role.Button, onClick = onClick)) {
            Box(Modifier.fillMaxWidth().height(ODVSize.continueCardThumb)) {
                thumbnail()
                if (duration != null) {
                    ODVDurationBadge(duration, Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 12.dp))
                }
                if (progress != null) {
                    ThumbnailProgress(progress, progressColors, Modifier.align(Alignment.BottomCenter))
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(title, style = type.bodySmStrong, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = type.meta, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // Vùng chạm 40, hình tròn nhìn thấy 28 (mục 4.4).
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(40.dp)
                // Cắt tròn để ripple/vùng chạm theo hình nút nhìn thấy, không vuông.
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics { this.contentDescription = dismissContentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(28.dp).background(ODVOverlayColors.scrim, CircleShape), contentAlignment = Alignment.Center) {
                ODVIcon(ODVIcon.Close, contentDescription = null, tint = ODVOverlayColors.onScrim, size = 16.dp)
            }
        }
    }
}
