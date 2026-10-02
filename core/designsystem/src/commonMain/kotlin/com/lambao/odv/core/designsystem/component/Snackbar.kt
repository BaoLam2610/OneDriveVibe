package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVElevation
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.delay

/**
 * Snackbar (mục 4.2): cao tối thiểu 48, padding 6/6/6/16, bo `sm`, nền `inverse-surface`, chữ `body-sm` `inverse-ink`,
 * hành động cao 36 chữ đậm 14 màu `inverse-accent`, bóng `shadow.lg`.
 */
@Composable
fun ODVSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.sm
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(ODVElevation.lg, shape)
            .background(colors.inverseSurface, shape)
            .heightIn(min = ODVSize.snackbarMin)
            .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = ODVTheme.typography.bodySm,
            color = colors.inverseInk,
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clickable(role = Role.Button, onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = ODVTheme.typography.bodySm.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.inverseAccent,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Nơi hiện Snackbar: lề 16 hai bên, cách đáy 24. Đặt ở đáy màn hình, trên nội dung.
 *
 * Mỗi snackbar tự ẩn sau 3 giây (`duration.snackbar`) tính từ lúc nó thật sự hiện (không phải lúc xếp hàng), và được kéo dài
 * theo cài đặt trợ năng của hệ thống (người dùng TalkBack có thêm thời gian để bấm hành động).
 */
@Composable
fun ODVSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val accessibilityManager = LocalAccessibilityManager.current
    SnackbarHost(hostState = state, modifier = modifier) { data ->
        val hasAction = data.visuals.actionLabel != null
        LaunchedEffect(data) {
            val base = ODVDuration.snackbar.toLong()
            val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = base,
                containsIcons = false,
                containsText = true,
                containsControls = hasAction,
            ) ?: base
            delay(timeout)
            data.dismiss()
        }
        ODVSnackbar(
            message = data.visuals.message,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            actionLabel = data.visuals.actionLabel,
            onAction = { data.performAction() },
        )
    }
}

/** Hiện Snackbar; [ODVSnackbarHost] lo việc tự ẩn. Suspend đến khi snackbar bị ẩn hoặc người dùng bấm hành động. */
suspend fun SnackbarHostState.showODVSnackbar(message: String, actionLabel: String? = null): SnackbarResult =
    showSnackbar(message = message, actionLabel = actionLabel, duration = SnackbarDuration.Indefinite)
