package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.lambao.odv.core.domain.model.CacheShares
import kotlin.math.roundToInt

private const val DemoLimitBytes = 2048L * 1024 * 1024

/** Board "Slider và tỉ lệ chia" (ODV Foundations): Slider, SplitBar và cụm tỉ lệ chia bộ nhớ đệm. Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
internal fun LazyListScope.sliderSection() {
    item { SectionTitle("Slider (rãnh 8dp, núm 24dp; chạm giữ để thấy núm 32dp và vòng volt-soft)") }
    item { SliderStates() }
    item { SectionTitle("Thanh tỉ lệ và các slider tỉ lệ chia (tổng luôn 100%)") }
    item { SharesDemo() }
}

private fun kindName(kind: CacheKind) = when (kind) {
    CacheKind.Thumbnail -> "Thumbnail"
    CacheKind.Image -> "Ảnh"
    CacheKind.Video -> "Video"
    CacheKind.Pdf -> "PDF"
}

@Composable
private fun kindColor(kind: CacheKind) = with(ODVTheme.colors) {
    when (kind) {
        CacheKind.Thumbnail -> lineStrong
        CacheKind.Image -> kindPhoto
        CacheKind.Video -> kindVideo
        CacheKind.Pdf -> kindPdf
    }
}

@Composable
private fun SliderStates() {
    val colors = ODVTheme.colors
    var value by rememberSaveable { mutableStateOf(45f) }
    var limit by rememberSaveable { mutableStateOf(2f) }
    val tag = odvLocale().toLanguageTag()
    Column(Modifier.fillMaxWidth().background(colors.surface, ODVTheme.shapes.md).padding(horizontal = 16.dp, vertical = 12.dp)) {
        ODVSliderRow(
            label = "Video",
            color = colors.kindVideo,
            valueText = "${value.roundToInt()}%",
            detailText = odvFormatFileSize(DemoLimitBytes * value.roundToInt() / 100, tag),
            stateDescription = "Video ${value.roundToInt()}%",
            value = value,
            onValueChange = { value = it },
            valueRange = 0f..100f,
        )
        Text("Giới hạn tối đa: ${limit.roundToInt()} GB (không có ô loại, nhấn mặc định volt-text)", style = ODVTheme.typography.caption, color = colors.inkMuted)
        ODVSlider(
            value = limit,
            onValueChange = { limit = it.roundToInt().toFloat() },
            valueRange = 1f..10f,
            steps = 8,
            contentDescription = "Giới hạn tối đa",
        )
        Text("Vô hiệu · 0.38", style = ODVTheme.typography.caption, color = colors.inkMuted)
        ODVSliderRow(
            label = "PDF",
            color = colors.kindPdf,
            valueText = "30%",
            detailText = odvFormatFileSize(DemoLimitBytes * 30 / 100, tag),
            stateDescription = "PDF 30%",
            value = 30f,
            onValueChange = {},
            valueRange = 0f..100f,
            enabled = false,
        )
    }
}

@Composable
private fun SharesDemo() {
    val colors = ODVTheme.colors
    val saver = listSaver<CacheShares, Int>(
        save = { shares -> CacheKind.entries.map { shares[it] } },
        restore = { CacheShares(it[0], it[1], it[2], it[3]) },
    )
    var shares by rememberSaveable(stateSaver = saver) { mutableStateOf(CacheShares.Default) }
    val tag = odvLocale().toLanguageTag()
    Column(
        Modifier.fillMaxWidth().background(colors.surface, ODVTheme.shapes.md).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ODVSplitBar(
            segments = CacheKind.entries.map { ODVSplitSegment(shares[it], kindColor(it)) },
            totalText = "${CacheKind.entries.sumOf { shares[it] }}%",
            barContentDescription = CacheKind.entries.joinToString(", ", "Tỉ lệ chia: ") { "${kindName(it)} ${shares[it]}%" },
        )
        CacheKind.entries.forEach { kind ->
            val percent = shares[kind]
            val size = odvFormatFileSize(DemoLimitBytes * percent / CacheShares.TOTAL, tag)
            ODVSliderRow(
                label = kindName(kind),
                color = kindColor(kind),
                valueText = "$percent%",
                detailText = size,
                stateDescription = "${kindName(kind)} $percent%, $size",
                value = percent.toFloat(),
                onValueChange = { shares = shares.withShare(kind, it.roundToInt()) },
                valueRange = 0f..CacheShares.TOTAL.toFloat(),
            )
        }
        ODVButton("Mặc định", { shares = CacheShares.Default }, style = ODVButtonStyle.Ghost, enabled = shares != CacheShares.Default, fullWidth = true)
    }
}
