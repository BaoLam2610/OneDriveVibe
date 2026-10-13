package com.lambao.odv.feature.browser

import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAccountAvatar
import com.lambao.odv.core.designsystem.component.ODVAccountAvatarSize
import com.lambao.odv.core.designsystem.component.ODVAccountCard
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVAppBarLogo
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVBreadcrumb
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVContinueCard
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVFileCard
import com.lambao.odv.core.designsystem.component.ODVFileKind
import com.lambao.odv.core.designsystem.component.ODVFileRow
import com.lambao.odv.core.designsystem.component.ODVFolderCard
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.component.ODVRemoteImage
import com.lambao.odv.core.designsystem.component.ODVReselectEffect
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSearchBar
import com.lambao.odv.core.designsystem.component.ODVSectionHeader
import com.lambao.odv.core.designsystem.component.ODVSortBar
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVThumbnailDefaults
import com.lambao.odv.core.designsystem.component.ODVThumbnailPlaceholder
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.designsystem.theme.ODVTransitions
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AccountStatus
import com.lambao.odv.core.domain.model.ContinueEntry
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.model.thumbnailSource
import java.text.NumberFormat

/** Khóa của mục dải Xem tiếp / Đọc tiếp trong danh sách và lưới; không trùng id tệp thật (id có dạng `<drive id>|<id Graph>`). */
private const val CONTINUE_STRIP_KEY = "continue-strip"

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
 * SortBar, rồi danh sách hoặc lưới, kéo xuống để làm mới. Từ Lát 8 không còn Tabs và nút Cài đặt (Cài đặt là tab của thanh điều hướng
 * đáy, ADR-0023). [banner] là banner secret sắp hết hạn (D9) do Màn chính dựng, đặt ở đầu thân (chỉ hiện khi không tìm kiếm).
 * [reselectSignal] tăng khi người dùng chạm lại tab Thư mục (DH-04): cuộn lên đầu, nếu đã ở đầu thì về thư mục gốc. Dải Xem tiếp / Đọc
 * tiếp (DS-02, Lát 10) nằm sau banner, chỉ ở gốc. Chưa có dấu `cloud-off` trên thẻ tệp chưa có trong cache (Lát 5 → 7, khi có cache tệp gốc).
 */
@Composable
internal fun ODVBrowserContent(
    state: BrowserState,
    onIntent: (BrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
    banner: @Composable () -> Unit = {},
    accountButton: @Composable () -> Unit = {},
    reselectSignal: Int = 0,
) {
    val search = state.search
    ODVScaffold(
        modifier = modifier,
        // Mở/đóng tìm kiếm: AppBar và nội dung cross-fade `fast` (mục 2.8). `contentKey` chỉ phân biệt "đang tìm" với "không tìm" nên gõ chữ
        // (SearchState đổi theo từng phím) không kích hoạt hoạt ảnh; phía đang mờ đi giữ nguyên giá trị `SearchState` cuối cùng của nó.
        topBar = {
            AnimatedContent(
                targetState = search,
                transitionSpec = { ODVTransitions.crossFade() },
                contentKey = { it != null },
                label = "browserTopBar",
            ) { shown ->
                if (shown != null) SearchTopBar(shown, onIntent) else BrowseTopBar(state, onIntent, accountButton)
            }
        },
    ) { contentPadding ->
        AnimatedContent(
            targetState = search,
            transitionSpec = { ODVTransitions.crossFade() },
            contentKey = { it != null },
            label = "browserBody",
        ) { shown ->
            if (shown != null) {
                SearchBody(shown, state.scope == AccountScope.All, contentPadding, onIntent)
            } else {
                BrowseBody(state, contentPadding, onIntent, banner, reselectSignal)
            }
        }
    }
    if (state.isSortSheetOpen) SortSheet(state.sort, onIntent)
}

@Composable
private fun BrowseTopBar(state: BrowserState, onIntent: (BrowserIntent) -> Unit, accountButton: @Composable () -> Unit) {
    val inRoot = state.path.isEmpty()
    ODVAppBar(
        // Gốc của phạm vi Tất cả tài khoản có tiêu đề riêng (D10, TA-05).
        title = when {
            !inRoot -> state.path.last().name
            state.scope == AccountScope.All -> stringResource(R.string.browser_all_accounts)
            else -> stringResource(R.string.browser_title)
        },
        navigation = {
            if (inRoot) {
                ODVAppBarLogo()
            } else {
                ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.browser_up), { onIntent(BrowserIntent.GoUp) })
            }
        },
        actions = {
            ODVIconButton(ODVIcon.Search, stringResource(R.string.browser_search), { onIntent(BrowserIntent.OpenSearch) })
            // Nút tài khoản (TA-03): sau nút Tìm kiếm, do Màn chính dựng. Không có ở thanh tìm kiếm (D4).
            accountButton()
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
    banner: @Composable () -> Unit,
    reselectSignal: Int,
) {
    val rootLabel = stringResource(if (state.scope == AccountScope.All) R.string.browser_all_accounts else R.string.browser_root)
    val sortLabel = state.sort.label()
    val isGrid = state.viewMode == ViewMode.Grid
    val canGoRoot = state.path.isNotEmpty()
    // D10 (TA-05): ở gốc của phạm vi Tất cả tài khoản không có Breadcrumb, SortBar và danh sách thư mục; chỉ có các ô tài khoản.
    val accountRoot = state.showsAccountRoot
    // DH-04: khi không có danh sách để cuộn (đang tải, lỗi, thư mục rỗng) thì chạm lại tab chỉ còn nghĩa là về thư mục gốc.
    val listShown = !state.isLoading && state.error == null && state.items.isNotEmpty()
    if (!listShown && !accountRoot) {
        ODVReselectEffect(reselectSignal) { if (canGoRoot) onIntent(BrowserIntent.GoToCrumb(0)) }
    }
    Column(Modifier.fillMaxSize()) {
        banner()
        SyncBanner(state, onIntent)
        if (!accountRoot) {
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
        }
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
            // DS-02: dải Xem tiếp / Đọc tiếp là mục đầu của vùng cuộn (cuộn chung với danh sách, đổi 2026-10-13, trước đó đứng yên phía trên);
            // chỉ ở gốc, kể cả gốc Tất cả tài khoản (D10).
            val continueItems = if (state.path.isEmpty()) state.continueItems else emptyList()
            // Đang tải → danh sách / trống / lỗi cross-fade `fast` (mục 2.8). Đổi thư mục mà vẫn cùng loại thì giữ nguyên (không hoạt ảnh).
            val bodyKind = when {
                accountRoot -> BrowseBodyKind.AccountRoot
                state.isLoading || state.error != null || state.items.isEmpty() -> BrowseBodyKind.Placeholder
                isGrid -> BrowseBodyKind.Grid
                else -> BrowseBodyKind.List
            }
            AnimatedContent(
                targetState = bodyKind,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { ODVTransitions.crossFade() },
                label = "browserBodyKind",
            ) { kind ->
                when (kind) {
                    BrowseBodyKind.AccountRoot -> AccountRoot(state.accounts, isGrid, contentPadding, onIntent, reselectSignal, continueItems)
                    // Đang tải, lỗi, thư mục rỗng: không có danh sách để cuộn nên dải đứng trên cùng, phần còn lại chiếm chỗ dưới nó.
                    BrowseBodyKind.Placeholder -> Column(Modifier.fillMaxSize()) {
                        if (continueItems.isNotEmpty()) ContinueStrip(continueItems, onIntent, horizontalPadding = 16.dp)
                        // Cross-fade riêng giữa tải / lỗi / trống để dải Xem tiếp phía trên không mờ theo.
                        val placeholder = when {
                            state.isLoading -> BrowsePlaceholder.Loading
                            state.error != null -> BrowsePlaceholder.Error
                            else -> BrowsePlaceholder.Empty
                        }
                        AnimatedContent(
                            targetState = placeholder,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            transitionSpec = { ODVTransitions.crossFade() },
                            label = "browserPlaceholder",
                        ) { which ->
                            Box(Modifier.fillMaxSize()) {
                                when (which) {
                                    BrowsePlaceholder.Loading -> Loading()
                                    BrowsePlaceholder.Error -> state.error?.let { ErrorState(it, onRetry = { onIntent(BrowserIntent.Retry) }) }
                                    BrowsePlaceholder.Empty -> ODVEmptyState(
                                        icon = ODVIcon.Folder,
                                        title = stringResource(R.string.browser_empty_title),
                                        modifier = Modifier.align(Alignment.Center),
                                        body = stringResource(R.string.browser_empty_body),
                                    )
                                }
                            }
                        }
                    }
                    BrowseBodyKind.Grid -> ItemGrid(state.items, contentPadding, onIntent, reselectSignal, canGoRoot, continueItems)
                    BrowseBodyKind.List -> ItemList(state.items, contentPadding, onIntent, reselectSignal, canGoRoot, continueItems)
                }
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
    // Banner trượt vào / mờ đi (mục 2.8). Khóa là LOẠI banner; số mục đã quét đổi liên tục nhưng không kích hoạt hoạt ảnh.
    val kind = when {
        state.isOffline -> SyncBannerKind.Offline
        // Phạm vi Tất cả (D7, TA-05): còn tài khoản đang quét lần đầu thì banner hiện kèm tên tài khoản đó, dù tài khoản khác đã xong.
        (!sync.initialSyncDone && sync.isSyncing) || sync.scanningAccountId != null -> SyncBannerKind.Indexing
        sync.failed -> SyncBannerKind.Failed
        else -> SyncBannerKind.None
    }
    val reduceMotion = ODVTheme.motion.reduceMotion
    AnimatedContent(
        targetState = kind,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = { ODVTransitions.banner(reduceMotion) },
        label = "syncBanner",
    ) { shown ->
        when (shown) {
            SyncBannerKind.Offline -> ODVBanner(
                text = stringResource(R.string.browser_offline),
                tone = ODVBannerTone.Neutral,
                icon = ODVIcon.CloudOff,
                modifier = spacing,
            )
            SyncBannerKind.Indexing -> {
                val locale = odvLocale()
                val scanned = remember(sync.scannedCount, locale) { NumberFormat.getIntegerInstance(locale).format(sync.scannedCount) }
                val accountName = state.accounts.firstOrNull { it.account.id == sync.scanningAccountId }?.account?.displayName
                ODVBanner(
                    text = if (accountName != null) {
                        stringResource(R.string.browser_indexing_account, accountName, scanned)
                    } else {
                        stringResource(R.string.browser_indexing, scanned)
                    },
                    tone = ODVBannerTone.Volt,
                    icon = ODVIcon.Sync,
                    modifier = spacing,
                )
            }
            SyncBannerKind.Failed -> ODVBanner(
                text = stringResource(R.string.browser_sync_failed),
                tone = ODVBannerTone.Warning,
                icon = ODVIcon.Alert,
                modifier = spacing,
                actionLabel = stringResource(R.string.browser_retry),
                onAction = { onIntent(BrowserIntent.Refresh) },
            )
            SyncBannerKind.None -> Unit
        }
    }
}

/** Loại banner đồng bộ, dùng làm khóa hoạt ảnh. */
private enum class SyncBannerKind { Offline, Indexing, Failed, None }

/** Loại thân Thư mục, dùng làm khóa cross-fade (mục 2.8): đổi loại thì cross-fade, cùng loại thì cập nhật tại chỗ. */
private enum class BrowseBodyKind { AccountRoot, Placeholder, Grid, List }

/** Ba trạng thái không có danh sách để cuộn; cross-fade với nhau bên trong [BrowseBodyKind.Placeholder]. */
private enum class BrowsePlaceholder { Loading, Error, Empty }

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
private fun ItemList(
    items: List<DriveItem>,
    contentPadding: PaddingValues,
    onIntent: (BrowserIntent) -> Unit,
    reselectSignal: Int,
    canGoRoot: Boolean,
    continueItems: List<ContinueEntry>,
) {
    val resources = LocalResources.current
    val languageTag = odvLocale().toLanguageTag()
    val listState = rememberLazyListState()
    // DH-04: cuộn lên đầu; đã ở đầu thì về thư mục gốc.
    ODVReselectEffect(reselectSignal) {
        if (listState.canScrollBackward) listState.animateScrollToItem(0) else if (canGoRoot) onIntent(BrowserIntent.GoToCrumb(0))
    }
    LazyColumn(state = listState, contentPadding = contentPadding) {
        if (continueItems.isNotEmpty()) {
            item(key = CONTINUE_STRIP_KEY) { ContinueStrip(continueItems, onIntent, horizontalPadding = 16.dp) }
        }
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
private fun ItemGrid(
    items: List<DriveItem>,
    contentPadding: PaddingValues,
    onIntent: (BrowserIntent) -> Unit,
    reselectSignal: Int,
    canGoRoot: Boolean,
    continueItems: List<ContinueEntry>,
) {
    val resources = LocalResources.current
    val languageTag = odvLocale().toLanguageTag()
    val gridState = rememberLazyGridState()
    // DH-04: cuộn lên đầu; đã ở đầu thì về thư mục gốc.
    ODVReselectEffect(reselectSignal) {
        if (gridState.canScrollBackward) gridState.animateScrollToItem(0) else if (canGoRoot) onIntent(BrowserIntent.GoToCrumb(0))
    }
    LazyVerticalGrid(
        state = gridState,
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
        if (continueItems.isNotEmpty()) {
            // Dải chiếm cả hàng; lưới đã đệm 16 hai bên nên dải không đệm thêm.
            item(key = CONTINUE_STRIP_KEY, span = { GridItemSpan(maxLineSpan) }) {
                ContinueStrip(continueItems, onIntent, horizontalPadding = 0.dp)
            }
        }
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

/**
 * Dải "Xem tiếp / Đọc tiếp" (DS-02, D1, D10): tiêu đề rồi hàng thẻ cuộn ngang. Video có thời lượng, thanh tiến độ volt và dòng phụ
 * "Còn … · dừng ở …"; PDF có thanh tiến độ màu PDF và "Trang x / y". Chạm thẻ mở tệp (video vào thẳng chỗ đã dừng), nút X ẩn khỏi dải.
 * Dải là mục đầu của vùng cuộn, cuộn chung với danh sách (2026-10-13; thiết kế cũ để dải đứng yên phía trên). [horizontalPadding] là đệm hai bên:
 * 16 khi nằm trong danh sách, 0 khi nằm trong lưới (lưới đã đệm 16).
 */
@Composable
private fun ContinueStrip(entries: List<ContinueEntry>, onIntent: (BrowserIntent) -> Unit, horizontalPadding: Dp) {
    val locale = odvLocale()
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        ODVSectionHeader(stringResource(R.string.browser_continue_title), Modifier.padding(horizontal = horizontalPadding))
        // Padding dọc để bóng shadow.sm của thẻ không bị cắt.
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 12.dp, bottom = 4.dp),
        ) {
            items(entries, key = { it.item.id }) { entry ->
                when (entry) {
                    is ContinueEntry.Video -> ODVContinueCard(
                        title = entry.item.name,
                        subtitle = stringResource(
                            R.string.browser_continue_video_meta,
                            odvFormatDuration((entry.durationMs - entry.positionMs).coerceAtLeast(0L)),
                            odvFormatDuration(entry.positionMs),
                        ),
                        onClick = { onIntent(BrowserIntent.OpenContinue(entry)) },
                        dismissContentDescription = stringResource(R.string.browser_continue_dismiss_video),
                        onDismiss = { onIntent(BrowserIntent.DismissContinue(entry.item.id)) },
                        progress = if (entry.durationMs > 0L) (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f) else null,
                        duration = odvFormatDuration(entry.durationMs),
                    ) { ItemThumbnail(entry.item, ThumbnailSize.Card) }
                    is ContinueEntry.Pdf -> ODVContinueCard(
                        title = entry.item.name,
                        subtitle = stringResource(
                            R.string.browser_continue_pdf_meta,
                            numbers.format(entry.page + 1),
                            numbers.format(entry.pageCount),
                        ),
                        onClick = { onIntent(BrowserIntent.OpenContinue(entry)) },
                        dismissContentDescription = stringResource(R.string.browser_continue_dismiss_pdf),
                        onDismiss = { onIntent(BrowserIntent.DismissContinue(entry.item.id)) },
                        progress = if (entry.pageCount > 0) ((entry.page + 1f) / entry.pageCount).coerceIn(0f, 1f) else null,
                        progressColors = ODVThumbnailDefaults.pdfProgressColors(),
                    ) { ItemThumbnail(entry.item, ThumbnailSize.Card) }
                }
            }
        }
    }
}

/**
 * Gốc của phạm vi Tất cả tài khoản (D10, TA-05): mỗi tài khoản là một ô (lưới) hoặc một hàng (danh sách), tên hiển thị và email; tài khoản
 * không truy cập được (TA-10) có dòng lỗi thay cho email nhưng vẫn mở được để xem dữ liệu đã đồng bộ. Chưa biết gốc của tài khoản (trang
 * đồng bộ đầu tiên chưa ghi) thì chạm chưa mở được. Chạm lại tab (DH-04) ở đây chỉ cuộn lên đầu.
 */
@Composable
private fun AccountRoot(
    accounts: List<AccountStatus>,
    isGrid: Boolean,
    contentPadding: PaddingValues,
    onIntent: (BrowserIntent) -> Unit,
    reselectSignal: Int,
    continueItems: List<ContinueEntry>,
) {
    val inaccessible = stringResource(R.string.browser_account_inaccessible)
    if (isGrid) {
        val gridState = rememberLazyGridState()
        ODVReselectEffect(reselectSignal) { if (gridState.canScrollBackward) gridState.animateScrollToItem(0) }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + contentPadding.calculateBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (continueItems.isNotEmpty()) {
                item(key = CONTINUE_STRIP_KEY, span = { GridItemSpan(maxLineSpan) }) {
                    ContinueStrip(continueItems, onIntent, horizontalPadding = 0.dp)
                }
            }
            items(accounts, key = { it.account.id }) { status ->
                ODVAccountCard(
                    name = status.account.displayName,
                    meta = status.account.email,
                    error = if (status.inaccessible) inaccessible else null,
                    onClick = { onIntent(BrowserIntent.OpenAccount(status.account.id)) },
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        ODVReselectEffect(reselectSignal) { if (listState.canScrollBackward) listState.animateScrollToItem(0) }
        LazyColumn(state = listState, contentPadding = contentPadding) {
            if (continueItems.isNotEmpty()) {
                item(key = CONTINUE_STRIP_KEY) { ContinueStrip(continueItems, onIntent, horizontalPadding = 16.dp) }
            }
            items(accounts, key = { it.account.id }) { status ->
                ODVFileRow(
                    title = status.account.displayName,
                    leading = { ODVAccountAvatar(status.account.displayName, size = ODVAccountAvatarSize.Tile) },
                    meta = if (status.inaccessible) inaccessible else status.account.email,
                    showChevron = true,
                    onClick = { onIntent(BrowserIntent.OpenAccount(status.account.id)) },
                )
            }
        }
    }
}

/** Kết quả tìm (D4, DS-03): danh sách, mỗi dòng có đường dẫn cha và từ khớp được tô. */
@Composable
private fun SearchBody(search: SearchState, allAccounts: Boolean, contentPadding: PaddingValues, onIntent: (BrowserIntent) -> Unit) {
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
                        // Phạm vi Tất cả tài khoản: đường dẫn đã bắt đầu bằng tên tài khoản (TA-05), không thêm nhãn gốc.
                        path = (if (allAccounts && result.parentPath.isNotEmpty()) emptyList() else listOf(rootLabel))
                            .plus(result.parentPath.map { it.name })
                            .joinToString(" › "),
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
