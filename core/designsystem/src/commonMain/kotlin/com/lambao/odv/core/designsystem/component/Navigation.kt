package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * AppBar (mục 4.4): cao 56, padding ngang 4, tiêu đề `title` một dòng cắt "…", nút bên phải là [actions] (mỗi nút 48).
 *
 * @param navigation ở gốc là [ODVAppBarLogo]; trong thư mục con là `ODVIconButton(ArrowLeft, "Lên một cấp", ...)`.
 */
@Composable
fun ODVAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ODVSize.appBar)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = ODVTheme.typography.title,
            color = ODVTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

/** Logo 28 cách lề 12, đặt ở bên trái AppBar gốc (mục 3.2, 4.4). */
@Composable
fun ODVAppBarLogo(modifier: Modifier = Modifier) {
    ODVLogo(modifier.padding(start = 12.dp), size = 28.dp)
}

/**
 * AppBar tìm kiếm (mục 4.4, DS-03): nút quay lại, rồi ô tìm cao 48 tròn nền `surface-2` gồm icon `search` 20,
 * chữ nhập `body`, placeholder, và nút X xóa từ khóa (chỉ hiện khi có chữ).
 */
@Composable
fun ODVSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    backContentDescription: String,
    onBack: () -> Unit,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ODVSize.appBar)
            .padding(start = 4.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVIconButton(ODVIcon.ArrowLeft, backContentDescription, onBack)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                // Ô tìm không có nhãn riêng nên dùng placeholder làm nhãn TalkBack.
                .semantics { contentDescription = placeholder },
            singleLine = true,
            textStyle = type.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.voltText),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ODVSize.tapTarget)
                        .background(colors.surface2, ODVTheme.shapes.full)
                        .padding(start = 16.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ODVIcon(ODVIcon.Search, contentDescription = null, tint = colors.inkMuted, size = 20.dp)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(placeholder, style = type.body, color = colors.inkMuted, maxLines = 1)
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        ODVIconButton(ODVIcon.Close, clearContentDescription, { onQueryChange("") })
                    }
                }
            },
        )
    }
}

/**
 * Breadcrumb (mục 4.4, TM-01): chữ `body-sm`, mục trước `ink-muted` cao chạm tối thiểu 32, mục cuối `body-sm-strong`
 * và không bấm được; giữa các mục là mũi tên 16 `ink-faint` cách 4. Cuộn ngang khi dài.
 * Từ [maxVisible] + 1 mục trở lên thì rút gọn thành "…" cộng ([maxVisible] - 1) cấp cuối; chạm "…" gọi [onItemClick] với mục
 * đứng ngay trước các cấp cuối. [ellipsisContentDescription] là nhãn TalkBack của "…" (ví dụ "Các thư mục phía trên").
 *
 * @param onItemClick nhận chỉ số trong [items].
 */
@Composable
fun ODVBreadcrumb(
    items: List<String>,
    onItemClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    ellipsisContentDescription: String = "…",
) {
    require(maxVisible >= 2) { "maxVisible phải ≥ 2 (một ô \"…\" và ít nhất cấp cuối)" }
    if (items.isEmpty()) return
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    // Quá [maxVisible]: "…" đại diện các cấp trên, theo sau là (maxVisible - 1) cấp cuối.
    // Chỉ số của ô "…" ghi riêng, không so chuỗi, để thư mục tên "…" không bị nhầm.
    val ellipsisIndex = if (items.size > maxVisible) items.size - maxVisible else -1
    val shown: List<Pair<Int, String>> = if (ellipsisIndex >= 0) {
        val tail = (items.size - (maxVisible - 1)) until items.size
        listOf(ellipsisIndex to "…") + tail.map { it to items[it] }
    } else {
        items.mapIndexed { index, label -> index to label }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEachIndexed { position, (index, label) ->
            if (position > 0) ODVIcon(ODVIcon.ChevronRight, contentDescription = null, tint = colors.inkFaint, size = 16.dp)
            if (index == items.lastIndex) {
                Text(label, style = type.bodySmStrong, color = colors.ink, maxLines = 1)
            } else {
                Box(
                    Modifier
                        // Vùng chạm ≥ 48 (thiet-ke-ui.md 1.6); `clickable` không tự nới như component Material.
                        .heightIn(min = ODVSize.tapTarget)
                        .clickable(role = Role.Button) { onItemClick(index) }
                        .then(
                            if (position == 0 && index == ellipsisIndex) Modifier.semantics { contentDescription = ellipsisContentDescription } else Modifier,
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = type.bodySm, color = colors.inkMuted, maxLines = 1)
                }
            }
        }
    }
}

/**
 * SortBar (mục 4.4, TM-05, TM-06): nút tonal Sm kèm icon `sort` và nhãn kiểu "Tên · A đến Z" ở trái, IconButton đổi
 * dạng lưới/danh sách ở phải.
 *
 * @param sortContentDescription ví dụ "Sắp xếp: Tên · A đến Z".
 * @param toggleIcon icon của dạng sẽ chuyển sang (ODVIcon.List khi đang lưới, ODVIcon.Grid khi đang danh sách).
 * @param toggleContentDescription ví dụ "Chuyển sang dạng danh sách".
 */
@Composable
fun ODVSortBar(
    sortLabel: String,
    sortContentDescription: String,
    onSortClick: () -> Unit,
    toggleIcon: ODVIcon,
    toggleContentDescription: String,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVButton(
            text = sortLabel,
            onClick = onSortClick,
            modifier = Modifier.semantics { contentDescription = sortContentDescription },
            style = ODVButtonStyle.Tonal,
            size = ODVButtonSize.Sm,
            icon = ODVIcon.Sort,
        )
        ODVIconButton(toggleIcon, toggleContentDescription, onToggleClick)
    }
}
