package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVTheme

enum class ODVCardTone {
    /** Nhóm thông tin: nền `surface`, viền 1dp `line`. */
    Surface,

    /** Cảnh báo (ví dụ Client Secret sắp hết hạn): nền `warning-soft`, không viền. */
    Warning,
}

/**
 * Card (mục 4.4, board 11): bo `md`, padding 16. Phẳng, không bóng; chỉ ContinueCard có bóng.
 * Nội dung do nơi gọi dựng (tiêu đề `heading`, các hàng nhãn/giá trị `caption`...).
 */
@Composable
fun ODVCard(
    modifier: Modifier = Modifier,
    tone: ODVCardTone = ODVCardTone.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ODVTheme.colors
    val shape = ODVTheme.shapes.md
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (tone == ODVCardTone.Warning) colors.warningSoft else colors.surface, shape)
            .then(if (tone == ODVCardTone.Surface) Modifier.border(1.dp, colors.line, shape) else Modifier)
            .padding(16.dp),
        content = content,
    )
}
