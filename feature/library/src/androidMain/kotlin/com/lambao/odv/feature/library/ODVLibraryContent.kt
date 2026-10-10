package com.lambao.odv.feature.library

import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVAppBarLogo
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVDateHeader
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVFastScroller
import com.lambao.odv.core.designsystem.component.ODVPhotoCell
import com.lambao.odv.core.designsystem.component.ODVPhotoCellPlaceholder
import com.lambao.odv.core.designsystem.component.ODVRemoteImage
import com.lambao.odv.core.designsystem.component.ODVReselectEffect
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.dayNumberOf
import com.lambao.odv.core.domain.model.libraryDate
import com.lambao.odv.core.domain.model.thumbnailSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// Lưới 4 cột, khe 2dp (thiet-ke-ui.md mục 5.2, token space.half).
private const val COLUMNS = 4
private val CellGap = 2.dp

// Chiều cao tiêu đề nhóm (ODVDateHeader: padding 12 + dòng chữ 22 + padding 8) để biết khi nào tiêu đề kế tiếp chạm lớp dính.
private val DateHeaderHeight = 42.dp

// Cuộn nhanh chỉ hiện khi nội dung dài hơn khoảng 3 màn hình (mục 4.4 FastScroller).
private const val FAST_SCROLLER_MIN_SCREENS = 3

// Cuộn nhanh mờ đi sau chừng này mili giây kể từ lần cuộn cuối.
private const val SCROLLER_HIDE_DELAY_MS = 2_000L

private const val HEADER = "header"
private const val CELL = "cell"

/**
 * Giao diện tab Thư viện (thiet-ke-ui.md mục 4.4, 5.2; D3, D7): AppBar, Banner (offline / đang lập chỉ mục / lỗi / secret sắp hết
 * hạn), chip lọc, rồi lưới 4 cột nhóm theo ngày với cuộn nhanh. Từ Lát 8 không còn Tabs và nút Cài đặt (ADR-0023). [banner] là banner
 * secret sắp hết hạn (D9) do Màn chính dựng; [reselectSignal] tăng khi người dùng chạm lại tab này (DH-04): cuộn lên đầu.
 */
@Composable
internal fun ODVLibraryContent(
    state: LibraryState,
    pages: LazyPagingItems<DriveItem>,
    onIntent: (LibraryIntent) -> Unit,
    onShowFolders: () -> Unit,
    modifier: Modifier = Modifier,
    banner: @Composable () -> Unit = {},
    reselectSignal: Int = 0,
) {
    ODVScaffold(
        modifier = modifier,
        topBar = {
            ODVAppBar(
                title = stringResource(R.string.library_title),
                navigation = { ODVAppBarLogo() },
            )
        },
    ) { contentPadding ->
        Column(Modifier.fillMaxSize()) {
            banner()
            LibraryBanner(state, onIntent, onShowFolders)
            if (state.showFilters) FilterRow(state.filter, onIntent)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val layout = remember(state.days) { LibraryLayout(state.days) }
                when {
                    layout.total > 0 -> LibraryGrid(state, pages, layout, contentPadding, onIntent, reselectSignal)
                    // Quét lần đầu đang chạy: banner D7 đã nói rõ, chưa có gì để hiện (TV-06).
                    !state.sync.initialSyncDone && state.sync.isSyncing -> Unit
                    !state.daysLoaded -> Loading()
                    else -> ODVEmptyState(
                        icon = ODVIcon.Image,
                        title = stringResource(R.string.library_empty_title),
                        modifier = Modifier.align(Alignment.Center),
                        body = stringResource(R.string.library_empty_body),
                    )
                }
            }
        }
    }
}

/** Thứ tự ưu tiên giống tab Thư mục: offline (DS-05), đang lập chỉ mục (TV-06, D7), đồng bộ lỗi. Mỗi lúc một banner. */
@Composable
private fun LibraryBanner(state: LibraryState, onIntent: (LibraryIntent) -> Unit, onShowFolders: () -> Unit) {
    val sync = state.sync
    val spacing = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    when {
        state.isOffline -> ODVBanner(
            text = stringResource(R.string.library_offline),
            tone = ODVBannerTone.Neutral,
            icon = ODVIcon.CloudOff,
            modifier = spacing,
        )
        !sync.initialSyncDone && sync.isSyncing -> {
            val locale = odvLocale()
            val scanned = remember(sync.scannedCount, locale) { NumberFormat.getIntegerInstance(locale).format(sync.scannedCount) }
            ODVBanner(
                text = stringResource(R.string.library_indexing, scanned),
                tone = ODVBannerTone.Volt,
                icon = ODVIcon.Sync,
                modifier = spacing,
                // TV-06: lối tắt sang tab Thư mục trong lúc chờ quét.
                actionLabel = stringResource(R.string.library_indexing_action),
                onAction = onShowFolders,
            )
        }
        sync.failed -> ODVBanner(
            text = stringResource(R.string.library_sync_failed),
            tone = ODVBannerTone.Warning,
            icon = ODVIcon.Alert,
            modifier = spacing,
            actionLabel = stringResource(R.string.library_retry),
            onAction = { onIntent(LibraryIntent.Refresh) },
        )
    }
}

/** Chip lọc Tất cả / Ảnh / Video (TV-03). Chỉ hiện khi cả ảnh và video đang bật ([LibraryState.showFilters], CD-01). */
@Composable
private fun FilterRow(selected: LibraryFilter, onIntent: (LibraryIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryFilter.entries.forEach { filter ->
            val label = stringResource(filter.labelRes())
            ODVChip(
                label = label,
                selected = filter == selected,
                onClick = { onIntent(LibraryIntent.SelectFilter(filter)) },
                selectedContentDescription = stringResource(R.string.library_chip_selected, label),
            )
        }
    }
}

@Composable
private fun Loading() {
    val description = stringResource(R.string.library_loading)
    Box(Modifier.fillMaxSize().semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        ODVSpinner(size = 36.dp)
    }
}

@Composable
private fun LibraryGrid(
    state: LibraryState,
    pages: LazyPagingItems<DriveItem>,
    layout: LibraryLayout,
    contentPadding: PaddingValues,
    onIntent: (LibraryIntent) -> Unit,
    reselectSignal: Int,
) {
    val resources = LocalResources.current
    val locale = odvLocale()
    val dates = remember(locale) { LibraryDateFormatter(locale) }
    val labels = remember(resources, dates, locale, state.utcOffsetMs) { DayLabels(resources, dates, locale, state.utcOffsetMs) }
    val gridState = rememberLazyGridState()
    // DH-04: chạm lại tab Thư viện thì cuộn lên đầu.
    ODVReselectEffect(reselectSignal) { gridState.animateScrollToItem(0) }
    val scope = rememberCoroutineScope()
    val headerHeightPx = with(LocalDensity.current) { DateHeaderHeight.toPx() }

    // Ngày của lớp tiêu đề dính: ngày của ô đầu tiên đang hiện, trừ khi một tiêu đề khác đã chạm tới lớp dính.
    val stickyDay by remember(layout) {
        derivedStateOf {
            val visible = gridState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                0
            } else {
                visible.lastOrNull { layout.isHeader(it.index) && it.offset.y <= headerHeightPx }
                    ?.let { layout.dayIndexAt(it.index) }
                    ?: layout.dayIndexAt(visible.first().index)
            }
        }
    }
    val atTop by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 } }
    val showScroller by remember(layout) {
        derivedStateOf {
            val visibleCount = gridState.layoutInfo.visibleItemsInfo.size
            visibleCount > 0 && layout.total > visibleCount * FAST_SCROLLER_MIN_SCREENS
        }
    }
    val fraction by remember(layout) {
        derivedStateOf {
            val visibleCount = gridState.layoutInfo.visibleItemsInfo.size
            val scrollable = (layout.total - visibleCount).coerceAtLeast(1)
            (gridState.firstVisibleItemIndex.toFloat() / scrollable).coerceIn(0f, 1f)
        }
    }
    // Cuộn nhanh chỉ hiện lúc đang cuộn rồi mờ đi sau một lúc: rãnh và tay cầm luôn hiện thì vạch dọc đè lên mép phải của
    // cả lưới (và số mục của tiêu đề nhóm) trông thừa.
    var scrollerActive by remember { mutableStateOf(false) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }.drop(1).collectLatest {
            scrollerActive = true
            delay(SCROLLER_HIDE_DELAY_MS)
            scrollerActive = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(CellGap),
            verticalArrangement = Arrangement.spacedBy(CellGap),
        ) {
            items(
                count = layout.total,
                key = { index ->
                    if (layout.isHeader(index)) "d${layout.days[layout.dayIndexAt(index)].dayNumber}" else "m${layout.mediaPositionAt(index)}"
                },
                span = { index -> GridItemSpan(if (layout.isHeader(index)) maxLineSpan else 1) },
                contentType = { index -> if (layout.isHeader(index)) HEADER else CELL },
            ) { index ->
                if (layout.isHeader(index)) {
                    val day = layout.days[layout.dayIndexAt(index)]
                    ODVDateHeader(title = labels.title(day), count = labels.count(day))
                } else {
                    val position = layout.mediaPositionAt(index)
                    // pages[position] kích hoạt nạp trang chứa ô này; chưa nạp (hoặc số mục lệch nhau lúc đang đồng bộ) là khung chờ (TV-06).
                    val item = if (position < pages.itemCount) pages[position] else null
                    if (item == null) {
                        ODVPhotoCellPlaceholder()
                    } else {
                        val isVideo = item.mediaKind == MediaKind.Video
                        ODVPhotoCell(
                            onClick = { onIntent(LibraryIntent.Open(item)) },
                            contentDescription = item.describe(resources, dates, locale.toLanguageTag(), state.utcOffsetMs),
                            kindIcon = if (isVideo) ODVIcon.Video else null,
                            duration = if (isVideo) item.durationMs?.let(::odvFormatDuration) else null,
                        ) {
                            ODVRemoteImage(model = item.thumbnailSource(ThumbnailSize.Cell), modifier = Modifier.fillMaxSize()) {}
                        }
                    }
                }
            }
        }
        if (!atTop) {
            val day = layout.days[stickyDay.coerceIn(0, layout.days.lastIndex)]
            // Bản dính chỉ để nhìn: tiêu đề thật đã có nhãn TalkBack nên bỏ ngữ nghĩa của bản này để không đọc lặp.
            ODVDateHeader(
                title = labels.title(day),
                count = labels.count(day),
                modifier = Modifier.align(Alignment.TopStart).clearAndSetSemantics {},
            )
        }
        AnimatedVisibility(
            visible = showScroller && scrollerActive,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Bắt đầu dưới tiêu đề nhóm để rãnh không cắt qua tiêu đề và số mục.
                .padding(top = DateHeaderHeight + 8.dp, bottom = 8.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val bubbleDay = layout.days[layout.dayIndexAt(gridState.firstVisibleItemIndex)]
            ODVFastScroller(
                fraction = fraction,
                onFractionChange = { target ->
                    scope.launch { gridState.scrollToItem((target * (layout.total - 1)).roundToInt().coerceIn(0, layout.total - 1)) }
                },
                bubbleText = dates.month(bubbleDay.dayNumber),
                contentDescription = stringResource(R.string.library_fast_scroller),
            )
        }
    }
}

/**
 * Chữ của tiêu đề nhóm ngày: "Hôm nay" / "Hôm qua" cho hai ngày gần nhất, các ngày khác ghi ngày đầy đủ theo ngôn ngữ;
 * bên phải là số ảnh và số video của ngày đó ("12 ảnh · 3 video") thay vì một con số trơ trọi. [today] tính theo cùng độ
 * lệch múi giờ với cách nhóm của lưới.
 */
private class DayLabels(
    private val resources: Resources,
    private val dates: LibraryDateFormatter,
    locale: Locale,
    utcOffsetMs: Long,
) {
    private val today = dayNumberOf(System.currentTimeMillis(), utcOffsetMs)
    private val numbers = NumberFormat.getIntegerInstance(locale)

    fun title(day: LibraryDay): String = when (day.dayNumber) {
        today -> resources.getString(R.string.library_today)
        today - 1 -> resources.getString(R.string.library_yesterday)
        else -> dates.day(day.dayNumber)
    }

    fun count(day: LibraryDay): String {
        val photos = day.count - day.videoCount
        return listOfNotNull(
            photos.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.library_count_photos, it, numbers.format(it)) },
            day.videoCount.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.library_count_videos, it, numbers.format(it)) },
        ).joinToString(" · ")
    }
}

private fun LibraryFilter.labelRes(): Int = when (this) {
    LibraryFilter.All -> R.string.library_filter_all
    LibraryFilter.Photos -> R.string.library_filter_photos
    LibraryFilter.Videos -> R.string.library_filter_videos
}

/** Nhãn TalkBack đủ thông tin, ví dụ "Video, mau-01.mp4, 1 giờ 26 phút, 974 MB, 23 tháng 5, 2026" (thiet-ke-ui.md mục 7). */
private fun DriveItem.describe(resources: Resources, dates: LibraryDateFormatter, languageTag: String, utcOffsetMs: Long): String {
    val size = odvFormatFileSize(sizeBytes, languageTag)
    val date = dates.day(libraryDate, utcOffsetMs)
    return if (mediaKind == MediaKind.Video) {
        val spoken = durationMs?.let { resources.spokenDuration(it) }
        if (spoken != null) {
            resources.getString(R.string.library_cd_video, name, spoken, size, date)
        } else {
            resources.getString(R.string.library_cd_video_no_duration, name, size, date)
        }
    } else {
        resources.getString(R.string.library_cd_image, name, size, date)
    }
}

/** "1 giờ 26 phút", "18 phút", "42 giây": giờ và phút nếu có, chỉ giây khi dưới một phút. */
private fun Resources.spokenDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0) / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val parts = buildList {
        if (hours > 0) add(getQuantityString(R.plurals.library_duration_hours, hours, hours))
        if (minutes > 0) add(getQuantityString(R.plurals.library_duration_minutes, minutes, minutes))
        if (hours == 0 && minutes == 0) add(getQuantityString(R.plurals.library_duration_seconds, seconds, seconds))
    }
    return parts.joinToString(" ")
}
