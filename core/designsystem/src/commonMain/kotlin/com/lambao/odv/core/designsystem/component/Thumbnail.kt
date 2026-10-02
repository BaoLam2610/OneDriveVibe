package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOverlayColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Loại tệp để chọn màu và icon nhận diện (mục 2.1 `kind-*`, mục 3.1). */
enum class ODVFileKind { Folder, Video, Photo, Pdf, Unsupported }

/**
 * Ô thumbnail giữ chỗ theo loại tệp: nền `*-soft`, icon cùng tông. Dùng khi chưa có ảnh thật (PDF, thư mục, tệp không hỗ trợ).
 * Với ảnh/video thật, nơi gọi truyền thumbnail riêng vào slot của [ODVFileRow], [ODVFileCard]...
 */
@Composable
fun ODVThumbnailPlaceholder(
    kind: ODVFileKind,
    modifier: Modifier = Modifier,
    shape: Shape = ODVTheme.shapes.sm,
    iconSize: Dp = 24.dp,
) {
    val colors = ODVTheme.colors
    val container = when (kind) {
        ODVFileKind.Folder -> colors.voltSoft
        ODVFileKind.Video -> colors.kindVideoSoft
        ODVFileKind.Photo -> colors.kindPhotoSoft
        ODVFileKind.Pdf -> colors.kindPdfSoft
        ODVFileKind.Unsupported -> colors.surface2
    }
    val tint = when (kind) {
        ODVFileKind.Folder -> colors.voltText
        ODVFileKind.Video -> colors.kindVideo
        ODVFileKind.Photo -> colors.kindPhoto
        ODVFileKind.Pdf -> colors.kindPdf
        ODVFileKind.Unsupported -> colors.inkMuted
    }
    val icon = when (kind) {
        ODVFileKind.Folder -> ODVIcon.Folder
        ODVFileKind.Video -> ODVIcon.Video
        ODVFileKind.Photo -> ODVIcon.Image
        ODVFileKind.Pdf -> ODVIcon.Book
        ODVFileKind.Unsupported -> ODVIcon.Alert
    }
    Box(modifier.background(container, shape), contentAlignment = Alignment.Center) {
        ODVIcon(icon, contentDescription = null, tint = tint, size = iconSize)
    }
}

/** Bộ màu thanh xem dở và các lớp phủ nằm chồng lên thumbnail. */
object ODVThumbnailDefaults {
    /** Thanh xem dở chồng lên ảnh/video: rãnh trắng 40%, phần đã xem `volt`. */
    fun imageProgressColors() = ODVProgressColors(ODVOverlayColors.progressTrack, ODVOverlayColors.progressIndicator)

    /** Thanh đọc dở của PDF (thumbnail nền sáng): rãnh `line`, phần đã đọc `kind-pdf`. */
    @Composable
    fun pdfProgressColors() = ODVProgressColors(ODVTheme.colors.line, ODVTheme.colors.kindPdf)
}

/**
 * Huy hiệu thời lượng chồng lên thumbnail: nền mờ đen, chữ trắng mono. Màu cố định, không đổi theo theme vì nằm trên ảnh.
 * [compact] dùng trong ô lưới dày (chữ 11, padding 0/4, bo 4); mặc định chữ 12, padding 2/6, bo 6.
 */
@Composable
fun ODVDurationBadge(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier
            .background(ODVOverlayColors.scrim, RoundedCornerShape(if (compact) 4.dp else 6.dp))
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 0.dp else 2.dp),
        style = if (compact) ODVTheme.typography.timecodeXs else ODVTheme.typography.timecodeSm,
        color = ODVOverlayColors.onScrim,
        maxLines = 1,
    )
}

/** Thanh tiến độ phẳng sát đáy thumbnail, cao `media-progress` 4dp. [progress] trong 0..1. */
@Composable
internal fun ThumbnailProgress(
    progress: Float,
    colors: ODVProgressColors,
    modifier: Modifier = Modifier,
) {
    val fraction = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(ODVSize.mediaProgress).background(colors.track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(colors.indicator))
    }
}
