package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVRadius
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Bottom sheet (mục 4.2): rộng toàn màn, nền `surface`, bo `lg` hai góc trên, padding 8/16/32, tay nắm 36 × 4 `line-strong`,
 * tiêu đề `heading`. Vuốt xuống hoặc chạm scrim để đóng.
 *
 * Màu hoàn toàn theo [ODVTheme] hiện tại (theo hệ thống hoặc cài đặt Giao diện của app); component không biết nó mở từ đâu.
 * Sheet là cửa sổ riêng nhưng mặc định `SecureFlagPolicy.Inherit`: mở trên màn có [ODVSecureWindow] thì cũng bị chặn chụp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ODVBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // Bỏ trạng thái nửa mở: chỉ Hidden và Expanded (thay cho rememberModalBottomSheetState(skipPartiallyExpanded = true) đã deprecated).
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        shape = RoundedCornerShape(topStart = ODVRadius.lg, topEnd = ODVRadius.lg),
        containerColor = colors.surface,
        contentColor = colors.ink,
        scrimColor = colors.scrim,
        dragHandle = {
            // Tay nắm: đệm 8 của sheet + lề 4 phía trên, lề 8 phía dưới (theo HTML thiết kế).
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(ODVSize.sheetHandleWidth, ODVSize.sheetHandleHeight)
                    .background(colors.lineStrong, RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                modifier = Modifier.padding(vertical = 4.dp).semantics { heading() },
                style = ODVTheme.typography.heading,
                color = colors.ink,
            )
            content()
        }
    }
}

/**
 * Hàng lựa chọn trong sheet (OptionRow): chạm là đặt giá trị và đóng sheet ngay, không có nút Lưu.
 * [checkStyle] = true dùng dấu check thay cho radio (sheet tốc độ trên video); khi đó [mono] mới có tác dụng.
 */
@Composable
fun ODVOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    mono: Boolean = false,
    checkStyle: Boolean = false,
) {
    if (!checkStyle) {
        ODVRadioRow(label, selected, onClick, modifier, description)
        return
    }
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ODVSize.tapTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(24.dp)) {
            if (selected) ODVIcon(ODVIcon.Check, contentDescription = null, tint = colors.voltText)
        }
        // Mô tả nằm dưới nhãn như ODVRadioRow; weight để nhãn dài xuống dòng thay vì đẩy tràn.
        Column(Modifier.weight(1f)) {
            Text(label, style = if (mono) type.code.copy(fontSize = type.body.fontSize) else type.body, color = colors.ink)
            if (description != null) Text(description, style = type.caption, color = colors.inkMuted)
        }
    }
}

/**
 * Hàng thông tin tệp (InfoRow, mục 4.5): nhãn `ink-muted` bên trái rộng 120, giá trị bên phải, kẻ đáy `line`, padding dọc 8.
 * Số liệu dùng [mono] (JetBrains Mono). Trường nào không có dữ liệu thì không vẽ hàng.
 * Nhãn và giá trị gộp thành một điểm dừng của TalkBack.
 */
@Composable
fun ODVInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val valueStyle: TextStyle = if (mono) type.code.copy(fontSize = type.bodySm.fontSize, lineHeight = type.bodySm.lineHeight) else type.bodySm
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                drawLine(colors.line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.width(120.dp), style = type.bodySm, color = colors.inkMuted)
        Text(value, style = valueStyle, color = colors.ink)
    }
}
