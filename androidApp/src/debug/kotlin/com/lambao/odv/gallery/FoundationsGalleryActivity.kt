package com.lambao.odv.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVSpacing
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.designsystem.theme.ODVThemeMode

/** Các trang của gallery, ứng với các board trên canvas "ODV Foundations". Thêm trang mới ở đây khi dựng nhóm component mới. */
internal enum class GalleryDestination(val title: String, val subtitle: String, val content: LazyListScope.() -> Unit) {
    Colors("Màu", "02, 03 · theo theme và màn xem", { colorsPage() }),
    Typography("Chữ", "04 · 21 style", { typographyPage() }),
    Spacing("Khoảng cách và bo góc", "05 · space, radius", { spacingPage() }),
    Icons("Icon và Logo", "06, 07 · 43 icon, 3 logo", { iconsPage() }),
    Layout("Bố cục, chuyển động, riêng tư", "05, 08, 09 · lưới, Giảm hiệu ứng, rung, FLAG_SECURE", { layoutPage() }),
    Controls("Điều khiển", "10 · Button, TextField, Switch, Chip...", { controlsSection() }),
    Overlays("Thông báo và lớp phủ", "11, 13, 14 · Banner, Snackbar, Dialog, Sheet, Loader", { overlaysSection() }),
    Pin("Mã PIN và bàn phím số", "15 · PinDots, Keypad", { pinPage() }),
    Lists("Thẻ và danh sách", "11, 16 · Card, FileRow, FileCard, AppBar, Thư viện...", { listPage() }),
    Viewer("Trình xem", "17 · TopBar, SeekBar, điều khiển giữa, HUD, lỗi, mất mạng...", { viewerPage() }),
    Settings("Cài đặt", "18, 11 · SettingsGroup, các loại hàng, CacheUsage, Slider, SplitBar, xác nhận", { settingsPage() }),
}

/** Màn kiểm tra thiết kế (chỉ bản debug). Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
class FoundationsGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { FoundationsGallery() }
    }
}

@Preview
@Composable
fun FoundationsGallery() {
    var mode by rememberSaveable { mutableStateOf(ODVThemeMode.System) }
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    val destination = GalleryDestination.entries.firstOrNull { it.name == page }

    ODVTheme(mode = mode) {
        BackHandler(enabled = destination != null) { page = null }
        if (destination == null) {
            GalleryPage("Foundations", mode, { mode = it }, onBack = null) {
                items(GalleryDestination.entries) { entry ->
                    GalleryRow(entry) { page = entry.name }
                }
            }
        } else {
            GalleryPage(destination.title, mode, { mode = it }, onBack = { page = null }, content = destination.content)
        }
    }
}

@Composable
private fun GalleryRow(entry: GalleryDestination, onClick: () -> Unit) {
    val colors = ODVTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ODVSize.fileRowMin)
            .background(colors.surface, ODVTheme.shapes.md)
            .clickable(onClick = onClick)
            .padding(horizontal = ODVSpacing.s4, vertical = ODVSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ODVSpacing.s3),
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = ODVTheme.typography.bodyStrong, color = colors.ink)
            Text(entry.subtitle, style = ODVTheme.typography.caption, color = colors.inkMuted)
        }
        ODVIcon(ODVIcon.ChevronRight, contentDescription = null, tint = colors.inkMuted, size = ODVSize.iconSm)
    }
}
