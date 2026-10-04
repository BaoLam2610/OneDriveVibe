@file:OptIn(UnstableApi::class)

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.designsystem.component.ODVPlayerLabels
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerBottomBar
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerCenterControls
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.component.ODVViewerMiniProgress
import com.lambao.odv.core.designsystem.component.ODVViewerNetworkNotice
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVViewerSeekBar
import com.lambao.odv.core.designsystem.component.ODVViewerSpeedButton
import com.lambao.odv.core.designsystem.component.ODVViewerTopBar
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.thumbnailSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject



/** Ghi nhớ giữa hai lần chạm của một cặp chạm đúp; không đọc trong composition nên không cần là state. */
private class TapMemo {
    /** Điều khiển đang hiện ngay trước lần chạm đầu: nếu có thì lần chạm thứ hai bị chặn, không tua. */
    var controlsWereVisible = false

    /** Hẹn giờ hiện điều khiển sau lần chạm đầu (khi đang ẩn); chạm đúp hoặc chạm tiếp thì hủy. */
    var pendingShow: Job? = null
}

/** Gợn tua đang hiện: [seconds] là tổng đã cộng dồn, [tick] đổi mỗi lần chạm để hẹn giờ tắt tính lại. */
private data class SeekFeedback(val rightSide: Boolean, val seconds: Int, val tick: Int)

/**
 * Nội dung màn xem video (V1 → V10, phần 6a): nền đen, khung video với thumbnail làm ảnh nền tới khi có khung hình đầu,
 * điều khiển phủ lên (thanh trên, cụm giữa, thanh tua) tự ẩn sau 3 giây, chạm đúp tua, giữ lâu phát 2x, bảng tốc độ,
 * thẻ lỗi codec / mất mạng. ExoPlayer do composition này sở hữu: tạo khi vào màn, nhả khi rời màn hoặc khi màn Khóa che lên,
 * lúc đó vị trí được ghi vào ViewModel để dựng lại đúng chỗ (video nằm chờ ở trạng thái tạm dừng).
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
            current == null -> if (!state.isLoaded) CenterSpinner()
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
            else -> PlayerLayer(ready, state, current, onIntent, onBack, onBarsHiddenChange = { barsHidden = it })
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
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var speedSheetOpen by rememberSaveable { mutableStateOf(false) }
    var scrub by remember { mutableStateOf<Float?>(null) }
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var boosting by remember { mutableStateOf(false) }
    // Tăng mỗi lần chạm vào điều khiển để hẹn giờ tự ẩn tính lại từ đầu (VD-01).
    var activity by remember { mutableIntStateOf(0) }

    val playState = controller.playState
    val failure = controller.failure

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

    // VD-01: tự ẩn sau 3 giây, trừ khi không đang phát (đang tải, tạm dừng, hết video), đang kéo thanh tua, đang mở bảng, hoặc có lỗi.
    val holdControls = playState != ODVPlayState.Playing || scrub != null || speedSheetOpen || failure != null
    LaunchedEffect(controlsVisible, holdControls, activity) {
        if (controlsVisible && !holdControls) {
            delay(ODVDuration.controlsAutoHide.toLong())
            controlsVisible = false
        }
    }
    // Hết video hoặc lỗi thì thanh điều khiển luôn hiện (VD-21), không đụng tới cờ người dùng đã đặt.
    val showControls = controlsVisible || playState == ODVPlayState.Replay || failure != null

    // Điều khiển ẩn thì ẩn luôn system bar (thanh trạng thái và thanh điều hướng); hiện lại cùng điều khiển. Rời PlayerLayer thì trả bar.
    LaunchedEffect(showControls) { onBarsHiddenChange(!showControls) }
    DisposableEffect(Unit) { onDispose { onBarsHiddenChange(false) } }

    LaunchedEffect(seekFeedback?.tick) {
        if (seekFeedback != null) {
            delay(PlayerConstants.SEEK_FEEDBACK_MS)
            seekFeedback = null
        }
    }
    BackHandler(enabled = speedSheetOpen && landscape) { speedSheetOpen = false }

    val duration = controller.durationMs
    val touch: () -> Unit = { activity++ }

    var centerFeedback by remember { mutableStateOf<CenterFeedback?>(null) }
    val tapMemo = remember { TapMemo() }
    val scope = rememberCoroutineScope()

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
            .playerGestures(
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
            ),
    ) {
        VideoFrame(
            controller = controller,
            item = current,
            seekFeedback = seekFeedback,
            centerFeedback = centerFeedback,
            onCenterFeedbackFinished = { centerFeedback = null },
            showMiniProgress = !showControls,
            modifier = if (landscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(PlayerConstants.FRAME_ASPECT).align(Alignment.Center),
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
                )
                if (failure == null) {
                    CenterControls(controller, state, playState, landscape, onIntent, touch)
                    BottomControls(controller, state, duration, landscape, { scrub }, { scrub = it }, { speedSheetOpen = true }, touch)
                }
            }
        }

        if (boosting) {
            ODVViewerPill(
                text = formatSpeed(PlayerConstants.BOOST_SPEED),
                mono = true,
                live = true,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp),
            )
        }

        failure?.let { FailureOverlay(it, current, state.isOnline, controller, onBack) }
    }

    if (speedSheetOpen) {
        SpeedSheet(
            current = state.speed,
            onSelect = { onIntent(PlayerIntent.SetSpeed(it)) },
            onDismiss = { speedSheetOpen = false },
        )
    }
}

/**
 * Khung video: bề mặt phát (TextureView để 6b zoom bằng graphicsLayer), thumbnail phủ lên tới khi có khung hình đầu tiên
 * (hết màn đen lúc mở), gợn chạm đúp và thanh tiến độ mảnh khi điều khiển ẩn.
 */
@Composable
private fun VideoFrame(
    controller: VideoPlayerController,
    item: DriveItem,
    seekFeedback: SeekFeedback?,
    centerFeedback: CenterFeedback?,
    onCenterFeedbackFinished: () -> Unit,
    showMiniProgress: Boolean,
    modifier: Modifier,
) {
    val failure = controller.failure
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val aspect = controller.videoAspect
        PlayerSurface(
            player = controller.player,
            modifier = if (aspect > 0f) Modifier.aspectRatio(aspect) else Modifier.fillMaxSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        )
        // Lỗi codec/đã xóa thì không phủ thumbnail lên thẻ lỗi; mất mạng vẫn giữ để có hình nền.
        if (!controller.hasRenderedFirstFrame && (failure == null || failure == PlayerFailure.Network)) {
            AsyncImage(
                model = item.thumbnailSource(ThumbnailSize.Viewer),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
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
 * Thanh dưới: thanh tua, thời gian "12:04 / 1:26:02" và nút tốc độ (VD-02, VD-05). Kéo thanh tua chỉ đổi vị trí hiển thị và
 * hiện bong bóng thời gian; chỉ tua thật khi nhả tay, để không bắn hàng chục yêu cầu tải qua mạng trong lúc kéo.
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
    onTouch: () -> Unit,
) {
    // Đọc qua hàm chứ không nhận giá trị: chạm vào thanh tua gọi onSeek rồi onSeekFinished ngay trong cùng một sự kiện, trước
    // khi composition kịp chạy lại, nên onSeekFinished phải thấy giá trị mới nhất chứ không phải giá trị lúc dựng.
    val scrub = scrubState()
    val shownMs = if (scrub != null) (scrub * duration).toLong() else controller.positionMs
    val speedText = formatSpeed(state.speed)
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
        ODVViewerSpeedButton(
            label = speedText,
            contentDescription = stringResource(R.string.player_speed_description, speedText),
            onClick = {
                onTouch()
                onOpenSpeed()
            },
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
