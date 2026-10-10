package com.lambao.odv.feature.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVSlider
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.VideoSettingOptions
import kotlin.math.roundToInt

/**
 * O6: bảng Thời lượng tối đa của Short (CD-13, thiet-ke-ui.md mục 5.4), cùng kiểu bảng Giới hạn tối đa của bộ nhớ đệm: thanh trượt 3 đến
 * 10 phút, bước 1, nháp nằm trong bảng và chỉ ghi khi bấm "Áp dụng" (tắt khi chưa đổi). Đổi giá trị thì lần quay lại tab Short kế tiếp
 * lọc lại và xáo mới (SV-16). Bố cục theo mô tả đã duyệt; cần đối chiếu lại với artboard `OptShortMax` khi kiểm tay.
 */
@Composable
internal fun ShortMaxSheet(current: Int, onIntent: (SettingsIntent) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    ODVBottomSheet(onDismissRequest = { onIntent(SettingsIntent.DismissSheet) }, title = stringResource(R.string.settings_short_max)) {
        Text(stringResource(R.string.settings_short_max_hint), style = ODVTheme.typography.caption, color = ODVTheme.colors.inkMuted)
        Text(
            stringResource(R.string.settings_minutes, draft),
            style = ODVTheme.typography.heading,
            color = ODVTheme.colors.ink,
        )
        ODVSlider(
            value = draft.toFloat(),
            onValueChange = { draft = it.roundToInt() },
            valueRange = VideoSettingOptions.SHORT_MIN_MINUTES.toFloat()..VideoSettingOptions.SHORT_MAX_MINUTES.toFloat(),
            // Mốc nguyên từ 3 đến 10 nên có 6 vạch ở giữa.
            steps = VideoSettingOptions.SHORT_MAX_MINUTES - VideoSettingOptions.SHORT_MIN_MINUTES - 1,
            contentDescription = stringResource(R.string.settings_short_max_desc, draft),
        )
        ODVButton(
            text = stringResource(R.string.settings_apply),
            onClick = { onIntent(SettingsIntent.ApplyShortMax(draft)) },
            enabled = draft != current,
            fullWidth = true,
        )
    }
}
