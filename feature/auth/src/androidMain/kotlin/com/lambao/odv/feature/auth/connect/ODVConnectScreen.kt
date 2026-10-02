package com.lambao.odv.feature.auth.connect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn Kết nối (KN-01 → KN-11). Nối ViewModel với giao diện; [onContinue] chuyển sang bước Thiết lập bảo mật.
 * Chặn chụp màn hình và ẩn ở danh sách app gần đây (KN-10, CH-05).
 */
@Composable
fun ODVConnectScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequesters = remember { ConnectField.entries.associateWith { FocusRequester() } }

    ODVSecureWindow()
    // KN-03: app xuống nền thì che lại mọi ô đang hiện ký tự.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onIntent(ConnectIntent.HideAll) }
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ConnectEffect.FocusField -> focusRequesters.getValue(effect.field).requestFocus()
            ConnectEffect.NavigateToSecuritySetup -> onContinue()
        }
    }

    ODVConnectContent(
        state = state,
        focusRequesters = focusRequesters,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
