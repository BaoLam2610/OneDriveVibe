package com.lambao.odv.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVKeypad
import com.lambao.odv.core.designsystem.component.ODVPinDots
import com.lambao.odv.core.designsystem.component.ODVPinDotsState
import com.lambao.odv.core.designsystem.component.ODVPinMessage
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVShakeState
import com.lambao.odv.core.designsystem.component.odvShake
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Giao diện màn PIN của Cài đặt (thiet-ke-ui.md mục 5.4, P1 → P3): cùng bố cục với màn Thiết lập bảo mật (mũi tên Back, icon `lock`,
 * tiêu đề, dãy chấm, dòng thông báo, bàn phím số tự vẽ sát đáy để bàn phím bên thứ ba không ghi nhận được PIN, BM-07). Back ở P2 và
 * P3 quay về P1 (làm lại từ đầu); ở P1 thoát về Cài đặt. Hộp thoại D1 hiện sau khi PIN hiện tại đúng khi tắt bảo vệ (CD-03).
 */
@Composable
internal fun ODVPinFlowContent(
    state: PinFlowState,
    shake: ODVShakeState,
    onIntent: (PinFlowIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    // Đang kiểm tra hoặc đang ghi thì khóa Back để không bỏ dở giữa lúc mã hóa lại config (CD-09).
    BackHandler(enabled = state.isBusy) {}
    ODVScaffold(
        modifier = modifier,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp)) {
                ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.settings_back), onBack, enabled = !state.isBusy)
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
                Text(stepTitle(state.step), style = type.display, color = colors.ink)
                Text(stepSubtitle(state), style = type.body, color = colors.inkMuted)
            }
            PinEntry(state, shake, onIntent)
        }
    }

    if (state.confirmDisable) DisableProtectionDialog(state.isBusy, onIntent)
}

@Composable
private fun stepTitle(step: PinStep): String = stringResource(
    when (step) {
        PinStep.Current -> R.string.settings_pin_current
        PinStep.New -> R.string.settings_pin_new
        PinStep.ConfirmNew -> R.string.settings_pin_confirm
    },
)

/** Dòng phụ đề: ở P1 là mục đích ("Để tắt bảo vệ ứng dụng"), ở P2 và P3 là gợi ý chọn PIN. */
@Composable
private fun stepSubtitle(state: PinFlowState): String = stringResource(
    when (state.step) {
        PinStep.Current -> when (state.purpose) {
            PinPurpose.DisableProtection -> R.string.settings_pin_for_disable
            PinPurpose.EnableWipe -> R.string.settings_pin_for_wipe
            PinPurpose.ChangePin -> R.string.settings_pin_for_change
            PinPurpose.UpdateSecret -> R.string.settings_pin_for_secret
        }
        PinStep.New -> R.string.settings_pin_new_hint
        PinStep.ConfirmNew -> R.string.settings_pin_confirm_hint
    },
)

/** Dãy chấm, dòng thông báo và bàn phím số. Chiếm phần còn lại của màn. */
@Composable
private fun ColumnScope.PinEntry(state: PinFlowState, shake: ODVShakeState, onIntent: (PinFlowIntent) -> Unit) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(
        Modifier.fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ODVPinDots(
            filled = state.entered,
            contentDescription = stringResource(R.string.settings_pin_description, state.entered),
            modifier = Modifier.odvShake(shake),
            state = if (state.error != null) ODVPinDotsState.Error else ODVPinDotsState.Default,
        )
        val attemptsLeft = state.attemptsBeforeWipe
        val message = when {
            state.isCoolingDown -> stringResource(R.string.settings_pin_cooldown, odvFormatDuration(state.cooldownRemainingMs))
            state.error == PinError.Wrong -> stringResource(R.string.settings_pin_wrong)
            state.error == PinError.Weak -> stringResource(R.string.settings_pin_weak)
            state.error == PinError.Mismatch -> stringResource(R.string.settings_pin_mismatch)
            state.error == PinError.StorageFailed -> stringResource(R.string.settings_pin_failed)
            attemptsLeft != null && state.step == PinStep.Current -> stringResource(R.string.settings_pin_attempts, attemptsLeft)
            else -> null
        }
        if (message != null) {
            ODVPinMessage(text = message, isError = state.error != null || state.isCoolingDown || attemptsLeft != null)
        } else {
            // Giữ chỗ để bàn phím không nhảy khi dòng thông báo xuất hiện.
            Text(" ", style = type.body, color = colors.inkMuted)
        }
    }
    ODVKeypad(
        onDigit = { onIntent(PinFlowIntent.Digit(it)) },
        onBackspace = { onIntent(PinFlowIntent.Backspace) },
        backspaceLabel = stringResource(R.string.settings_pin_backspace),
        modifier = Modifier.align(Alignment.CenterHorizontally),
        enabled = !state.isBusy && !state.isCoolingDown,
    )
}

/** D1 (CD-03): cảnh báo trước khi tắt bảo vệ; nút chính là `danger-solid`. */
@Composable
private fun DisableProtectionDialog(busy: Boolean, onIntent: (PinFlowIntent) -> Unit) {
    ODVDialog(
        onDismissRequest = { if (!busy) onIntent(PinFlowIntent.CancelDisable) },
        title = stringResource(R.string.settings_disable_title),
        icon = ODVIcon.Alert,
        tone = ODVDialogTone.Warning,
        body = stringResource(R.string.settings_disable_body),
        alert = true,
        dismissOnOutsideClick = !busy,
        dismissOnBackPress = !busy,
    ) {
        ODVButton(stringResource(R.string.settings_cancel), { onIntent(PinFlowIntent.CancelDisable) }, style = ODVButtonStyle.Ghost, enabled = !busy)
        ODVButton(stringResource(R.string.settings_disable_confirm), { onIntent(PinFlowIntent.ConfirmDisable) }, style = ODVButtonStyle.DangerSolid, enabled = !busy)
    }
}
