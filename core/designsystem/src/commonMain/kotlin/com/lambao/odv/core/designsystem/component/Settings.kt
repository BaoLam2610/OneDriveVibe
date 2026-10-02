package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVOpacity
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Các hàng của một [ODVSettingsGroup]; mỗi [row] là một hàng, nhóm tự vẽ kẻ ngăn giữa các hàng (hàng cuối không có). */
class ODVSettingsGroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable () -> Unit>()

    fun row(content: @Composable () -> Unit) {
        rows += content
    }
}

/**
 * Nhóm cài đặt (SettingsGroup, mục 4.6): tiêu đề `caption-strong` giãn 0.02em màu `volt-text` nằm ngoài thẻ (cách trên 24,
 * trái 16, dưới 8); thẻ lề 16, nền `surface`, viền 1dp `line`, bo `md`, các hàng kẻ `line` ở giữa.
 *
 * ```
 * ODVSettingsGroup("Bảo mật") {
 *     row { ODVSwitchRow(...) }
 *     row { ODVValueRow(...) }
 * }
 * ```
 */
@Composable
fun ODVSettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: ODVSettingsGroupScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.md
    val rows = ODVSettingsGroupScope().apply(content).rows
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp).semantics { heading() },
                style = ODVTheme.typography.captionStrong.copy(letterSpacing = 0.02.em),
                color = colors.voltText,
            )
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.line, shape),
        ) {
            rows.forEachIndexed { index, row ->
                // key theo vị trí để state bên trong hàng không lệch khi nơi gọi thêm/bớt hàng theo điều kiện.
                key(index) { row() }
                if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            }
        }
    }
}

/** Khung chung của một hàng cài đặt: cao tối thiểu [minHeight], padding 8/16, phần tử bên phải cách 16. */
@Composable
private fun SettingsRowFrame(
    modifier: Modifier,
    minHeight: Dp,
    trailing: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { content() }
        if (trailing != null) trailing()
    }
}

@Composable
private fun SettingsRowText(
    title: String,
    description: String?,
    titleStyle: TextStyle,
    titleColor: Color,
) {
    val type = ODVTheme.typography
    Column {
        Text(title, style = titleStyle, color = titleColor)
        if (description != null) Text(description, style = type.caption, color = ODVTheme.colors.inkMuted)
    }
}

/**
 * SwitchRow (mục 4.6): cả hàng là một công tắc, chạm đâu cũng đổi; tên `body`, mô tả `caption` `ink-muted`.
 * Switch bên phải chỉ để hiển thị nên TalkBack chỉ thấy một điều khiển với trạng thái bật/tắt.
 */
@Composable
fun ODVSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    SettingsRowFrame(
        modifier = modifier
            .alpha(if (enabled) 1f else ODVOpacity.disabled)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        minHeight = ODVSize.settingsRowMin,
        trailing = { ODVSwitch(checked, onCheckedChange = null, contentDescription = null) },
    ) {
        SettingsRowText(title, description, ODVTheme.typography.body, ODVTheme.colors.ink)
    }
}

/** ValueRow (mục 4.6): giá trị hiện tại `body-sm` `ink-muted` kèm `chevron-right` 20; mở bottom sheet chọn giá trị. */
@Composable
fun ODVValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = ODVTheme.colors
    SettingsRowFrame(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        minHeight = ODVSize.settingsRowMin,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = ODVTheme.typography.bodySm, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ODVIcon(ODVIcon.ChevronRight, contentDescription = null, tint = colors.inkMuted, size = 20.dp)
            }
        },
    ) {
        SettingsRowText(title, description, ODVTheme.typography.body, colors.ink)
    }
}

/** NavRow (mục 4.6): chỉ có `chevron-right` 20; mở màn con. */
@Composable
fun ODVNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = ODVTheme.colors
    SettingsRowFrame(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        minHeight = ODVSize.settingsRowMin,
        trailing = { ODVIcon(ODVIcon.ChevronRight, contentDescription = null, tint = colors.inkMuted, size = 20.dp) },
    ) {
        SettingsRowText(title, description, ODVTheme.typography.body, colors.ink)
    }
}

/** InfoRow của Cài đặt (mục 4.6): cao 48, giá trị `code` `ink-muted` căn phải (ID đã che, mã phiên bản...). */
@Composable
fun ODVSettingsInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    SettingsRowFrame(
        modifier = modifier.semantics(mergeDescendants = true) {},
        minHeight = ODVSize.infoRowMin,
        trailing = { Text(value, style = ODVTheme.typography.code, color = colors.inkMuted, maxLines = 1) },
    ) {
        Text(title, style = ODVTheme.typography.body, color = colors.ink)
    }
}

/** ActionRow (mục 4.6): không có phần tử bên phải; chạm mở hộp thoại xác nhận. */
@Composable
fun ODVActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    SettingsRowFrame(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        minHeight = ODVSize.settingsRowMin,
        trailing = null,
    ) {
        SettingsRowText(title, description, ODVTheme.typography.body, ODVTheme.colors.ink)
    }
}

/** DangerRow (mục 4.6): như ActionRow nhưng tên màu `danger` đậm 600, cho hành động không thể hoàn tác. */
@Composable
fun ODVDangerRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    SettingsRowFrame(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        minHeight = ODVSize.settingsRowMin,
        trailing = null,
    ) {
        SettingsRowText(title, description, ODVTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), ODVTheme.colors.danger)
    }
}

/**
 * ChipsRow (mục 4.6, Loại tệp hiển thị): tên + mô tả, rồi các chip chọn nhiều cách 8 (cách trên 10).
 * Khi chỉ còn một chip bật, nơi gọi không cho tắt chip đó và truyền [warning] ("Phải bật ít nhất 1 loại tệp"):
 * dòng `caption` màu `warning` kèm icon `alert` 16, đọc ngay.
 *
 * @param chips các [ODVChip] của hàng.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ODVChipsRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    warning: String? = null,
    chips: @Composable FlowRowScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    SettingsRowFrame(modifier = modifier, minHeight = ODVSize.settingsRowMin, trailing = null) {
        Column {
            SettingsRowText(title, description, type.body, colors.ink)
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = chips,
            )
            if (warning != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ODVIcon(ODVIcon.Alert, contentDescription = null, tint = colors.warning, size = 16.dp)
                    Text(
                        warning,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        style = type.caption,
                        color = colors.warning,
                    )
                }
            }
        }
    }
}

/**
 * Một phân đoạn của [ODVCacheUsage]. [fraction] là tỉ lệ so với giới hạn (0..1), [color] là màu loại tệp.
 */
@Immutable
class ODVCacheSegment(val label: String, val valueText: String, val fraction: Float, val color: Color)

/**
 * CacheUsage (mục 4.6, BN-01): tên và "1,4 GB / 2 GB" (`code`) trên một dòng; thanh phân đoạn cao 8 bo 4, rãnh `surface-2`,
 * các đoạn cách 1dp, độ dài theo giới hạn; chú thích lưới 2 cột (ô màu 10 bo 3, tên `ink-muted`, dung lượng mono căn phải).
 *
 * @param barContentDescription ví dụ "Đã dùng 1,4 GB trên 2 GB"; chú thích bên dưới vẫn đọc được từng loại.
 */
@Composable
fun ODVCacheUsage(
    title: String,
    usageText: String,
    segments: List<ODVCacheSegment>,
    barContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, modifier = Modifier.weight(1f), style = type.body, color = colors.ink)
            Text(usageText, style = type.code, color = colors.inkMuted, maxLines = 1)
        }
        Row(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surface2)
                .semantics { contentDescription = barContentDescription },
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            // Dùng weight (không phải fillMaxWidth(fraction): Row đo con trên phần còn lại nên các đoạn sau sẽ bị ngắn).
            // Bỏ đoạn rỗng (weight(0f) ném lỗi); tổng vượt 1 thì chuẩn hóa; thiếu thì chừa rãnh trống ở cuối.
            val drawn = segments.filter { !it.fraction.isNaN() && it.fraction > 0f }
            val total = drawn.sumOf { it.fraction.toDouble() }.toFloat()
            val scale = if (total > 1f) 1f / total else 1f
            drawn.forEach { segment ->
                Box(Modifier.weight(segment.fraction * scale).fillMaxHeight().background(segment.color))
            }
            val rest = 1f - total * scale
            if (rest > 0.0001f) Spacer(Modifier.weight(rest))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    pair.forEach { segment ->
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).background(segment.color, RoundedCornerShape(3.dp)))
                            Text(segment.label, style = type.caption, color = colors.inkMuted, modifier = Modifier.weight(1f))
                            Text(segment.valueText, style = type.code.copy(fontSize = type.caption.fontSize, lineHeight = type.caption.lineHeight), color = colors.ink)
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}
