package com.lambao.odv.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.logo.ODVLogoVariant
import com.lambao.odv.core.designsystem.theme.ODVSpacing
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Trang "Màu": màu theo theme (đổi theo chip Sáng/Tối) và màu màn xem. */
internal fun LazyListScope.colorsPage() {
    item { SectionTitle("Màu theo theme (mục 2.1)") }
    item {
        val colors = ODVTheme.colors
        Column { themeColorSwatches(colors).forEach { (name, color) -> Swatch(name, color) } }
    }
    item { SectionTitle("Màu màn xem, không đổi theo theme (mục 2.2)") }
    item {
        // Nền đen để thấy các màu trong suốt đúng như trên màn xem.
        Column(Modifier.background(Color.Black, ODVTheme.shapes.md).padding(ODVSpacing.s3)) {
            mediaColorSwatches().forEach { (name, color) -> Swatch(name, color, onMedia = true) }
        }
    }
}

/** Trang "Chữ": 21 style (mục 2.3). */
internal fun LazyListScope.typographyPage() {
    item { SectionTitle("21 style chữ") }
    items(typographySamples) { (name, mono) ->
        val colors = ODVTheme.colors
        Column(Modifier.padding(vertical = ODVSpacing.s1)) {
            Text(name, style = ODVTheme.typography.meta, color = colors.inkFaint)
            Text(
                if (mono) "12:04 / 1:26:02 · a1b2••••9f0e" else "Tiếng Việt có dấu: Kết nối OneDrive",
                style = styleByName(name),
                color = colors.ink,
            )
        }
    }
}

/** Trang "Khoảng cách và bo góc" (mục 2.4, 2.5). */
internal fun LazyListScope.spacingPage() {
    item { SectionTitle("Khoảng cách") }
    item {
        Column {
            SpaceRow("half", ODVSpacing.half)
            SpaceRow("s1", ODVSpacing.s1)
            SpaceRow("s2", ODVSpacing.s2)
            SpaceRow("s3", ODVSpacing.s3)
            SpaceRow("s4", ODVSpacing.s4)
            SpaceRow("s6", ODVSpacing.s6)
            SpaceRow("s8", ODVSpacing.s8)
        }
    }
    item { SectionTitle("Bo góc") }
    item {
        val shapes = ODVTheme.shapes
        Row(horizontalArrangement = Arrangement.spacedBy(ODVSpacing.s3)) {
            RadiusBox("xs", shapes.xs, "6")
            RadiusBox("sm", shapes.sm, "10")
            RadiusBox("md", shapes.md, "14")
            RadiusBox("lg", shapes.lg, "22")
            RadiusBox("full", shapes.full, "tròn")
        }
    }
}

/** Trang "Icon và Logo": 43 icon (mục 3.1) và 3 biến thể logo (mục 3.2). */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.iconsPage() {
    item { SectionTitle("Icon (${ODVIcon.entries.size})") }
    item {
        val colors = ODVTheme.colors
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVIcon.entries.forEach { icon ->
                Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ODVIcon(icon, contentDescription = icon.name, tint = colors.ink)
                    Text(icon.name, style = ODVTheme.typography.meta, color = colors.inkMuted, maxLines = 1)
                }
            }
        }
    }
    item { SectionTitle("Logo: màu, ink, trắng") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ODVLogo(size = 48.dp)
            ODVLogo(size = 48.dp, variant = ODVLogoVariant.Ink)
            ODVLogo(size = 48.dp, variant = ODVLogoVariant.White)
        }
    }
}

@Composable
internal fun styleByName(name: String): TextStyle {
    val t = ODVTheme.typography
    return when (name) {
        "display" -> t.display
        "title" -> t.title
        "heading" -> t.heading
        "body" -> t.body
        "bodyStrong" -> t.bodyStrong
        "button" -> t.button
        "buttonSm" -> t.buttonSm
        "caption" -> t.caption
        "label" -> t.label
        "timecode" -> t.timecode
        "code" -> t.code
        "screenTitle" -> t.screenTitle
        "stateTitle" -> t.stateTitle
        "bodySm" -> t.bodySm
        "bodySmStrong" -> t.bodySmStrong
        "captionStrong" -> t.captionStrong
        "meta" -> t.meta
        "stepLabel" -> t.stepLabel
        "timecodeSm" -> t.timecodeSm
        "timecodeXs" -> t.timecodeXs
        else -> t.timer
    }
}
