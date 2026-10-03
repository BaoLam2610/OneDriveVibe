package com.lambao.odv.tools.debug

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Nút icon gọn của công cụ debug: vùng chạm 48dp, icon 20dp, tô theo token màu. Icon lấy từ `res/drawable` của module này
 * (`ic_debug_*`) vì không thuộc bộ icon sản phẩm. [contentDescription] bắt buộc cho TalkBack.
 */
@Composable
internal fun DebugIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ODVTheme.colors.inkMuted,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}
