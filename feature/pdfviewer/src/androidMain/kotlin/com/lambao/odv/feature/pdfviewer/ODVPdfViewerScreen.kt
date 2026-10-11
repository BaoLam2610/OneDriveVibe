package com.lambao.odv.feature.pdfviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Màn xem PDF (PD-01 → PD-08). [itemId] là tệp được chạm; [onBack] đóng màn (nút quay lại, Hủy khi đang tải, hoặc khi tệp không còn).
 * Trang đang đọc được ghi ngay khi app xuống nền hoặc rời màn, ngoài lần ghi gộp lúc lật trang (PD-05).
 */
@Composable
fun ODVPdfViewerScreen(
    itemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfViewerViewModel = koinViewModel(parameters = { parametersOf(itemId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Yêu cầu cuộn/lật tới một trang: vùng đọc được dựng lại khi đổi kiểu đọc nên nhận qua kênh chứ không giữ trong State.
    val scrollRequests = remember { Channel<Int>(Channel.UNLIMITED) }
    val scrollFlow = remember(scrollRequests) { scrollRequests.receiveAsFlow() }

    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PdfViewerEffect.Close -> onBack()
            is PdfViewerEffect.ScrollTo -> scrollRequests.trySend(effect.page)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onIntent(PdfViewerIntent.Persist) }
    DisposableEffect(viewModel) { onDispose { viewModel.onIntent(PdfViewerIntent.Persist) } }

    ODVPdfViewerContent(
        state = state,
        scrollRequests = scrollFlow,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}
