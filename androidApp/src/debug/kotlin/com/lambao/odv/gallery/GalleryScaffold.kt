package com.lambao.odv.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVAppBarLogo
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVSpacing
import com.lambao.odv.core.designsystem.theme.ODVThemeMode

/** Khung chung cho mọi trang gallery: AppBar cố định ở trên (tiêu đề, nút quay lại), bộ chọn theme và nội dung cuộn bên dưới. */
@Composable
internal fun GalleryPage(
    title: String,
    mode: ODVThemeMode,
    onModeChange: (ODVThemeMode) -> Unit,
    onBack: (() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    ODVScaffold(
        topBar = {
            ODVAppBar(
                title = title,
                navigation = {
                    if (onBack == null) ODVAppBarLogo() else ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", onBack)
                },
            )
        },
    ) { inset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ODVSpacing.s4,
                end = ODVSpacing.s4,
                top = ODVSpacing.s2,
                bottom = ODVSpacing.s2 + inset.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(ODVSpacing.s2),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(ODVSpacing.s2)) {
                    ODVThemeMode.entries.forEach { entry ->
                        ODVChip(entry.name, selected = entry == mode, onClick = { onModeChange(entry) })
                    }
                }
            }
            content()
        }
    }
}
