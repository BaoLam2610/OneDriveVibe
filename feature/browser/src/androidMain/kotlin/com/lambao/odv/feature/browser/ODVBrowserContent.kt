package com.lambao.odv.feature.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVAppBarLogo
import com.lambao.odv.core.designsystem.component.ODVBreadcrumb
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVFileKind
import com.lambao.odv.core.designsystem.component.ODVFileRow
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVThumbnailPlaceholder
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind

/**
 * Giao diện tab Thư mục ở dạng danh sách (thiet-ke-ui.md mục 5.2, D2): AppBar, Breadcrumb, rồi hàng tệp.
 * Chưa có Tabs Thư mục/Thư viện (Lát 4), thanh sắp xếp và dạng lưới (Lát 3), dải Xem tiếp (Lát 8), nút Cài đặt (Lát 9).
 */
@Composable
internal fun ODVBrowserContent(
    state: BrowserState,
    onIntent: (BrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootLabel = stringResource(R.string.browser_root)
    val inRoot = state.path.isEmpty()

    ODVScaffold(
        modifier = modifier,
        topBar = {
            ODVAppBar(
                title = if (inRoot) stringResource(R.string.browser_title) else state.path.last().name,
                navigation = {
                    if (inRoot) {
                        ODVAppBarLogo()
                    } else {
                        ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.browser_up), { onIntent(BrowserIntent.GoUp) })
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(Modifier.fillMaxSize()) {
            ODVBreadcrumb(
                items = listOf(rootLabel) + state.path.map { it.name },
                onItemClick = { onIntent(BrowserIntent.GoToCrumb(it)) },
                ellipsisContentDescription = stringResource(R.string.browser_breadcrumb_hidden),
            )
            Box(Modifier.weight(1f).fillMaxSize()) {
                when {
                    state.isLoading -> Loading()
                    state.error != null -> ErrorState(state.error, onRetry = { onIntent(BrowserIntent.Retry) })
                    state.items.isEmpty() -> ODVEmptyState(
                        icon = ODVIcon.Folder,
                        title = stringResource(R.string.browser_empty_title),
                        modifier = Modifier.align(Alignment.Center),
                        body = stringResource(R.string.browser_empty_body),
                    )
                    else -> ItemList(state.items, contentPadding, onIntent)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.Loading() {
    val description = stringResource(R.string.browser_loading)
    Box(Modifier.align(Alignment.Center).semantics { contentDescription = description }) {
        ODVSpinner(size = 36.dp)
    }
}

@Composable
private fun BoxScope.ErrorState(error: BrowserError, onRetry: () -> Unit) {
    val body = when {
        error.kind == BrowserErrorKind.Network -> stringResource(R.string.browser_error_network)
        error.code != null -> stringResource(R.string.browser_error_other, error.code)
        else -> stringResource(R.string.browser_error_other_no_code)
    }
    ODVEmptyState(
        icon = if (error.kind == BrowserErrorKind.Network) ODVIcon.CloudOff else ODVIcon.Alert,
        title = stringResource(R.string.browser_error_title),
        modifier = Modifier.align(Alignment.Center),
        body = body,
        action = {
            ODVButton(stringResource(R.string.browser_retry), onRetry, style = ODVButtonStyle.Secondary)
        },
    )
}

@Composable
private fun ItemList(items: List<DriveItem>, contentPadding: PaddingValues, onIntent: (BrowserIntent) -> Unit) {
    val resources = LocalContext.current.resources
    LazyColumn(contentPadding = contentPadding) {
        items(items, key = { it.id }) { item ->
            ODVFileRow(
                title = item.name,
                leading = { ODVThumbnailPlaceholder(item.fileKind(), Modifier.fillMaxSize()) },
                meta = if (item.isFolder) {
                    item.childCount?.let { resources.getQuantityString(R.plurals.browser_item_count, it, it) }
                } else {
                    odvFormatFileSize(item.sizeBytes)
                },
                metaMono = item.durationMs?.let(::odvFormatDuration),
                showChevron = item.isFolder,
                onClick = { onIntent(BrowserIntent.Open(item)) },
            )
        }
    }
}

private fun DriveItem.fileKind(): ODVFileKind = when {
    isFolder -> ODVFileKind.Folder
    mediaKind == MediaKind.Video -> ODVFileKind.Video
    mediaKind == MediaKind.Image -> ODVFileKind.Photo
    mediaKind == MediaKind.Pdf -> ODVFileKind.Pdf
    else -> ODVFileKind.Unsupported
}
