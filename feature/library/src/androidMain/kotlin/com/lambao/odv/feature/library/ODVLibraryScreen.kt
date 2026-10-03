package com.lambao.odv.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ViewerContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tab Thư viện (TV-01 → TV-06). [onOpenFile] chạy khi chạm một ô; màn xem làm ở Lát 5–6 nên nơi gọi có thể truyền hàm rỗng.
 * [onShowFolders] là lối tắt sang tab Thư mục ở banner "Đang lập chỉ mục" (TV-06). [tabs] là thanh Tabs Thư mục/Thư viện
 * do màn chứa (Home) dựng.
 */
@Composable
fun ODVLibraryScreen(
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem, ViewerContext) -> Unit = { _, _ -> },
    onShowFolders: () -> Unit = {},
    tabs: @Composable () -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pages = viewModel.pages.collectAsLazyPagingItems()

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is LibraryEffect.OpenFile -> onOpenFile(effect.item, effect.context)
        }
    }

    ODVLibraryContent(
        state = state,
        pages = pages,
        onIntent = viewModel::onIntent,
        onShowFolders = onShowFolders,
        modifier = modifier,
        tabs = tabs,
    )
}
