package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSpacing
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.designsystem.theme.ODVThemeMode

/** Khung chung cho mọi trang gallery: tiêu đề, nút quay lại (nếu có), bộ chọn theme, rồi nội dung cuộn. */
@Composable
internal fun GalleryPage(
    title: String,
    mode: ODVThemeMode,
    onModeChange: (ODVThemeMode) -> Unit,
    onBack: (() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = ODVSpacing.s4, vertical = ODVSpacing.s2),
        verticalArrangement = Arrangement.spacedBy(ODVSpacing.s2),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", onBack)
                Text(title, style = ODVTheme.typography.title, color = colors.ink, modifier = Modifier.padding(start = if (onBack == null) ODVSpacing.s2 else ODVSpacing.half))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(ODVSpacing.s2)) {
                ODVThemeMode.entries.forEach { entry ->
                    ODVChip(entry.name, selected = entry == mode, onClick = { onModeChange(entry) })
                }
            }
        }
        content()
    }
}
