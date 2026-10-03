package com.lambao.odv.feature.auth.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVKeypad
import com.lambao.odv.core.designsystem.component.ODVKeypadAction
import com.lambao.odv.core.designsystem.component.ODVPinDots
import com.lambao.odv.core.designsystem.component.ODVPinDotsState
import com.lambao.odv.core.designsystem.component.ODVPinMessage
import com.lambao.odv.core.designsystem.component.ODVProgressBar
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVShakeState
import com.lambao.odv.core.designsystem.component.odvShake
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.feature.auth.R

/**
 * Giao diện màn Khóa (thiet-ke-ui.md mục 5.1, L1 → L8): mark 48 và tiêu đề `screen-title`, dãy chấm PIN và dòng thông báo,
 * bàn phím số tự vẽ sát đáy (BM-07, KH-05: vị trí phím cố định) rồi nút ghost "Quên mã PIN". Khi bị phạt (KH-02) dãy chấm
 * được thay bằng đồng hồ `timer`, thanh tiến độ rộng 220 và ghi chú; bàn phím mờ và không nhận chạm; ẩn phím sinh trắc học.
 * Không hiện tên tệp hay nội dung nào của OneDrive.
 */
@Composable
internal fun ODVLockContent(
    state: LockUiState,
    shake: ODVShakeState,
    onIntent: (LockIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography

    ODVScaffold(modifier = modifier) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ODVLogo(size = 48.dp)
            Text(
                stringResource(R.string.lock_title),
                modifier = Modifier.padding(top = 16.dp),
                style = type.screenTitle,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            if (state.isCoolingDown) {
                CooldownBlock(state)
            } else {
                PinBlock(state, shake)
            }
            Spacer(Modifier.weight(1f))
            ODVKeypad(
                onDigit = { onIntent(LockIntent.Digit(it)) },
                onBackspace = { onIntent(LockIntent.Backspace) },
                backspaceLabel = stringResource(R.string.lock_backspace),
                // L1/L2: phím sinh trắc học ở góc trái dưới khi đã bật; ẩn hẳn khi bị khóa tạm (KH-02, đã chốt).
                secondary = if (state.biometricEnabled && !state.isCoolingDown) {
                    val label = stringResource(R.string.lock_biometric)
                    ODVKeypadAction(ODVIcon.Fingerprint, label) { onIntent(LockIntent.UseBiometric) }
                } else {
                    null
                },
                enabled = !state.isCoolingDown && !state.isBusy,
            )
            ODVButton(
                text = stringResource(R.string.lock_forgot),
                onClick = { onIntent(LockIntent.ForgotClicked) },
                modifier = Modifier.padding(top = 8.dp),
                style = ODVButtonStyle.Ghost,
                enabled = !state.isBusy,
            )
        }
    }

    when (state.forgot) {
        ForgotStep.None -> Unit
        ForgotStep.Warn -> ODVDialog(
            onDismissRequest = { onIntent(LockIntent.ForgotDismiss) },
            title = stringResource(R.string.lock_forgot_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Warning,
            body = stringResource(R.string.lock_forgot_body),
        ) {
            ODVButton(stringResource(R.string.lock_cancel), { onIntent(LockIntent.ForgotDismiss) }, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.lock_continue), { onIntent(LockIntent.ForgotContinue) })
        }
        ForgotStep.Confirm -> ODVDialog(
            onDismissRequest = { onIntent(LockIntent.ForgotDismiss) },
            title = stringResource(R.string.lock_forgot_confirm_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Danger,
            body = stringResource(R.string.lock_forgot_confirm_body),
            alert = true,
        ) {
            ODVButton(stringResource(R.string.lock_cancel), { onIntent(LockIntent.ForgotDismiss) }, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.lock_forgot_confirm_action), { onIntent(LockIntent.ForgotConfirm) }, style = ODVButtonStyle.DangerSolid)
        }
    }
}

/** Dãy chấm và dòng thông báo (L1, L3, L4). Lỗi dùng cả màu, icon và chữ, không chỉ đổi màu (mục 4.3). */
@Composable
private fun PinBlock(state: LockUiState, shake: ODVShakeState) {
    val attemptsLeft = state.attemptsBeforeWipe
    val message = when {
        state.error == LockError.StorageFailed -> stringResource(R.string.lock_storage_failed)
        attemptsLeft != null && (state.error != null || state.entered == 0) -> stringResource(R.string.lock_attempts_left, attemptsLeft)
        state.error == LockError.WrongPin -> stringResource(R.string.lock_wrong_pin)
        state.isBusy -> stringResource(R.string.lock_unlocking)
        else -> ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ODVPinDots(
            filled = state.entered,
            contentDescription = stringResource(R.string.lock_pin_description, state.entered),
            modifier = Modifier.odvShake(shake),
            state = if (state.error != null) ODVPinDotsState.Error else ODVPinDotsState.Default,
        )
        ODVPinMessage(text = message, isError = state.error != null || (attemptsLeft != null && state.entered == 0))
    }
}

/** Khóa tạm (L5, L6): đồng hồ `timer` "mm:ss", thanh tiến độ rộng 220 và ghi chú `caption`. */
@Composable
private fun CooldownBlock(state: LockUiState) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(formatCountdown(state.cooldownRemainingMs), style = type.timer, color = colors.ink)
        ODVProgressBar(
            progress = if (state.cooldownTotalMs > 0) state.cooldownRemainingMs.toFloat() / state.cooldownTotalMs else 0f,
            modifier = Modifier.width(220.dp),
        )
        Text(
            stringResource(R.string.lock_cooldown_note),
            style = type.caption,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        state.attemptsBeforeWipe?.let { left ->
            ODVPinMessage(stringResource(R.string.lock_attempts_left, left), isError = true)
        }
    }
}

/** "mm:ss", làm tròn lên để đồng hồ không hiện 00:00 khi còn chút ít. */
private fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
}
