package com.lambao.odv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVActionRow
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVCacheSegment
import com.lambao.odv.core.designsystem.component.ODVCacheUsage
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVSettingsGroup
import com.lambao.odv.core.designsystem.component.ODVValueRow
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.CachePolicy

/**
 * Nhóm Bộ nhớ đệm (CD, CD-07; thiet-ke-ui.md mục 4.6 CacheUsage, D5, D6): thanh dung lượng theo loại, Giới hạn tối đa (tùy chỉnh 1 đến
 * 10 GB), Tỉ lệ chia theo loại (tổng 100%), Xóa bộ nhớ đệm. Hai hàng giữa mở bảng chọn có thanh trượt và nút "Áp dụng" (không có trong
 * thiết kế, chốt 2026-10-07 theo yêu cầu người dùng).
 */
@Composable
internal fun CacheGroup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val tag = odvLocale().toLanguageTag()
    val limitBytes = CachePolicy.limitBytes(state.cacheLimitGb)
    val usage = state.cacheUsage
    val limitText = odvFormatFileSize(limitBytes, tag)
    val usedText = odvFormatFileSize(usage.total, tag)
    val segments = CacheKind.entries.map { kind ->
        ODVCacheSegment(
            label = kind.label(),
            valueText = odvFormatFileSize(usage.of(kind), tag),
            fraction = usage.of(kind).toFloat() / limitBytes,
            color = kind.color(),
        )
    }
    ODVSettingsGroup(stringResource(R.string.settings_group_cache)) {
        row {
            ODVCacheUsage(
                title = stringResource(R.string.settings_cache_usage),
                usageText = "$usedText / $limitText",
                segments = segments,
                barContentDescription = stringResource(R.string.settings_cache_usage_description, usedText, limitText),
            )
        }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_cache_limit),
                value = limitText,
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.CacheLimit)) },
            )
        }
        row {
            val shares = state.cacheShares
            ODVValueRow(
                title = stringResource(R.string.settings_cache_shares),
                value = "${shares.thumbnail} · ${shares.image} · ${shares.video} · ${shares.pdf}%",
                description = stringResource(R.string.settings_cache_shares_desc),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.CacheShares)) },
            )
        }
        row {
            ODVActionRow(
                title = stringResource(R.string.settings_cache_clear),
                description = stringResource(R.string.settings_cache_clear_desc, usedText),
                onClick = { onIntent(SettingsIntent.AskClearCache) },
            )
        }
    }
}

/** D5 (xóa bộ nhớ đệm) và D6 (giảm giới hạn, CD-07). Không có thì không vẽ gì. */
@Composable
internal fun CacheDialogs(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val tag = odvLocale().toLanguageTag()
    val usedText = odvFormatFileSize(state.cacheUsage.total, tag)
    when (state.dialog) {
        SettingsDialog.ClearCache -> ODVDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
            title = stringResource(R.string.settings_cache_clear_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Warning,
            body = stringResource(R.string.settings_cache_clear_body, usedText),
            alert = true,
        ) {
            ODVButton(stringResource(R.string.settings_cancel), { onIntent(SettingsIntent.DismissDialog) }, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.settings_cache_clear_confirm), { onIntent(SettingsIntent.ConfirmClearCache) }, style = ODVButtonStyle.Danger)
        }
        SettingsDialog.ShrinkCache -> {
            val newLimit = odvFormatFileSize(CachePolicy.limitBytes(state.pendingLimitGb ?: state.cacheLimitGb), tag)
            ODVDialog(
                onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
                title = stringResource(R.string.settings_cache_shrink_title, newLimit),
                icon = ODVIcon.Alert,
                tone = ODVDialogTone.Warning,
                body = stringResource(R.string.settings_cache_shrink_body, usedText, newLimit),
                alert = true,
            ) {
                ODVButton(stringResource(R.string.settings_cancel), { onIntent(SettingsIntent.DismissDialog) }, style = ODVButtonStyle.Ghost)
                ODVButton(stringResource(R.string.settings_cache_shrink_confirm), { onIntent(SettingsIntent.ConfirmShrinkCache) })
            }
        }
        else -> Unit
    }
}
