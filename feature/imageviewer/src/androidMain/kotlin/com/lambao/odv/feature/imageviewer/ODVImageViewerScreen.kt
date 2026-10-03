package com.lambao.odv.feature.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.domain.model.ViewerContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Màn xem ảnh (AN-01 → AN-07). [context] cho biết ảnh nào để vuốt trước/sau (cùng thư mục hoặc cùng bộ lọc Thư viện),
 * [startItemId] là ảnh được chạm. [onBack] đóng màn (nút quay lại, hoặc khi không còn ảnh nào).
 */
@Composable
fun ODVImageViewerScreen(
    context: ViewerContext,
    startItemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageViewerViewModel = koinViewModel(parameters = { parametersOf(context, startItemId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            ImageViewerEffect.Close -> onBack()
        }
    }

    ODVImageViewerContent(
        state = state,
        originalOf = viewModel::originalOf,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}
