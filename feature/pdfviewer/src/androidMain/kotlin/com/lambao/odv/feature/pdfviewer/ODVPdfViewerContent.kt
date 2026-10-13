package com.lambao.odv.feature.pdfviewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerSystemBars
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.model.PdfFile
import kotlinx.coroutines.flow.Flow

/**
 * Nội dung màn xem PDF (P1 → P8): nền đen; đang tải thì hiện tiến trình (PD-01), tải đủ thì mở bằng `PdfRenderer` và hiện vùng đọc kèm thanh
 * công cụ (PD-02, PD-04, PD-06). Chạm một lần ẩn/hiện thanh công cụ và system bar; lúc đang đọc màn luôn sáng (PD-06).
 */
@Composable
internal fun ODVPdfViewerContent(
    state: PdfViewerState,
    scrollRequests: Flow<Int>,
    onIntent: (PdfViewerIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reading = state.download is CachedFileState.Ready && state.pageCount > 0 && !state.openFailed
    ODVViewerSystemBars(hidden = reading && !state.controlsVisible)
    PdfKeepScreenOn(enabled = reading)
    BackHandler(enabled = state.info != null) { onIntent(PdfViewerIntent.HideInfo) }

    Box(modifier.fillMaxSize().background(ODVMediaColors.background)) {
        val file = state.file
        when {
            !state.isFileLoaded -> LoadingSpinner()
            // Tệp không còn (đã xóa trên OneDrive): ViewModel đã gửi Close.
            file == null -> Unit
            else -> PdfBody(file, state, scrollRequests, onIntent, onBack)
        }
        if (reading) PdfDialogs(state, onIntent)
    }
}

@Composable
private fun BoxScope.PdfBody(
    file: PdfFile,
    state: PdfViewerState,
    scrollRequests: Flow<Int>,
    onIntent: (PdfViewerIntent) -> Unit,
    onBack: () -> Unit,
) {
    when (val download = state.download) {
        is CachedFileState.Downloading -> PdfDownloading(file.item.name, download.receivedBytes, download.totalBytes, onBack)
        is CachedFileState.Failed -> PdfDownloadFailed(file.item.name, download.error, { onIntent(PdfViewerIntent.Retry) }, onBack)
        is CachedFileState.Ready -> PdfOpened(download.path, file, state, scrollRequests, onIntent, onBack)
    }
}

@Composable
private fun BoxScope.PdfOpened(
    path: String,
    file: PdfFile,
    state: PdfViewerState,
    scrollRequests: Flow<Int>,
    onIntent: (PdfViewerIntent) -> Unit,
    onBack: () -> Unit,
) {
    when (val open = rememberPdfDocument(path)) {
        PdfOpenState.Loading -> LoadingSpinner()
        is PdfOpenState.Failed -> {
            val corrupted = open.reason == PdfOpenFailure.Corrupt
            LaunchedEffect(Unit) { onIntent(PdfViewerIntent.OpenFailed(corrupted)) }
            // Hỏng thì cho Thử lại (ViewModel đã xóa bản cache hỏng); có mật khẩu thì chỉ Quay lại (PD-07).
            PdfOpenError(file.item.name, onBack, onRetry = if (corrupted) ({ onIntent(PdfViewerIntent.Retry) }) else null)
        }
        is PdfOpenState.Opened -> {
            val document = open.document
            LaunchedEffect(document) { onIntent(PdfViewerIntent.Opened(document.pageCount)) }
            // Chưa có số trang thì chưa dựng vùng đọc: ViewModel cần kẹp trang đã lưu vào 0 until pageCount trước (PD-05).
            if (state.pageCount > 0) {
                // Đổi kiểu đọc thì dựng lại vùng đọc từ trang hiện tại; xoay màn hình thì danh sách/Pager tự khôi phục vị trí.
                key(state.readingStyle) {
                    PdfReader(
                        document = document,
                        style = state.readingStyle,
                        initialPage = state.currentPage,
                        scrollRequests = scrollRequests,
                        onPageChanged = { onIntent(PdfViewerIntent.PageChanged(it)) },
                        onTap = { onIntent(PdfViewerIntent.ToggleControls) },
                    )
                }
                PdfControls(state, file, onIntent, onBack)
            } else {
                LoadingSpinner()
            }
        }
    }
}

@Composable
private fun BoxScope.LoadingSpinner() {
    Box(Modifier.align(Alignment.Center)) {
        ODVSpinner(size = 36.dp, color = ODVMediaColors.accent, trackColor = ODVMediaColors.track)
    }
}
