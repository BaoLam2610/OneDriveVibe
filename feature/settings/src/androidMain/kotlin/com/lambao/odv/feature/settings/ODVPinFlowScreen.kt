package com.lambao.odv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import com.lambao.odv.core.designsystem.component.rememberODVHaptics
import com.lambao.odv.core.designsystem.component.rememberODVShakeState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Màn nhập PIN trong Cài đặt (P1 → P3): xác nhận PIN hiện tại cho [purpose], và nhập PIN mới khi đổi PIN (CD-03, CD-08, CD-09).
 * Chặn chụp màn hình và ẩn ở danh sách app gần đây (CH-05). [onDone] chạy khi xong việc hoặc khi hủy ở hộp thoại tắt bảo vệ;
 * [onDisconnected] khi nhập sai quá nhiều và dữ liệu đã bị xóa (KH-06); [onBack] là nút quay lại.
 */
@Composable
fun ODVPinFlowScreen(
    purpose: PinPurpose,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onDisconnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinFlowViewModel = koinViewModel(parameters = { parametersOf(purpose) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shake = rememberODVShakeState()
    val haptics = rememberODVHaptics()

    ODVSecureWindow()
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PinFlowEffect.Shake -> {
                haptics.reject()
                shake.shake()
            }
        }
    }
    LaunchedEffect(state.completion) {
        when (state.completion) {
            PinCompletion.Done -> onDone()
            PinCompletion.Disconnected -> onDisconnected()
            null -> Unit
        }
    }

    ODVPinFlowContent(state = state, shake = shake, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}
