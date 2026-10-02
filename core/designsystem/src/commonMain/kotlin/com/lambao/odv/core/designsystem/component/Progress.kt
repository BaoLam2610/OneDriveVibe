package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Màu của [ODVProgressBar]: rãnh và phần đã có. */
@Immutable
class ODVProgressColors(val track: Color, val indicator: Color)

object ODVProgressBarDefaults {
    /** Rãnh `surface-2`, phần đã có `volt-text` (mục 4.1). */
    @Composable
    fun colors() = ODVProgressColors(ODVTheme.colors.surface2, ODVTheme.colors.voltText)
}

/**
 * Thanh tiến độ tròn (mục 4.1): cao 4 (trong hàng), 6 (tải PDF) hoặc 8 (dung lượng drive).
 * [progress] trong 0..1; giá trị ngoài khoảng hoặc NaN được đưa về trong khoảng.
 */
@Composable
fun ODVProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    colors: ODVProgressColors = ODVProgressBarDefaults.colors(),
) {
    val shape = ODVTheme.shapes.full
    val fraction = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
            .background(colors.track, shape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(colors.indicator, shape),
        )
    }
}

/**
 * StepBar (mục 4.1): nhãn `step-label` ("BƯỚC 1 / 2") màu `volt-text`, bên dưới cách 6 là các đoạn cao 4 cách nhau 4.
 * Đoạn đã qua `volt-text`, chưa qua `surface-2`. [step] đếm từ 1.
 */
@Composable
fun ODVStepBar(
    label: String,
    step: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.full
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = ODVTheme.typography.stepLabel, color = colors.voltText)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (index < step) colors.voltText else colors.surface2, shape),
                )
            }
        }
    }
}
