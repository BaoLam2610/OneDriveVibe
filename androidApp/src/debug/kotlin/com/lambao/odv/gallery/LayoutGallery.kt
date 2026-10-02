package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVPinDots
import com.lambao.odv.core.designsystem.component.ODVPinDotsState
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import com.lambao.odv.core.designsystem.component.ODVSwitchRow
import com.lambao.odv.core.designsystem.component.odvShake
import com.lambao.odv.core.designsystem.component.rememberODVHaptics
import com.lambao.odv.core.designsystem.component.rememberODVShakeState
import com.lambao.odv.core.designsystem.theme.ODVLayout
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.launch

/** Boards "05 Bố cục", "08 Chuyển động", "09 Trợ năng, trạng thái, riêng tư". Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
internal fun LazyListScope.layoutPage() {
    item { SectionTitle("Lưới nội dung (ODVLayout)") }
    item {
        val colors = ODVTheme.colors
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Thư viện · ${ODVLayout.LIBRARY_COLUMNS} cột · khe 2dp", style = ODVTheme.typography.caption, color = colors.inkMuted)
            LazyVerticalGrid(
                columns = ODVLayout.libraryCells,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(ODVLayout.libraryGap),
                verticalArrangement = Arrangement.spacedBy(ODVLayout.libraryGap),
                userScrollEnabled = false,
            ) {
                items((1..8).toList()) { Box(Modifier.fillMaxWidth().height(80.dp).background(colors.surface2, ODVTheme.shapes.xs)) }
            }
            Text("Thư mục · ${ODVLayout.FOLDER_COLUMNS} cột · khe ngang 12, dọc 16", style = ODVTheme.typography.caption, color = colors.inkMuted)
            LazyVerticalGrid(
                columns = ODVLayout.folderCells,
                modifier = Modifier.fillMaxWidth().height(224.dp),
                horizontalArrangement = Arrangement.spacedBy(ODVLayout.folderColumnGap),
                verticalArrangement = Arrangement.spacedBy(ODVLayout.folderRowGap),
                userScrollEnabled = false,
            ) {
                items((1..4).toList()) { Box(Modifier.fillMaxWidth().height(104.dp).background(colors.voltSoft, ODVTheme.shapes.md)) }
            }
        }
    }

    item { SectionTitle("Chuyển động và Giảm hiệu ứng") }
    item {
        val colors = ODVTheme.colors
        val shake = rememberODVShakeState()
        val haptics = rememberODVHaptics()
        val scope = rememberCoroutineScope()
        var error by rememberSaveable { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Giảm hiệu ứng của hệ thống: ${if (ODVTheme.motion.reduceMotion) "ĐANG BẬT (rung và trượt bị bỏ)" else "tắt"}",
                style = ODVTheme.typography.caption, color = colors.inkMuted,
            )
            Text("Đổi chip Sáng/Tối ở đầu trang để xem cross-fade theme 250ms.", style = ODVTheme.typography.caption, color = colors.inkMuted)
            ODVPinDots(
                filled = 6, contentDescription = "Mã PIN, đã nhập 6 trên 6 số", modifier = Modifier.odvShake(shake),
                state = if (error) ODVPinDotsState.Error else ODVPinDotsState.Default,
            )
            ODVButton(
                "Giả lập nhập sai PIN (rung + haptic)",
                {
                    error = true
                    haptics.reject()
                    scope.launch { shake.shake() }
                },
                style = ODVButtonStyle.Tonal, size = ODVButtonSize.Sm,
            )
            ODVButton("Xóa lỗi", { error = false }, style = ODVButtonStyle.Ghost, size = ODVButtonSize.Sm)
        }
    }

    item { SectionTitle("Riêng tư · chặn chụp màn hình") }
    item {
        var secure by rememberSaveable { mutableStateOf(false) }
        // Chỉ trang này bật cờ khi công tắc bật; rời trang là gỡ.
        ODVSecureWindow(enabled = secure)
        ODVSwitchRow("Chặn chụp màn hình (FLAG_SECURE)", secure, { secure = it }, description = "Thử chụp màn hình hoặc mở danh sách app gần đây khi bật")
    }
}
