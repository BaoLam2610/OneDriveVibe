package com.lambao.odv.feature.auth.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.feature.auth.R

/**
 * Giao diện màn Thiết lập bảo mật (thiet-ke-ui.md mục 5.1): mũi tên Back cố định ở topBar (BM-08), icon `lock` 40 màu
 * `volt-text`, tiêu đề `display`. Không có StepBar và không có thẻ công tắc "Bảo vệ ứng dụng" (BM-01).
 *
 * Khối PIN (PinDots, thông báo, bàn phím số tự vẽ) thêm ở Lát 2.
 */
@Composable
internal fun ODVSecuritySetupContent(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography

    ODVScaffold(
        modifier = modifier,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp)) {
                ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.security_back), onBack)
            }
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = contentPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ODVIcon(ODVIcon.Lock, contentDescription = null, tint = colors.voltText, size = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.security_title), style = type.display, color = colors.ink)
                Text(stringResource(R.string.security_subtitle), style = type.body, color = colors.inkMuted)
            }
        }
    }
}
