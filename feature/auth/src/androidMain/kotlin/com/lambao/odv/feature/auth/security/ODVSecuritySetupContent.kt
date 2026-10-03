package com.lambao.odv.feature.auth.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVFullScreenLoader
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVKeypad
import com.lambao.odv.core.designsystem.component.ODVPinDots
import com.lambao.odv.core.designsystem.component.ODVPinDotsState
import com.lambao.odv.core.designsystem.component.ODVPinMessage
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVShakeState
import com.lambao.odv.core.designsystem.component.odvShake
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.feature.auth.R

/**
 * Giao diện màn Thiết lập bảo mật (thiet-ke-ui.md mục 5.1, B1 → B8): mũi tên Back cố định ở topBar (BM-08), icon `lock`
 * 40 màu `volt-text`, tiêu đề `display`, khối PIN (tiêu đề `heading`, dãy chấm, dòng thông báo) rồi bàn phím số tự vẽ sát
 * đáy (BM-07). Không có StepBar và không có thẻ công tắc "Bảo vệ ứng dụng" (BM-01). B8 là loader toàn màn.
 *
 * Back: ở bước nhập lại thì về bước đặt PIN; ở bước đặt PIN thì [onBack] (quay lại hộp thoại KN-13); đang hoàn tất thì khóa.
 */
@Composable
internal fun ODVSecuritySetupContent(
    state: SetupUiState,
    shake: ODVShakeState,
    onIntent: (SetupIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val isFinishing = state.step == SetupStep.Finishing

    val goBack: () -> Unit = {
        if (state.step == SetupStep.Confirm) onIntent(SetupIntent.BackToCreate) else onBack()
    }
    // BM-04: đang mã hóa lại config thì khóa mọi thao tác, kể cả Back. Ở bước nhập lại, Back về bước đặt PIN.
    BackHandler(enabled = isFinishing) {}
    BackHandler(enabled = state.step == SetupStep.Confirm) { onIntent(SetupIntent.BackToCreate) }

    Box(modifier.fillMaxSize()) {
        ODVScaffold(
            topBar = {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp)) {
                    ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.security_back), goBack, enabled = !isFinishing)
                }
            },
        ) { contentPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ODVIcon(ODVIcon.Lock, contentDescription = null, tint = colors.voltText, size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.security_title), style = type.display, color = colors.ink)
                    Text(stringResource(R.string.security_subtitle), style = type.body, color = colors.inkMuted)
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(if (state.step == SetupStep.Confirm) R.string.security_confirm_title else R.string.security_create_title),
                        style = type.heading,
                        color = colors.ink,
                    )
                    ODVPinDots(
                        filled = state.entered,
                        contentDescription = stringResource(R.string.lock_pin_description, state.entered),
                        modifier = Modifier.odvShake(shake),
                        state = if (state.error != null) ODVPinDotsState.Error else ODVPinDotsState.Default,
                    )
                    ODVPinMessage(
                        text = when (state.error) {
                            SetupError.Weak -> stringResource(R.string.security_weak)
                            SetupError.Mismatch -> stringResource(R.string.security_mismatch)
                            null -> stringResource(R.string.security_hint)
                        },
                        isError = state.error != null,
                    )
                }
                // BM-07: bàn phím số tự vẽ, không dùng bàn phím hệ thống để bàn phím bên thứ ba không ghi nhận được PIN.
                ODVKeypad(
                    onDigit = { onIntent(SetupIntent.Digit(it)) },
                    onBackspace = { onIntent(SetupIntent.Backspace) },
                    backspaceLabel = stringResource(R.string.lock_backspace),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !isFinishing,
                )
            }
        }

        if (isFinishing) {
            ODVFullScreenLoader(
                title = stringResource(R.string.security_saving_title),
                loadingDescription = stringResource(R.string.security_saving_description),
            )
        }
    }

    if (state.saveFailed) {
        ODVDialog(
            onDismissRequest = { onIntent(SetupIntent.DismissSaveFailure) },
            title = stringResource(R.string.connect_save_failed_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Danger,
            body = stringResource(R.string.connect_save_failed_body),
            alert = true,
        ) {
            ODVButton(stringResource(R.string.connect_error_close), { onIntent(SetupIntent.DismissSaveFailure) })
        }
    }
}
