package com.lambao.odv.feature.settings

import android.content.Context
import android.content.res.Resources
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVChipsRow
import com.lambao.odv.core.designsystem.component.LocalODVNavBarOwnsInset
import com.lambao.odv.core.designsystem.component.ODVReselectEffect
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSettingsGroup
import com.lambao.odv.core.designsystem.component.ODVSnackbarHost
import com.lambao.odv.core.designsystem.component.ODVSwitchRow
import com.lambao.odv.core.designsystem.component.ODVValueRow
import com.lambao.odv.core.designsystem.odvFormatSpeed
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.MediaKind
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Giao diện màn Cài đặt: một màn cuộn gồm các nhóm (thiet-ke-ui.md mục 5.4), AppBar cố định có kẻ đáy khi đã cuộn.
 * Đã có Hiển thị, Bảo mật, Video, PDF, Bộ nhớ đệm và Kết nối chỉ đọc (thứ tự nhóm theo thiết kế: Hiển thị, Bảo mật, Video, PDF,
 * Bộ nhớ đệm, Kết nối).
 */
@Composable
internal fun ODVSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    snackbarHost: SnackbarHostState,
    modifier: Modifier = Modifier,
    reselectSignal: Int = 0,
) {
    val scroll = rememberScrollState()
    // DH-04: chạm lại tab Cài đặt thì cuộn lên đầu.
    ODVReselectEffect(reselectSignal) { scroll.animateScrollTo(0) }
    val lineColor = ODVTheme.colors.line
    Box(modifier) {
        ODVScaffold(
            topBar = {
                ODVAppBar(
                    // Từ Lát 8 Cài đặt là tab: tiêu đề lề trái 16, không có nút quay lại (thiet-ke-ui.md mục 5.4).
                    title = stringResource(R.string.settings_title),
                    // Kẻ đáy chỉ hiện khi nội dung đã cuộn lên dưới AppBar.
                    modifier = Modifier
                        .drawBehind {
                            if (scroll.value > 0) drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                        }
                        .padding(start = 12.dp),
                )
            },
        ) { contentPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(scroll),
            ) {
                // C7 (CD-06): banner secret sắp hết hạn ở đầu màn.
                state.expiryNotice?.let { SecretExpiryBanner(it, onUpdate = { onIntent(SettingsIntent.UpdateSecret) }, modifier = Modifier.padding(16.dp)) }
                DisplayGroup(state, onIntent)
                SecurityGroup(state, onIntent)
                VideoGroup(state, onIntent)
                PdfGroup(state, onIntent)
                CacheGroup(state, onIntent)
                state.connection?.let { ConnectionGroup(it, state, onIntent) }
                VersionFooter()
            }
        }
        // Snackbar S5 "Đã cập nhật Client Secret" nổi ở đáy, trên cả nội dung cuộn.
        ODVSnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter).then(if (LocalODVNavBarOwnsInset.current) Modifier else Modifier.navigationBarsPadding()).padding(16.dp))
    }
    SettingsSheets(state, onIntent)
    SettingsDialogs(state, onIntent)
}

@Composable
private fun DisplayGroup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    ODVSettingsGroup(stringResource(R.string.settings_group_display)) {
        row { FileKindsRow(state, onIntent) }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_theme),
                value = state.themeMode.label(),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.Theme)) },
            )
        }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_language),
                value = state.language.label(),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.Language)) },
            )
        }
    }
}

/** Loại tệp hiển thị (CD-01): chip chọn nhiều; chip cuối cùng đang bật không tắt được và hiện cảnh báo. */
@Composable
private fun FileKindsRow(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val colors = ODVTheme.colors
    ODVChipsRow(
        title = stringResource(R.string.settings_file_kinds),
        description = stringResource(R.string.settings_file_kinds_desc),
        warning = if (state.onlyOneKindLeft) stringResource(R.string.settings_file_kinds_warning) else null,
    ) {
        MediaKind.entries.forEach { kind ->
            val dot: Color = when (kind) {
                MediaKind.Image -> colors.kindPhoto
                MediaKind.Video -> colors.kindVideo
                MediaKind.Pdf -> colors.kindPdf
            }
            ODVChip(
                label = kind.label(),
                selected = kind in state.enabledKinds,
                onClick = { onIntent(SettingsIntent.ToggleKind(kind)) },
                dotColor = dot,
            )
        }
    }
}

@Composable
private fun VideoGroup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    ODVSettingsGroup(stringResource(R.string.settings_group_video)) {
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_seek_step),
                value = stringResource(R.string.settings_seconds, state.seekStepSeconds),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.SeekStep)) },
            )
        }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_default_speed),
                value = odvFormatSpeed(state.defaultSpeed, odvLocale()),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.Speed)) },
            )
        }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_default_fit),
                value = videoFitLabel(state.defaultVideoFit),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.VideoFit)) },
            )
        }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_orientation),
                value = orientationLabel(state.openVideoLandscape),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.Orientation)) },
            )
        }
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_play_mode),
                value = state.playMode.label(),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.PlayMode)) },
            )
        }
        row {
            ODVSwitchRow(
                title = stringResource(R.string.settings_remember_position),
                description = stringResource(R.string.settings_remember_position_desc),
                checked = state.rememberVideoPosition,
                onCheckedChange = { onIntent(SettingsIntent.SetRememberPosition(it)) },
            )
        }
    }
}

@Composable
private fun PdfGroup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    ODVSettingsGroup(stringResource(R.string.settings_group_pdf)) {
        row {
            ODVValueRow(
                title = stringResource(R.string.settings_pdf_style),
                value = state.pdfReadingStyle.label(),
                onClick = { onIntent(SettingsIntent.ShowSheet(SettingsSheet.PdfStyle)) },
            )
        }
    }
}

/** Dòng phiên bản cuối màn (`meta`, căn giữa). */
@Composable
private fun VersionFooter() {
    val context = LocalContext.current
    val version = remember(context) { appVersionName(context) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.settings_version, version),
            style = ODVTheme.typography.meta,
            color = ODVTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Hôm nay 09:12", "Hôm qua 21:40" hoặc ngày giờ đầy đủ theo ngôn ngữ đang dùng (CD-10); chưa đồng bộ lần nào thì báo "Chưa có". */
internal fun formatLastSync(resources: Resources, epochMs: Long?, locale: Locale): String {
    if (epochMs == null) return resources.getString(R.string.settings_last_sync_never)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(epochMs))
    return when {
        DateUtils.isToday(epochMs) -> resources.getString(R.string.settings_last_sync_today, time)
        DateUtils.isToday(epochMs + DateUtils.DAY_IN_MILLIS) -> resources.getString(R.string.settings_last_sync_yesterday, time)
        else -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(epochMs))
    }
}

private fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
