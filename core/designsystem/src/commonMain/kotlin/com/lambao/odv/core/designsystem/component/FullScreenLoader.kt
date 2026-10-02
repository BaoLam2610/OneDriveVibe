package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.logo.ODVLogo
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme

enum class ODVLoaderStepState { Done, Running, Pending }

/** Một bước trong danh sách tiến trình. [description] là nhãn TalkBack, ví dụ "Đã lấy access token, xong". */
class ODVLoaderStep(
    val label: String,
    val state: ODVLoaderStepState,
    val description: String? = null,
)

/**
 * Loading toàn màn hình (mục 4.2; K3, B8, Cài đặt · S2): phủ cả thanh tiêu đề bằng `bg` alpha 94%, chặn mọi chạm.
 * Ở giữa: vòng 72 (rãnh `line` 4dp, cung `volt-text` xoay 1 vòng/giây, logo 36), tiêu đề `title`, phụ đề `body`,
 * rồi danh sách bước.
 *
 * Spec chốt "không có nút Hủy", nhưng board "14 Loading" có trạng thái chờ lâu (sau 10 giây) với dòng cảnh báo và nút Hủy.
 * Vì vậy [slowNotice] và [onCancel] là tùy chọn; nơi gọi quyết định có dùng hay không (xem docs/thiet-ke-ui.md mục 9).
 */
@Composable
fun ODVFullScreenLoader(
    title: String,
    modifier: Modifier = Modifier,
    loadingDescription: String? = null,
    subtitle: String? = null,
    steps: List<ODVLoaderStep> = emptyList(),
    slowNotice: String? = null,
    cancelLabel: String? = null,
    onCancel: (() -> Unit)? = null,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg.copy(alpha = 0xF0 / 255f))
            // Chặn chạm xuyên xuống nội dung phía sau mà không biến lớp phủ thành phần tử bấm được trong TalkBack.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(ODVSize.loaderRing)
                    .semantics { if (loadingDescription != null) contentDescription = loadingDescription },
                contentAlignment = Alignment.Center,
            ) {
                ODVSpinner(size = ODVSize.loaderRing, color = colors.voltText, trackColor = colors.line, strokeWidth = 4.dp)
                ODVLogo(size = 36.dp)
            }
            // Vùng live đặt trên tiêu đề để TalkBack đọc khi nội dung đổi, mà không gộp mất nút Hủy.
            Text(
                title,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) Text(subtitle, style = type.body, color = colors.inkMuted, textAlign = TextAlign.Center)
            if (steps.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    steps.forEach { step -> LoaderStepRow(step) }
                }
            }
            if (slowNotice != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ODVIcon(ODVIcon.Alert, contentDescription = null, tint = colors.warning, size = 16.dp)
                    Text(slowNotice, style = type.caption, color = colors.warning)
                }
            }
            if (cancelLabel != null && onCancel != null) {
                ODVButton(cancelLabel, onCancel, style = ODVButtonStyle.Ghost, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun LoaderStepRow(step: ODVLoaderStep) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Row(
        modifier = Modifier.semantics { if (step.description != null) contentDescription = step.description },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(ODVSize.iconSm), contentAlignment = Alignment.Center) {
            when (step.state) {
                ODVLoaderStepState.Done -> ODVIcon(ODVIcon.Check, contentDescription = null, tint = colors.success, size = ODVSize.iconSm)
                ODVLoaderStepState.Running -> ODVSpinner(size = ODVSize.iconSm, color = colors.voltText, trackColor = colors.line)
                ODVLoaderStepState.Pending -> Box(Modifier.size(8.dp).border(2.dp, colors.lineStrong, CircleShape))
            }
        }
        Text(
            step.label,
            style = if (step.state == ODVLoaderStepState.Running) type.bodyStrong else type.body,
            color = if (step.state == ODVLoaderStepState.Pending) colors.inkMuted else colors.ink,
        )
    }
}
