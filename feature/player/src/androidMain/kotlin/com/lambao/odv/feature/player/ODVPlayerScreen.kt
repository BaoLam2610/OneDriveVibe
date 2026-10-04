package com.lambao.odv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.domain.model.ViewerContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Màn xem video (VD). [context] cho biết danh sách phát (cùng thư mục hoặc cùng bộ lọc Thư viện, VD-10), [startItemId] là
 * video được chạm. [onBack] đóng màn (nút quay lại, hoặc khi không còn video nào).
 */
@Composable
fun ODVPlayerScreen(
    context: ViewerContext,
    startItemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(parameters = { parametersOf(context, startItemId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PlayerEffect.Close -> onBack()
        }
    }

    ODVPlayerContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}
