package com.lambao.odv.feature.pdfviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.designsystem.component.ODVProgressBar
import com.lambao.odv.core.designsystem.component.ODVProgressColors
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Đang tải tệp (PD-01, P1): vòng 72 nền `kind-pdf-soft` có icon `book`, tên tệp, thanh tiến độ cao 6, "% · dung lượng" `timecode` và nút Hủy.
 * Chưa biết tổng dung lượng thì thanh đứng ở 0 và chỉ hiện dung lượng đã nhận.
 */
@Composable
internal fun PdfDownloading(
    name: String,
    receivedBytes: Long,
    totalBytes: Long?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val languageTag = odvLocale().toLanguageTag()
    val total = totalBytes?.takeIf { it > 0 }
    val fraction = if (total != null) (receivedBytes.toFloat() / total).coerceIn(0f, 1f) else 0f
    val progressText = if (total != null) {
        stringResource(
            R.string.pdf_download_progress,
            (fraction * 100).toInt(),
            odvFormatFileSize(receivedBytes, languageTag),
            odvFormatFileSize(total, languageTag),
        )
    } else {
        odvFormatFileSize(receivedBytes, languageTag)
    }
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(ODVSize.emptyIconWell).background(ODVTheme.colors.kindPdfSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ODVIcon(ODVIcon.Book, contentDescription = null, tint = ODVTheme.colors.kindPdf, size = 32.dp)
        }
        Text(
            name,
            style = ODVTheme.typography.bodyStrong,
            color = ODVMediaColors.onMedia,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ODVProgressBar(
            progress = fraction,
            modifier = Modifier.widthIn(max = 320.dp),
            height = 6.dp,
            colors = ODVProgressColors(track = ODVMediaColors.track, indicator = ODVMediaColors.accent),
        )
        Text(progressText, style = ODVTheme.typography.timecode, color = ODVMediaColors.onMediaMuted)
        ODVViewerButton(stringResource(R.string.pdf_cancel), onCancel)
    }
}

/** Không tải được tệp (PD-01): mất mạng thì nói rõ cần mạng; có nút Thử lại (tải tiếp phần đã có, BN-03) và Quay lại. */
@Composable
internal fun PdfDownloadFailed(
    name: String,
    error: AppError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offline = error is AppError.Network || error is AppError.Timeout
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ODVViewerError(
            title = stringResource(if (offline) R.string.pdf_download_offline_title else R.string.pdf_download_failed_title),
            fileLine = name,
            icon = if (offline) ODVIcon.CloudOff else ODVIcon.Alert,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ODVViewerButton(stringResource(R.string.pdf_back), onBack)
                    ODVViewerButton(stringResource(R.string.pdf_retry), onRetry, filled = true)
                }
            },
        )
    }
}

/**
 * Không mở được tệp (PD-07, P6): có mật khẩu hoặc hỏng. [onRetry] null (có mật khẩu) thì chỉ có Quay lại vì tải lại không đổi được gì;
 * tệp hỏng thì thêm "Thử lại" tải lại bản mới (M3).
 */
@Composable
internal fun PdfOpenError(
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ODVViewerError(
            title = stringResource(R.string.pdf_open_error_title),
            body = stringResource(R.string.pdf_open_error_body),
            fileLine = name,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ODVViewerButton(stringResource(R.string.pdf_back), onBack)
                    if (onRetry != null) ODVViewerButton(stringResource(R.string.pdf_retry), onRetry, filled = true)
                }
            },
        )
    }
}
