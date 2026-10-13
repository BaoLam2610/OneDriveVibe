package com.lambao.odv.feature.pdfviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.domain.model.PdfReadingStyle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** Zoom 2,02x vẫn dựng ở mức 2 chứ không nhảy lên 3: sai số của cử chỉ không nên tốn thêm một cấp độ phân giải. */
private const val RENDER_SCALE_TOLERANCE = 0.05f

/**
 * Vùng đọc PDF (PD-02): nền `media.pdf-canvas`, trang rộng toàn màn cách nhau 12 ở chế độ cuộn dọc liên tục, hoặc từng trang vừa khung ở
 * chế độ lật ngang. Chạm một lần báo [onTap] (ẩn/hiện thanh công cụ, PD-06). [initialPage] chỉ dùng lúc dựng lần đầu; sau đó vị trí do danh
 * sách hoặc Pager giữ (và khôi phục khi xoay màn hình). Đổi chế độ đọc thì nơi gọi dựng lại bằng `key(style)` với trang hiện tại.
 *
 * @param scrollRequests số trang (đếm từ 0) cần nhảy tới sau khi người dùng nhập số trang (PD-04).
 */
@OptIn(FlowPreview::class)
@Composable
internal fun PdfReader(
    document: PdfDocument,
    style: PdfReadingStyle,
    initialPage: Int,
    scrollRequests: Flow<Int>,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnTap by rememberUpdatedState(onTap)
    val latestOnPageChanged by rememberUpdatedState(onPageChanged)
    val start = initialPage.coerceIn(0, document.pageCount - 1)
    val scope = rememberCoroutineScope()
    val zoom = remember { PdfZoomState() }
    val flingHolder = remember { FlingHolder() }
    // Danh sách/Pager nằm ở đây (không phải trong nhánh con) để cử chỉ zoom chuyển phần kéo dư thành cuộn danh sách. `style` cố định trong
    // đời của PdfReader (nơi gọi dựng lại bằng key(style)) nên gọi remember có điều kiện là an toàn.
    val listState = if (style == PdfReadingStyle.Vertical) rememberLazyListState(initialFirstVisibleItemIndex = start) else null
    val pagerState = if (style == PdfReadingStyle.Horizontal) rememberPagerState(initialPage = start) { document.pageCount } else null

    // Mức dựng lại trang: số nguyên làm tròn lên của zoom, đổi sau khi zoom dừng để không dựng liên tục lúc ngón còn chụm (PD-03).
    var renderScale by remember { mutableIntStateOf(1) }
    LaunchedEffect(zoom) {
        snapshotFlow { zoom.scale }.debounce(PdfViewerConstants.ZOOM_SETTLE_MS).collect {
            renderScale = ceil(it - RENDER_SCALE_TOLERANCE).toInt().coerceIn(1, PdfViewerConstants.MAX_ZOOM.toInt())
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(ODVMediaColors.pdfCanvas)) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        SideEffect { zoom.updateViewport(widthPx.toFloat(), heightPx.toFloat()) }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { latestOnTap() },
                        // Cùng chỗ giữ việc với quán tính: cử chỉ mới bắt đầu thì hủy hoạt ảnh đang chạy, không để hai bên cùng ghi zoom.
                        onDoubleTap = { point ->
                            flingHolder.job?.cancel()
                            flingHolder.job = scope.launch { zoom.toggleAt(point) }
                        },
                    )
                }
                .pointerInput(zoom, listState) {
                    detectPdfZoomGestures(
                        isZoomed = { zoom.isZoomed },
                        onStart = { flingHolder.job?.cancel() },
                        onTransform = { centroid, pan, zoomChange ->
                            val overflow = zoom.applyTransform(centroid, pan, zoomChange)
                            // Kéo vượt mép cửa sổ đã zoom thì cuộn danh sách để đọc tiếp xuyên các trang (ngón đi xuống: cuộn lùi).
                            if (listState != null && overflow != 0f) listState.dispatchRawDelta(-overflow / zoom.scale)
                        },
                        onEnd = { velocity ->
                            if (listState != null && velocity != null && zoom.isZoomed) {
                                flingHolder.job = scope.launch { flingList(listState, velocity.y, zoom) }
                            }
                        },
                    )
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = zoom.scale
                        scaleY = zoom.scale
                        translationX = zoom.translationX
                        translationY = zoom.translationY
                    },
            ) {
                if (listState != null) {
                    VerticalPages(document, listState, zoom, scrollRequests, widthPx, renderScale) { latestOnPageChanged(it) }
                } else if (pagerState != null) {
                    HorizontalPages(document, pagerState, zoom, scrollRequests, widthPx, heightPx, renderScale) { latestOnPageChanged(it) }
                }
            }
        }
    }
}

/** Giữ việc đang chạy (quán tính, hoạt ảnh chạm đúp) để hủy khi có cử chỉ mới. */
private class FlingHolder {
    var job: Job? = null
}

@Composable
private fun VerticalPages(
    document: PdfDocument,
    listState: LazyListState,
    zoom: PdfZoomState,
    scrollRequests: Flow<Int>,
    widthPx: Int,
    renderScale: Int,
    onPageChanged: (Int) -> Unit,
) {
    var page by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    LaunchedEffect(listState, zoom) {
        snapshotFlow { mostVisiblePage(listState, zoom) }.filterNotNull().distinctUntilChanged().collect {
            page = it
            onPageChanged(it)
        }
    }
    LaunchedEffect(listState, scrollRequests) {
        scrollRequests.collect {
            // Nhảy trang từ hộp thoại: về 1x trước để vị trí đích đúng như người dùng nhập.
            zoom.reset()
            listState.scrollToItem(it)
        }
    }
    // Dựng trước trang kế tiếp để lật sang là có hình ngay. Kết quả nằm trong cache nên bỏ giá trị trả về. Đang zoom thì thôi: mỗi trang
    // dựng ở độ phân giải cao rất tốn bộ nhớ, trang kế tiếp dựng khi nó hiện ra.
    LaunchedEffect(document, page, widthPx, renderScale) {
        if (renderScale == 1 && page + 1 < document.pageCount) document.render(page + 1, widthPx, null)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(PdfViewerConstants.PAGE_GAP_DP.dp),
    ) {
        items(count = document.pageCount, key = { it }) { index ->
            PdfPage(document, index, widthPx, maxHeightPx = null, renderScale = renderScale, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HorizontalPages(
    document: PdfDocument,
    pagerState: PagerState,
    zoom: PdfZoomState,
    scrollRequests: Flow<Int>,
    widthPx: Int,
    heightPx: Int,
    renderScale: Int,
    onPageChanged: (Int) -> Unit,
) {
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect {
            // Sang trang khác thì về 1x: zoom và dịch chuyển chỉ có nghĩa với trang đang xem.
            zoom.reset()
            onPageChanged(it)
        }
    }
    LaunchedEffect(pagerState, scrollRequests) {
        scrollRequests.collect {
            zoom.reset()
            pagerState.scrollToPage(it)
        }
    }
    // derivedStateOf: chỉ recompose khi qua ngưỡng zoom, không recompose theo từng khung hình lúc đang chụm.
    val zoomed by remember(zoom) { derivedStateOf { zoom.isZoomed } }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // Đang zoom thì một ngón là kéo ảnh, không lật trang (zoom về 1x bằng chạm đúp hoặc chụm lại để lật).
        userScrollEnabled = !zoomed,
        // Giữ sẵn trang kề hai bên để lật sang là có hình ngay.
        beyondViewportPageCount = 1,
        key = { it },
    ) { index ->
        PdfPage(document, index, widthPx, maxHeightPx = heightPx, renderScale = renderScale, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Trang chiếm nhiều diện tích nhìn thấy nhất trong cửa sổ đang hiện (cùng diện tích thì trang đứng trước). Dùng diện tích thay vì "trang
 * ở giữa màn" để trang vừa nhảy tới (nằm sát mép trên) luôn là trang được tính, kể cả khi trang thấp hơn nửa màn. Đang zoom thì cửa sổ chỉ là
 * một phần của danh sách (toạ độ danh sách = toạ độ màn hình / zoom), nên tính theo đoạn đó chứ không theo cả vùng đọc.
 */
private fun mostVisiblePage(state: LazyListState, zoom: PdfZoomState): Int? {
    val info = state.layoutInfo
    val windowTop = -zoom.translationY / zoom.scale
    val windowBottom = windowTop + info.viewportSize.height / zoom.scale
    return info.visibleItemsInfo.maxByOrNull { item ->
        minOf(item.offset + item.size.toFloat(), windowBottom) - maxOf(item.offset.toFloat(), windowTop)
    }?.index
}

/**
 * Một trang. Chưa dựng xong thì là ô trắng đúng tỉ lệ ([PdfDocument.aspectOrDefault]); dựng xong thì tỉ lệ theo bitmap thật. [maxHeightPx] null
 * là cuộn dọc (rộng bằng chiều rộng màn), khác null là lật ngang (vừa trọn khung, căn giữa).
 *
 * [renderScale] > 1 là đang zoom: dựng bitmap lớn hơn chừng đó lần để chữ còn nét khi phóng, nhưng khung bố cục giữ nguyên (bitmap được
 * kéo vừa khung, lớp zoom phóng khung lên). Trong lúc bản nét hơn đang dựng thì vẫn hiện bản đang có (không nháy trắng).
 */
@Composable
private fun PdfPage(
    document: PdfDocument,
    index: Int,
    maxWidthPx: Int,
    maxHeightPx: Int?,
    renderScale: Int,
    modifier: Modifier = Modifier,
) {
    val renderWidth = maxWidthPx * renderScale
    val renderHeight = maxHeightPx?.let { it * renderScale }
    val maxBytes = if (renderScale > 1) PdfViewerConstants.MAX_ZOOMED_PAGE_BITMAP_BYTES else PdfViewerConstants.MAX_PAGE_BITMAP_BYTES
    var aspect by remember(document, index) { mutableFloatStateOf(document.aspectOrDefault(index)) }
    var bitmap by remember(document, index) {
        mutableStateOf(document.cached(index, renderWidth, renderHeight) ?: document.cached(index, maxWidthPx, maxHeightPx))
    }
    LaunchedEffect(document, index, renderWidth, renderHeight, maxBytes) {
        val rendered = document.cached(index, renderWidth, renderHeight)
            ?: document.render(index, renderWidth, renderHeight, maxBytes)
            ?: return@LaunchedEffect
        aspect = document.aspectOrDefault(index)
        bitmap = rendered
    }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val description = stringResource(R.string.pdf_page_description, index + 1, document.pageCount)
    val ratio = 1f / aspect
    Box(
        modifier = if (maxHeightPx == null) modifier.aspectRatio(ratio) else modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            (if (maxHeightPx == null) Modifier.fillMaxSize() else Modifier.aspectRatio(ratio))
                // Giấy trắng của tài liệu: token cố định, không đổi theo theme (L3).
                .background(ODVMediaColors.pdfPaper)
                .semantics { contentDescription = description },
        ) {
            image?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
        }
    }
}
