package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Năm tông Banner (mục 4.2). */
enum class ODVBannerTone { Neutral, Volt, Warning, Danger, Success }

/**
 * Banner trạng thái của cả màn: padding 12, bo `sm`, icon 20 + chữ `body-sm` 500, nút hành động cao 36 bên phải.
 * Tông Danger đọc ngay (vùng live assertive); các tông khác đọc lịch sự (polite). Vùng live đặt trên chính dòng chữ.
 *
 * @param progress có giá trị thì hiện thanh tiến độ 4dp dưới chữ (ví dụ "Đang lập chỉ mục").
 */
@Composable
fun ODVBanner(
    text: String,
    tone: ODVBannerTone,
    icon: ODVIcon,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val container: Color
    val content: Color
    when (tone) {
        ODVBannerTone.Neutral -> { container = colors.surface2; content = colors.ink }
        ODVBannerTone.Volt -> { container = colors.voltSoft; content = colors.voltText }
        ODVBannerTone.Warning -> { container = colors.warningSoft; content = colors.warning }
        ODVBannerTone.Danger -> { container = colors.dangerSoft; content = colors.danger }
        ODVBannerTone.Success -> { container = colors.successSoft; content = colors.success }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, ODVTheme.shapes.sm)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVIcon(icon, contentDescription = null, tint = content, size = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text,
                modifier = Modifier.semantics {
                    liveRegion = if (tone == ODVBannerTone.Danger) LiveRegionMode.Assertive else LiveRegionMode.Polite
                },
                style = type.bodySm.copy(fontWeight = FontWeight.Medium),
                color = content,
            )
            if (progress != null) ODVProgressBar(progress)
        }
        if (actionLabel != null && onAction != null) {
            // Cao 36 theo spec (mục 4.2). Đây là chỗ spec tự đặt thấp hơn vùng chạm 48 của mục 1.6.
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clickable(role = Role.Button, onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}
