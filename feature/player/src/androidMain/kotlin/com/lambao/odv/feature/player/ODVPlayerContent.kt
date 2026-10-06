@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.designsystem.component.ODVPlayerLabels
import com.lambao.odv.core.designsystem.component.ODVSidePanel
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerBottomBar
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerCenterControls
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.component.ODVViewerHud
import com.lambao.odv.core.designsystem.component.ODVViewerLockHoldButton
import com.lambao.odv.core.designsystem.component.ODVViewerMiniProgress
import com.lambao.odv.core.designsystem.component.ODVViewerNetworkNotice
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVViewerResetZoomButton
import com.lambao.odv.core.designsystem.component.ODVViewerSeekBar
import com.lambao.odv.core.designsystem.component.ODVViewerSpeedButton
import com.lambao.odv.core.designsystem.component.ODVViewerTopBar
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.ThumbnailSource
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.thumbnailSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** Ghi nhớ giữa hai lần chạm của một cặp chạm đúp; không đọc trong composition nên không cần là state. */
private class TapMemo {
    /** Điều khiển đang hiện ngay trước lần chạm đầu: nếu có thì lần chạm thứ hai bị chặn, không tua. */
    var controlsWereVisible = false

    /** Hẹn giờ hiện điều khiển sau lần chạm đầu (khi đang ẩn); chạm đúp hoặc chạm tiếp thì hủy. */
    var pendingShow: Job? = null
}

/** Mức đang chỉnh trong một lần vuốt dọc (cộng dồn qua các lần [PlayerConstants.SWIPE_FULL_RANGE_RATIO]). */
private class SwipeSession {
    var level = 0f
}

/** Có đang phát trước khi mở bảng thông tin không, để đóng bảng thì phát tiếp (VD-17: video tạm dừng trong lúc xem bảng). */
private class InfoMemo {
    var wasPlaying = false
}

/** Gợn tua đang hiện: [seconds] là tổng đã cộng dồn, [tick] đổi mỗi lần chạm để hẹn giờ tắt tính lại. */
private data class SeekFeedback(val rightSide: Boolean, val seconds: Int, val tick: Int)

/** HUD độ sáng/âm lượng đang hiện; [tick] đổi mỗi lần vuốt để hẹn giờ ẩn tính lại. */
private data class SwipeHud(val kind: SwipeKind, val level: Float, val tick: Int)

/** Viên thuốc nhãn đổi khung hình / chế độ phát (VD-06, VD-20), hiện [PlayerConstants.LABEL_MS]. */
private data class LabelPill(val text: String, val icon: ODVIcon?, val tick: Int)

/**
 * Nội dung màn xem video (V1 → V19): nền đen, khung video với thumbnail làm ảnh nền tới khi có khung hình đầu, điều khiển phủ lên
 * (thanh trên có Thông tin và Khóa, cụm giữa, thanh tua, nút Tốc độ / Chế độ phát / Khung hình / Xoay) tự ẩn sau 3 giây, chạm đúp
 * tua hoặc tạm dừng, vuốt dọc độ sáng/âm lượng, hai ngón zoom, khóa thao tác, tự phát tiếp, thẻ lỗi codec / mất mạng.
 *
 * ExoPlayer do composition này sở hữu: tạo khi vào màn, nhả khi rời màn hoặc khi màn Khóa che lên, lúc đó vị trí và trạng thái
 * đang phát được ghi vào ViewModel để dựng lại đúng chỗ. Hướng màn hình do nút xoay quyết định (VD-07), giữ qua các video.
 */
@Composable
internal fun ODVPlayerContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ẩn luôn thanh trạng thái và thanh điều hướng của hệ thống khi điều khiển ẩn. PlayerLayer báo qua onBarsHiddenChange; ở mọi
    // trạng thái khác (đang tạo player, lỗi) bar hiện bình thường nhưng icon vẫn sáng trên nền đen.
    var barsHidden by remember { mutableStateOf(false) }
    ODVPlayerSystemBars(hidden = barsHidden)
    // Hướng do nút xoay quyết định, không nhớ giữa các lần mở màn (VD-19: mở từ Danh sách luôn vào hướng dọc).
    var requestedLandscape by rememberSaveable { mutableStateOf(false) }
    PlayerOrientationEffect(requestedLandscape)

    val factory = koinInject<VideoPlayerFactory>()
    val latestOnIntent by rememberUpdatedState(onIntent)
    var creationFailed by remember { mutableStateOf(false) }

    val controller by produceState<VideoPlayerController?>(initialValue = null, factory) {
        val created = try {
            factory.create()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Chỉ ghi tên lớp và dòng đầu của message: không ghi stack/đường dẫn (CH-06). Thẻ lỗi hiện giống lỗi chung nên log là
            // chỗ duy nhất phân biệt "không dựng được player" với "player dựng được nhưng phát lỗi".
            playerLog.e { "[UI] KHÔNG dựng được player: ${e.javaClass.simpleName}: ${e.message?.lineSequence()?.firstOrNull()}" }
            creationFailed = true
            return@produceState
        }
        value = created
        awaitDispose {
            // playWhenReady (ý định phát) chứ không phải isPlaying: đang đệm mà vẫn muốn phát thì tính là đang phát.
            val playing = created.player.playWhenReady
            playerLog.i { "[UI] gỡ màn xem, lưu vị trí=${created.player.currentPosition}ms đang phát=$playing" }
            latestOnIntent(PlayerIntent.SavePosition(created.player.currentPosition, playing))
            created.release()
        }
    }

    Box(modifier.fillMaxSize().background(ODVMediaColors.background)) {
        val current = state.current
        val ready = controller
        when {
            current == null -> if (!state.isLoaded) {
                // Đang chờ video được chạm xuất hiện trong Room (quét lần đầu chưa xong): chưa biết cTag nên dùng thumbnail theo id
                // làm ảnh nền cho đỡ màn đen, kèm vòng quay.
                state.currentId?.let { id ->
                    AsyncImage(
                        model = ThumbnailSource(id, cTag = null, size = ThumbnailSize.Viewer),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                CenterSpinner()
            }
            creationFailed -> {
                ODVViewerError(
                    title = stringResource(R.string.player_error_other_title),
                    body = stringResource(R.string.player_error_other_body),
                    fileLine = current.name,
                    action = { ODVViewerButton(stringResource(R.string.player_back), onBack) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            ready == null -> CenterSpinner()
            else -> PlayerLayer(
                controller = ready,
                state = state,
                current = current,
                onIntent = onIntent,
                onBack = onBack,
                onBarsHiddenChange = { barsHidden = it },
                onToggleOrientation = { requestedLandscape = !requestedLandscape },
            )
        }
    }
}

@Composable
private fun BoxScope.CenterSpinner() {
    Box(Modifier.align(Alignment.Center)) {
        ODVSpinner(size = 36.dp, color = ODVMediaColors.accent, trackColor = ODVMediaColors.track)
    }
}

@Composable
private fun PlayerLayer(
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
    val infoMemo = remember { InfoMemo() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val playState = controller.playState
    // Chỉ nhận lỗi của đúng video đang xem: ngay sau khi chuyển video, lỗi của video trước còn nằm trong controller tới khi load chạy.
    val failure = controller.failure.takeIf { controller.failedItemId == current.id }
    val info = state.info
    val infoOpen = info != null
    val modeLabels = PlayMode.entries.associateWith { it.label() }
    val fitLabels = VideoFit.entries.associateWith { it.label() }

    // Nạp video khi vào màn hoặc khi chuyển video (VD-10). Đọc vị trí và chế độ tự phát tại thời điểm nạp.
    LaunchedEffect(controller, current.id) {
        playerLog.i { "[UI] vào video id=${current.id.shortId()} (${state.videos.size} video trong danh sách)" }
        controller.load(current, state.resumePositionMs, state.autoPlay, state.speed)
    }
    LaunchedEffect(controller, state.speed) { controller.applySpeed(state.speed) }
    // Zoom về 1x khi chuyển video hoặc đổi hướng (VD-18).
    LaunchedEffect(current.id, landscape) { zoom.reset() }

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

    val duration = controller.durationMs
    val touch: () -> Unit = { activity++ }

    fun seekTapped(rightSide: Boolean) {
        controller.seekBy(if (rightSide) PlayerConstants.SEEK_STEP_MS else -PlayerConstants.SEEK_STEP_MS)
        val previous = seekFeedback
        val total = if (previous != null && previous.rightSide == rightSide) previous.seconds + PlayerConstants.SEEK_STEP_SECONDS else PlayerConstants.SEEK_STEP_SECONDS
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

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(PlayerConstants.CONTROLS_FADE_MS)),
            exit = fadeOut(tween(PlayerConstants.CONTROLS_FADE_MS)),
        ) {
            Box(Modifier.fillMaxSize().background(ODVMediaColors.scrimControls)) {
                ODVViewerTopBar(
                    title = current.name,
                    backContentDescription = stringResource(R.string.player_back),
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                    actions = {
                        ODVIconButton(
                            ODVIcon.Info,
                            stringResource(R.string.player_info),
                            {
                                touch()
                                onIntent(PlayerIntent.ShowInfo)
                            },
                            colors = ODVMediaDefaults.iconButtonColors(),
                        )
                        ODVIconButton(
                            ODVIcon.Lock,
                            stringResource(R.string.player_lock),
                            {
                                locked = true
                                controlsVisible = false
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            colors = ODVMediaDefaults.iconButtonColors(),
                        )
                    },
                )
                if (failure == null) {
                    CenterControls(controller, state, playState, landscape, onIntent, touch)
                    BottomControls(
                        controller = controller,
                        state = state,
                        duration = duration,
                        landscape = landscape,
                        scrubState = { scrub },
                        onScrub = { scrub = it },
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
                        onTouch = touch,
                    )
                }
            }
        }

        // Nhãn đổi khung hình / chế độ phát nằm trên cụm giữa (thiet-ke-ui.md mục 4.5).
        labelPill?.let {
            ODVViewerPill(
                text = it.text,
                icon = it.icon,
                prominent = true,
                live = true,
                modifier = Modifier.align(Alignment.Center).offset(y = (-96).dp),
            )
        }

        hud?.let { shown ->
            val percent = (shown.level * 100).roundToInt()
            val brightness = shown.kind == SwipeKind.Brightness
            ODVViewerHud(
                level = shown.level,
                icon = if (brightness) ODVIcon.Sun else ODVIcon.Volume,
                percentText = stringResource(R.string.player_hud_percent, percent),
                contentDescription = stringResource(if (brightness) R.string.player_brightness_description else R.string.player_volume_description, percent),
                modifier = Modifier.align(if (brightness) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 32.dp),
            )
        }

        if (boosting) {
            ODVViewerPill(
                text = formatSpeed(PlayerConstants.BOOST_SPEED),
                mono = true,
                live = true,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp),
            )
        }

        // Nút "Đặt lại zoom" kèm dòng gợi ý (VD-18) chỉ hiện khi đang zoom VÀ điều khiển đang hiện: lúc xem thì không che hình.
        // Hướng dọc: cách đáy 112 (thiet-ke-ui.md mục 4.5). Hướng ngang: chiều cao thấp, mọi chỗ trống đều trùng cụm giữa hoặc thanh
        // trên, nên nút nằm ngay trong hàng nút của thanh dưới (BottomControls, tham số onResetZoom), trước nút Tốc độ.
        if (!landscape && zoom.isZoomed && showControls) {
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 112.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                ODVViewerResetZoomButton(stringResource(R.string.player_zoom_reset), { zoom.reset() })
                Text(stringResource(R.string.player_zoom_hint), style = ODVTheme.typography.caption, color = ODVMediaColors.onMediaMuted)
            }
        }

        // VD-13: thẻ "Tiếp theo sau 5 giây" khi video chạy hết ở Tự phát tiếp / Lặp danh sách.
        if (showNextUp && queued != null) {
            NextUpCard(
                nextTitle = queued.name,
                onFinished = {
                    if (queued.id == current.id) controller.replay() else onIntent(PlayerIntent.Advance)
                },
                onCancel = { countdownCancelled = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = if (landscape) 88.dp else 112.dp),
            )
        }

        failure?.let { FailureOverlay(it, current, state.isOnline, controller, onBack) }

        // VD-15: đếm ngược trước khi bỏ qua video lỗi. key theo id để vòng đếm của video lỗi kế tiếp bắt đầu lại từ đầu.
        if (skipTarget != null) {
            key(current.id) {
                NextUpCard(
                    nextTitle = skipTarget.name,
                    onFinished = { onIntent(PlayerIntent.VideoFailed(current.id)) },
                    onCancel = { skipCancelled = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = if (landscape) 88.dp else 112.dp),
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

    if (speedSheetOpen) {
        SpeedSheet(
            current = state.speed,
            onSelect = { onIntent(PlayerIntent.SetSpeed(it)) },
            onDismiss = { speedSheetOpen = false },
        )
    }

    // VD-17: bảng thông tin tệp, hướng dọc là bottom sheet, hướng ngang là bảng bên phải.
    info?.let { shown ->
        val title = stringResource(R.string.player_info_title)
        val close = { onIntent(PlayerIntent.HideInfo) }
        if (landscape) {
            ODVSidePanel(title, stringResource(R.string.player_close), close) { PlayerInfoContent(shown) }
        } else {
            ODVBottomSheet(onDismissRequest = close, title = title) { PlayerInfoContent(shown) }
        }
    }
}

/**
 * Khung video: bề mặt phát (TextureView để zoom bằng graphicsLayer, VD-18), thumbnail phủ lên tới khi có khung hình đầu tiên
 * (hết màn đen lúc mở), gợn chạm đúp và thanh tiến độ mảnh khi điều khiển ẩn. [fit] quyết định cách đặt video vào khung (VD-06).
 * Zoom chỉ áp lên video (và thumbnail), không áp lên gợn chạm đúp hay thanh tiến độ.
 */
@Composable
private fun VideoFrame(
    controller: VideoPlayerController,
    item: DriveItem,
    fit: VideoFit,
    zoom: ZoomState,
    seekFeedback: SeekFeedback?,
    centerFeedback: CenterFeedback?,
    onCenterFeedbackFinished: () -> Unit,
    showMiniProgress: Boolean,
    modifier: Modifier,
) {
    val failure = controller.failure.takeIf { controller.failedItemId == item.id }
    Box(modifier.clipToBounds().onSizeChanged { zoom.frameSize = it }, contentAlignment = Alignment.Center) {
        val aspect = controller.videoAspect
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                },
            contentAlignment = Alignment.Center,
        ) {
            val rendered = controller.hasRenderedFirstFrame(item.id)
            PlayerSurface(
                player = controller.player,
                modifier = when {
                    aspect <= 0f || fit == VideoFit.Stretch -> Modifier.fillMaxSize()
                    fit == VideoFit.Crop -> Modifier.coverAspect(aspect)
                    else -> Modifier.aspectRatio(aspect)
                }
                    // TextureView giữ lại khung hình cuối của video trước (khi chuyển video hoặc video chạy hết): ẩn tới khi video này
                    // vẽ khung đầu, nếu không nó lộ ra ở phần chừa trống quanh thumbnail.
                    .graphicsLayer { alpha = if (rendered) 1f else 0f },
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            )
            // Lỗi codec/đã xóa thì không phủ thumbnail lên thẻ lỗi; mất mạng vẫn giữ để có hình nền.
            if (!rendered && (failure == null || failure == PlayerFailure.Network)) {
                // key theo id: AsyncImage giữ ảnh cũ trong lúc tải ảnh mới, nên không có key thì thumbnail video trước hiện lại phía sau.
                key(item.id) {
                    AsyncImage(
                        model = item.thumbnailSource(ThumbnailSize.Viewer),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = when (fit) {
                            VideoFit.Fit -> ContentScale.Fit
                            VideoFit.Crop -> ContentScale.Crop
                            VideoFit.Stretch -> ContentScale.FillBounds
                        },
                    )
                }
            }
        }
        // Vùng ripple (nửa trái/phải khung video) kèm sóng lan, và biểu tượng Phát/Tạm dừng giữa khung: đều nằm trong video.
        seekFeedback?.let { feedback ->
            Box(
                Modifier
                    .align(if (feedback.rightSide) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f),
            ) {
                SeekRipple(
                    label = stringResource(
                        if (feedback.rightSide) R.string.player_seek_forward else R.string.player_seek_back,
                        feedback.seconds,
                    ),
                    rightSide = feedback.rightSide,
                    tick = feedback.tick,
                )
            }
        }
        centerFeedback?.let { CenterFeedbackIcon(it, onFinished = onCenterFeedbackFinished, modifier = Modifier.align(Alignment.Center)) }
        if (showMiniProgress) {
            val duration = controller.durationMs
            ODVViewerMiniProgress(
                position = fraction(controller.positionMs, duration),
                buffered = fraction(controller.bufferedPositionMs, duration),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Cắt đầy (VD-06): giữ tỉ lệ [aspect] của video và phóng cho phủ kín khung, phần thừa lố ra ngoài (khung cắt bằng `clipToBounds`).
 */
private fun Modifier.coverAspect(aspect: Float): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    if (width <= 0 || height <= 0) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
    val coverWidth: Int
    val coverHeight: Int
    if (width.toFloat() / height > aspect) {
        coverWidth = width
        coverHeight = (width / aspect).roundToInt()
    } else {
        coverHeight = height
        coverWidth = (height * aspect).roundToInt()
    }
    val placeable = measurable.measure(Constraints.fixed(coverWidth, coverHeight))
    layout(width, height) { placeable.placeRelative((width - coverWidth) / 2, (height - coverHeight) / 2) }
}

/**
 * Lớp khóa thao tác (VD-08, V7): chặn mọi chạm bên dưới. Viên thuốc "Đã khóa thao tác" và nút giữ mở khóa ẩn/hiện giống thanh điều
 * khiển video: chạm thì hiện, chạm nữa thì ẩn, tự ẩn sau [PlayerConstants.LOCK_HINT_MS]; bấm Back thì hiện. Video vẫn phát bình thường
 * phía dưới.
 */
@Composable
private fun BoxScope.LockedLayer(landscape: Boolean, onUnlock: () -> Unit) {
    // Ẩn/hiện giống thanh điều khiển video (VD-01): chạm thì hiện, chạm nữa thì ẩn, và tự ẩn sau LOCK_HINT_MS.
    var hintVisible by remember { mutableStateOf(true) }
    // Đổi mỗi lần hiện/chạm để hẹn giờ tự ẩn tính lại từ đầu.
    var hintTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(hintVisible, hintTick) {
        if (hintVisible) {
            delay(PlayerConstants.LOCK_HINT_MS)
            hintVisible = false
        }
    }
    // Back không được thoát màn khi đang khóa (tránh thoát nhầm), chỉ nhắc cách mở khóa.
    BackHandler {
        hintVisible = true
        hintTick++
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        if (event.type == PointerEventType.Press) {
                            hintVisible = !hintVisible
                            hintTick++
                        }
                    }
                }
            },
    )
    AnimatedVisibility(
        visible = hintVisible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(PlayerConstants.CONTROLS_FADE_MS)),
        exit = fadeOut(tween(PlayerConstants.CONTROLS_FADE_MS)),
    ) {
        Box(Modifier.fillMaxSize()) {
            ODVViewerPill(
                text = stringResource(R.string.player_locked),
                icon = ODVIcon.Lock,
                live = true,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = if (landscape) 24.dp else 92.dp),
            )
            ODVViewerLockHoldButton(
                contentDescription = stringResource(R.string.player_unlock_description),
                hint = stringResource(R.string.player_unlock_hint),
                onUnlocked = onUnlock,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (landscape) 32.dp else 96.dp),
                subHint = stringResource(R.string.player_unlock_sub_hint),
            )
        }
    }
}

/** Cụm giữa: trước, tua lùi, Play/Pause/Phát lại/đang tải, tua tới, sau (VD-02, VD-10, VD-21). */
@Composable
private fun BoxScope.CenterControls(
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
        rewind = stringResource(R.string.player_rewind, PlayerConstants.SEEK_STEP_SECONDS),
        forward = stringResource(R.string.player_forward, PlayerConstants.SEEK_STEP_SECONDS),
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
            controller.seekBy(-PlayerConstants.SEEK_STEP_MS)
        },
        onForward = {
            onTouch()
            controller.seekBy(PlayerConstants.SEEK_STEP_MS)
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
private fun BoxScope.BottomControls(
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
    val speedText = formatSpeed(state.speed)
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

/** Thẻ lỗi (V9, V10): mất mạng có nút Tiếp tục (VD-16), còn lại báo rõ nguyên nhân thay vì để màn hình đen (VD-15). */
@Composable
private fun BoxScope.FailureOverlay(
    failure: PlayerFailure,
    item: DriveItem,
    isOnline: Boolean,
    controller: VideoPlayerController,
    onBack: () -> Unit,
) {
    if (failure == PlayerFailure.Network) {
        ODVViewerNetworkNotice(
            title = stringResource(R.string.player_error_network_title),
            body = stringResource(R.string.player_error_network_body),
            actionLabel = stringResource(R.string.player_resume),
            onAction = controller::retry,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            actionEnabled = isOnline,
            hint = stringResource(R.string.player_error_network_hint),
        )
        return
    }
    val (title, body) = when (failure) {
        PlayerFailure.Unsupported -> R.string.player_error_unsupported_title to R.string.player_error_unsupported_body
        PlayerFailure.Removed -> R.string.player_error_removed_title to R.string.player_error_removed_body
        else -> R.string.player_error_other_title to R.string.player_error_other_body
    }
    ODVViewerError(
        title = stringResource(title),
        body = stringResource(body),
        fileLine = item.name,
        modifier = Modifier.align(Alignment.Center),
        action = {
            if (failure == PlayerFailure.Other) {
                ODVViewerButton(stringResource(R.string.player_retry), controller::retry, filled = true)
            } else {
                ODVViewerButton(stringResource(R.string.player_back), onBack)
            }
        },
    )
}

private fun fraction(valueMs: Long, durationMs: Long): Float =
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
