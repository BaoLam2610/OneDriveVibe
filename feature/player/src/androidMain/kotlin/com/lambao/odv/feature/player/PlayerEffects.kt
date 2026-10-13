@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.PlayMode
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
    onIntent: (PlayerIntent) -> Unit,
) {
    val infoMemo = remember { InfoMemo() }
    // Nạp video khi vào màn hoặc khi chuyển video (VD-10). Đọc vị trí và chế độ tự phát tại thời điểm nạp. Video mở đầu tiên chờ
    // ViewModel quyết định bắt đầu từ đâu (VD-12: đọc vị trí đã lưu, có thể hỏi "Xem tiếp từ mm:ss?") rồi mới nạp.
    LaunchedEffect(controller, current.id, state.resumeResolved) {
        if (!state.resumeResolved) return@LaunchedEffect
        playerLog.i { "[UI] vào video id=${current.id.shortId()} (${state.videos.size} video trong danh sách)" }
        controller.load(current, state.resumePositionMs, state.autoPlay, state.speed)
    }

    // VD-12: báo vị trí xem cho ViewModel ghi (UseCase ghi mọi vị trí, chỉ video chạy hết mới là đã xem xong). Chưa quyết xong điểm bắt đầu thì không báo, để không ghi đè
    // vị trí đã lưu. Lặp một video: vòng đầu tính, các vòng sau không lưu ([suppressedId]).
    val latestOnIntent by rememberUpdatedState(onIntent)
    val resolved by rememberUpdatedState(state.resumeResolved)
    val suppressedId = remember { mutableStateOf<String?>(null) }
    val report = remember(controller) {
        { itemId: String, positionMs: Long ->
            if (resolved && suppressedId.value != itemId) {
                latestOnIntent(PlayerIntent.ReportProgress(itemId, positionMs, controller.durationMs))
            }
        }
    }
    // Theo nhịp khi đang phát, để tiến trình bị hệ điều hành thu hồi vẫn còn vị trí gần nhất.
    LaunchedEffect(controller, current.id) {
        val id = current.id
        while (true) {
            delay(PlayerConstants.PROGRESS_SAVE_MS)
            if (controller.isPlaying) report(id, controller.positionMs)
        }
    }
    // Chạy hết: báo ở cuối video (là xem xong, rời dải Xem tiếp); Lặp một video thì từ đây các vòng sau không lưu.
    // Chỉ khi player đang giữ đúng video này: ngay sau khi tự chuyển sang video kế, trạng thái ENDED của video trước có thể còn một nhịp,
    // báo lúc đó sẽ ghi nhầm video kế là đã xem xong.
    LaunchedEffect(controller.playbackState, current.id) {
        if (controller.playbackState == Player.STATE_ENDED && controller.player.currentMediaItem?.mediaId == current.id) {
            report(current.id, controller.durationMs)
            if (state.playMode == PlayMode.RepeatOne) suppressedId.value = current.id
        }
    }
    // Hết chặn khi sang video khác hoặc đổi khỏi Lặp một video: các vòng lặp sau của video đó mới là thứ không lưu (VD-12).
    LaunchedEffect(current.id, state.playMode) {
        if (state.playMode != PlayMode.RepeatOne || suppressedId.value != current.id) suppressedId.value = null
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { report(current.id, controller.player.currentPosition) }
    // Chuyển sang video khác hoặc rời màn: ghi vị trí cuối của video cũ. Dùng vị trí đã đọc (positionMs) chứ không gọi player vì khi rời
    // màn player có thể đã bị nhả trước effect này.
    DisposableEffect(controller, current.id) {
        val id = current.id
        onDispose { report(id, controller.positionMs) }
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
