package com.lambao.odv.feature.browser

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ViewerContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tab Thư mục (TM-01 → TM-07, DS-03 → DS-06). Back đóng thanh tìm nếu đang tìm, nếu không thì quay lên một cấp thư mục;
 * ở thư mục gốc thì Back thoát app như thường.
 * [onOpenFile] chạy khi chạm một tệp media. [banner] là banner secret sắp hết hạn do Màn chính dựng (D9). [reselectSignal] tăng khi
 * người dùng chạm lại tab Thư mục trên thanh điều hướng đáy (DH-04). Từ Lát 8 không còn Tabs và nút bánh răng (ADR-0023).
 */
@Composable
fun ODVBrowserScreen(
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem, ViewerContext) -> Unit = { _, _ -> },
    banner: @Composable () -> Unit = {},
    reselectSignal: Int = 0,
    viewModel: BrowserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is BrowserEffect.OpenFile -> onOpenFile(effect.item, effect.context)
        }
    }
    val searching = state.search != null
    BackHandler(enabled = searching || state.path.isNotEmpty()) {
        viewModel.onIntent(if (searching) BrowserIntent.CloseSearch else BrowserIntent.GoUp)
    }

    ODVBrowserContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        banner = banner,
        reselectSignal = reselectSignal,
    )
}
