package com.lambao.odv.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVBanner
import com.lambao.odv.core.designsystem.component.ODVBannerTone
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogCard
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVFullScreenLoader
import com.lambao.odv.core.designsystem.component.ODVInfoRow
import com.lambao.odv.core.designsystem.component.ODVLoaderStep
import com.lambao.odv.core.designsystem.component.ODVLoaderStepState
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.component.ODVSnackbar
import com.lambao.odv.core.designsystem.component.ODVSnackbarHost
import com.lambao.odv.core.designsystem.component.showODVSnackbar
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.launch

/** Boards "11 Banner/Snackbar", "13 Dialog và Bottom Sheet", "14 Loading". Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.overlaysSection() {
    item { SectionTitle("Banner") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVBanner("Đang offline. Hiển thị dữ liệu đã đồng bộ.", ODVBannerTone.Neutral, ODVIcon.CloudOff)
            ODVBanner("Đang lập chỉ mục, đã quét 12.480 mục", ODVBannerTone.Volt, ODVIcon.Sync, progress = 0.4f, actionLabel = "Xem thư mục", onAction = {})
            ODVBanner("Client Secret hết hạn sau 12 ngày", ODVBannerTone.Warning, ODVIcon.Clock, actionLabel = "Cập nhật", onAction = {})
            ODVBanner("Client Secret đã hết hạn", ODVBannerTone.Danger, ODVIcon.Alert, actionLabel = "Cập nhật", onAction = {})
            ODVBanner("Đã kết nối OneDrive", ODVBannerTone.Success, ODVIcon.Check)
        }
    }

    item { SectionTitle("Snackbar") }
    item {
        val host = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVSnackbar("Đang ở trang 72")
            ODVSnackbar("Đã xóa khỏi Xem tiếp", actionLabel = "Hoàn tác", onAction = {})
            ODVButton("Hiện Snackbar thật (3 giây)", { scope.launch { host.showODVSnackbar("Đã xóa khỏi Xem tiếp", "Hoàn tác") } }, style = ODVButtonStyle.Tonal)
            ODVSnackbarHost(host)
        }
    }

    item { SectionTitle("Dialog (xem trước thẻ)") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ODVDialogCard("Xem tiếp từ 12:04?", icon = ODVIcon.Play, tone = ODVDialogTone.Volt, body = "phim-tai-lieu-01.mp4") {
                ODVButton("Xem từ đầu", {}, style = ODVButtonStyle.Ghost)
                ODVButton("Xem tiếp", {})
            }
            ODVDialogCard(
                "Không kết nối được", icon = ODVIcon.Alert, tone = ODVDialogTone.Danger, alert = true,
                body = "Client Secret không đúng. Kiểm tra đã copy cột Value, không phải Secret ID.",
                errorCode = "Mã lỗi: AADSTS7000215",
            ) {
                ODVButton("Đóng", {}, style = ODVButtonStyle.Ghost)
                ODVButton("Sửa Client Secret", {})
            }
            ODVDialogCard("Ngắt kết nối?", icon = ODVIcon.Alert, tone = ODVDialogTone.Warning, body = "Cấu hình, lịch sử xem, bộ nhớ đệm và cài đặt trên máy sẽ bị xóa. Tệp trên OneDrive không bị ảnh hưởng.") {
                ODVButton("Hủy", {}, style = ODVButtonStyle.Ghost)
                ODVButton("Ngắt kết nối", {}, style = ODVButtonStyle.DangerSolid)
            }
        }
    }

    item { SectionTitle("Dialog và Bottom sheet (mở thật)") }
    item { OverlayLauncher() }

    item { SectionTitle("FullScreenLoader (khung 240 × 480)") }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LoaderFrame { ODVFullScreenLoader("Đang kết nối…", subtitle = "Giữ ứng dụng mở trong vài giây.", loadingDescription = "Đang tải") }
            LoaderFrame {
                ODVFullScreenLoader(
                    "Đang kết nối OneDrive",
                    steps = listOf(
                        ODVLoaderStep("Đã lấy access token", ODVLoaderStepState.Done, "Đã lấy access token, xong"),
                        ODVLoaderStep("Đang kiểm tra OneDrive…", ODVLoaderStepState.Running, "Đang kiểm tra OneDrive, đang chạy"),
                        ODVLoaderStep("Lưu cấu hình", ODVLoaderStepState.Pending, "Lưu cấu hình, chờ"),
                    ),
                )
            }
            LoaderFrame {
                ODVFullScreenLoader(
                    "Đang kết nối…",
                    steps = listOf(
                        ODVLoaderStep("Đã lấy access token", ODVLoaderStepState.Done),
                        ODVLoaderStep("Đang kiểm tra OneDrive…", ODVLoaderStepState.Running),
                    ),
                    slowNotice = "Lâu hơn dự kiến. Kiểm tra mạng của bạn hoặc hủy để sửa thông tin.",
                    cancelLabel = "Hủy", onCancel = {},
                )
            }
        }
    }
}

@Composable
private fun LoaderFrame(content: @Composable () -> Unit) {
    Box(Modifier.size(240.dp, 480.dp).clip(ODVTheme.shapes.lg)) { content() }
}

@Composable
private fun OverlayLauncher() {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(1) }
    var speed by rememberSaveable { mutableStateOf(3) }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ODVButton("Dialog xác nhận", { dialog = "confirm" }, style = ODVButtonStyle.Secondary, size = com.lambao.odv.core.designsystem.component.ODVButtonSize.Sm)
        ODVButton("Dialog lỗi", { dialog = "error" }, style = ODVButtonStyle.Secondary, size = com.lambao.odv.core.designsystem.component.ODVButtonSize.Sm)
        ODVButton("Sheet sắp xếp", { sheet = "sort" }, style = ODVButtonStyle.Secondary, size = com.lambao.odv.core.designsystem.component.ODVButtonSize.Sm)
        ODVButton("Sheet tốc độ", { sheet = "speed" }, style = ODVButtonStyle.Secondary, size = com.lambao.odv.core.designsystem.component.ODVButtonSize.Sm)
        ODVButton("Sheet thông tin", { sheet = "info" }, style = ODVButtonStyle.Secondary, size = com.lambao.odv.core.designsystem.component.ODVButtonSize.Sm)
    }

    when (dialog) {
        "confirm" -> ODVDialog({ dialog = null }, "Ngắt kết nối?", icon = ODVIcon.Alert, tone = ODVDialogTone.Warning, body = "Cấu hình, lịch sử xem và cài đặt trên máy sẽ bị xóa.") {
            ODVButton("Hủy", { dialog = null }, style = ODVButtonStyle.Ghost)
            ODVButton("Ngắt kết nối", { dialog = null }, style = ODVButtonStyle.DangerSolid)
        }
        "error" -> ODVDialog({ dialog = null }, "Không có quyền truy cập", icon = ODVIcon.Alert, tone = ODVDialogTone.Danger, alert = true, body = "Ứng dụng chưa được cấp quyền Files.Read.All hoặc chưa Grant admin consent.", errorCode = "Mã lỗi: HTTP 403") {
            ODVButton("Đóng", { dialog = null }, style = ODVButtonStyle.Ghost)
            ODVButton("Thử lại", { dialog = null })
        }
    }
    when (sheet) {
        "sort" -> ODVBottomSheet({ sheet = null }, "Sắp xếp theo") {
            listOf("Tên", "Ngày sửa", "Dung lượng").forEachIndexed { i, label -> ODVOptionRow(label, sort == i, { sort = i; sheet = null }) }
        }
        "speed" -> ODVBottomSheet({ sheet = null }, "Tốc độ phát") {
            listOf("0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x").forEachIndexed { i, label ->
                ODVOptionRow(label, speed == i, { speed = i; sheet = null }, mono = true, checkStyle = true, description = if (i == 3) "Bình thường" else null)
            }
        }
        "info" -> ODVBottomSheet({ sheet = null }, "Thông tin ảnh") {
            ODVInfoRow("Tên tệp", "IMG_2041.HEIC")
            ODVInfoRow("Kích thước", "4032 × 3024", mono = true)
            ODVInfoRow("Dung lượng", "4,9 MB")
            ODVInfoRow("Ngày chụp", "23/05/2026 14:32")
            ODVInfoRow("Thư mục", "OneDrive › Ảnh › 2026")
        }
    }
}
