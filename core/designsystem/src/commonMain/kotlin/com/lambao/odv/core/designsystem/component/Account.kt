package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

// Component tài khoản (Lát T, thiet-ke-ui.md mục 4.9, TA-01 → TA-12; board Foundations 21 "Components · Tài khoản").
// Không có kiểu domain ở đây: nơi gọi truyền tên, email và câu chữ đã dịch.

/** Cỡ [ODVAccountAvatar]: AppBar 32, hàng tài khoản 40, ô tài khoản ở D10 56 (mục 4.9). */
enum class ODVAccountAvatarSize(internal val dp: Dp) {
    Sm(ODVSize.accountAvatarSm),
    Md(ODVSize.accountAvatarMd),
    Tile(ODVSize.thumbRow),
}

/**
 * AccountAvatar (mục 4.9, TA-02): hình tròn chứa chữ cái đầu của [name] viết hoa, **một màu chung** cho mọi tài khoản (nền `volt-soft`,
 * chữ `volt-text`); không tải ảnh. [aggregate] là avatar gộp của phạm vi Tất cả tài khoản: nền `surface-2`, icon `users` `ink`.
 * [hasError] vẽ chấm lỗi 8dp `danger` viền 2dp `surface` ở góc trên phải (TA-10).
 *
 * Chỉ để trang trí: nhãn TalkBack do thành phần cha đặt (nút hoặc hàng), nên avatar không đọc chữ cái.
 * [container] ghi đè màu nền, dùng ở ô D10 (nền `surface` trên ô `volt-soft`).
 */
@Composable
fun ODVAccountAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: ODVAccountAvatarSize = ODVAccountAvatarSize.Md,
    aggregate: Boolean = false,
    hasError: Boolean = false,
    container: Color? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val background = container ?: if (aggregate) colors.surface2 else colors.voltSoft
    Box(modifier.size(size.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(Modifier.size(size.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            if (aggregate) {
                ODVIcon(ODVIcon.Users, contentDescription = null, tint = colors.ink, size = if (size == ODVAccountAvatarSize.Sm) 20.dp else ODVSize.iconMd)
            } else {
                // Chữ cái đầu của tên; tên rỗng thì "?" để không có vòng trống.
                // Lấy theo code point (cặp surrogate như emoji ở đầu tên đi cùng nhau), không `take(1)` vì sẽ cắt đôi cặp surrogate.
                val initial = remember(name) {
                    val trimmed = name.trim()
                    val length = if (trimmed.length >= 2 && trimmed[0].isHighSurrogate() && trimmed[1].isLowSurrogate()) 2 else 1
                    trimmed.take(length).uppercase().ifEmpty { "?" }
                }
                Text(
                    initial,
                    style = when (size) {
                        ODVAccountAvatarSize.Sm -> type.bodySmStrong
                        ODVAccountAvatarSize.Md -> type.bodyStrong
                        ODVAccountAvatarSize.Tile -> type.heading
                    },
                    color = colors.voltText,
                )
            }
        }
        if (hasError) {
            // Chấm 8 viền 2 theo `nav-dot`; lệch ra góc để không che chữ cái.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(ODVSize.navDot + 4.dp)
                    .background(colors.surface, CircleShape)
                    .padding(2.dp)
                    .background(colors.danger, CircleShape),
            )
        }
    }
}

/**
 * Nút tài khoản trên AppBar (mục 4.4, 4.9, TA-03): vùng chạm 48 tròn chứa avatar 32. Nhấn phủ `surface-2`, focus dùng focus ring.
 * [contentDescription] bắt buộc ("Tài khoản: Bao Lam, bao@contoso.com. Chọn tài khoản").
 */
@Composable
fun ODVAccountButton(
    name: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aggregate: Boolean = false,
    hasError: Boolean = false,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.full
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val pressScale = rememberPressScale(interaction)
    Box(
        modifier = modifier
            .size(ODVSize.tapTarget)
            .pressScale(pressScale)
            .focusRing(focused, shape, colors.voltText, colors.surface)
            .background(if (pressed) colors.surface2 else Color.Transparent, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        ODVAccountAvatar(name, size = ODVAccountAvatarSize.Sm, aggregate = aggregate, hasError = hasError)
    }
}

/**
 * Hàng trong sheet Chọn tài khoản (mục 4.9, TA-04): avatar 40, tên `body-strong`, rồi [subtitle] `caption` `ink-muted` (email cắt "…" **ở
 * giữa** để giữ tên miền). Mục đang chọn có dấu `check` `volt-text`, không đổi nền (giống OptionRow). [error] thay dòng phụ bằng icon
 * `alert` 16 và chữ `danger` (TA-10); hàng vẫn chọn được. Cả hàng là một lựa chọn cho TalkBack.
 */
@Composable
fun ODVAccountPickerRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aggregate: Boolean = false,
    error: String? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ODVSize.settingsRowMin)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVAccountAvatar(name, size = ODVAccountAvatarSize.Md, aggregate = aggregate)
        Column(Modifier.weight(1f)) {
            Text(name, style = type.bodyStrong, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (error != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ODVIcon(ODVIcon.Alert, contentDescription = null, tint = colors.danger, size = 16.dp)
                    Text(error, style = type.caption, color = colors.danger, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(subtitle, style = type.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            }
        }
        if (selected) ODVIcon(ODVIcon.Check, contentDescription = null, tint = colors.voltText)
    }
}

/**
 * Hàng tài khoản ở Cài đặt (mục 4.9, AC9, TA-03, TA-08): avatar 40; dòng 1 tên `body-strong` kèm viên [currentBadge] ("Đang xem") ngay cạnh
 * tên khi là tài khoản đang xem; dòng 2 email `caption` cắt giữa; dòng 3 [statusLine] `caption` ("Đồng bộ lúc…", "Đang lập chỉ mục…") hoặc
 * lỗi (`danger`, kèm icon `alert`) khi [statusIsError].
 *
 * [onRemove] khác null thì có nút xóa (IconButton 48, `trash` `ink-muted`) ở cuối hàng; nút hoặc cả hàng ([onClick]) mở hộp thoại xóa. Tài
 * khoản đang xem và trường hợp chỉ có một tài khoản không có nút xóa (TA-08): truyền `onRemove = null`. Có [onRemove] thì
 * [removeContentDescription] bắt buộc (nhãn TalkBack của nút xóa).
 */
@Composable
fun ODVAccountSettingsRow(
    name: String,
    email: String,
    statusLine: String,
    modifier: Modifier = Modifier,
    statusIsError: Boolean = false,
    currentBadge: String? = null,
    removeContentDescription: String? = null,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = ODVSize.settingsRowMin)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = if (onRemove != null) 4.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVAccountAvatar(name, size = ODVAccountAvatarSize.Md)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = type.bodyStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (currentBadge != null) {
                    Box(
                        Modifier
                            .height(24.dp)
                            .clip(ODVTheme.shapes.full)
                            .background(colors.voltSoft)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(currentBadge, style = type.label, color = colors.voltText, maxLines = 1)
                    }
                }
            }
            Text(email, style = type.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            if (statusIsError) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ODVIcon(ODVIcon.Alert, contentDescription = null, tint = colors.danger, size = 16.dp)
                    Text(statusLine, style = type.caption, color = colors.danger, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(statusLine, style = type.caption, color = colors.inkMuted)
            }
        }
        if (onRemove != null) {
            ODVIconButton(
                icon = ODVIcon.Trash,
                // Nút icon không nhãn là lỗi trợ năng: có [onRemove] thì bắt buộc có [removeContentDescription].
                contentDescription = requireNotNull(removeContentDescription) { "ODVAccountSettingsRow: onRemove != null thì removeContentDescription bắt buộc" },
                onClick = onRemove,
                colors = ODVIconButtonColors(Color.Transparent, colors.inkMuted),
            )
        }
    }
}

/**
 * Hàng "Thêm tài khoản" (mục 4.9, TA-07): vòng 40 nền `volt-soft` chứa icon `user-plus` 24 `volt-text`, chữ `body-strong` `volt-text`. Ở
 * Cài đặt là NavRow nên có `chevron-right` 20 ([showChevron]) và đệm ngang [horizontalPadding] 16; trong sheet không có chevron và
 * không đệm (sheet đã có lề 16).
 */
@Composable
fun ODVAddAccountRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    horizontalPadding: Dp = 0.dp,
) {
    val colors = ODVTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = ODVSize.settingsRowMin)
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(ODVSize.accountAvatarMd).background(colors.voltSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ODVIcon(ODVIcon.UserPlus, contentDescription = null, tint = colors.voltText)
        }
        Text(title, modifier = Modifier.weight(1f), style = ODVTheme.typography.bodyStrong, color = colors.voltText)
        if (showChevron) ODVIcon(ODVIcon.ChevronRight, contentDescription = null, tint = colors.inkMuted, size = 20.dp)
    }
}

/**
 * Ô tài khoản dạng lưới ở gốc Tất cả tài khoản (D10, TA-05): như [ODVFolderCard] nhưng thay icon `folder` bằng avatar 56 nền `surface`
 * trên ô `volt-soft`; tên `body-sm-strong`, dòng meta là email hoặc lỗi [error] (`danger`, icon `alert` 16, TA-10).
 */
@Composable
fun ODVAccountCard(
    name: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ODVSize.folderCardThumb)
                .clip(ODVTheme.shapes.md)
                .background(colors.voltSoft),
            contentAlignment = Alignment.Center,
        ) {
            ODVAccountAvatar(name, size = ODVAccountAvatarSize.Tile, container = colors.surface)
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(name, style = type.bodySmStrong, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (error != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ODVIcon(ODVIcon.Alert, contentDescription = null, tint = colors.danger, size = 16.dp)
                    Text(error, style = type.meta, color = colors.danger, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(meta, style = type.meta, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            }
        }
    }
}
