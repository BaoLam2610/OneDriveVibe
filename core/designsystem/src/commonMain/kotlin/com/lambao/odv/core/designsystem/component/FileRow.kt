package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Tô sáng phần khớp [query] trong [text] (không phân biệt hoa thường), dùng cho kết quả tìm kiếm (mục 4.4):
 * nền [highlight] (thường `volt-soft`). Vẫn là chữ thường kèm nền, không chỉ dựa vào màu.
 */
fun odvHighlightedText(text: String, query: String, highlight: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val start = text.indexOf(query.trim(), ignoreCase = true)
    if (start < 0) return AnnotatedString(text)
    val end = start + query.trim().length
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(background = highlight)) { append(text.substring(start, end)) }
        append(text.substring(end))
    }
}

/**
 * Hàng danh sách tệp/thư mục (FileRow, mục 4.4): cao tối thiểu 72, padding 8/16, kẻ đáy 1dp `line`, thumbnail 56.
 * Tên `body-strong` một dòng cắt "…", meta `caption` `ink-muted`; thời lượng/số liệu trong meta dùng mono ([metaMono]).
 *
 * @param leading thumbnail 56 × 56 (ảnh thật hoặc [ODVThumbnailPlaceholder]); hàng tự đặt kích thước.
 * @param progress thanh xem dở rộng 160, cao 4 dưới meta.
 * @param path dòng đường dẫn `meta` `ink-faint` (kết quả tìm kiếm).
 * @param titleHighlight tô sáng phần khớp trong tên (kết quả tìm kiếm).
 * @param showChevron mũi tên `chevron-right` 20 ở cuối (thư mục).
 * @param dimmed mờ 0.5 (tệp không hỗ trợ khi bật hiển thị, TM-03).
 * @param onClick null thì hàng không bấm được.
 */
@Composable
fun ODVFileRow(
    title: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    metaMono: String? = null,
    progress: Float? = null,
    progressColors: ODVProgressColors = ODVProgressBarDefaults.colors(),
    path: String? = null,
    titleHighlight: String? = null,
    showChevron: Boolean = false,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val titleText = remember(title, titleHighlight, colors) {
        odvHighlightedText(title, titleHighlight.orEmpty(), colors.voltSoft)
    }
    val metaText = remember(meta, metaMono, type) {
        buildAnnotatedString {
            if (meta != null) append(meta)
            if (metaMono != null) {
                if (meta != null) append(" · ")
                withStyle(SpanStyle(fontFamily = type.code.fontFamily)) { append(metaMono) }
            }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dimmed) ODVOpacity.unsupported else 1f)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .drawBehind {
                drawLine(colors.line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            .heightIn(min = ODVSize.fileRowMin)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(ODVSize.thumbRow).clip(ODVTheme.shapes.sm)) { leading() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titleText, style = type.bodyStrong, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (metaText.isNotEmpty()) Text(metaText, style = type.caption, color = colors.inkMuted)
            if (progress != null) ODVProgressBar(progress, Modifier.width(160.dp).padding(top = 2.dp), colors = progressColors)
            if (path != null) Text(path, style = type.meta, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showChevron) ODVIcon(ODVIcon.ChevronRight, contentDescription = null, tint = colors.inkMuted, size = 20.dp)
    }
}
