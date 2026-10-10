@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.domain.model.DriveItem
import kotlinx.coroutines.delay

/** Có đang phát trước khi mở bảng thông tin không, để đóng bảng thì phát tiếp (VD-17: video tạm dừng trong lúc xem bảng). */
private class InfoMemo {
    var wasPlaying = false
}

/** Các effect điều khiển player của màn xem video; không giữ state hiển thị. [infoOpen]: bảng thông tin đang mở (VD-17). */
@Composable
internal fun PlayerPlaybackEffects(
    controller: VideoPlayerController,
    state: PlayerState,
    current: DriveItem,
    infoOpen: Boolean,
) {
    val infoMemo = remember { InfoMemo() }
    // Nạp video khi vào màn hoặc khi chuyển video (VD-10). Đọc vị trí và chế độ tự phát tại thời điểm nạp.
    LaunchedEffect(controller, current.id) {
        playerLog.i { "[UI] vào video id=${current.id.shortId()} (${state.videos.size} video trong danh sách)" }
        controller.load(current, state.resumePositionMs, state.autoPlay, state.speed)
    }
    LaunchedEffect(controller, state.speed) { controller.applySpeed(state.speed) }

    // Cập nhật vị trí và buffer theo nhịp: dày khi đang phát, thưa khi dừng.
    LaunchedEffect(controller) {
        while (true) {
            controller.refreshProgress()
            delay(if (controller.isPlaying) PlayerConstants.PROGRESS_TICK_MS else PlayerConstants.IDLE_TICK_MS)
        }
    }

    // Không có chế độ phát nền (đặc tả không yêu cầu): xuống nền thì dừng.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { controller.player.pause() }

    // VD-09: giữ màn hình sáng khi đang phát.
    val view = LocalView.current
    val isPlaying = controller.isPlaying
    DisposableEffect(view, isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    // VD-17: video tạm dừng trong lúc xem bảng thông tin, đóng bảng thì phát tiếp nếu trước đó đang phát.
    LaunchedEffect(infoOpen) {
        if (infoOpen) {
            infoMemo.wasPlaying = controller.player.playWhenReady
            controller.player.pause()
        } else if (infoMemo.wasPlaying) {
            infoMemo.wasPlaying = false
            controller.player.play()
        }
    }
}
