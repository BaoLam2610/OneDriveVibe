package com.lambao.odv.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVViewerNextUpCard
import kotlin.math.ceil

/**
 * Thẻ "Tiếp theo sau 5 giây" của Tự phát tiếp và Lặp danh sách (VD-13, V8): vòng đếm 5 giây đầy dần tuyến tính, hết thì gọi
 * [onFinished] để sang [nextTitle]; bấm Hủy gọi [onCancel] (ở lại màn, hiện nút Phát lại). Vòng đếm là thông tin nên vẫn chạy khi bật
 * Giảm hiệu ứng (thiet-ke-ui.md mục 4.5).
 */
@Composable
internal fun NextUpCard(
    nextTitle: String,
    onFinished: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finished by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(PlayerConstants.AUTOPLAY_COUNTDOWN_MS, easing = LinearEasing))
        finished()
    }
    val secondsLeft = ceil((1f - progress.value) * PlayerConstants.AUTOPLAY_COUNTDOWN_MS / 1000f).toInt().coerceAtLeast(1)
    ODVViewerNextUpCard(
        heading = stringResource(R.string.player_next_up, PlayerConstants.AUTOPLAY_COUNTDOWN_MS / 1000),
        title = nextTitle,
        secondsText = secondsLeft.toString(),
        progress = progress.value,
        cancelLabel = stringResource(R.string.player_next_up_cancel),
        onCancel = onCancel,
        modifier = modifier,
    )
}
