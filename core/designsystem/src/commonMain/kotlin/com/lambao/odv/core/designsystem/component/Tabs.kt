package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

@Immutable
class ODVTab(val label: String, val icon: ODVIcon)

/**
 * Tabs dạng viên thuốc (Thư mục / Thư viện, mục 4.1): khung `surface-2` padding 4, mỗi tab cao 40 chia đều.
 * Tab chọn: nền `ink`, chữ `bg` (đảo màu). Tab không chọn: chữ `ink-muted`.
 */
@Composable
fun ODVTabs(
    tabs: List<ODVTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.full
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface2, shape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val content = if (selected) colors.bg else colors.inkMuted
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(ODVSize.tabItem)
                    // clip trước selectable để gợn chạm bo theo viên thuốc, không vuông ở góc.
                    .clip(shape)
                    .background(if (selected) colors.ink else Color.Transparent, shape)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ODVIcon(tab.icon, contentDescription = null, tint = content, size = 18.dp)
                Text(tab.label, style = ODVTheme.typography.buttonSm, color = content, maxLines = 1)
            }
        }
    }
}
