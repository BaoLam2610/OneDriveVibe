package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVActionRow
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVCacheSegment
import com.lambao.odv.core.designsystem.component.ODVCacheUsage
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVChipsRow
import com.lambao.odv.core.designsystem.component.ODVDangerRow
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVNavRow
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.component.ODVSettingsGroup
import com.lambao.odv.core.designsystem.component.ODVSettingsInfoRow
import com.lambao.odv.core.designsystem.component.ODVSwitchRow
import com.lambao.odv.core.designsystem.component.ODVValueRow
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Board "18 Components · Cài đặt" (kèm SettingsRow ở board 11). Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.settingsPage() {
    item {
        // Nhóm tự có lề 16 hai bên như trên màn Cài đặt thật, nên bù lại lề của trang.
        val bleed = Modifier.fillMaxWidth()
        var protect by rememberSaveable { mutableStateOf(true) }
        var lock by rememberSaveable { mutableStateOf(1) }
        var photo by rememberSaveable { mutableStateOf(true) }
        var video by rememberSaveable { mutableStateOf(true) }
        var pdf by rememberSaveable { mutableStateOf(false) }
        var sheet by rememberSaveable { mutableStateOf(false) }
        val enabledCount = listOf(photo, video, pdf).count { it }
        val colors = ODVTheme.colors

        Column(bleed.background(colors.bg)) {
            ODVSettingsGroup("Các loại hàng") {
                row { ODVSwitchRow("Bảo vệ ứng dụng", protect, { protect = it }, description = "Yêu cầu mã PIN khi mở") }
                row { ODVValueRow("Tự khóa khi rời app", listOf("Ngay lập tức", "1 phút", "5 phút")[lock], { sheet = true }, description = "Mở bottom sheet chọn giá trị") }
                row { ODVNavRow("Đổi mã PIN", {}, description = "Mở màn con") }
                row { ODVSettingsInfoRow("Tenant ID", "a1b2••••9f0e") }
                row { ODVActionRow("Xóa bộ nhớ đệm", {}, description = "Hành động có xác nhận") }
                row { ODVDangerRow("Ngắt kết nối", {}, description = "Xóa toàn bộ dữ liệu trên máy") }
            }
            ODVSettingsGroup("Hiển thị") {
                row {
                    ODVChipsRow(
                        title = "Loại tệp hiển thị",
                        description = "Chọn nhiều, giữ ít nhất 1",
                        warning = if (enabledCount == 1) "Phải bật ít nhất 1 loại tệp" else null,
                    ) {
                        // Không cho tắt chip cuối cùng (CD-01).
                        ODVChip("Ảnh", photo, { if (!(photo && enabledCount == 1)) photo = !photo }, dotColor = colors.kindPhoto, selectedContentDescription = "Ảnh, đang chọn")
                        ODVChip("Video", video, { if (!(video && enabledCount == 1)) video = !video }, dotColor = colors.kindVideo, selectedContentDescription = "Video, đang chọn")
                        ODVChip("PDF", pdf, { if (!(pdf && enabledCount == 1)) pdf = !pdf }, dotColor = colors.kindPdf, selectedContentDescription = "PDF, đang chọn")
                    }
                }
            }
            ODVSettingsGroup("Bộ nhớ đệm") {
                row {
                    ODVCacheUsage(
                        title = "Dung lượng đang dùng",
                        usageText = "1,4 GB / 2 GB",
                        segments = listOf(
                            ODVCacheSegment("Thumbnail", "120 MB", 0.06f, colors.lineStrong),
                            ODVCacheSegment("Ảnh", "410 MB", 0.20f, colors.kindPhoto),
                            ODVCacheSegment("Video", "820 MB", 0.41f, colors.kindVideo),
                            ODVCacheSegment("PDF", "90 MB", 0.05f, colors.kindPdf),
                        ),
                        barContentDescription = "Đã dùng 1,4 GB trên 2 GB",
                    )
                }
            }
        }

        if (sheet) {
            ODVBottomSheet({ sheet = false }, "Tự khóa khi rời app") {
                listOf("Ngay lập tức", "1 phút", "5 phút").forEachIndexed { i, label ->
                    ODVOptionRow(label, lock == i, { lock = i; sheet = false }, description = if (i == 1) "Mặc định" else null)
                }
            }
        }
    }

    item { SectionTitle("Hộp thoại xác nhận (mục 4.6)") }
    item { ConfirmLauncher() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmLauncher() {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ODVButton("Cảnh báo · bước 1", { open = "warn" }, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
        ODVButton("Nguy hiểm · bước 2", { open = "danger" }, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
        ODVButton("Hành động an toàn", { open = "safe" }, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
        ODVButton("Tắt bảo vệ", { open = "off" }, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
    }
    val close = { open = null }
    when (open) {
        "warn" -> ODVDialog(close, "Ngắt kết nối OneDrive?", icon = ODVIcon.Alert, tone = ODVDialogTone.Warning, body = "Cách duy nhất để dùng lại là kết nối từ đầu. Các tệp trên OneDrive không bị ảnh hưởng.") {
            ODVButton("Hủy", close, style = ODVButtonStyle.Ghost)
            ODVButton("Tiếp tục", close, style = ODVButtonStyle.Danger)
        }
        "danger" -> ODVDialog(close, "Xóa toàn bộ dữ liệu?", icon = ODVIcon.Alert, tone = ODVDialogTone.Danger, alert = true, body = "Dữ liệu trên máy sẽ bị xóa vĩnh viễn và không thể khôi phục.") {
            ODVButton("Hủy", close, style = ODVButtonStyle.Ghost)
            ODVButton("Xóa tất cả", close, style = ODVButtonStyle.DangerSolid)
        }
        "safe" -> ODVDialog(close, "Giảm giới hạn bộ nhớ đệm?", icon = ODVIcon.Alert, tone = ODVDialogTone.Warning, body = "Một phần dữ liệu đệm sẽ bị xóa để vừa giới hạn mới.") {
            ODVButton("Hủy", close, style = ODVButtonStyle.Ghost)
            ODVButton("Giảm", close)
        }
        "off" -> ODVDialog(close, "Tắt bảo vệ ứng dụng?", icon = ODVIcon.Alert, tone = ODVDialogTone.Warning, body = "Ai mở máy cũng xem được dữ liệu của bạn trong ứng dụng.") {
            ODVButton("Hủy", close, style = ODVButtonStyle.Ghost)
            ODVButton("Tắt bảo vệ", close, style = ODVButtonStyle.DangerSolid)
        }
    }
}
