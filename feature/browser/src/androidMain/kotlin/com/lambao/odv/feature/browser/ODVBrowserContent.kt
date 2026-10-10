package com.lambao.odv.feature.browser

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVAppBarLogo
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVBreadcrumb
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVFileCard
import com.lambao.odv.core.designsystem.component.ODVFileKind
import com.lambao.odv.core.designsystem.component.ODVFileRow
import com.lambao.odv.core.designsystem.component.ODVFolderCard
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.component.ODVRemoteImage
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSearchBar
import com.lambao.odv.core.designsystem.component.ODVSortBar
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVThumbnailPlaceholder
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.model.thumbnailSource
import java.text.NumberFormat

// Thứ tự trong bottom sheet sắp xếp (TM-05): theo Tên, Ngày sửa, Dung lượng; mỗi trường một cặp tăng/giảm.
private val SortChoices = listOf(
    SortOrder(SortField.Name, SortDirection.Ascending),
    SortOrder(SortField.Name, SortDirection.Descending),
    SortOrder(SortField.Modified, SortDirection.Descending),
    SortOrder(SortField.Modified, SortDirection.Ascending),
    SortOrder(SortField.Size, SortDirection.Ascending),
    SortOrder(SortField.Size, SortDirection.Descending),
)

/**
 * Giao diện tab Thư mục (thiet-ke-ui.md mục 4.4, 5.2; D1, D2, D4 → D6, D8): AppBar (hoặc thanh tìm), Banner, Breadcrumb,
 * SortBar, rồi danh sách hoặc lưới, kéo xuống để làm mới. [tabs] là thanh Tabs Thư mục/Thư viện do màn chứa dựng, đặt ngay
 * dưới AppBar (chỉ hiện khi không tìm kiếm). Chưa có dải Xem tiếp (Lát 9) và dấu `cloud-off` trên
 * thẻ tệp chưa có trong cache (Lát 5 → 7, khi có cache tệp gốc).
 */
@Composable
internal fun ODVBrowserContent(
    state: BrowserState,
    onIntent: (BrowserIntent) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit = {},
) {
    val search = state.search
    ODVScaffold(
        modifier = modifier,
        topBar = {
            if (search != null) SearchTopBar(search, onIntent) else BrowseTopBar(state, onIntent, onOpenSettings)
        },
    ) { contentPadding ->
        if (search != null) {
            SearchBody(search, contentPadding, onIntent)
        } else {
            BrowseBody(state, contentPadding, onIntent, tabs)
        }
    }
    if (state.isSortSheetOpen) SortSheet(state.sort, onIntent)
}

@Composable
private fun BrowseTopBar(state: BrowserState, onIntent: (BrowserIntent) -> Unit, onOpenSettings: () -> Unit) {
    val inRoot = state.path.isEmpty()
    ODVAppBar(
        title = if (inRoot) stringResource(R.string.browser_title) else state.path.last().name,
        navigation = {
            if (inRoot) {
                ODVAppBarLogo()
            } else {
                ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.browser_up), { onIntent(BrowserIntent.GoUp) })
            }
        },
        actions = {
            ODVIconButton(ODVIcon.Search, stringResource(R.string.browser_search), { onIntent(BrowserIntent.OpenSearch) })
            ODVIconButton(ODVIcon.Settings, stringResource(R.string.browser_settings), onOpenSettings)
        },
    )
}

@Composable
private fun SearchTopBar(search: SearchState, onIntent: (BrowserIntent) -> Unit) {
    ODVSearchBar(
        query = search.query,
        onQueryChange = { onIntent(BrowserIntent.QueryChanged(it)) },
        placeholder = stringResource(R.string.browser_search_placeholder),
        backContentDescription = stringResource(R.string.browser_search_back),
        onBack = { onIntent(BrowserIntent.CloseSearch) },
        clearContentDescription = stringResource(R.string.browser_search_clear),
        autoFocus = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseBody(
    state: BrowserState,
    contentPadding: PaddingValues,
    onIntent: (BrowserIntent) -> Unit,
    tabs: @Composable () -> Unit,
) {
    val rootLabel = stringResource(R.string.browser_root)
    val sortLabel = state.sort.label()
    val isGrid = state.viewMode == ViewMode.Grid
    Column(Modifier.fillMaxSize()) {
        tabs()
        SyncBanner(state, onIntent)
        ODVBreadcrumb(
            items = listOf(rootLabel) + state.path.map { it.name },
            onItemClick = { onIntent(BrowserIntent.GoToCrumb(it)) },
            ellipsisContentDescription = stringResource(R.string.browser_breadcrumb_hidden),
        )
        ODVSortBar(
            sortLabel = sortLabel,
            sortContentDescription = stringResource(R.string.browser_sort_description, sortLabel),
            onSortClick = { onIntent(BrowserIntent.OpenSortSheet) },
            toggleIcon = if (isGrid) ODVIcon.List else ODVIcon.Grid,
            toggleContentDescription = stringResource(
                if (isGrid) R.string.browser_view_to_list else R.string.browser_view_to_grid,
            ),
            onToggleClick = { onIntent(BrowserIntent.ToggleViewMode) },
        )
        // DS-04: kéo xuống để đồng bộ. Chỉ quay khi đang đồng bộ lại (không quay lúc quét lần đầu, đã có banner riêng).
        val refreshing = state.sync.isSyncing && state.sync.initialSyncDone
        val pullState = rememberPullToRefreshState()
        val colors = ODVTheme.colors
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { onIntent(BrowserIntent.Refresh) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = colors.surface,
                    color = colors.voltText,
                )
            },
        ) {
            when {
                state.isLoading -> Loading()
                state.error != null -> ErrorState(state.error, onRetry = { onIntent(BrowserIntent.Retry) })
                state.items.isEmpty() -> ODVEmptyState(
                    icon = ODVIcon.Folder,
                    title = stringResource(R.string.browser_empty_title),
                    modifier = Modifier.align(Alignment.Center),
                    body = stringResource(R.string.browser_empty_body),
                )
                isGrid -> ItemGrid(state.items, contentPadding, onIntent)
                else -> ItemList(state.items, contentPadding, onIntent)
            }
        }
    }
}

/**
 * Banner theo thứ tự ưu tiên: offline (DS-05, D6), đang lập chỉ mục (TV-06, D7), đồng bộ lỗi. Mỗi lúc chỉ một banner.
 * Thiết kế D7 có thanh tiến độ nhưng tổng số mục không biết trước nên chỉ hiện số mục đã quét.
 */
@Composable
private fun SyncBanner(state: BrowserState, onIntent: (BrowserIntent) -> Unit) {
    val sync = state.sync
    val spacing = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    when {
        state.isOffline -> ODVBanner(
            text = stringResource(R.string.browser_offline),
            tone = ODVBannerTone.Neutral,
            icon = ODVIcon.CloudOff,
            modifier = spacing,
        )
        !sync.initialSyncDone && sync.isSyncing -> {
            val locale = odvLocale()
            val scanned = remember(sync.scannedCount, locale) { NumberFormat.getIntegerInstance(locale).format(sync.scannedCount) }
            ODVBanner(
                text = stringResource(R.string.browser_indexing, scanned),
                tone = ODVBannerTone.Volt,
                icon = ODVIcon.Sync,
                modifier = spacing,
            )
        }
        sync.failed -> ODVBanner(
            text = stringResource(R.string.browser_sync_failed),
            tone = ODVBannerTone.Warning,
            icon = ODVIcon.Alert,
            modifier = spacing,
            actionLabel = stringResource(R.string.browser_retry),
            onAction = { onIntent(BrowserIntent.Refresh) },
        )
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
    val resources = LocalResources.current
    val languageTag = odvLocale().toLanguageTag()
    LazyColumn(contentPadding = contentPadding) {
        items(items, key = { it.id }) { item ->
            ODVFileRow(
                title = item.name,
                leading = { ItemThumbnail(item, ThumbnailSize.Cell) },
                meta = item.meta(resources, languageTag),
                metaMono = item.durationMs?.let(::odvFormatDuration),
                showChevron = item.isFolder,
                onClick = { onIntent(BrowserIntent.Open(item)) },
            )
        }
    }
}

/** Lưới 2 cột, khe 12 ngang và 16 dọc (mục 4.4 FileCard). */
@Composable
private fun ItemGrid(items: List<DriveItem>, contentPadding: PaddingValues, onIntent: (BrowserIntent) -> Unit) {
    val resources = LocalResources.current
    val languageTag = odvLocale().toLanguageTag()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 16.dp + contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.id }) { item ->
            if (item.isFolder) {
                ODVFolderCard(
                    name = item.name,
                    meta = item.meta(resources, languageTag).orEmpty(),
                    onClick = { onIntent(BrowserIntent.Open(item)) },
                )
            } else {
                ODVFileCard(
                    title = item.name,
                    meta = item.meta(resources, languageTag).orEmpty(),
                    onClick = { onIntent(BrowserIntent.Open(item)) },
                    kindIcon = if (item.mediaKind == MediaKind.Video) ODVIcon.Video else null,
                    duration = item.durationMs?.let(::odvFormatDuration),
                    thumbnail = { ItemThumbnail(item, ThumbnailSize.Card) },
                )
            }
        }
    }
}

/** Kết quả tìm (D4, DS-03): danh sách, mỗi dòng có đường dẫn cha và từ khớp được tô. */
@Composable
private fun SearchBody(search: SearchState, contentPadding: PaddingValues, onIntent: (BrowserIntent) -> Unit) {
    val resources = LocalResources.current
    val languageTag = odvLocale().toLanguageTag()
    val rootLabel = stringResource(R.string.browser_root)
    val query = search.query.trim()
    Box(Modifier.fillMaxSize()) {
        when {
            query.isEmpty() -> Unit
            search.results.isEmpty() && !search.isSearching -> ODVEmptyState(
                icon = ODVIcon.Search,
                title = stringResource(R.string.browser_search_empty_title),
                modifier = Modifier.align(Alignment.Center),
                body = stringResource(R.string.browser_search_empty_body, query),
            )
            else -> LazyColumn(contentPadding = contentPadding) {
                items(search.results, key = { it.item.id }) { result ->
                    val item = result.item
                    ODVFileRow(
                        title = item.name,
                        leading = { ItemThumbnail(item, ThumbnailSize.Cell) },
                        meta = item.meta(resources, languageTag),
                        metaMono = item.durationMs?.let(::odvFormatDuration),
                        path = (listOf(rootLabel) + result.parentPath.map { it.name }).joinToString(" › "),
                        titleHighlight = query,
                        showChevron = item.isFolder,
                        onClick = { onIntent(BrowserIntent.OpenSearchResult(result)) },
                    )
                }
            }
        }
    }
}

/** Bottom sheet sắp xếp (D5, TM-05): chạm một lựa chọn là áp dụng và đóng, không có nút Lưu. */
@Composable
private fun SortSheet(current: SortOrder, onIntent: (BrowserIntent) -> Unit) {
    ODVBottomSheet(
        onDismissRequest = { onIntent(BrowserIntent.DismissSortSheet) },
        title = stringResource(R.string.browser_sort_title),
    ) {
        SortChoices.forEach { choice ->
            ODVOptionRow(
                label = choice.label(),
                selected = choice == current,
                onClick = { onIntent(BrowserIntent.SelectSort(choice)) },
            )
        }
    }
}

@Composable
private fun SortOrder.label(): String = stringResource(
    when (field) {
        SortField.Name ->
            if (direction == SortDirection.Ascending) R.string.browser_sort_name_asc else R.string.browser_sort_name_desc
        SortField.Modified ->
            if (direction == SortDirection.Descending) R.string.browser_sort_modified_desc else R.string.browser_sort_modified_asc
        SortField.Size ->
            if (direction == SortDirection.Ascending) R.string.browser_sort_size_asc else R.string.browser_sort_size_desc
    },
)

/**
 * Thumbnail thật cho ảnh/video (DS-01), phủ lên ô giữ chỗ theo loại. Thư mục, PDF và tệp không hỗ trợ không có thumbnail
 * (`thumbnailSource` null) nên chỉ hiện ô giữ chỗ; ảnh chưa tải được (offline, chưa cache) cũng rơi về ô giữ chỗ.
 */
@Composable
private fun ItemThumbnail(item: DriveItem, size: ThumbnailSize) {
    ODVRemoteImage(model = item.thumbnailSource(size), modifier = Modifier.fillMaxSize()) {
        ODVThumbnailPlaceholder(item.fileKind(), Modifier.fillMaxSize())
    }
}

/** Thư mục: "N mục"; tệp: dung lượng theo ngôn ngữ đang dùng (CD-10). */
private fun DriveItem.meta(resources: Resources, languageTag: String): String? =
    if (isFolder) {
        childCount?.let { resources.getQuantityString(R.plurals.browser_item_count, it, it) }
    } else {
        odvFormatFileSize(sizeBytes, languageTag)
    }

private fun DriveItem.fileKind(): ODVFileKind = when {
    isFolder -> ODVFileKind.Folder
    mediaKind == MediaKind.Video -> ODVFileKind.Video
    mediaKind == MediaKind.Image -> ODVFileKind.Photo
    mediaKind == MediaKind.Pdf -> ODVFileKind.Pdf
    else -> ODVFileKind.Unsupported
}
