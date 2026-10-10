package com.lambao.odv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn Cài đặt (CD, thiet-ke-ui.md mục 5.4). **Không** tự đặt `FLAG_SECURE` (đổi 2026-10-08, người dùng chốt; trước đây luôn chặn theo
 * CH-05): chụp được khi tắt "Bảo vệ màn hình", còn bật thì chặn nhờ `ODVSecureWindowPolicy.appWide` (CD-12) như mọi màn khác. Đánh đổi đã
 * chấp nhận: thông tin kết nối đã che bớt (Tenant ID, Client ID) chụp được. Vì cờ này theo cả cửa sổ chứ không theo màn nên DH-08 (áp lại khi
 * đổi tab) đã thỏa: không cần xử lý riêng theo tab. Từ Lát 8 Cài đặt là một tab của thanh điều hướng đáy (ADR-0023), không còn nút quay lại;
 * [reselectSignal] tăng khi người dùng chạm lại tab này (DH-04): cuộn lên đầu. [onOpenSecuritySetup] mở luồng thiết lập PIN khi bật bảo vệ
 * (CD-02) và [onOpenPin] mở màn PIN cho một thao tác nhạy cảm (CD-03, CD-08, CD-09, CD-04); [onOpenSecretForm] mở form cập nhật Client Secret khi bảo vệ tắt; [onDisconnected] chạy sau khi ngắt kết nối xong (CD-05: về Kết nối với back stack sạch); app sở hữu back stack nên điều hướng đi qua đây.
 */
@Composable
fun ODVSettingsScreen(
    onOpenSecuritySetup: () -> Unit,
    onOpenPin: (PinPurpose) -> Unit,
    onOpenSecretForm: () -> Unit,
    onDisconnected: () -> Unit,
    modifier: Modifier = Modifier,
    reselectSignal: Int = 0,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.secret_saved)
    // Màn được dựng lại khi quay về từ màn thiết lập hoặc màn PIN (ViewModel còn sống): đọc lại trạng thái sinh trắc học.
    LaunchedEffect(Unit) { viewModel.onIntent(SettingsIntent.Refresh) }
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.OpenSecuritySetup -> onOpenSecuritySetup()
            is SettingsEffect.OpenPin -> onOpenPin(effect.purpose)
            SettingsEffect.OpenSecretForm -> onOpenSecretForm()
            SettingsEffect.Disconnected -> onDisconnected()
            // showSnackbar chờ tới khi Snackbar tắt, nên chạy ở scope riêng để không chặn các effect khác.
            SettingsEffect.ShowSecretSaved -> scope.launch { snackbarHost.showSnackbar(savedMessage) }
        }
    }
    ODVSettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHost = snackbarHost,
        modifier = modifier,
        reselectSignal = reselectSignal,
    )
}
