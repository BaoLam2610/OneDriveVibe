package com.lambao.odv.feature.auth.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn Thiết lập bảo mật (BM-01 → BM-07). Lát 1 chỉ có nhánh tắt bảo vệ.
 * [onFinished] vào Danh sách sau khi config đã lưu; [onMissingConnection] quay về Kết nối khi không còn config chờ lưu.
 * Chặn chụp màn hình và ẩn ở danh sách app gần đây (CH-05).
 */
@Composable
fun ODVSecuritySetupScreen(
    onFinished: () -> Unit,
    onMissingConnection: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecuritySetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ODVSecureWindow()
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SecuritySetupEffect.NavigateToHome -> onFinished()
            SecuritySetupEffect.NavigateToConnect -> onMissingConnection()
        }
    }

    ODVSecuritySetupContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}
