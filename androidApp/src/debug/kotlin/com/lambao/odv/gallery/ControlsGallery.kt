package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVCheckState
import com.lambao.odv.core.designsystem.component.ODVCheckbox
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVIconButtonDefaults
import com.lambao.odv.core.designsystem.component.ODVMaskToggle
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVProgressBar
import com.lambao.odv.core.designsystem.component.ODVRadioRow
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVStepBar
import com.lambao.odv.core.designsystem.component.ODVSwitch
import com.lambao.odv.core.designsystem.component.ODVTab
import com.lambao.odv.core.designsystem.component.ODVTabs
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme

/** Board "10 Components · Điều khiển". Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.controlsSection() {
    item { SectionTitle("Button") }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVButton("Kết nối", {})
            ODVButton("Thử lại", {}, style = ODVButtonStyle.Secondary)
            ODVButton("Xem từ đầu", {}, style = ODVButtonStyle.Ghost)
            ODVButton("Sắp xếp", {}, style = ODVButtonStyle.Tonal, icon = ODVIcon.Sort)
            ODVButton("Ngắt kết nối", {}, style = ODVButtonStyle.Danger)
            ODVButton("Xóa tất cả", {}, style = ODVButtonStyle.DangerSolid)
            ODVButton("Vô hiệu", {}, enabled = false)
            ODVButton("Đang kết nối…", {}, loading = true)
            ODVButton("Hủy", {}, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
            ODVButton("Hoàn tác", {}, style = ODVButtonStyle.Ghost, size = ODVButtonSize.Sm)
        }
    }
    item { ODVButton("Tiếp tục", {}, modifier = Modifier.fillMaxWidth(), fullWidth = true) }

    item { SectionTitle("IconButton") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", {})
            ODVIconButton(ODVIcon.Search, "Tìm kiếm", {}, colors = ODVIconButtonDefaults.tonal())
            ODVIconButton(ODVIcon.Grid, "Dạng lưới", {}, colors = ODVIconButtonDefaults.selected())
            ODVIconButton(ODVIcon.Play, "Phát", {}, colors = ODVIconButtonDefaults.volt())
            ODVIconButton(ODVIcon.Sync, "Đồng bộ", {}, enabled = false)
        }
    }

    item { SectionTitle("Màu trên media (ODVMediaDefaults)") }
    item {
        // Component dùng chung không biết "media": màn xem tự truyền bộ màu.
        Column(
            Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black, ODVTheme.shapes.md).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ODVIconButton(ODVIcon.Rewind, "Tua lùi 10 giây", {}, colors = ODVMediaDefaults.iconButtonColors())
                ODVIconButton(ODVIcon.Lock, "Khóa thao tác", {}, colors = ODVMediaDefaults.scrimIconButtonColors())
            }
            ODVProgressBar(0.45f, colors = ODVMediaDefaults.progressColors())
        }
    }

    item { SectionTitle("TextField") }
    item {
        var text by rememberSaveable { mutableStateOf("lambao@contoso.onmicrosoft.com") }
        var masked by rememberSaveable { mutableStateOf(true) }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ODVTextField("", {}, label = "Email tài khoản OneDrive", placeholder = "ban@contoso.onmicrosoft.com", helperText = "Gợi ý dưới ô", modifier = Modifier.fillMaxWidth())
            ODVTextField(text, { text = it }, label = "Email (gõ để thấy focus)", modifier = Modifier.fillMaxWidth())
            ODVTextField(
                "3f2a9c1e-7b4d-4e8a-9c21-5d6e7f8a9b0c", {}, label = "Client ID", mono = !masked, masked = masked,
                maskToggle = ODVMaskToggle({ masked = !masked }, if (masked) "Hiện ký tự" else "Ẩn ký tự"),
                helperText = "Che ký tự, bấm con mắt để hiện", modifier = Modifier.fillMaxWidth(),
            )
            ODVTextField("ban@contoso", {}, label = "Email", errorText = "Email chưa đúng định dạng", modifier = Modifier.fillMaxWidth())
            ODVTextField("contoso.onmicrosoft.com", {}, label = "Tenant ID", enabled = false, helperText = "Vô hiệu: đang kết nối", modifier = Modifier.fillMaxWidth())
        }
    }

    item { SectionTitle("Checkbox · Radio · Switch") }
    item {
        var photo by rememberSaveable { mutableStateOf(true) }
        var video by rememberSaveable { mutableStateOf(true) }
        var pdf by rememberSaveable { mutableStateOf(false) }
        var lock by rememberSaveable { mutableStateOf(1) }
        var on by rememberSaveable { mutableStateOf(true) }
        Column(Modifier.fillMaxWidth()) {
            ODVCheckbox("Ảnh", if (photo) ODVCheckState.Checked else ODVCheckState.Unchecked, { photo = !photo })
            ODVCheckbox("Video", if (video) ODVCheckState.Checked else ODVCheckState.Unchecked, { video = !video })
            ODVCheckbox("PDF", if (pdf) ODVCheckState.Checked else ODVCheckState.Unchecked, { pdf = !pdf })
            ODVCheckbox("Một phần", ODVCheckState.Indeterminate, {})
            ODVCheckbox("Vô hiệu", ODVCheckState.Checked, {}, enabled = false)
            listOf("Ngay lập tức", "1 phút", "5 phút").forEachIndexed { i, label ->
                ODVRadioRow(label, selected = lock == i, onClick = { lock = i }, description = if (i == 1) "Mặc định" else null)
            }
            ODVRadioRow("Vô hiệu", selected = false, onClick = {}, enabled = false)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ODVSwitch(on, { on = it }, "Tự phát video kế tiếp")
                ODVSwitch(false, {}, "Tắt")
                ODVSwitch(true, {}, "Vô hiệu", enabled = false)
            }
        }
    }

    item { SectionTitle("Chip · Tabs · Progress · StepBar · Spinner") }
    item {
        var chip by rememberSaveable { mutableStateOf(0) }
        var tab by rememberSaveable { mutableStateOf(1) }
        val colors = ODVTheme.colors
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ODVChip("Tất cả", selected = chip == 0, onClick = { chip = 0 }, selectedContentDescription = "Tất cả, đang chọn")
                ODVChip("Ảnh", selected = chip == 1, onClick = { chip = 1 }, dotColor = colors.kindPhoto, selectedContentDescription = "Ảnh, đang chọn")
                ODVChip("Video", selected = chip == 2, onClick = { chip = 2 }, dotColor = colors.kindVideo, selectedContentDescription = "Video, đang chọn")
                ODVChip("Ngày sửa", selected = chip == 3, onClick = { chip = 3 }, icon = ODVIcon.Sort)
            }
            ODVTabs(
                listOf(ODVTab("Thư mục", ODVIcon.Folder), ODVTab("Thư viện", ODVIcon.Image)),
                selectedIndex = tab, onSelect = { tab = it },
            )
            ODVProgressBar(0.45f)
            ODVProgressBar(0.45f, height = 8.dp)
            ODVStepBar("BƯỚC 1 / 2", step = 1, total = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ODVSpinner()
                ODVSpinner(size = 36.dp)
            }
        }
    }
}
