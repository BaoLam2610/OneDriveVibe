package com.lambao.odv.feature.player

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVInfoRow
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.odvLocale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Các dòng của bảng thông tin video (VD-17, V15/V16). Mọi dữ liệu lấy từ Room nên hiện ngay và dùng được khi offline. Trường nào không
 * có thì **ẩn dòng**. Số liệu dùng JetBrains Mono (thiet-ke-ui.md mục 4.5).
 *
 * Giới hạn đã biết: Room chỉ lưu một ngày tạo duy nhất (ngày tạo trên máy gốc, không có thì ngày tải lên, TV-02) nên chưa tách được
 * "Ngày tải lên OneDrive" khỏi "Ngày tạo gốc" như đặc tả; hiện một dòng "Ngày tạo". Tách ra cần thêm cột Room (tăng version, ADR-0015).
 */
@Composable
internal fun PlayerInfoContent(info: PlayerInfo, modifier: Modifier = Modifier) {
    val item = info.item
    val video = item.video
    val locale = odvLocale()
    val dateFormat = remember(locale) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "yMMMMdHm"), locale) }
    val numberFormat = remember(locale) { DecimalFormat("0.##", DecimalFormatSymbols(locale)) }
    val root = stringResource(R.string.player_info_root)

    Column(modifier.verticalScroll(rememberScrollState())) {
        ODVInfoRow(stringResource(R.string.player_info_name), item.name)
        ODVInfoRow(stringResource(R.string.player_info_size), odvFormatFileSize(item.sizeBytes, locale.toLanguageTag()), mono = true)
        item.durationMs?.let { ODVInfoRow(stringResource(R.string.player_info_duration), odvFormatDuration(it), mono = true) }
        val width = video?.width
        val height = video?.height
        if (width != null && height != null) {
            ODVInfoRow(stringResource(R.string.player_info_resolution), "$width × $height", mono = true)
        }
        video?.frameRate?.let {
            ODVInfoRow(stringResource(R.string.player_info_frame_rate), stringResource(R.string.player_info_fps, numberFormat.format(it)), mono = true)
        }
        video?.fourCc?.takeIf { it.isNotBlank() }?.let { ODVInfoRow(stringResource(R.string.player_info_codec), it, mono = true) }
        video?.bitRate?.let { ODVInfoRow(stringResource(R.string.player_info_bitrate), formatBitRate(it, numberFormat), mono = true) }
        item.createdAt?.let { ODVInfoRow(stringResource(R.string.player_info_created), dateFormat.format(Date(it)), mono = true) }
        item.modifiedAt?.let { ODVInfoRow(stringResource(R.string.player_info_modified), dateFormat.format(Date(it)), mono = true) }
        ODVInfoRow(
            stringResource(R.string.player_info_folder),
            (listOf(root) + info.folderPath.map { it.name }).joinToString(" / "),
        )
    }
}

/** Bit/giây → "1,38 Mbps" hoặc "820 kbps", số theo ngôn ngữ đang dùng (CD-10). */
private fun formatBitRate(bitsPerSecond: Long, numberFormat: DecimalFormat): String =
    if (bitsPerSecond >= 1_000_000) {
        "${numberFormat.format(bitsPerSecond / 1_000_000.0)} Mbps"
    } else {
        "${numberFormat.format(bitsPerSecond / 1_000.0)} kbps"
    }
