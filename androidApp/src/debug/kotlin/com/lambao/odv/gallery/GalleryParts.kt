package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVSpacing
import com.lambao.odv.core.designsystem.theme.ODVTheme

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = ODVSpacing.s6, bottom = ODVSpacing.s2),
        style = ODVTheme.typography.captionStrong,
        color = ODVTheme.colors.voltText,
    )
}

@Composable
internal fun Swatch(name: String, color: Color, onMedia: Boolean = false) {
    val colors = ODVTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ODVSpacing.s3),
    ) {
        Box(
            Modifier
                .size(ODVSize.buttonSm)
                .background(color, ODVTheme.shapes.xs)
                .border(1.dp, colors.lineStrong, ODVTheme.shapes.xs),
        )
        Text(
            name,
            modifier = Modifier.width(140.dp),
            style = ODVTheme.typography.bodySm,
            color = if (onMedia) ODVTheme.media.onMedia else colors.ink,
        )
        Text(
            "#%08X".format(color.toArgb()),
            style = ODVTheme.typography.code,
            color = if (onMedia) ODVTheme.media.onMediaMuted else colors.inkMuted,
        )
    }
}

@Composable
internal fun SpaceRow(name: String, size: Dp) {
    val colors = ODVTheme.colors
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ODVSpacing.s3),
    ) {
        Text("$name · ${size.value}dp", modifier = Modifier.width(100.dp), style = ODVTheme.typography.code, color = colors.inkMuted)
        Box(Modifier.height(ODVSpacing.s2).width(size).background(colors.volt))
    }
}

@Composable
internal fun RadiusBox(name: String, shape: Shape, caption: String) {
    val colors = ODVTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(48.dp)
                .background(colors.surface, shape)
                .border(1.dp, colors.line, shape),
        )
        Text(name, style = ODVTheme.typography.meta, color = colors.ink)
        Text(caption, style = ODVTheme.typography.meta, color = colors.inkMuted)
    }
}
