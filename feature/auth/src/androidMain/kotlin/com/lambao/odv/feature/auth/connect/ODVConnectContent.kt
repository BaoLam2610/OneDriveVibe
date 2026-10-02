package com.lambao.odv.feature.auth.connect

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVFullScreenLoader
import com.lambao.odv.core.designsystem.component.ODVMaskToggle
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.feature.auth.R

/**
 * Giao diện màn Kết nối (thiet-ke-ui.md mục 5.1): padding 48/16/32; header CỐ ĐỊNH (mark 40) nằm ở topBar của ODVScaffold nên không cuộn; tiêu đề `display`,
 * form cách đầu trang 28 gồm 4 ô cách nhau 16, nút "Kết nối" toàn chiều rộng sát đáy.
 * Không giữ state; cả dialog lỗi (K5) và hộp thoại hỏi thiết lập PIN (K6) đều dựng từ [state].
 */
@Composable
internal fun ODVConnectContent(
    state: ConnectState,
    focusRequesters: Map<ConnectField, FocusRequester>,
    onIntent: (ConnectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography

    // KN-07: đang kết nối thì khóa mọi thao tác, kể cả nút Back (spec không có nút Hủy).
    BackHandler(enabled = state.isConnecting) {}

    Box(modifier.fillMaxSize()) {
        ODVScaffold(
            // Header cố định: Logo không cuộn theo form. Không có StepBar (thiet-ke-ui.md mục 4.1: luồng có nhánh).
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ODVLogo(size = 40.dp)
                }
            },
        ) { contentPadding ->
            val keyboardBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val bottom = maxOf(contentPadding.calculateBottomPadding(), keyboardBottom)
            Column(Modifier.fillMaxSize().padding(bottom = bottom)) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp),
                ) {
                    Text(
                        stringResource(R.string.connect_title),
                        modifier = Modifier.padding(top = 4.dp),
                        style = type.display,
                        color = colors.ink,
                    )
                    Text(
                        stringResource(R.string.connect_subtitle),
                        modifier = Modifier.padding(top = 8.dp),
                        style = type.body,
                        color = colors.inkMuted,
                    )
                    Column(
                        Modifier.padding(top = 28.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SensitiveField(
                            field = ConnectField.TenantId,
                            state = state,
                            label = stringResource(R.string.connect_tenant_label),
                            helper = stringResource(R.string.connect_tenant_helper),
                            error = stringResource(R.string.connect_tenant_error),
                            focusRequester = focusRequesters.getValue(ConnectField.TenantId),
                            onIntent = onIntent,
                        )
                        SensitiveField(
                            field = ConnectField.ClientId,
                            state = state,
                            label = stringResource(R.string.connect_client_label),
                            helper = stringResource(R.string.connect_client_helper),
                            error = stringResource(R.string.connect_client_error),
                            focusRequester = focusRequesters.getValue(ConnectField.ClientId),
                            onIntent = onIntent,
                            // Riêng Client ID khi hiện ký tự dùng JetBrains Mono (mục 4.1).
                            mono = ConnectField.ClientId in state.revealed,
                        )
                        SensitiveField(
                            field = ConnectField.ClientSecret,
                            state = state,
                            label = stringResource(R.string.connect_secret_label),
                            helper = stringResource(R.string.connect_secret_helper),
                            error = null,
                            focusRequester = focusRequesters.getValue(ConnectField.ClientSecret),
                            onIntent = onIntent,
                        )
                        ODVTextField(
                            value = state.upn,
                            onValueChange = { onIntent(ConnectIntent.FieldChanged(ConnectField.Upn, it)) },
                            label = stringResource(R.string.connect_upn_label),
                            modifier = Modifier.focusRequester(focusRequesters.getValue(ConnectField.Upn)),
                            helperText = stringResource(R.string.connect_upn_helper),
                            errorText = if (state.hasFormatError(ConnectField.Upn)) stringResource(R.string.connect_upn_error) else null,
                            enabled = !state.isConnecting,
                            // KN-04: không gợi ý, không học từ cá nhân.
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                }
                ODVButton(
                    text = stringResource(R.string.connect_button),
                    onClick = { onIntent(ConnectIntent.Connect) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    enabled = state.canConnect,
                    fullWidth = true,
                )
            }
        }

        if (state.isConnecting) {
            ODVFullScreenLoader(
                title = stringResource(R.string.connect_loading_title),
                subtitle = stringResource(R.string.connect_loading_subtitle),
                loadingDescription = stringResource(R.string.connect_loading_description),
            )
        }
    }

    state.failure?.let { failure ->
        ConnectFailureDialog(failure, onIntent)
    }
    if (state.showPinPrompt) {
        PinPromptDialog(onIntent)
    }
}

/** Ô nhập của Tenant ID, Client ID, Client Secret: che ký tự, có nút con mắt (KN-01, KN-03) và gõ không gợi ý (KN-04). */
@Composable
private fun SensitiveField(
    field: ConnectField,
    state: ConnectState,
    label: String,
    helper: String,
    error: String?,
    focusRequester: FocusRequester,
    onIntent: (ConnectIntent) -> Unit,
    mono: Boolean = false,
) {
    val revealed = field in state.revealed
    ODVTextField(
        value = state.value(field),
        onValueChange = { onIntent(ConnectIntent.FieldChanged(field, it)) },
        label = label,
        modifier = Modifier.focusRequester(focusRequester),
        helperText = helper,
        errorText = if (error != null && state.hasFormatError(field)) error else null,
        enabled = !state.isConnecting,
        mono = mono,
        masked = !revealed,
        maskToggle = ODVMaskToggle(
            onToggle = { onIntent(ConnectIntent.ToggleReveal(field)) },
            contentDescription = stringResource(if (revealed) R.string.connect_hide_value else R.string.connect_show_value),
        ),
        // KeyboardType.Password: bàn phím không gợi ý, không học từ. autoCorrect tắt thêm cho chắc (KN-04).
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next,
        ),
    )
}

/** K5: Dialog danger. Lỗi do Client Secret có thêm nút đi thẳng tới ô Client Secret. */
@Composable
private fun ConnectFailureDialog(failure: ConnectFailure, onIntent: (ConnectIntent) -> Unit) {
    ODVDialog(
        onDismissRequest = { onIntent(ConnectIntent.DismissFailure) },
        title = stringResource(
            if (failure.kind == ConnectFailureKind.SaveFailed) R.string.connect_save_failed_title else R.string.connect_error_title,
        ),
        icon = ODVIcon.Alert,
        tone = ODVDialogTone.Danger,
        body = stringResource(failure.kind.messageRes()),
        errorCode = failure.code?.let { stringResource(R.string.connect_error_code, it) },
        alert = true,
    ) {
        if (failure.isSecretProblem) {
            ODVButton(
                stringResource(R.string.connect_error_close),
                { onIntent(ConnectIntent.DismissFailure) },
                style = ODVButtonStyle.Ghost,
            )
            ODVButton(stringResource(R.string.connect_error_fix_secret), { onIntent(ConnectIntent.EditSecret) })
        } else {
            ODVButton(stringResource(R.string.connect_error_close), { onIntent(ConnectIntent.DismissFailure) })
        }
    }
}

private fun ConnectFailureKind.messageRes(): Int = when (this) {
    ConnectFailureKind.TenantFormat -> R.string.connect_error_tenant_format
    ConnectFailureKind.TenantNotFound -> R.string.connect_error_tenant_not_found
    ConnectFailureKind.ClientNotFound -> R.string.connect_error_client_not_found
    ConnectFailureKind.SecretInvalid -> R.string.connect_error_secret_invalid
    ConnectFailureKind.SecretExpired -> R.string.connect_error_secret_expired
    ConnectFailureKind.AppDisabled -> R.string.connect_error_app_disabled
    ConnectFailureKind.Forbidden -> R.string.connect_error_forbidden
    ConnectFailureKind.DriveNotFound -> R.string.connect_error_drive_not_found
    ConnectFailureKind.Network -> R.string.connect_error_network
    ConnectFailureKind.SaveFailed -> R.string.connect_save_failed_body
    ConnectFailureKind.Other -> R.string.connect_error_other
}

/**
 * K6 (KN-13): Dialog bắt buộc chọn (thiet-ke-ui.md mục 4.2). Chạm ngoài và Back hệ thống không đóng, chỉ đóng bằng
 * một trong hai nút. Câu chữ tự viết, chờ xác nhận (thiet-ke-ui.md mục 9, đề xuất 7).
 */
@Composable
private fun PinPromptDialog(onIntent: (ConnectIntent) -> Unit) {
    ODVDialog(
        onDismissRequest = {},
        title = stringResource(R.string.connect_pin_prompt_title),
        icon = ODVIcon.Lock,
        body = stringResource(R.string.connect_pin_prompt_body),
        dismissOnOutsideClick = false,
        dismissOnBackPress = false,
    ) {
        ODVButton(
            stringResource(R.string.connect_pin_prompt_later),
            { onIntent(ConnectIntent.SkipPin) },
            style = ODVButtonStyle.Ghost,
        )
        ODVButton(stringResource(R.string.connect_pin_prompt_setup), { onIntent(ConnectIntent.SetupPin) })
    }
}
