package com.lambao.odv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.odvFormatSpeed
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.domain.model.AppLanguage
import com.lambao.odv.core.domain.model.AutoLockDelay
import com.lambao.odv.core.domain.model.PdfReadingStyle
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.VideoSettingOptions

/** Một dòng trong bảng chọn. Chạm là đặt giá trị và đóng bảng ngay, không có nút Lưu (thiet-ke-ui.md mục 4.6, OptionRow). */
private class Choice(val label: String, val selected: Boolean, val onClick: () -> Unit, val description: String? = null)

@Composable
private fun ChoiceSheet(title: String, onIntent: (SettingsIntent) -> Unit, choices: List<Choice>) {
    ODVBottomSheet(onDismissRequest = { onIntent(SettingsIntent.DismissSheet) }, title = title) {
        choices.forEach { ODVOptionRow(it.label, selected = it.selected, onClick = it.onClick, description = it.description) }
    }
}

/** Bảng chọn đang mở theo [SettingsState.sheet]; không có thì không vẽ gì. */
@Composable
internal fun SettingsSheets(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    when (state.sheet) {
        SettingsSheet.Theme -> ChoiceSheet(
            stringResource(R.string.settings_theme),
            onIntent,
            ThemeMode.entries.map { Choice(it.label(), it == state.themeMode, { onIntent(SettingsIntent.SelectTheme(it)) }) },
        )
        SettingsSheet.Language -> ChoiceSheet(
            stringResource(R.string.settings_language),
            onIntent,
            AppLanguage.entries.map { Choice(it.label(), it == state.language, { onIntent(SettingsIntent.SelectLanguage(it)) }) },
        )
        SettingsSheet.AutoLock -> ChoiceSheet(
            stringResource(R.string.settings_auto_lock),
            onIntent,
            AutoLockDelay.entries.map { Choice(it.label(), it == state.autoLockDelay, { onIntent(SettingsIntent.SelectAutoLock(it)) }) },
        )
        SettingsSheet.CacheLimit -> CacheLimitSheet(state.cacheLimitGb, onIntent)
        SettingsSheet.ShortMax -> ShortMaxSheet(state.shortMaxMinutes, onIntent)
        SettingsSheet.SecretExpiry -> SecretExpirySheet(state.secretExpiryEpochDay, onIntent)
        SettingsSheet.CacheShares -> CacheSharesSheet(state.cacheShares, state.cacheLimitGb, onIntent)
        SettingsSheet.SeekStep -> ChoiceSheet(
            stringResource(R.string.settings_seek_step),
            onIntent,
            VideoSettingOptions.SEEK_STEPS.map {
                Choice(stringResource(R.string.settings_seconds, it), it == state.seekStepSeconds, { onIntent(SettingsIntent.SelectSeekStep(it)) })
            },
        )
        SettingsSheet.Speed -> ChoiceSheet(
            stringResource(R.string.settings_default_speed),
            onIntent,
            VideoSettingOptions.SPEEDS.map { Choice(odvFormatSpeed(it, odvLocale()), it == state.defaultSpeed, { onIntent(SettingsIntent.SelectSpeed(it)) }) },
        )
        SettingsSheet.VideoFit -> ChoiceSheet(
            stringResource(R.string.settings_default_fit),
            onIntent,
            // null (nhớ lần gần nhất) đứng đầu, rồi 3 cách đặt khung theo thứ tự của nút Khung hình.
            (listOf<VideoFit?>(null) + VideoFit.entries).map {
                Choice(videoFitLabel(it), it == state.defaultVideoFit, { onIntent(SettingsIntent.SelectVideoFit(it)) }, videoFitDescription(it))
            },
        )
        SettingsSheet.Orientation -> ChoiceSheet(
            stringResource(R.string.settings_orientation),
            onIntent,
            listOf(false, true).map {
                Choice(orientationLabel(it), it == state.openVideoLandscape, { onIntent(SettingsIntent.SelectOrientation(it)) })
            },
        )
        SettingsSheet.PlayMode -> ChoiceSheet(
            stringResource(R.string.settings_play_mode),
            onIntent,
            PlayMode.entries.map {
                Choice(it.label(), it == state.playMode, { onIntent(SettingsIntent.SelectPlayMode(it)) }, it.description())
            },
        )
        SettingsSheet.PdfStyle -> ChoiceSheet(
            stringResource(R.string.settings_pdf_style),
            onIntent,
            PdfReadingStyle.entries.map { Choice(it.label(), it == state.pdfReadingStyle, { onIntent(SettingsIntent.SelectPdfStyle(it)) }) },
        )
        null -> Unit
    }
}
