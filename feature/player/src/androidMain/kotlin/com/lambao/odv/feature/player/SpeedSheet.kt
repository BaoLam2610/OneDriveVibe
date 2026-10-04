package com.lambao.odv.feature.player

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.component.ODVSidePanel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val speedFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")))

/** "1x", "0,25x", "1,5x": số theo vi-VN (thiet-ke-ui.md mục 1). */
internal fun formatSpeed(speed: Float): String = speedFormat.format(speed) + "x"

/**
 * Bảng chọn tốc độ phát (VD-05, V6): hướng dọc là bottom sheet 7 hàng có dấu check, hướng ngang là bảng bên phải (mục 4.5).
 * Chạm một hàng là đặt tốc độ và đóng ngay, không có nút Lưu.
 */
@Composable
internal fun SpeedSheet(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.player_speed_title)
    val content: @Composable () -> Unit = {
        // Cuộn dọc: 7 hàng cao 48 dp cao hơn chiều cao màn hình ngang nên hàng cuối (2x) bị cắt nếu không cuộn được.
        Column(Modifier.verticalScroll(rememberScrollState())) {
            PlayerConstants.SPEEDS.forEach { speed ->
                val label = if (speed == PlayerConstants.NORMAL_SPEED) {
                    stringResource(R.string.player_speed_normal, formatSpeed(speed))
                } else {
                    formatSpeed(speed)
                }
                ODVOptionRow(
                    label = label,
                    selected = speed == current,
                    onClick = {
                        onSelect(speed)
                        onDismiss()
                    },
                    mono = true,
                    checkStyle = true,
                )
            }
        }
    }
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        ODVSidePanel(title, stringResource(R.string.player_close), onDismiss) { content() }
    } else {
        ODVBottomSheet(onDismissRequest = onDismiss, title = title) { content() }
    }
}
