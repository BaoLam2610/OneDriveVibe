package com.lambao.odv.feature.library

import androidx.activity.compose.BackHandler
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
 * Tab Thư viện (TV-01 → TV-06). [onOpenFile] chạy khi chạm một ô. [onShowFolders] là lối tắt sang tab Thư mục ở banner "Đang lập
 * chỉ mục" (TV-06). [banner] là banner secret sắp hết hạn do Màn chính dựng (D9). [accountButton] là nút tài khoản trên AppBar do Màn
 * chính dựng (TA-03). [reselectSignal] tăng khi người dùng chạm lại tab
 * Thư viện trên thanh điều hướng đáy (DH-04). Từ Lát 8 không còn Tabs và nút bánh răng (ADR-0023).
 */
@Composable
fun ODVLibraryScreen(
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem, ViewerContext) -> Unit = { _, _ -> },
    onShowFolders: () -> Unit = {},
    banner: @Composable () -> Unit = {},
    accountButton: @Composable () -> Unit = {},
    reselectSignal: Int = 0,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pages = viewModel.pages.collectAsLazyPagingItems()
    // Kết quả tìm (TV-07). Thu liên tục: khi không tìm, luồng này là PagingData rỗng nên không tốn gì.
    val searchPages = viewModel.searchPages.collectAsLazyPagingItems()

    // Đang tìm thì Back đóng tìm kiếm trước; đứng sau BackHandler của Màn chính (DH-03) nên được xử lý trước nó.
    BackHandler(enabled = state.search != null) { viewModel.onIntent(LibraryIntent.CloseSearch) }

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is LibraryEffect.OpenFile -> onOpenFile(effect.item, effect.context)
        }
    }

    ODVLibraryContent(
        state = state,
        pages = pages,
        searchPages = searchPages,
        onIntent = viewModel::onIntent,
        onShowFolders = onShowFolders,
        modifier = modifier,
        banner = banner,
        accountButton = accountButton,
        reselectSignal = reselectSignal,
    )
}
