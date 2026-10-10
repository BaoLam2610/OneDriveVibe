package com.lambao.odv.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lambao.odv.R
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.domain.model.SecretExpiryNotice

/**
 * Banner C7 "Client Secret hết hạn sau N ngày" ở đầu Danh sách (CD-06, Q4), dùng lại Banner của Cài đặt. Hết hạn rồi thì tông Danger.
 * Chuỗi nằm ở `:androidApp` vì `:feature:settings` không để lộ tài nguyên của nó; [onUpdate] đi tới cập nhật Client Secret (CD-04).
 */
@Composable
fun SecretExpiryBanner(notice: SecretExpiryNotice, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    val text = when {
        notice.isExpired -> stringResource(R.string.home_expiry_banner_expired)
        notice.daysLeft == 0 -> stringResource(R.string.home_expiry_banner_today)
        else -> pluralStringResource(R.plurals.home_expiry_banner, notice.daysLeft, notice.daysLeft)
    }
    ODVBanner(
        text = text,
        tone = if (notice.isExpired) ODVBannerTone.Danger else ODVBannerTone.Warning,
        icon = if (notice.isExpired) ODVIcon.Alert else ODVIcon.Clock,
        modifier = modifier,
        actionLabel = stringResource(R.string.home_expiry_banner_action),
        onAction = onUpdate,
    )
}
