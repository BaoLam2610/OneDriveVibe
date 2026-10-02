package com.lambao.odv.feature.auth.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVCard
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVFullScreenLoader
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVStepBar
import com.lambao.odv.core.designsystem.component.ODVSwitch
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.feature.auth.R

/**
 * Giao diện màn Thiết lập bảo mật, trạng thái B7 "Tắt bảo vệ" (thiet-ke-ui.md mục 5.1): icon `lock` 40 + StepBar 2/2,
 * tiêu đề `display`, thẻ công tắc "Bảo vệ ứng dụng", cảnh báo BM-03, nút "Hoàn tất" sát đáy.
 *
 * Công tắc chỉ để hiển thị (tắt) ở Lát 1; Lát 2 cho bật và thêm khối nhập PIN (BM-01, BM-02).
 */
@Composable
internal fun ODVSecuritySetupContent(
    state: SecuritySetupState,
    onIntent: (SecuritySetupIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography

    // BM-04: đang mã hóa và lưu thì khóa mọi thao tác, kể cả nút Back.
    BackHandler(enabled = state.isSaving) {}

    Box(modifier.fillMaxSize()) {
        ODVScaffold { contentPadding ->
            Column(Modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding())) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ODVIcon(ODVIcon.Lock, contentDescription = null, tint = colors.voltText, size = 40.dp)
                        ODVStepBar(stringResource(R.string.security_step), step = 2, total = 2, modifier = Modifier.weight(1f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.security_title), style = type.display, color = colors.ink)
                        Text(stringResource(R.string.security_subtitle), style = type.body, color = colors.inkMuted)
                    }
                    ODVCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.security_toggle_title), style = type.bodyStrong, color = colors.ink)
                                Text(
                                    stringResource(R.string.security_toggle_description),
                                    style = type.caption,
                                    color = colors.inkMuted,
                                )
                            }
                            ODVSwitch(
                                checked = false,
                                onCheckedChange = null,
                                contentDescription = stringResource(R.string.security_toggle_title),
                            )
                        }
                    }
                    ODVBanner(
                        text = stringResource(R.string.security_warning),
                        tone = ODVBannerTone.Warning,
                        icon = ODVIcon.Alert,
                    )
                }
                ODVButton(
                    text = stringResource(R.string.security_complete),
                    onClick = { onIntent(SecuritySetupIntent.Complete) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    enabled = !state.isSaving,
                    fullWidth = true,
                )
            }
        }

        if (state.isSaving) {
            ODVFullScreenLoader(
                title = stringResource(R.string.security_saving_title),
                loadingDescription = stringResource(R.string.security_saving_description),
            )
        }
    }

    if (state.saveFailed) {
        ODVDialog(
            onDismissRequest = { onIntent(SecuritySetupIntent.DismissSaveFailure) },
            title = stringResource(R.string.security_save_failed_title),
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Danger,
            body = stringResource(R.string.security_save_failed_body),
            alert = true,
        ) {
            ODVButton(stringResource(R.string.security_save_failed_close), { onIntent(SecuritySetupIntent.DismissSaveFailure) })
        }
    }
}
