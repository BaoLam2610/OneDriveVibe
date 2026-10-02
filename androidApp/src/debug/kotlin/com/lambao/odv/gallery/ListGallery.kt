package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVAppBarLogo
import com.lambao.odv.core.designsystem.component.ODVBreadcrumb
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVCard
import com.lambao.odv.core.designsystem.component.ODVCardTone
import com.lambao.odv.core.designsystem.component.ODVContinueCard
import com.lambao.odv.core.designsystem.component.ODVDateHeader
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVFastScroller
import com.lambao.odv.core.designsystem.component.ODVFileCard
import com.lambao.odv.core.designsystem.component.ODVFileKind
import com.lambao.odv.core.designsystem.component.ODVFileRow
import com.lambao.odv.core.designsystem.component.ODVFolderCard
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVPhotoCell
import com.lambao.odv.core.designsystem.component.ODVPhotoCellPlaceholder
import com.lambao.odv.core.designsystem.component.ODVProgressBar
import com.lambao.odv.core.designsystem.component.ODVSearchBar
import com.lambao.odv.core.designsystem.component.ODVSectionHeader
import com.lambao.odv.core.designsystem.component.ODVSortBar
import com.lambao.odv.core.designsystem.component.ODVThumbnailDefaults
import com.lambao.odv.core.designsystem.component.ODVThumbnailPlaceholder
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Boards "11 Thẻ và danh sách" và "16 Điều hướng và danh sách". Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
internal fun LazyListScope.listPage() {
    item { SectionTitle("Card") }
    item {
        val colors = ODVTheme.colors
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVCard {
                Text("Kết nối", style = ODVTheme.typography.heading, color = colors.ink, modifier = Modifier.padding(bottom = 8.dp))
                listOf("UPN" to "lambao@contoso.onmicrosoft.com", "Tenant ID" to "a1b2••••9f0e", "Đồng bộ gần nhất" to "23:41 · Hôm nay").forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k, style = ODVTheme.typography.caption, color = colors.inkMuted)
                        Text(v, style = ODVTheme.typography.caption, color = colors.ink)
                    }
                }
            }
            ODVCard {
                Text("OneDrive for Business", style = ODVTheme.typography.bodyStrong, color = colors.ink)
                Text("Bộ nhớ đã dùng", style = ODVTheme.typography.caption, color = colors.inkMuted, modifier = Modifier.padding(bottom = 12.dp))
                ODVProgressBar(0.31f, height = 8.dp)
                Text("312,4 GB / 1 TB", style = ODVTheme.typography.code, color = colors.ink, modifier = Modifier.padding(top = 8.dp))
            }
            ODVCard(tone = ODVCardTone.Warning) {
                Text("Client Secret sắp hết hạn", style = ODVTheme.typography.bodyStrong, color = colors.warning)
                Text("Hết hạn ngày 15/10/2026. Tạo secret mới trên Entra rồi cập nhật trong Cài đặt.", style = ODVTheme.typography.caption, color = colors.ink, modifier = Modifier.padding(vertical = 10.dp))
                ODVButton("Cập nhật Client Secret", {}, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
            }
        }
    }

    item { SectionTitle("AppBar · SearchBar · Breadcrumb · SortBar") }
    item {
        var query by rememberSaveable { mutableStateOf("phim tài") }
        var list by rememberSaveable { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVAppBar(
                title = "OneDrive",
                navigation = { ODVAppBarLogo() },
                actions = {
                    ODVIconButton(ODVIcon.Search, "Tìm kiếm", {})
                    ODVIconButton(ODVIcon.Settings, "Cài đặt", {})
                },
            )
            ODVAppBar(
                title = "Phim tài liệu",
                navigation = { ODVIconButton(ODVIcon.ArrowLeft, "Lên một cấp", {}) },
                actions = {
                    ODVIconButton(ODVIcon.Search, "Tìm kiếm", {})
                    ODVIconButton(ODVIcon.Settings, "Cài đặt", {})
                },
            )
            ODVSearchBar(query, { query = it }, "Tìm tên tệp hoặc thư mục", "Quay lại", {}, "Xóa từ khóa")
            ODVBreadcrumb(listOf("OneDrive", "Phim", "2024"), {})
            ODVBreadcrumb(listOf("OneDrive", "Phim", "Phim tài liệu", "2024"), {}, ellipsisContentDescription = "Các thư mục phía trên")
            ODVSortBar(
                sortLabel = "Tên · A đến Z",
                sortContentDescription = "Sắp xếp: Tên · A đến Z",
                onSortClick = {},
                toggleIcon = if (list) ODVIcon.Grid else ODVIcon.List,
                toggleContentDescription = if (list) "Chuyển sang dạng lưới" else "Chuyển sang dạng danh sách",
                onToggleClick = { list = !list },
            )
        }
    }

    item { SectionTitle("FolderCard · FileCard (lưới 2 cột)") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ODVFolderCard("Phim tài liệu", "24 mục", {}, Modifier.weight(1f))
                ODVFileCard("chuyen-di-da-lat.mp4", "1,2 GB · 02/10/2026", {}, Modifier.weight(1f), kindIcon = ODVIcon.Video, duration = "18:20", progress = 0.7f) { Stand(0xFF6F8296) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ODVFileCard("IMG_2041.HEIC", "4,9 MB · 23/05/2026", {}, Modifier.weight(1f)) { Stand(0xFF8D7B63) }
                ODVFileCard("truyen-tap-07.pdf", "27,6 MB · Trang 72 / 310", {}, Modifier.weight(1f), progress = 0.23f, progressColors = ODVThumbnailDefaults.pdfProgressColors()) {
                    ODVThumbnailPlaceholder(ODVFileKind.Pdf, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), iconSize = 36.dp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ODVFileCard(
                    "phim-offline.mp4", "974 MB · 21/09/2026", {}, Modifier.weight(1f),
                    kindIcon = ODVIcon.Video, duration = "1:26:02", unavailableDescription = "Chưa có trong bộ nhớ đệm",
                ) { Stand(0xFF5D7466) }
                Box(Modifier.weight(1f))
            }
        }
    }

    item { SectionTitle("FileRow · kết quả tìm") }
    item {
        val colors = ODVTheme.colors
        Column(Modifier.fillMaxWidth().clip(ODVTheme.shapes.md).background(colors.surface).border(1.dp, colors.line, ODVTheme.shapes.md)) {
            ODVFileRow("Phim tài liệu", leading = { ODVThumbnailPlaceholder(ODVFileKind.Folder, Modifier.fillMaxSize()) }, meta = "24 mục · 23/05/2026", showChevron = true, onClick = {})
            ODVFileRow("phim-tai-lieu-01.mp4", leading = { Stand(0xFF5D7466, ODVTheme.shapes.sm) }, meta = "974 MB", metaMono = "1:26:02", progress = 0.14f, onClick = {})
            ODVFileRow("truyen-tap-07.pdf", leading = { ODVThumbnailPlaceholder(ODVFileKind.Pdf, Modifier.fillMaxSize()) }, meta = "27,6 MB · Trang 72 / 310", progress = 0.23f, onClick = {})
            ODVFileRow("IMG_2041.HEIC", leading = { ODVThumbnailPlaceholder(ODVFileKind.Photo, Modifier.fillMaxSize()) }, meta = "4,9 MB · 23/05/2026", onClick = {})
            ODVFileRow("ban-ghi.avi", leading = { ODVThumbnailPlaceholder(ODVFileKind.Unsupported, Modifier.fillMaxSize()) }, meta = "1,2 GB · Không hỗ trợ", dimmed = true)
            ODVFileRow(
                "phim-tai-lieu-01.mp4", leading = { Stand(0xFF5D7466, ODVTheme.shapes.sm) }, meta = "974 MB", metaMono = "1:26:02",
                path = "OneDrive › Phim tài liệu › 2024", titleHighlight = "phim-tai", onClick = {},
            )
        }
    }

    item { SectionTitle("ContinueCard · Xem tiếp") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVSectionHeader("Xem tiếp", trailing = "2")
            // contentPadding dọc để bóng shadow.sm của ContinueCard không bị cắt.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                item {
                    ODVContinueCard("phim-tai-lieu-01.mp4", "Còn 1:13:58 · dừng ở 12:04", {}, "Xóa khỏi Xem tiếp", {}, progress = 0.14f, duration = "1:26:02") { Stand(0xFF6F8296) }
                }
                item {
                    ODVContinueCard(
                        "truyen-tap-07.pdf", "Trang 72 / 310", {}, "Xóa khỏi Đọc tiếp", {},
                        progress = 0.23f, progressColors = ODVThumbnailDefaults.pdfProgressColors(),
                    ) { ODVThumbnailPlaceholder(ODVFileKind.Pdf, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), iconSize = 36.dp) }
                }
            }
        }
    }

    item { SectionTitle("Thư viện · DateHeader · PhotoCell · FastScroller") }
    item {
        var fraction by remember { mutableFloatStateOf(0.15f) }
        Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                ODVDateHeader("Thứ Sáu, 2 tháng 10", "8")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ODVPhotoCell({}, "Ảnh, IMG_01.jpg", Modifier.weight(1f)) { Stand(0xFF8D7B63) }
                    ODVPhotoCell({}, "Video, mau-01.mp4, 42 giây", Modifier.weight(1f), kindIcon = ODVIcon.Video, duration = "0:42") { Stand(0xFF6F8296) }
                    ODVPhotoCell({}, "Ảnh, IMG_03.jpg", Modifier.weight(1f)) { Stand(0xFF7B6A86) }
                    ODVPhotoCell({}, "Video, mau-02.mp4, 12 phút 48 giây", Modifier.weight(1f), kindIcon = ODVIcon.Video, duration = "12:48") { Stand(0xFF5D7466) }
                }
                ODVDateHeader("Tháng 9, 2026", "1.248")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ODVPhotoCell({}, "Ảnh, IMG_05.jpg", Modifier.weight(1f)) { Stand(0xFF8D7B63) }
                    ODVPhotoCell({}, "Ảnh, IMG_06.jpg", Modifier.weight(1f)) { Stand(0xFF6F8296) }
                    ODVPhotoCellPlaceholder(Modifier.weight(1f))
                    ODVPhotoCellPlaceholder(Modifier.weight(1f))
                }
            }
            ODVFastScroller(
                fraction = fraction,
                onFractionChange = { fraction = it },
                bubbleText = "Tháng 9, 2026",
                contentDescription = "Cuộn nhanh theo thời gian",
            )
        }
    }

    item { SectionTitle("EmptyState") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                ODVEmptyState(
                    ODVIcon.Folder, "Chưa có tệp phù hợp",
                    body = "Thư mục này không có ảnh, video hay PDF nào đang được bật hiển thị.",
                    action = { ODVButton("Mở Cài đặt", {}, style = ODVButtonStyle.Secondary) },
                )
            }
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                ODVEmptyState(ODVIcon.Search, "Không tìm thấy “xyz”", body = "Tìm theo tên tệp hoặc thư mục trong dữ liệu đã đồng bộ.")
            }
        }
    }
}

/** Ảnh giả: ô màu đặc thay cho thumbnail thật (gallery không có ảnh). */
@Composable
private fun Stand(argb: Long, shape: Shape = RoundedCornerShape(0.dp)) {
    Box(Modifier.fillMaxSize().background(Color(argb), shape))
}
