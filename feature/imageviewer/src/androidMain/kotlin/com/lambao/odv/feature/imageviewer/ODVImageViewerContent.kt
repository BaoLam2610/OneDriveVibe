package com.lambao.odv.feature.imageviewer

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVSidePanel
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVViewerTopBar
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.repository.OriginalImageState
import kotlinx.coroutines.flow.Flow

/**
 * Nội dung màn xem ảnh (A1 → A5): nền đen, Pager ngang các ảnh cùng ngữ cảnh (AN-03), thanh trên có nút Thông tin (AN-05),
 * bộ đếm "12 / 248" dưới đáy. Chạm một lần ẩn/hiện cả thanh trên, bộ đếm và system bar (AN-04). Khi ảnh đang zoom thì khóa
 * vuốt ngang của Pager (AN-03) và bộ đếm nhường chỗ cho gợi ý "Chạm đúp để về vừa khung".
 */
@Composable
internal fun ODVImageViewerContent(
    state: ImageViewerState,
    originalOf: (DriveItem) -> Flow<OriginalImageState>,
    onIntent: (ImageViewerIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ODVViewerSystemBars(hidden = !state.controlsVisible)
    BackHandler(enabled = state.info != null) { onIntent(ImageViewerIntent.HideInfo) }

    Box(modifier.fillMaxSize().background(ODVMediaColors.background)) {
        val images = state.images
        if (images.isEmpty()) {
            if (!state.isLoaded) {
                Box(Modifier.align(Alignment.Center)) {
                    ODVSpinner(size = 36.dp, color = ODVMediaColors.accent, trackColor = ODVMediaColors.track)
                }
            }
            return@Box
        }
        ViewerPager(state, images, originalOf, onIntent, onBack)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ViewerPager(
    state: ImageViewerState,
    images: List<DriveItem>,
    originalOf: (DriveItem) -> Flow<OriginalImageState>,
    onIntent: (ImageViewerIntent) -> Unit,
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = state.initialIndex.coerceIn(0, images.lastIndex)) { images.size }
    val latestImages by rememberUpdatedState(images)
    var zoomedIds by remember { mutableStateOf(emptySet<String>()) }

    // Ảnh đang nằm ở trang đã dừng, theo chính Pager chứ không theo ViewModel: sau khi tiến trình bị thu hồi, ViewModel mới
    // chỉ biết ảnh mở đầu còn Pager khôi phục đúng trang đã xem, nên lấy ViewModel làm mốc sẽ kéo người xem về ảnh đầu.
    var anchorId by remember { mutableStateOf<String?>(null) }
    // Đồng bộ thêm/xóa ảnh làm đổi danh sách: giữ nguyên ảnh đang xem thay vì để trang trượt sang ảnh khác.
    LaunchedEffect(images) {
        val index = images.indexOfFirst { it.id == anchorId }
        if (index >= 0 && index != pagerState.currentPage && !pagerState.isScrollInProgress) pagerState.scrollToPage(index)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            latestImages.getOrNull(page)?.let {
                anchorId = it.id
                onIntent(ImageViewerIntent.PageChanged(it.id))
            }
        }
    }

    val current = images.getOrNull(pagerState.currentPage)
    val zoomed = current != null && current.id in zoomedIds

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // AN-03: khi đang zoom, vuốt ngang chỉ di chuyển ảnh.
        userScrollEnabled = !zoomed,
        beyondViewportPageCount = 0,
        key = { images.getOrNull(it)?.id ?: it },
    ) { page ->
        val item = images.getOrNull(page) ?: return@HorizontalPager
        ImagePage(
            item = item,
            originalOf = originalOf,
            onTap = { onIntent(ImageViewerIntent.ToggleControls) },
            onZoomedChange = { isZoomed -> zoomedIds = if (isZoomed) zoomedIds + item.id else zoomedIds - item.id },
        )
    }

    val fade = tween<Float>(ODVDuration.fast)
    AnimatedVisibility(
        visible = state.controlsVisible,
        modifier = Modifier.align(Alignment.TopCenter),
        enter = fadeIn(fade),
        exit = fadeOut(fade),
    ) {
        // Gradient đen phủ cả vùng status bar để icon hệ thống đọc được trên ảnh sáng; thanh nằm dưới status bar.
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(ODVMediaColors.scrimStrong, Color.Transparent)))
                .statusBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            ODVViewerTopBar(
                title = current?.name.orEmpty(),
                backContentDescription = stringResource(R.string.image_viewer_back),
                onBack = onBack,
                actions = {
                    ODVIconButton(
                        ODVIcon.Info,
                        stringResource(R.string.image_viewer_info),
                        { onIntent(ImageViewerIntent.ShowInfo) },
                        colors = ODVMediaDefaults.iconButtonColors(),
                    )
                },
            )
        }
    }
    AnimatedVisibility(
        visible = state.controlsVisible && !zoomed,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = fadeIn(fade),
        exit = fadeOut(fade),
    ) {
        ODVViewerPill(
            text = stringResource(R.string.image_viewer_counter, pagerState.currentPage + 1, images.size),
            mono = true,
            modifier = Modifier.navigationBarsPadding().padding(bottom = 32.dp),
        )
    }

    state.info?.let { info ->
        val title = stringResource(R.string.image_info_title)
        val close = { onIntent(ImageViewerIntent.HideInfo) }
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Hướng ngang: bảng bên phải rộng 360 thay cho bottom sheet (thiet-ke-ui.md mục 4.5).
            ODVSidePanel(title, stringResource(R.string.image_info_close), close) { ImageInfoContent(info) }
        } else {
            ODVBottomSheet(onDismissRequest = close, title = title) { ImageInfoContent(info) }
        }
    }
}
