package com.lambao.odv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVSlider
import com.lambao.odv.core.designsystem.component.ODVSliderRow
import com.lambao.odv.core.designsystem.component.ODVSplitBar
import com.lambao.odv.core.designsystem.component.ODVSplitSegment
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.model.CacheShares
import kotlin.math.roundToInt

/** Bảng chọn giới hạn chung 1 đến 10 GB bằng thanh trượt (bước 1 GB). Nháp nằm trong bảng, chỉ ghi khi bấm "Áp dụng". */
@Composable
internal fun CacheLimitSheet(current: Int, onIntent: (SettingsIntent) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    val tag = odvLocale().toLanguageTag()
    ODVBottomSheet(onDismissRequest = { onIntent(SettingsIntent.DismissSheet) }, title = stringResource(R.string.settings_cache_limit)) {
        Text(
            odvFormatFileSize(CachePolicy.limitBytes(draft), tag),
            style = ODVTheme.typography.heading,
            color = ODVTheme.colors.ink,
        )
        ODVSlider(
            value = draft.toFloat(),
            onValueChange = { draft = it.roundToInt() },
            valueRange = CachePolicy.MIN_LIMIT_GB.toFloat()..CachePolicy.MAX_LIMIT_GB.toFloat(),
            // 10 mốc nguyên từ 1 đến 10 GB nên có 8 vạch ở giữa.
            steps = CachePolicy.MAX_LIMIT_GB - CachePolicy.MIN_LIMIT_GB - 1,
            contentDescription = stringResource(R.string.settings_cache_limit),
        )
        Text(stringResource(R.string.settings_cache_limit_hint), style = ODVTheme.typography.caption, color = ODVTheme.colors.inkMuted)
        ODVButton(
            text = stringResource(R.string.settings_apply),
            onClick = { onIntent(SettingsIntent.ApplyCacheLimit(draft)) },
            enabled = draft != current,
            fullWidth = true,
        )
    }
}

/**
 * Bảng tỉ lệ chia theo loại: bốn thanh trượt 0 đến 100%, kéo một thanh thì ba thanh còn lại **tự tính lại** để tổng luôn 100
 * (`CacheShares.withShare`). Mỗi dòng ghi phần trăm và dung lượng trần tương ứng với [limitGb] hiện tại.
 */
@Composable
internal fun CacheSharesSheet(current: CacheShares, limitGb: Int, onIntent: (SettingsIntent) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    val tag = odvLocale().toLanguageTag()
    val limitBytes = CachePolicy.limitBytes(limitGb)
    ODVBottomSheet(onDismissRequest = { onIntent(SettingsIntent.DismissSheet) }, title = stringResource(R.string.settings_cache_shares)) {
        Text(stringResource(R.string.settings_cache_shares_hint), style = ODVTheme.typography.caption, color = ODVTheme.colors.inkMuted)
        // Thanh tỉ lệ nhiều đoạn (SplitBar) đổi theo bản nháp; tổng luôn 100 nên chỉ có dấu check, không có trạng thái lỗi.
        val kinds = CacheKind.entries.map { it to it.label() }
        val barText = stringResource(
            R.string.settings_cache_shares_bar,
            kinds.joinToString(", ") { (kind, label) -> "$label ${draft[kind]}%" },
        )
        ODVSplitBar(
            segments = CacheKind.entries.map { ODVSplitSegment(draft[it], it.color()) },
            totalText = "${CacheKind.entries.sumOf { draft[it] }}%",
            barContentDescription = barText,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            kinds.forEach { (kind, label) ->
                val percent = draft[kind]
                val size = odvFormatFileSize(limitBytes * percent / CacheShares.TOTAL, tag)
                ODVSliderRow(
                    label = label,
                    color = kind.color(),
                    valueText = "$percent%",
                    detailText = size,
                    stateDescription = stringResource(R.string.settings_cache_shares_state, label, percent, size),
                    value = percent.toFloat(),
                    onValueChange = { draft = draft.withShare(kind, it.roundToInt()) },
                    valueRange = 0f..CacheShares.TOTAL.toFloat(),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ODVButton(
                text = stringResource(R.string.settings_cache_shares_default),
                onClick = { draft = CacheShares.Default },
                modifier = Modifier.weight(1f),
                style = ODVButtonStyle.Ghost,
                enabled = draft != CacheShares.Default,
            )
            ODVButton(
                text = stringResource(R.string.settings_apply),
                onClick = { onIntent(SettingsIntent.ApplyCacheShares(draft)) },
                modifier = Modifier.weight(1f),
                enabled = draft != current,
            )
        }
    }
}
