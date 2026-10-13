package com.lambao.odv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.format.odvFormatDuration

/**
 * Hộp thoại "Xem tiếp từ mm:ss?" (VD-12, artboard V3): tiêu đề có chỗ đã dừng, dòng phụ "Bạn đã xem N% video này.", nút phụ "Xem từ đầu"
 * và nút chính "Xem tiếp". Không đóng khi chạm ra ngoài; Back thoát màn xem như khi chưa mở video ([onBack]).
 */
@Composable
internal fun ResumePromptDialog(
    prompt: ResumePrompt,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onBack: () -> Unit,
) {
    ODVDialog(
        onDismissRequest = onBack,
        title = stringResource(R.string.player_resume_title, odvFormatDuration(prompt.positionMs)),
        body = stringResource(R.string.player_resume_body, prompt.percent),
        dismissOnOutsideClick = false,
        actions = {
            ODVButton(stringResource(R.string.player_resume_restart), onStartOver, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.player_resume_continue), onResume)
        },
    )
}
