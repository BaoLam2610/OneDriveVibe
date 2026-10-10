@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.VideoFit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PlayerLayer(
    controller: VideoPlayerController,
    state: PlayerState,
    current: DriveItem,
    onIntent: (PlayerIntent) -> Unit,
    onBack: () -> Unit,
    onBarsHiddenChange: (Boolean) -> Unit,
    onToggleOrientation: () -> Unit,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var speedSheetOpen by rememberSaveable { mutableStateOf(false) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var scrub by remember { mutableStateOf<Float?>(null) }
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var centerFeedback by remember { mutableStateOf<CenterFeedback?>(null) }
    var boosting by remember { mutableStateOf(false) }
    var hud by remember { mutableStateOf<SwipeHud?>(null) }
    var labelPill by remember { mutableStateOf<LabelPill?>(null) }
    // Tăng mỗi lần chạm vào điều khiển để hẹn giờ tự ẩn tính lại từ đầu (VD-01).
    var activity by remember { mutableIntStateOf(0) }
    // Hủy đếm ngược tự phát tiếp của video này (VD-13): ở lại màn, hiện nút Phát lại.
    var countdownCancelled by remember(current.id) { mutableStateOf(false) }

    val zoom = remember { ZoomState() }
    val levels = rememberDeviceLevels()
    val swipeSession = remember { SwipeSession() }
    val tapMemo = remember { TapMemo() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val playState = controller.playState
    // Chỉ nhận lỗi của đúng video đang xem: ngay sau khi chuyển video, lỗi của video trước còn nằm trong controller tới khi load chạy.
    val failure = controller.failure.takeIf { controller.failedItemId == current.id }
    val info = state.info
    val infoOpen = info != null
    val modeLabels = PlayMode.entries.associateWith { it.label() }
    val fitLabels = VideoFit.entries.associateWith { it.label() }

    // Nạp video, nhịp tiến độ, dừng khi xuống nền, giữ màn sáng, tạm dừng khi xem thông tin (VD-09, VD-10, VD-17).
    PlayerPlaybackEffects(controller, state, current, infoOpen)
    // Zoom về 1x khi chuyển video hoặc đổi hướng (VD-18).
    LaunchedEffect(current.id, landscape) { zoom.reset() }
    val isPlaying = controller.isPlaying
    // VD-13: Lặp một video phát lại ngay, không đếm ngược. Tự phát tiếp / Lặp danh sách thì thẻ đếm ngược lo (bên dưới).
    val ended = playState == ODVPlayState.Replay
    LaunchedEffect(ended, state.playMode, current.id) {
        if (ended && state.playMode == PlayMode.RepeatOne) controller.replay()
    }
    // VD-15: ở Tự phát tiếp và Lặp danh sách, video lỗi (không phải mất mạng) bị bỏ qua sau 5 giây đếm ngược để người xem kịp đọc
    // thẻ lỗi (bấm Hủy thì ở lại). ViewModel chặn lặp vô hạn; không còn video chưa lỗi thì không có thẻ đếm ngược.
    var skipCancelled by remember(current.id) { mutableStateOf(false) }
    val skippable = failure != null && failure != PlayerFailure.Network &&
        (state.playMode == PlayMode.AutoNext || state.playMode == PlayMode.RepeatList)
    val skipTarget = if (skippable && !skipCancelled) state.nextPlayable(state.failedIds + current.id) else null
    // Có video phát được thì quên các video từng lỗi: lỗi tạm thời (5xx, mạng chập chờn) không bị bỏ qua mãi trong phiên.
    LaunchedEffect(isPlaying) { if (isPlaying) onIntent(PlayerIntent.ClearFailed) }
    val queued = state.nextInQueue
    val showNextUp = ended && queued != null && !countdownCancelled && failure == null

    // VD-01: tự ẩn sau 3 giây, trừ khi không đang phát (đang tải, tạm dừng, hết video), đang kéo thanh tua, đang mở bảng, hoặc có lỗi.
    val holdControls = playState != ODVPlayState.Playing || scrub != null || speedSheetOpen || infoOpen || failure != null
    LaunchedEffect(controlsVisible, holdControls, activity) {
        if (controlsVisible && !holdControls) {
            delay(ODVDuration.controlsAutoHide.toLong())
            controlsVisible = false
        }
    }
    // Hết video thì điều khiển hiện lên và không tự ẩn (VD-21), nhưng người xem vẫn chạm để ẩn được (khác lỗi, luôn hiện). Làm bằng
    // cách bật cờ lúc vừa hết video chứ không ép hiện, nếu ép thì chạm không ẩn được.
    LaunchedEffect(ended) { if (ended) controlsVisible = true }
    // Có lỗi thì thẻ lỗi và thanh điều khiển luôn hiện. Đang khóa thì luôn ẩn (VD-08).
    val showControls = !locked && (controlsVisible || failure != null)

    // Điều khiển ẩn thì ẩn luôn system bar (thanh trạng thái và thanh điều hướng); hiện lại cùng điều khiển. Rời PlayerLayer thì trả bar.
    LaunchedEffect(showControls) { onBarsHiddenChange(!showControls) }
    DisposableEffect(Unit) { onDispose { onBarsHiddenChange(false) } }

    LaunchedEffect(seekFeedback?.tick) {
        if (seekFeedback != null) {
            delay(PlayerConstants.SEEK_FEEDBACK_MS)
            seekFeedback = null
        }
    }
    LaunchedEffect(hud?.tick) {
        if (hud != null) {
            delay(PlayerConstants.HUD_HOLD_MS)
            hud = null
        }
    }
    LaunchedEffect(labelPill?.tick) {
        if (labelPill != null) {
            delay(PlayerConstants.LABEL_MS)
            labelPill = null
        }
    }
    // Bảng bên phải ở hướng ngang do mình xử lý Back (bottom sheet ở hướng dọc tự xử lý).
    BackHandler(enabled = landscape && (speedSheetOpen || infoOpen)) {
        if (infoOpen) onIntent(PlayerIntent.HideInfo) else speedSheetOpen = false
    }

    val touch: () -> Unit = { activity++ }

    fun seekTapped(rightSide: Boolean) {
        val stepSeconds = state.seekStepSeconds
        controller.seekBy(if (rightSide) stepSeconds * 1000L else -stepSeconds * 1000L)
        val previous = seekFeedback
        val total = if (previous != null && previous.rightSide == rightSide) previous.seconds + stepSeconds else stepSeconds
        seekFeedback = SeekFeedback(rightSide, total, (previous?.tick ?: 0) + 1)
    }

    /** Chạm đúp vùng giữa: đổi Phát/Tạm dừng và hiện biểu tượng trạng thái mới giữa khung video. */
    fun toggleFromCenter() {
        val playing = controller.togglePlay()
        centerFeedback = CenterFeedback(playing, (centerFeedback?.tick ?: 0) + 1)
    }

    Box(
        Modifier
            .fillMaxSize()
            // Thứ tự quan trọng: sự kiện đi vào trong rồi quay ra ngoài, nên bộ nhận chạm (ngoài cùng) thấy được việc zoom/vuốt đã nhận
            // sự kiện và không coi đó là chạm.
            .playerGestures(
                enabled = !locked,
                // Ẩn điều khiển thì tức thì. Hiện điều khiển thì chờ hết cửa sổ chạm đúp (DOUBLE_TAP_WINDOW_MS) để chạm đúp không làm
                // điều khiển nháy lên rồi mới tua. Đang cộng dồn tua thì chạm tiếp ở hai bên là một lần tua nữa (VD-03).
                onTap = { zone, _ ->
                    tapMemo.pendingShow?.cancel()
                    when {
                        seekFeedback != null && zone != TapZone.Center -> {
                            // Đang trong chuỗi tua: lần chạm kế nếu thành chạm đúp cũng phải là tua.
                            tapMemo.controlsWereVisible = false
                            seekTapped(zone == TapZone.Right)
                        }
                        showControls -> {
                            tapMemo.controlsWereVisible = true
                            controlsVisible = false
                        }
                        else -> {
                            tapMemo.controlsWereVisible = false
                            tapMemo.pendingShow = scope.launch {
                                delay(PlayerConstants.DOUBLE_TAP_WINDOW_MS)
                                controlsVisible = true
                                touch()
                            }
                        }
                    }
                },
                onDoubleTap = { zone, _ ->
                    playerLog.d { "[UI] chạm đúp vùng=$zone, điều khiển đã hiện trước đó=${tapMemo.controlsWereVisible}" }
                    tapMemo.pendingShow?.cancel()
                    if (tapMemo.controlsWereVisible) {
                        // Điều khiển đã hiện thì chặn tua/tạm dừng bằng chạm đúp: lần chạm đầu vừa ẩn nó, lần này hiện lại như chạm thường.
                        controlsVisible = true
                        touch()
                    } else {
                        // Điều khiển đang ẩn và chưa kịp hiện (lần chạm đầu còn chờ): giữ ẩn, chỉ làm hành động. Đặt false phòng khi
                        // hẹn giờ vừa kịp chạy trước lần chạm thứ hai.
                        controlsVisible = false
                        if (zone == TapZone.Center) toggleFromCenter() else seekTapped(zone == TapZone.Right)
                    }
                },
                onBoostStart = {
                    if (controller.isPlaying) {
                        boosting = true
                        controller.applySpeed(PlayerConstants.BOOST_SPEED)
                    }
                },
                onBoostEnd = {
                    if (boosting) {
                        boosting = false
                        controller.applySpeed(state.speed)
                    }
                },
            )
            // VD-04: vuốt dọc nửa trái độ sáng, nửa phải âm lượng. Tắt khi zoom lớn hơn 1x (VD-18) hoặc khóa thao tác (VD-08).
            .playerSwipeGestures(
                enabled = !locked && !zoom.isZoomed,
                onStart = { kind ->
                    swipeSession.level = if (kind == SwipeKind.Brightness) levels.brightness() else levels.volume()
                    // Vuốt chỉnh độ sáng/âm lượng thì ẩn điều khiển ngay (HUD không chồng lên điều khiển) và để ẩn sau khi chỉnh xong,
                    // giống thói quen của các trình phát khác: người xem đang tập trung vào hình chứ không phải thanh điều khiển.
                    tapMemo.pendingShow?.cancel()
                    controlsVisible = false
                },
                onDelta = { kind, delta ->
                    swipeSession.level = (swipeSession.level + delta).coerceIn(0f, 1f)
                    if (kind == SwipeKind.Brightness) levels.setBrightness(swipeSession.level) else levels.setVolume(swipeSession.level)
                    hud = SwipeHud(kind, swipeSession.level, (hud?.tick ?: 0) + 1)
                },
                onEnd = {},
            )
            // VD-18: hai ngón zoom 1x đến 4x, khi lớn hơn 1x một ngón kéo.
            .playerZoomable(zoom, enabled = !locked),
    ) {
        VideoFrame(
            controller = controller,
            item = current,
            fit = state.videoFit,
            zoom = zoom,
            seekFeedback = seekFeedback,
            centerFeedback = centerFeedback,
            onCenterFeedbackFinished = { centerFeedback = null },
            showMiniProgress = !showControls,
            // Hướng dọc + Vừa khung: khung 16:9 căn giữa (thiet-ke-ui.md mục 4.5). Cắt đầy / Kéo giãn / hướng ngang: lấp cả màn.
            modifier = if (!landscape && state.videoFit == VideoFit.Fit) {
                Modifier.fillMaxWidth().aspectRatio(PlayerConstants.FRAME_ASPECT).align(Alignment.Center)
            } else {
                Modifier.fillMaxSize()
            },
        )

        PlayerControlsOverlay(
            visible = showControls,
            controller = controller,
            state = state,
            current = current,
            playState = playState,
            landscape = landscape,
            showPlaybackControls = failure == null,
            scrubState = { scrub },
            onScrub = { scrub = it },
            onBack = onBack,
            onInfo = {
                touch()
                onIntent(PlayerIntent.ShowInfo)
            },
            onLock = {
                locked = true
                controlsVisible = false
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onOpenSpeed = { speedSheetOpen = true },
            onCyclePlayMode = {
                val next = state.playMode.next()
                onIntent(PlayerIntent.CyclePlayMode)
                labelPill = LabelPill(modeLabels.getValue(next), next.icon(), (labelPill?.tick ?: 0) + 1)
            },
            onCycleFit = {
                val next = state.videoFit.next()
                onIntent(PlayerIntent.CycleVideoFit)
                labelPill = LabelPill(fitLabels.getValue(next), null, (labelPill?.tick ?: 0) + 1)
            },
            onRotate = onToggleOrientation,
            onResetZoom = if (landscape && zoom.isZoomed) ({ zoom.reset() }) else null,
            onIntent = onIntent,
            onTouch = touch,
        )

        PlayerFeedbackOverlays(
            labelPill = labelPill,
            hud = hud,
            boosting = boosting,
            showResetZoom = !landscape && zoom.isZoomed && showControls,
            onResetZoom = { zoom.reset() },
        )

        // VD-13: thẻ "Tiếp theo sau 5 giây" khi video chạy hết ở Tự phát tiếp / Lặp danh sách.
        if (showNextUp && queued != null) {
            PlayerNextUpCard(
                nextTitle = queued.name,
                landscape = landscape,
                onFinished = {
                    if (queued.id == current.id) controller.replay() else onIntent(PlayerIntent.Advance)
                },
                onCancel = { countdownCancelled = true },
            )
        }

        failure?.let { FailureOverlay(it, current, state.isOnline, controller, onBack) }

        // VD-15: đếm ngược trước khi bỏ qua video lỗi. key theo id để vòng đếm của video lỗi kế tiếp bắt đầu lại từ đầu.
        if (skipTarget != null) {
            key(current.id) {
                PlayerNextUpCard(
                    nextTitle = skipTarget.name,
                    landscape = landscape,
                    onFinished = { onIntent(PlayerIntent.VideoFailed(current.id)) },
                    onCancel = { skipCancelled = true },
                )
            }
        }

        // VD-08: khóa thao tác chặn mọi chạm; chỉ giữ nút mở khóa. Đặt cuối để nằm trên cùng.
        if (locked) {
            LockedLayer(
                landscape = landscape,
                onUnlock = {
                    locked = false
                    controlsVisible = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
        }
    }

    PlayerSheets(
        speed = state.speed,
        speedSheetOpen = speedSheetOpen,
        info = info,
        landscape = landscape,
        onIntent = onIntent,
        onDismissSpeed = { speedSheetOpen = false },
    )
}
