package com.lambao.odv.feature.browser

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.domain.model.DriveItem
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tab Thư mục (TM-01, TM-02, TM-07). Back quay lên một cấp thư mục; ở thư mục gốc thì Back thoát app như thường.
 * [onOpenFile] chạy khi chạm một tệp media; màn xem làm ở Lát 5–7 nên Lát 1 truyền hàm rỗng.
 */
@Composable
fun ODVBrowserScreen(
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem) -> Unit = {},
    viewModel: BrowserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is BrowserEffect.OpenFile -> onOpenFile(effect.item)
        }
    }
    BackHandler(enabled = state.path.isNotEmpty()) { viewModel.onIntent(BrowserIntent.GoUp) }

    ODVBrowserContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}
