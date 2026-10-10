package com.lambao.odv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVNavRow
import com.lambao.odv.core.designsystem.component.ODVSettingsGroup
import com.lambao.odv.core.designsystem.component.ODVSwitchRow
import com.lambao.odv.core.designsystem.component.ODVValueRow
import com.lambao.odv.core.designsystem.icon.ODVIcon

/**
 * Nhóm Bảo mật (CD-02, CD-03, CD-08, CD-09; thiet-ke-ui.md mục 5.4): công tắc Bảo vệ ứng dụng luôn hiện; khi bảo vệ **tắt** (kể cả khi
 * chọn "Để sau" ở K6) mọi hàng phụ thuộc PIN ẩn đi và công tắc báo ai cầm máy cũng xem được. Bật thì thêm Đổi mã PIN, sinh trắc học
 * (nếu máy hỗ trợ), Tự khóa khi rời app và Xóa dữ liệu khi nhập sai quá nhiều. "Bảo vệ màn hình" (FLAG_SECURE toàn app) luôn hiện vì không
 * phụ thuộc PIN.
 */
@Composable
internal fun SecurityGroup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    ODVSettingsGroup(stringResource(R.string.settings_group_security)) {
        row {
            ODVSwitchRow(
                title = stringResource(R.string.settings_protection),
                description = stringResource(if (state.protectionEnabled) R.string.settings_protection_on else R.string.settings_protection_off),
                checked = state.protectionEnabled,
                onCheckedChange = { onIntent(SettingsIntent.ToggleProtection(it)) },
            )
        }
        // Không phụ thuộc PIN: ai cũng có thể muốn chặn chụp màn hình (FLAG_SECURE toàn app).
        row {
            ODVSwitchRow(
                title = stringResource(R.string.settings_screen_protection),
                description = stringResource(R.string.settings_screen_protection_desc),
                checked = state.screenProtection,
                onCheckedChange = { onIntent(SettingsIntent.SetScreenProtection(it)) },
            )
        }
        if (state.protectionEnabled) {
            row { ODVNavRow(stringResource(R.string.settings_change_pin), onClick = { onIntent(SettingsIntent.ChangePin) }) }
            if (state.biometric.available) {
                row {
                    ODVSwitchRow(
                        title = stringResource(R.string.settings_biometric),
                        description = stringResource(R.string.settings_biometric_desc),
                        checked = state.biometric.enabled,
                        onCheckedChange = { onIntent(SettingsIntent.ToggleBiometric(it)) },
                    )
                }
            }
            row {
                ODVValueRow(
                    title = stringResource(R.string.settings_auto_lock),
                    value = state.autoLockDelay.label(),
                    onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.AutoLock)) },
                )
            }
            row {
                ODVSwitchRow(
                    title = stringResource(R.string.settings_wipe),
                    description = stringResource(R.string.settings_wipe_desc),
                    checked = state.wipeOnFailures,
                    onCheckedChange = { onIntent(SettingsIntent.ToggleWipe(it)) },
                )
            }
        }
    }
}

/** Hộp thoại xác nhận đang mở ở màn Cài đặt; không có thì không vẽ gì. */
@Composable
internal fun SettingsDialogs(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    when (state.dialog) {
        // D2 (CD-08): cảnh báo, nút chính "Nhập PIN để bật" (danger, tonal) rồi sang màn PIN.
        SettingsDialog.WipeOnFailures -> ODVDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
            title = stringResource(R.string.settings_wipe_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Warning,
            body = stringResource(R.string.settings_wipe_body),
            alert = true,
        ) {
            ODVButton(stringResource(R.string.settings_cancel), { onIntent(SettingsIntent.DismissDialog) }, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.settings_wipe_confirm), { onIntent(SettingsIntent.ConfirmWipeDialog) }, style = ODVButtonStyle.Danger)
        }
        SettingsDialog.ClearCache, SettingsDialog.ShrinkCache -> CacheDialogs(state, onIntent)
        SettingsDialog.DisconnectStep1, SettingsDialog.DisconnectStep2 -> DisconnectDialogs(state, onIntent)
        null -> Unit
    }
}
