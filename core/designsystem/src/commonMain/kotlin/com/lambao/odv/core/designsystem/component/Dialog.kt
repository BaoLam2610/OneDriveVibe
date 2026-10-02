package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVElevation
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Tông icon của Dialog (mục 4.2): volt (thông tin/xác nhận), warning (cảnh báo), danger (lỗi, nguy hiểm). */
enum class ODVDialogTone { Volt, Warning, Danger }

/**
 * Thẻ Dialog (không kèm cửa sổ): rộng ≤ 360, nền `surface`, bo `lg`, padding 24, các khối cách nhau 16.
 * Tách riêng để xem trước trong gallery; trong app dùng [ODVDialog].
 *
 * @param errorCode dòng "Mã lỗi: ..." kiểu `code` màu `ink-muted`, dùng cho Dialog lỗi (KN-09).
 * @param extra khối giữa nội dung và nút, ví dụ ô nhập PIN.
 * @param actions các nút căn phải, cách nhau 8: nút phụ là Ghost, nút chính là Primary/Danger/DangerSolid.
 * @param alert Dialog nguy hiểm/lỗi (alertdialog): TalkBack đọc ngay tiêu đề khi hiện.
 * @param elevated vẽ bóng `shadow.lg`. Tắt khi thẻ nằm trong cửa sổ vừa khít (bóng sẽ bị cắt), xem [ODVDialog].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ODVDialogCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ODVIcon? = null,
    tone: ODVDialogTone = ODVDialogTone.Volt,
    body: String? = null,
    errorCode: String? = null,
    alert: Boolean = false,
    elevated: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable FlowRowScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val shape = ODVTheme.shapes.lg
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = ODVSize.dialogMaxWidth)
            .then(if (elevated) Modifier.shadow(ODVElevation.lg, shape) else Modifier)
            .background(colors.surface, shape)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            val container: Color
            val tint: Color
            when (tone) {
                ODVDialogTone.Volt -> { container = colors.voltSoft; tint = colors.voltText }
                ODVDialogTone.Warning -> { container = colors.warningSoft; tint = colors.warning }
                ODVDialogTone.Danger -> { container = colors.dangerSoft; tint = colors.danger }
            }
            Box(Modifier.size(48.dp).background(container, CircleShape), contentAlignment = Alignment.Center) {
                ODVIcon(icon, contentDescription = null, tint = tint)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                modifier = Modifier.semantics {
                    heading()
                    if (alert) liveRegion = LiveRegionMode.Assertive
                },
                style = type.title,
                color = colors.ink,
            )
            if (body != null) Text(body, style = type.body, color = colors.inkMuted)
            if (errorCode != null) Text(errorCode, style = type.code, color = colors.inkMuted)
        }
        if (extra != null) extra()
        FlowRow(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = { actions() },
        )
    }
}

/**
 * Dialog (mục 4.2) trên scrim của hệ thống. Android không cho đặt độ mờ scrim của cửa sổ Dialog, nên scrim ≈ mặc định
 * của nền tảng, không phải `#00000099` chính xác. Màu theo [ODVTheme] hiện tại.
 *
 * Dialog là cửa sổ riêng nhưng mặc định `SecureFlagPolicy.Inherit`: mở trên màn có [ODVSecureWindow] thì cũng bị chặn chụp.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ODVDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ODVIcon? = null,
    tone: ODVDialogTone = ODVDialogTone.Volt,
    body: String? = null,
    errorCode: String? = null,
    alert: Boolean = false,
    dismissOnOutsideClick: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable FlowRowScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = dismissOnOutsideClick),
    ) {
        // Cửa sổ Dialog vừa khít nội dung nên bóng sẽ bị cắt: tắt elevated.
        ODVDialogCard(title, modifier, icon, tone, body, errorCode, alert, elevated = false, extra = extra, actions = actions)
    }
}
