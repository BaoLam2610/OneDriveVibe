package com.lambao.odv.feature.auth.lock

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

/**
 * Màn Khóa (KH-01 → KH-06). [onUnlocked] bỏ màn Khóa để thấy lại màn đang xem; [onDisconnected] về màn Kết nối sau khi
 * dữ liệu đã bị xóa (Quên mã PIN hoặc sai quá nhiều). Cả hai gọi từ `State.completion` nên không mất khi xoay màn hình.
 * Chặn chụp màn hình và ẩn ở danh sách app gần đây (KH-04, CH-05).
 */
@Composable
fun ODVLockScreen(
    onUnlocked: () -> Unit,
    onDisconnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shake = rememberODVShakeState()
    val haptics = rememberODVHaptics()

    ODVSecureWindow()
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            LockEffect.Shake -> {
                haptics.reject()
                shake.shake()
            }
        }
    }
    LaunchedEffect(state.completion) {
        when (state.completion) {
            LockCompletion.Unlocked -> onUnlocked()
            LockCompletion.Disconnected -> onDisconnected()
            null -> Unit
        }
    }

    ODVLockContent(state = state, shake = shake, onIntent = viewModel::onIntent, modifier = modifier)
}
