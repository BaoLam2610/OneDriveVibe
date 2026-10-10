package com.lambao.odv.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVFullScreenLoader
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVMaskToggle
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn Cập nhật Client Secret (CD-04, thiet-ke-ui.md mục 5.4, S1 đến S3). Luôn chặn chụp màn hình và ẩn ở danh sách app gần đây vì có
 * bí mật đang nhập (CH-05). [onBack] là nút quay lại; [onSaved] chạy khi đã lưu secret mới (app đóng form, Cài đặt hiện Snackbar S5).
 */
@Composable
fun ODVUpdateSecretScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpdateSecretViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ODVSecureWindow()
    // Đang kiểm tra thì không có Hủy (S2): chặn cả Back hệ thống.
    BackHandler(enabled = state.isChecking) {}
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            UpdateSecretEffect.Saved -> onSaved()
        }
    }
    ODVUpdateSecretContent(state = state, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}

@Composable
private fun ODVUpdateSecretContent(
    state: UpdateSecretState,
    onIntent: (UpdateSecretIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Box(modifier.fillMaxSize()) {
        ODVScaffold(
            topBar = {
                ODVAppBar(
                    title = stringResource(R.string.secret_title),
                    // Đang kiểm tra thì không có Hủy (S2): khóa luôn nút quay lại để không rời màn giữa chừng.
                    navigation = { ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.settings_back), onBack, enabled = !state.isChecking) },
                )
            },
        ) { contentPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.secret_description), style = type.body, color = colors.inkMuted)
                ODVTextField(
                    value = state.secret,
                    onValueChange = { onIntent(UpdateSecretIntent.SecretChanged(it)) },
                    label = stringResource(R.string.secret_label),
                    placeholder = stringResource(R.string.secret_placeholder),
                    errorText = state.failure?.let { stringResource(it.messageRes()) },
                    // S3: sau lỗi, dòng ghi chú đổi thành "Chưa lưu. OneDriveVibe vẫn dùng secret cũ."
                    helperText = stringResource(if (state.failure == null) R.string.secret_note else R.string.secret_error_not_saved),
                    enabled = !state.isChecking,
                    masked = !state.revealed,
                    mono = true,
                    maskToggle = ODVMaskToggle(
                        onToggle = { onIntent(UpdateSecretIntent.ToggleReveal) },
                        contentDescription = stringResource(if (state.revealed) R.string.secret_hide else R.string.secret_show),
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                ODVButton(
                    text = stringResource(R.string.secret_submit),
                    onClick = { onIntent(UpdateSecretIntent.Submit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSubmit,
                    fullWidth = true,
                )
            }
        }
        if (state.isChecking) {
            // S2: loading toàn màn hình, không có nút Hủy (KN-07).
            ODVFullScreenLoader(
                title = stringResource(R.string.secret_loading_title),
                subtitle = stringResource(R.string.secret_loading_subtitle),
                loadingDescription = stringResource(R.string.secret_loading_description),
            )
        }
    }
}

private fun SecretFailure.messageRes(): Int = when (this) {
    SecretFailure.Invalid -> R.string.secret_error_invalid
    SecretFailure.Expired -> R.string.secret_error_expired
    SecretFailure.Network -> R.string.secret_error_network
    SecretFailure.SaveFailed -> R.string.secret_error_save
    SecretFailure.Other -> R.string.secret_error_other
}
