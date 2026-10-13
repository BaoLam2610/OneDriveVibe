package com.lambao.odv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.lambao.odv.core.designsystem.component.ODVAccountSettingsRow
import com.lambao.odv.core.designsystem.component.ODVAddAccountRow
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVDangerRow
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVNavRow
import com.lambao.odv.core.designsystem.component.ODVSettingsGroup
import com.lambao.odv.core.designsystem.component.ODVSettingsInfoRow
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.component.ODVValueRow
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AccountStatus
import com.lambao.odv.core.domain.model.ConnectionInfo
import com.lambao.odv.core.domain.model.SyncPhase
import java.text.NumberFormat
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.SecretExpiryPolicy
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Nhóm "Tài khoản và kết nối" (CD-04 đến CD-06, AC9; thiet-ke-ui.md mục 4.9, 5.4; TA-03, TA-08, TA-09). Thứ tự: hàng "Đang xem" (mở sheet
 * Chọn tài khoản), các hàng tài khoản (trạng thái đồng bộ riêng, nút xóa trừ tài khoản đang xem), "Thêm tài khoản", rồi phần **dùng chung**
 * (Tenant ID, Client ID), Cập nhật Client Secret, Ngày hết hạn secret và Ngắt kết nối. Cập nhật secret mở form riêng (đi qua P1 nếu bảo vệ
 * đang bật), ngày hết hạn mở sheet S4, ngắt kết nối hỏi hai bước.
 */
@Composable
internal fun ConnectionGroup(info: ConnectionInfo, state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val locale = odvLocale()
    val colors = ODVTheme.colors
    val current = (state.scope as? AccountScope.One)?.accountId
    val viewingName = if (state.scope == AccountScope.All) {
        stringResource(R.string.settings_viewing_all)
    } else {
        state.accounts.firstOrNull { it.account.id == current }?.account?.displayName ?: state.accounts.firstOrNull()?.account?.displayName.orEmpty()
    }
    ODVSettingsGroup(stringResource(R.string.settings_group_connection)) {
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_viewing),
                value = viewingName,
                onClick = { onIntent(SettingsIntent.OpenAccountPicker) },
            )
        }
        state.accounts.forEach { status ->
            row {
                val isCurrent = status.account.id == current
                // Tài khoản đang xem và trường hợp chỉ có một tài khoản không có nút xóa (TA-08); chạm hàng đó vẫn có Snackbar giải thích.
                val canRemove = !isCurrent && state.accounts.size > 1
                ODVAccountSettingsRow(
                    name = status.account.displayName,
                    email = status.account.email,
                    statusLine = accountStatusLine(status, locale),
                    statusIsError = status.inaccessible,
                    currentBadge = if (isCurrent) stringResource(R.string.settings_account_current_badge) else null,
                    removeContentDescription = stringResource(R.string.settings_account_remove, status.account.email),
                    onRemove = if (canRemove) ({ onIntent(SettingsIntent.AskRemoveAccount(status.account.id)) }) else null,
                    onClick = { onIntent(SettingsIntent.AskRemoveAccount(status.account.id)) },
                )
            }
        }
        row {
            ODVAddAccountRow(
                title = stringResource(R.string.settings_account_add),
                onClick = { onIntent(SettingsIntent.AddAccount) },
                showChevron = true,
                horizontalPadding = 16.dp,
            )
        }
        // Dòng "Dùng chung cho mọi tài khoản" nằm trong thẻ, ngay trên Tenant ID, không có kẻ giữa hai dòng (AC9).
        row {
            Column {
                Text(
                    stringResource(R.string.settings_shared_caption),
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    style = ODVTheme.typography.caption,
                    color = colors.inkMuted,
                )
                ODVSettingsInfoRow(stringResource(R.string.settings_tenant_id), info.tenantIdMasked)
            }
        }
        row { ODVSettingsInfoRow(stringResource(R.string.settings_client_id), info.clientIdMasked) }
        row { ODVNavRow(stringResource(R.string.settings_update_secret), onClick = { onIntent(SettingsIntent.UpdateSecret) }) }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_secret_expiry),
                description = stringResource(R.string.settings_secret_expiry_desc, SecretExpiryPolicy.WARN_DAYS),
                value = state.secretExpiryEpochDay?.let { formatEpochDay(it, locale) } ?: stringResource(R.string.settings_secret_expiry_none),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.SecretExpiry)) },
            )
        }
        row {
            ODVDangerRow(
                title = stringResource(R.string.settings_disconnect),
                description = stringResource(R.string.settings_disconnect_desc),
                onClick = { onIntent(SettingsIntent.AskDisconnect) },
            )
        }
    }
}

/**
 * Dòng 3 của hàng tài khoản (AC9): đang quét lần đầu thì "Đang lập chỉ mục, đã quét N mục"; không truy cập được thì câu lỗi (TA-10); còn lại
 * "Đồng bộ lúc 14:05, 12/10/2026" theo định dạng của ngôn ngữ đang dùng (CD-10), hoặc "Chưa đồng bộ" khi chưa có mốc nào.
 */
@Composable
private fun accountStatusLine(status: AccountStatus, locale: Locale): String {
    val sync = status.sync
    val lastSyncedAt = sync.lastSyncedAt
    return when {
        status.inaccessible -> stringResource(R.string.settings_account_inaccessible)
        sync.phase == SyncPhase.Syncing && !sync.initialSyncDone -> {
            val scanned = remember(sync.scannedCount, locale) { NumberFormat.getIntegerInstance(locale).format(sync.scannedCount) }
            stringResource(R.string.settings_account_indexing, scanned)
        }
        lastSyncedAt == null -> stringResource(R.string.settings_account_never_synced)
        else -> stringResource(R.string.settings_account_synced_at, formatSyncedAt(lastSyncedAt, locale))
    }
}

/** "14:05, 12/10/2026" (vi) hoặc "2:05 PM, 10/12/2026" (en): giờ rồi ngày, theo quy ước của ngôn ngữ (CD-10). */
private fun formatSyncedAt(epochMs: Long, locale: Locale): String {
    val date = Date(epochMs)
    return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date) + ", " + DateFormat.getDateInstance(DateFormat.SHORT, locale).format(date)
}

/** AC8 (TA-08): xác nhận xóa tài khoản khỏi app, một bước. Chỉ xóa dữ liệu trên máy; khóa nút trong lúc đang xóa để không bấm đôi. */
@Composable
internal fun RemoveAccountDialog(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val target = state.removeCandidate ?: return
    ODVDialog(
        onDismissRequest = { if (!state.isRemovingAccount) onIntent(SettingsIntent.DismissDialog) },
        title = stringResource(R.string.settings_remove_account_title),
        icon = ODVIcon.Alert,
        tone = ODVDialogTone.Warning,
        body = stringResource(R.string.settings_remove_account_body, target.account.email),
    ) {
        ODVButton(
            stringResource(R.string.settings_cancel),
            { onIntent(SettingsIntent.DismissDialog) },
            style = ODVButtonStyle.Ghost,
            enabled = !state.isRemovingAccount,
        )
        ODVButton(
            stringResource(R.string.settings_remove_account_confirm),
            { onIntent(SettingsIntent.ConfirmRemoveAccount) },
            style = ODVButtonStyle.Danger,
            enabled = !state.isRemovingAccount,
        )
    }
}

/** C7 (CD-06): banner đầu màn khi secret sắp hết hạn hoặc đã hết hạn; nút "Cập nhật" đi tới form cập nhật Client Secret. */
@Composable
internal fun SecretExpiryBanner(notice: SecretExpiryNotice, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    val text = when {
        notice.isExpired -> stringResource(R.string.settings_expiry_banner_expired)
        notice.daysLeft == 0 -> stringResource(R.string.settings_expiry_banner_today)
        else -> pluralStringResource(R.plurals.settings_expiry_banner, notice.daysLeft, notice.daysLeft)
    }
    ODVBanner(
        text = text,
        tone = if (notice.isExpired) ODVBannerTone.Danger else ODVBannerTone.Warning,
        icon = if (notice.isExpired) ODVIcon.Alert else ODVIcon.Clock,
        modifier = modifier,
        actionLabel = stringResource(R.string.settings_expiry_banner_action),
        onAction = onUpdate,
    )
}

/**
 * S4: sheet "Ngày hết hạn secret". Ô nhập ngày theo định dạng của ngôn ngữ đang dùng (CD-10, năm 4 chữ số), ba nút nhanh +6 tháng, +1 năm,
 * +2 năm tính từ hôm nay, "Xóa ngày" và "Lưu". Chưa nhập đúng ngày thì "Lưu" tắt; ngày hợp lệ nhưng không đổi cũng tắt.
 */
@Composable
internal fun SecretExpirySheet(currentEpochDay: Long?, onIntent: (SettingsIntent) -> Unit) {
    val locale = odvLocale()
    val pattern = remember(locale) { datePattern(locale).uppercase(locale) }
    var text by remember(currentEpochDay) { mutableStateOf(currentEpochDay?.let { formatEpochDay(it, locale) } ?: "") }
    val parsed = remember(text, locale) { parseEpochDay(text, locale) }
    val invalid = text.isNotBlank() && parsed == null
    ODVBottomSheet(onDismissRequest = { onIntent(SettingsIntent.DismissSheet) }, title = stringResource(R.string.settings_secret_expiry)) {
        ODVTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.settings_secret_expiry_label),
            placeholder = pattern,
            helperText = stringResource(R.string.settings_secret_expiry_hint, SecretExpiryPolicy.WARN_DAYS),
            errorText = if (invalid) stringResource(R.string.settings_secret_expiry_invalid, pattern) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text) // bàn phím số của nhiều IME không có phím "/",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ODVChip(stringResource(R.string.settings_expiry_plus_6_months), selected = false, onClick = { text = formatEpochDay(epochDayFromToday(months = 6), locale) })
            ODVChip(stringResource(R.string.settings_expiry_plus_1_year), selected = false, onClick = { text = formatEpochDay(epochDayFromToday(months = 12), locale) })
            ODVChip(stringResource(R.string.settings_expiry_plus_2_years), selected = false, onClick = { text = formatEpochDay(epochDayFromToday(months = 24), locale) })
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ODVButton(
                text = stringResource(R.string.settings_expiry_clear),
                onClick = { onIntent(SettingsIntent.SaveSecretExpiry(null)) },
                modifier = Modifier.weight(1f),
                style = ODVButtonStyle.Ghost,
                enabled = currentEpochDay != null,
            )
            ODVButton(
                text = stringResource(R.string.settings_save),
                onClick = { parsed?.let { onIntent(SettingsIntent.SaveSecretExpiry(it)) } },
                modifier = Modifier.weight(1f),
                enabled = parsed != null && parsed != currentEpochDay,
            )
        }
    }
}

/** D3 và D4 (CD-05): hai bước xác nhận trước khi xóa dữ liệu. Bước 2 khóa nút trong lúc đang xóa để không bấm đôi. */
@Composable
internal fun DisconnectDialogs(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    when (state.dialog) {
        SettingsDialog.DisconnectStep1 -> ODVDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
            title = stringResource(R.string.settings_disconnect_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Warning,
            body = stringResource(R.string.settings_disconnect_body),
        ) {
            ODVButton(stringResource(R.string.settings_cancel), { onIntent(SettingsIntent.DismissDialog) }, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.settings_continue), { onIntent(SettingsIntent.ContinueDisconnect) }, style = ODVButtonStyle.Danger)
        }
        SettingsDialog.DisconnectStep2 -> ODVDialog(
            onDismissRequest = { if (!state.isDisconnecting) onIntent(SettingsIntent.DismissDialog) },
            title = stringResource(R.string.settings_disconnect_final_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Danger,
            body = stringResource(R.string.settings_disconnect_final_body),
            alert = true,
        ) {
            ODVButton(
                stringResource(R.string.settings_cancel),
                { onIntent(SettingsIntent.DismissDialog) },
                style = ODVButtonStyle.Ghost,
                enabled = !state.isDisconnecting,
            )
            ODVButton(
                stringResource(R.string.settings_disconnect_final_confirm),
                { onIntent(SettingsIntent.ConfirmDisconnect) },
                style = ODVButtonStyle.DangerSolid,
                enabled = !state.isDisconnecting,
            )
        }
        else -> Unit
    }
}
