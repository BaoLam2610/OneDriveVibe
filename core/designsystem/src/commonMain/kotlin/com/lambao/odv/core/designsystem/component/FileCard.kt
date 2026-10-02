package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOverlayColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Thẻ thư mục dạng lưới (FolderCard, mục 4.4): ô cao 104 bo `md` nền `volt-soft`, icon `folder` 36 `volt-text`;
 * tên `body-sm-strong` một dòng, meta `meta` `ink-muted` cách 6.
 */
@Composable
fun ODVFolderCard(
    name: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ODVThumbnailPlaceholder(
            kind = ODVFileKind.Folder,
            modifier = Modifier.fillMaxWidth().height(ODVSize.folderCardThumb),
            shape = ODVTheme.shapes.md,
            iconSize = 36.dp,
        )
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(name, style = type.bodySmStrong, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = type.meta, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Thẻ tệp dạng lưới (FileCard, mục 4.4): thumbnail cao 120 bo `sm`, tên `body-sm-strong`, meta `meta`.
 *
 * Các lớp phủ nằm trên thumbnail:
 * - [kindIcon]: icon loại tệp trắng 18 ở góc trên trái (video).
 * - [duration]: huy hiệu thời lượng ở góc dưới phải.
 * - [progress]: thanh xem/đọc dở cao 4 sát đáy; [progressColors] mặc định cho ảnh/video, PDF dùng [ODVThumbnailDefaults.pdfProgressColors].
 * - [unavailableDescription]: có giá trị thì hiện nút tròn 24 `cloud-off` ở góc trên phải (tệp chưa có trong bộ nhớ đệm khi offline).
 *
 * @param thumbnail ảnh thật hoặc [ODVThumbnailPlaceholder]; nơi gọi tự load ảnh.
 */
@Composable
fun ODVFileCard(
    title: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kindIcon: ODVIcon? = null,
    duration: String? = null,
    progress: Float? = null,
    progressColors: ODVProgressColors = ODVThumbnailDefaults.imageProgressColors(),
    unavailableDescription: String? = null,
    thumbnail: @Composable BoxScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ODVSize.fileCardThumb)
                .clip(ODVTheme.shapes.sm),
        ) {
            thumbnail()
            if (kindIcon != null) {
                ODVIcon(kindIcon, contentDescription = null, tint = ODVOverlayColors.onScrim, size = 18.dp, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
            }
            if (unavailableDescription != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(ODVOverlayColors.scrim, CircleShape)
                        .semantics { contentDescription = unavailableDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    ODVIcon(ODVIcon.CloudOff, contentDescription = null, tint = ODVOverlayColors.onScrim, size = 14.dp)
                }
            }
            if (duration != null) {
                ODVDurationBadge(duration, Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 10.dp))
            }
            if (progress != null) {
                ThumbnailProgress(progress, progressColors, Modifier.align(Alignment.BottomCenter))
            }
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(title, style = type.bodySmStrong, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = type.meta, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
