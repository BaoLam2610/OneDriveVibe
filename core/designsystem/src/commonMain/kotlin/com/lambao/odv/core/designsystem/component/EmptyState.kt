package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Trạng thái rỗng (EmptyState, mục 4.4): căn giữa, padding ngang 40; vòng 72 nền `surface-2` chứa icon 32 `ink-muted`;
 * tiêu đề `state-title`, nội dung `body` `ink-muted`, tối đa một [action] (thường là nút Secondary).
 * Nơi gọi đặt kích thước (thường `fillMaxSize()`).
 */
@Composable
fun ODVEmptyState(
    icon: ODVIcon,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Box(modifier.fillMaxWidth().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(ODVSize.emptyIconWell).background(colors.surface2, CircleShape), contentAlignment = Alignment.Center) {
                ODVIcon(icon, contentDescription = null, tint = colors.inkMuted, size = 32.dp)
            }
            Text(
                title,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
                style = type.stateTitle,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            if (body != null) Text(body, style = type.body, color = colors.inkMuted, textAlign = TextAlign.Center)
            if (action != null) Box(Modifier.padding(top = 8.dp)) { action() }
        }
    }
}
