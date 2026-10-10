package com.lambao.odv.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import com.lambao.odv.core.domain.model.ConnectionInfo
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.SecretExpiryPolicy
import java.text.DateFormat
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Nhóm Kết nối (CD-04 đến CD-06; thiet-ke-ui.md mục 5.4): thông tin chỉ đọc, rồi Cập nhật Client Secret, Ngày hết hạn secret và
 * Ngắt kết nối. Cập nhật secret mở form riêng (đi qua P1 nếu bảo vệ đang bật), ngày hết hạn mở sheet S4, ngắt kết nối hỏi hai bước.
 */
@Composable
internal fun ConnectionGroup(info: ConnectionInfo, state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val locale = odvLocale()
    val resources = LocalResources.current
    ODVSettingsGroup(stringResource(R.string.settings_group_connection)) {
        row { ODVSettingsInfoRow(stringResource(R.string.settings_account), info.upn) }
        row { ODVSettingsInfoRow(stringResource(R.string.settings_tenant_id), info.tenantIdMasked) }
        row { ODVSettingsInfoRow(stringResource(R.string.settings_client_id), info.clientIdMasked) }
        row { ODVSettingsInfoRow(stringResource(R.string.settings_last_sync), formatLastSync(resources, info.lastSyncedAt, locale)) }
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

/**
 * Mẫu ngày ngắn của ngôn ngữ đang dùng nhưng năm luôn 4 chữ số (vi: `dd/MM/yyyy`, en-US: `M/d/yyyy`), để "30/11/2026" không bị hiểu nhầm
 * như `yy` và để nhập tay không mơ hồ.
 */
private fun datePattern(locale: Locale): String {
    val base = (DateFormat.getDateInstance(DateFormat.SHORT, locale) as? java.text.SimpleDateFormat)?.toPattern() ?: "dd/MM/yyyy"
    return if ("yyyy" in base) base else base.replace(Regex("y+"), "yyyy")
}

// Ngày hết hạn là ngày lịch, không có giờ: coi như 00:00 UTC để epoch day không lệch theo múi giờ. Dùng SimpleDateFormat/Calendar thay vì
// java.time vì minSdk 24 và dự án chưa bật core library desugaring.
private const val MILLIS_PER_DAY = 86_400_000L
private const val MIN_EPOCH_DAY = 10_957L // 2000-01-01, loại năm gõ thiếu như "26"
private const val MAX_EPOCH_DAY = 47_482L // 2099-12-31

private fun dateFormat(locale: Locale) = java.text.SimpleDateFormat(datePattern(locale), locale).apply {
    isLenient = false
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun formatEpochDay(epochDay: Long, locale: Locale): String = dateFormat(locale).format(Date(epochDay * MILLIS_PER_DAY))

/** Epoch day của chuỗi ngày; null nếu sai định dạng, ngày không có thật (vd. 31/02) hoặc ngoài năm 2000 đến 2099. */
private fun parseEpochDay(text: String, locale: Locale): Long? {
    val trimmed = text.trim()
    val position = ParsePosition(0)
    val date = dateFormat(locale).parse(trimmed, position) ?: return null
    if (position.index != trimmed.length) return null
    return (date.time / MILLIS_PER_DAY).takeIf { it in MIN_EPOCH_DAY..MAX_EPOCH_DAY }
}

/** Epoch day của hôm nay (theo ngày lịch địa phương) cộng [months] tháng. */
private fun epochDayFromToday(months: Int): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        add(Calendar.MONTH, months)
    }
    return utc.timeInMillis / MILLIS_PER_DAY
}
