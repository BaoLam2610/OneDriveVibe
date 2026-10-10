package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.DarkColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.designsystem.theme.fade

/** Chấm nhắc ở góc trên phải viên chọn (DH-07, N3): [Warning] khi secret sắp hết hạn, [Danger] khi đã hết hạn. */
enum class ODVNavBadge { Warning, Danger }

/**
 * Một mục của [ODVNavBar]. [contentDescription] là nhãn TalkBack đầy đủ do nơi gọi dựng, vì nó phụ thuộc trạng thái chọn và vị trí
 * ("Thư mục, tab 1 trên 4, đang chọn"; mục có chấm thêm ", có thông báo", thiet-ke-ui.md mục 4.7).
 */
@Immutable
class ODVNavItem(
    val label: String,
    val icon: ODVIcon,
    val contentDescription: String,
    val badge: ODVNavBadge? = null,
)

/**
 * Thanh điều hướng đáy (mục 4.7, N1 đến N4; ADR-0023): cao [ODVSize.navBar] cộng lề dưới của thanh điều hướng hệ thống, nền `surface`
 * kẻ trên 1dp `line`, tối đa 4 mục chia đều. Mỗi mục có viên chọn 56 × 32 chứa icon 24 và nhãn `label` bên dưới.
 *
 * [media] là bản N4 cho tab Short (SV-10): nền đen, kẻ trên `media.icon-well`, không phụ thuộc giao diện Sáng/Tối; đổi qua lại với bản
 * thường bằng cross-fade 250ms (thu về ≤ 150ms khi hệ thống bật Giảm hiệu ứng).
 *
 * Thanh tự đệm theo `navigationBars` để nền kéo xuống sát cạnh màn; nơi đặt thanh phải báo cho `ODVScaffold` bên trên biết (xem
 * `LocalODVNavBarOwnsInset`) để nội dung không đệm đáy hai lần.
 */
@Composable
fun ODVNavBar(
    items: List<ODVNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    media: Boolean = false,
) {
    val colors = ODVTheme.colors
    val mediaColors = ODVTheme.media
    val spec = ODVTheme.motion.fade<Color>()
    val container by animateColorAsState(if (media) mediaColors.background else colors.surface, spec, label = "navContainer")
    val line by animateColorAsState(if (media) mediaColors.iconWell else colors.line, spec, label = "navLine")
    val indicator by animateColorAsState(if (media) DarkColors.voltSoft else colors.voltSoft, spec, label = "navIndicator")
    val selectedIcon by animateColorAsState(if (media) DarkColors.voltText else colors.voltText, spec, label = "navSelectedIcon")
    val selectedLabel by animateColorAsState(if (media) mediaColors.onMedia else colors.ink, spec, label = "navSelectedLabel")
    val idleContent by animateColorAsState(if (media) mediaColors.onMediaFaint else colors.inkMuted, spec, label = "navIdle")
    val pressedOverlay by animateColorAsState(if (media) mediaColors.ripple else colors.surface2, spec, label = "navPressed")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .drawBehind { drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ODVSize.navBar)
                .padding(horizontal = 4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val focused by interaction.collectIsFocusedAsState()
                val shape = ODVTheme.shapes.full
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(ODVSize.navBar)
                        // Cả cột là vùng chạm (≥ 48dp); không có gợn Material, trạng thái nhấn vẽ trên viên chọn.
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .semantics { contentDescription = item.contentDescription }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(ODVSize.navIndicatorWidth, ODVSize.navIndicatorHeight)
                            .focusRing(focused, shape, colors.voltText, container)
                            .background(
                                when {
                                    selected -> indicator
                                    pressed -> pressedOverlay
                                    else -> Color.Transparent
                                },
                                shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        ODVIcon(
                            item.icon,
                            contentDescription = null,
                            tint = if (selected) selectedIcon else idleContent,
                        )
                        item.badge?.let { badge ->
                            // Chấm 8, ở góc trên phải viên, lệch vào trong 4 (DH-07, N3).
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .size(ODVSize.navDot)
                                    .background(if (badge == ODVNavBadge.Danger) colors.danger else colors.warning, shape),
                            )
                        }
                    }
                    Text(
                        item.label,
                        style = ODVTheme.typography.label,
                        color = if (selected) selectedLabel else idleContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
