@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.designsystem.component.ODVPlayerLabels
import com.lambao.odv.core.designsystem.component.ODVViewerBottomBar
import com.lambao.odv.core.designsystem.component.ODVViewerCenterControls
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVViewerResetZoomButton
import com.lambao.odv.core.designsystem.component.ODVViewerSeekBar
import com.lambao.odv.core.designsystem.component.ODVViewerSpeedButton
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvFormatSpeed
import com.lambao.odv.core.designsystem.odvLocale

/** Cụm giữa: trước, tua lùi, Play/Pause/Phát lại/đang tải, tua tới, sau (VD-02, VD-10, VD-21). */
@Composable
internal fun BoxScope.CenterControls(
    controller: VideoPlayerController,
    state: PlayerState,
    playState: ODVPlayState,
    landscape: Boolean,
    onIntent: (PlayerIntent) -> Unit,
    onTouch: () -> Unit,
) {
    val labels = ODVPlayerLabels(
        previous = stringResource(R.string.player_previous),
        next = stringResource(R.string.player_next),
        rewind = stringResource(R.string.player_rewind, state.seekStepSeconds),
        forward = stringResource(R.string.player_forward, state.seekStepSeconds),
        play = stringResource(R.string.player_play),
        pause = stringResource(R.string.player_pause),
        replay = stringResource(R.string.player_replay),
        loading = stringResource(R.string.player_loading),
    )
    ODVViewerCenterControls(
        state = playState,
        labels = labels,
        onPlayPause = {
            onTouch()
            controller.togglePlay()
        },
        onPrevious = {
            onTouch()
            onIntent(PlayerIntent.Previous)
        },
        onNext = {
            onTouch()
            onIntent(PlayerIntent.Next)
        },
        onRewind = {
            onTouch()
            controller.seekBy(-state.seekStepSeconds * 1000L)
        },
        onForward = {
            onTouch()
            controller.seekBy(state.seekStepSeconds * 1000L)
        },
        modifier = Modifier.align(Alignment.Center),
        hasPrevious = state.hasPrevious,
        hasNext = state.hasNext,
        spacing = if (landscape) 20.dp else 12.dp,
    )
}

/**
 * Thanh dưới: thanh tua, thời gian "12:04 / 1:26:02" và các nút Tốc độ, Chế độ phát, Khung hình, Xoay màn hình (VD-02, VD-05,
 * VD-20, VD-06, VD-07). Kéo thanh tua chỉ đổi vị trí hiển thị và hiện bong bóng thời gian; chỉ tua thật khi nhả tay, để không bắn
 * hàng chục yêu cầu tải qua mạng trong lúc kéo.
 */
@Composable
internal fun BoxScope.BottomControls(
    controller: VideoPlayerController,
    state: PlayerState,
    duration: Long,
    landscape: Boolean,
    scrubState: () -> Float?,
    onScrub: (Float?) -> Unit,
    onOpenSpeed: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onCycleFit: () -> Unit,
    onRotate: () -> Unit,
    onResetZoom: (() -> Unit)?,
    onTouch: () -> Unit,
) {
    // Đọc qua hàm chứ không nhận giá trị: chạm vào thanh tua gọi onSeek rồi onSeekFinished ngay trong cùng một sự kiện, trước
    // khi composition kịp chạy lại, nên onSeekFinished phải thấy giá trị mới nhất chứ không phải giá trị lúc dựng.
    val scrub = scrubState()
    val shownMs = if (scrub != null) (scrub * duration).toLong() else controller.positionMs
    val speedText = odvFormatSpeed(state.speed, odvLocale())
    val iconColors = ODVMediaDefaults.iconButtonColors()
    ODVViewerBottomBar(
        seekBar = {
            ODVViewerSeekBar(
                position = scrub ?: fraction(controller.positionMs, duration),
                buffered = fraction(controller.bufferedPositionMs, duration),
                onSeek = {
                    onTouch()
                    onScrub(it)
                },
                contentDescription = stringResource(R.string.player_seek_description),
                valueDescription = stringResource(R.string.player_seek_value, spokenDuration(shownMs), spokenDuration(duration)),
                onSeekFinished = {
                    scrubState()?.let { controller.seekTo((it * duration).toLong()) }
                    onScrub(null)
                },
            )
        },
        timeText = "${odvFormatDuration(shownMs)} / ${odvFormatDuration(duration)}",
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (landscape) 8.dp else 24.dp),
    ) {
        // Hướng ngang đang zoom: "Đặt lại zoom" nằm trong hàng nút này (VD-18).
        if (onResetZoom != null) {
            ODVViewerResetZoomButton(
                stringResource(R.string.player_zoom_reset),
                {
                    onTouch()
                    onResetZoom()
                },
            )
        }
        ODVViewerSpeedButton(
            label = speedText,
            contentDescription = stringResource(R.string.player_speed_description, speedText),
            onClick = {
                onTouch()
                onOpenSpeed()
            },
        )
        ODVIconButton(
            state.playMode.icon(),
            stringResource(R.string.player_mode_description, state.playMode.label()),
            {
                onTouch()
                onCyclePlayMode()
            },
            colors = iconColors,
        )
        ODVIconButton(
            ODVIcon.Fit,
            stringResource(R.string.player_fit_description, state.videoFit.label()),
            {
                onTouch()
                onCycleFit()
            },
            colors = iconColors,
        )
        ODVIconButton(
            ODVIcon.Rotate,
            stringResource(R.string.player_rotate),
            {
                onTouch()
                onRotate()
            },
            colors = iconColors,
        )
    }
    if (scrub != null) {
        ODVViewerPill(
            text = odvFormatDuration(shownMs),
            mono = true,
            prominent = true,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (landscape) 84.dp else 112.dp),
        )
    }
}

internal fun fraction(valueMs: Long, durationMs: Long): Float =
    if (durationMs > 0L) (valueMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

/** "12 phút 4 giây" cho TalkBack (thiet-ke-ui.md mục 4.5: SeekBar đọc bằng lời). */
@Composable
private fun spokenDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000).toInt()
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return when {
        hours > 0 -> stringResource(R.string.player_spoken_hms, hours, minutes, seconds)
        minutes > 0 -> stringResource(R.string.player_spoken_ms, minutes, seconds)
        else -> stringResource(R.string.player_spoken_s, seconds)
    }
}
