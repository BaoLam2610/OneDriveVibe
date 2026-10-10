@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.lambao.odv.core.designsystem.format.odvFormatDuration
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerBufferSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.component.ODVViewerMiniProgress
import com.lambao.odv.core.designsystem.component.ODVViewerNetworkNotice
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.media.PlayerFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Giao diện tab Short (thiet-ke-ui.md mục 4.8, 5.5; SH1 → SH9): nền đen, vuốt dọc qua các video, không AppBar và không nút tương tác
 * (SV-06). Chỉ **một** bề mặt phát, gắn vào trang đang hiện (ADR-0024); trang khác chỉ vẽ thumbnail. [controller] null cho tới khi
 * player dựng xong.
 */
@Composable
internal fun ShortsContent(
    state: ShortsState,
    controller: ShortPlayerController?,
    reshuffledShown: Boolean,
    onIntent: (ShortsIntent) -> Unit,
    onPositionChanged: (Long) -> Unit,
    onPreload: (List<ShortVideo>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(ODVMediaColors.background)) {
        when (state.phase) {
            ShortsPhase.Loading -> ODVViewerBufferSpinner(
                label = stringResource(R.string.shorts_loading),
                modifier = Modifier.align(Alignment.Center),
            )
            // SH9 (SV-15): trạng thái trống trên nền đen, nút chuyển sang Cài đặt để tăng thời lượng tối đa.
            ShortsPhase.Empty -> ODVViewerError(
                title = stringResource(R.string.shorts_empty_title),
                modifier = Modifier.align(Alignment.Center),
                body = stringResource(R.string.shorts_empty_body, state.maxMinutes),
                icon = ODVIcon.Video,
                action = { ODVViewerButton(stringResource(R.string.shorts_open_settings), onOpenSettings) },
            )
            // Dựng lại Pager từ đầu mỗi lần danh sách được xây hoặc xáo lại (generation đổi).
            ShortsPhase.Ready -> key(state.generation) {
                ShortsPager(state, controller, reshuffledShown, onIntent, onPositionChanged, onPreload)
            }
        }
    }
}

@Composable
private fun BoxScope.ShortsPager(
    state: ShortsState,
    controller: ShortPlayerController?,
    reshuffledShown: Boolean,
    onIntent: (ShortsIntent) -> Unit,
    onPositionChanged: (Long) -> Unit,
    onPreload: (List<ShortVideo>) -> Unit,
) {
    val ids = state.ids
    val currentOnIntent by rememberUpdatedState(onIntent)
    val currentOnPosition by rememberUpdatedState(onPositionChanged)
    val currentOnPreload by rememberUpdatedState(onPreload)
    val pagerState = rememberPagerState(initialPage = state.index.coerceIn(0, (ids.size - 1).coerceAtLeast(0))) { ids.size }
    val settled = pagerState.settledPage

    LaunchedEffect(Unit) {
        shortsLog.i { "[Short][UI] dựng Pager gen=${state.generation} index=${state.index} ${ids.size} video, player=${if (controller != null) "sẵn sàng" else "chưa có"}" }
    }

    // SV-04: báo ViewModel khi người dùng vuốt sang video khác (không báo trang mở đầu).
    LaunchedEffect(settled) {
        if (settled != state.index) currentOnIntent(ShortsIntent.PageSettled(settled))
    }

    // Nạp video của trang đang hiện. Cùng video và cùng đợt danh sách thì là quay lại tab: chỉ phát tiếp đúng vị trí (SV-11).
    val currentId = ids.getOrNull(settled)
    val currentVideo = currentId?.let { state.videos[it] }
    // Lưu ý: sau khi app bị khóa, controller.loadedId về null nhưng không phải key của effect này; nạp lại chạy được vì màn Khóa đẩy Màn chính ra
    // khỏi composition nên effect khởi động lại khi mở khóa. Nếu sau này Màn chính được giữ dưới màn Khóa thì phải thêm loadedId vào key.
    LaunchedEffect(controller, currentVideo?.item?.id, state.generation) {
        val player = controller ?: return@LaunchedEffect
        val video = currentVideo
        if (video == null) {
            // Chi tiết trang mới chưa có (hoặc bản ghi vừa bị xóa): trang vẽ đen, nên đừng để video trước còn phát tiếng ở phía sau.
            shortsLog.d { "[Short][UI] trang $settled chưa có chi tiết, tạm dừng video đang nạp" }
            player.pause()
            return@LaunchedEffect
        }
        val firstOfGeneration = player.loadedGeneration != state.generation
        shortsLog.d {
            "[Short][UI] quyết định nạp: trang=$settled id=${video.item.id.takeLast(ID_LOG_LENGTH)} đang nạp=${player.loadedId?.takeLast(ID_LOG_LENGTH)} " +
                "gen=${state.generation}/${player.loadedGeneration} → ${if (player.loadedId != video.item.id || firstOfGeneration) "nạp mới" else "phát tiếp"}"
        }
        if (player.loadedId != video.item.id || firstOfGeneration) {
            // Chỉ lần nạp đầu của một đợt danh sách mới dùng vị trí và trạng thái khôi phục (DH-06, SV-11).
            val restoring = firstOfGeneration && state.startPaused
            // Sau khi app bị khóa (CH-03) player đã bị dừng và bỏ nguồn: nạp lại đúng vị trí, giữ nguyên ý định dừng/phát của người dùng.
            val afterLock = if (firstOfGeneration) null else player.takeLockedResume(video.item.id)
            player.load(
                item = video.item,
                generation = state.generation,
                startPositionMs = if (firstOfGeneration) state.resumePositionMs else afterLock ?: 0L,
                autoPlay = if (afterLock != null) !player.userPaused else !restoring,
            )
            if (firstOfGeneration) currentOnIntent(ShortsIntent.RestoreConsumed)
        } else {
            player.resume()
        }
    }

    // SV-13: tải trước đầu các video kế cận (kế tiếp trước, rồi video trước), chỉ sau khi video đang xem đã lên hình để không giành băng thông
    // với nó. Mất mạng thì holder tự bỏ qua.
    val currentRendered = controller != null && currentId != null && controller.renderedItemId == currentId
    val neighbours = listOf(settled + 1, settled - 1).mapNotNull { ids.getOrNull(it) }.mapNotNull { state.videos[it] }
    LaunchedEffect(currentRendered, settled, state.generation, neighbours.map { it.item.id }) {
        if (currentRendered && neighbours.isNotEmpty()) currentOnPreload(neighbours)
    }

    // SV-07: đọc vị trí theo nhịp ngắn cho thanh tiến độ, đồng thời để màn lưu lại cho lần khôi phục.
    LaunchedEffect(controller) {
        val player = controller ?: return@LaunchedEffect
        while (isActive) {
            player.refreshProgress()
            currentOnPosition(player.positionMs)
            delay(ShortsConstants.PROGRESS_TICK_MS)
        }
    }

    // SV-11: tạm dừng khi rời tab, xuống nền hoặc bị khóa; quay lại thì phát tiếp (trừ khi người dùng đã tạm dừng).
    if (controller != null) {
        LifecycleResumeEffect(controller) {
            shortsLog.d { "[Short][UI] vào/tiếp tục màn hình" }
            // Chỉ phát tiếp video của đúng đợt danh sách này: sau khi xáo lại, player còn video đợt cũ cho tới khi load chạy xong.
            if (controller.loadedGeneration == state.generation) controller.resume()
            onPauseOrDispose {
                shortsLog.d { "[Short][UI] rời/tạm dừng màn hình (đổi tab, xuống nền hoặc bị khóa)" }
                controller.pause()
            }
        }
    }

    // SV-05: giữ màn hình sáng khi đang phát.
    val view = LocalView.current
    val playing = controller?.isPlaying == true
    DisposableEffect(playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }

    val thresholdPx = with(LocalDensity.current) { ShortsConstants.RESHUFFLE_PULL_DP.dp.toPx() }
    val reshuffle = remember(pagerState, thresholdPx) {
        ReshuffleConnection(
            thresholdPx = thresholdPx,
            // Dùng chỉ số trang chứ không dùng canScrollBackward: thuộc tính đó cập nhật chậm một khung nên có lúc còn `true` dù Pager đã về 0.
            canPull = { pagerState.currentPage == 0 },
            onTrigger = { currentOnIntent(ShortsIntent.Reshuffle) },
        )
    }
    val scope = rememberCoroutineScope()
    val nextLabel = stringResource(R.string.shorts_action_next)
    val previousLabel = stringResource(R.string.shorts_action_previous)
    val reshuffleLabel = stringResource(R.string.shorts_action_reshuffle)

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(reshuffle)
            // TalkBack: thay thao tác vuốt bằng hành động tùy chỉnh (thiet-ke-ui.md mục 4.8).
            .semantics {
                customActions = buildList {
                    if (settled < ids.lastIndex) {
                        add(CustomAccessibilityAction(nextLabel) { scope.launch { pagerState.animateScrollToPage(settled + 1) }; true })
                    }
                    if (settled > 0) {
                        add(CustomAccessibilityAction(previousLabel) { scope.launch { pagerState.animateScrollToPage(settled - 1) }; true })
                    } else {
                        add(CustomAccessibilityAction(reshuffleLabel) { currentOnIntent(ShortsIntent.Reshuffle); true })
                    }
                }
            },
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            key = { ids[it] },
        ) { page ->
            ShortPage(
                video = state.videos[ids[page]],
                controller = controller,
                isCurrent = page == settled,
            )
        }

        // SV-07: thanh tiến độ mỏng sát mép trên thanh đáy; chạm vào thì hiện thanh tua, đang tạm dừng cũng hiện.
        val loaded = controller?.takeIf { it.loadedId == currentId }
        if (loaded != null) {
            ShortSeek(loaded, Modifier.align(Alignment.BottomCenter))
        }

        // SH6: vòng xáo lại trượt xuống theo ngón tay khi kéo ở video đầu (SV-03).
        // Đọc `pull` trong composable con để chỉ vòng này dựng lại theo từng px kéo, không kéo theo cả Pager.
        ReshuffleIndicator(reshuffle, Modifier.align(Alignment.TopCenter))
        AnimatedVisibility(
            visible = reshuffledShown,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
            enter = fadeIn(tween(ShortsConstants.PAUSE_FADE_MS)),
            exit = fadeOut(tween(ShortsConstants.PAUSE_FADE_MS)),
        ) {
            ODVViewerPill(text = stringResource(R.string.shorts_reshuffled), live = true)
        }
    }
}

/** SH6: vòng xáo lại trượt xuống theo ngón tay khi kéo ở video đầu (SV-03); không vẽ gì khi chưa kéo. */
@Composable
private fun ReshuffleIndicator(reshuffle: ReshuffleConnection, modifier: Modifier = Modifier) {
    if (reshuffle.pull <= 0f) return
    Box(
        modifier
            .statusBarsPadding()
            .padding(top = 12.dp)
            .offset { IntOffset(0, (reshuffle.pull * 0.5f).roundToInt()) }
            .graphicsLayer { alpha = reshuffle.progress }
            .size(40.dp)
            .background(ODVMediaColors.pill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ODVIcon(ODVIcon.Sync, contentDescription = null, tint = ODVMediaColors.onMedia, size = 24.dp)
    }
}

/**
 * Một trang video. Bề mặt phát chỉ dựng ở trang đang hiện và đúng video player đang nạp; trước khung hình đầu là nền đen kèm spinner (SH5,
 * không dùng thumbnail của OneDrive vì nó không phải khung hình đầu và gây nháy). Khung hình theo [decideFit] (SV-08), tên tệp và thư mục
 * chứa nó luôn hiện ở góc dưới trái (SV-09).
 */
@Composable
private fun ShortPage(
    video: ShortVideo?,
    controller: ShortPlayerController?,
    isCurrent: Boolean,
) {
    val item = video?.item
    val playingLabel = stringResource(R.string.shorts_state_playing)
    val pausedLabel = stringResource(R.string.shorts_state_paused)
    val tapPauseLabel = stringResource(R.string.shorts_action_pause)
    val tapPlayLabel = stringResource(R.string.shorts_action_play)
    val loadingLabel = stringResource(R.string.shorts_loading)
    val rootLabel = stringResource(R.string.shorts_root)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(ODVMediaColors.background).clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        // Chưa có thông tin video này (đang nạp từ Room): để trang đen.
        if (item == null || video == null) return@BoxWithConstraints

        val containerAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else 0f
        val active = if (isCurrent && controller != null && controller.loadedId == item.id) controller else null
        val rendered = active != null && active.renderedItemId == item.id
        val failure = active?.takeIf { it.failedItemId == item.id }?.failure
        val aspect = active?.videoAspect?.takeIf { it > 0f } ?: item.video.guessAspect()
        val fit = decideFit(aspect, containerAspect)
        val paused = active?.userPaused == true

        Box(
            Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = item.name + ", " + if (paused) pausedLabel else playingLabel
                },
            contentAlignment = Alignment.Center,
        ) {
            if (active != null) {
                PlayerSurface(
                    player = active.player,
                    modifier = when {
                        aspect <= 0f -> Modifier.fillMaxSize()
                        fit == ShortFit.Cover -> Modifier.coverAspect(aspect)
                        else -> Modifier.aspectRatio(aspect)
                    }
                        // TextureView giữ lại khung hình cuối của video trước: ẩn tới khi video này vẽ khung đầu.
                        .graphicsLayer { alpha = if (rendered) 1f else 0f },
                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                )
            }
            // Cố ý KHÔNG phủ thumbnail của OneDrive trước khung hình đầu (kiểm tay 2026-10-11): thumbnail không phải khung hình đầu của video nên
            // khi khung hình thật hiện ra, hình bị nháy từ ảnh này sang ảnh khác. Nền đen cộng spinner cho tới khi có khung hình đầu thật.
            // Việc dùng đúng khung hình đầu làm poster (như các app Short khác) cần tải trước và giải mã sẵn, ghi ở kế hoạch 8c.
            if (isCurrent && !rendered && failure == null) {
                ODVSpinner(
                    size = 36.dp,
                    color = ODVMediaColors.onMedia,
                    trackColor = ODVMediaColors.track,
                    modifier = Modifier.semantics { contentDescription = loadingLabel },
                )
            }
            // SV-06: chạm một lần để tạm dừng/phát tiếp. Không có tua, chạm đúp, zoom hay khóa thao tác.
            if (active != null && failure == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = if (paused) tapPlayLabel else tapPauseLabel,
                            onClick = active::togglePlay,
                        ),
                )
            }
            AnimatedVisibility(
                visible = active != null && paused && failure == null,
                enter = fadeIn(tween(ShortsConstants.PAUSE_FADE_MS)),
                exit = fadeOut(tween(ShortsConstants.PAUSE_FADE_MS)),
            ) {
                Box(Modifier.size(ODVSize.playButton).background(ODVMediaColors.pill, CircleShape), contentAlignment = Alignment.Center) {
                    ODVIcon(ODVIcon.Play, contentDescription = null, tint = ODVMediaColors.onMedia, size = 36.dp)
                }
            }
            if (failure != null && active != null) ShortFailure(failure, item.name, active)
            ShortInfo(
                name = item.name,
                folder = video.folderName ?: rootLabel,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/**
 * Thẻ lỗi (SH7, SH8; SV-14): mất mạng có nút Tiếp tục; các lỗi khác báo rõ nguyên nhân, **không** đếm ngược và **không** tự chuyển, người
 * dùng tự vuốt sang video khác. Chỉ lỗi chung mới có nút Thử lại.
 */
@Composable
private fun BoxScope.ShortFailure(failure: PlayerFailure, fileName: String, controller: ShortPlayerController) {
    if (failure == PlayerFailure.Network) {
        ODVViewerNetworkNotice(
            title = stringResource(R.string.shorts_error_network_title),
            body = stringResource(R.string.shorts_error_network_body),
            actionLabel = stringResource(R.string.shorts_resume),
            onAction = controller::retry,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            // VD-16: nút chỉ bật khi có mạng lại (NetworkMonitor); lúc tắt có dòng nhắc.
            actionEnabled = controller.isOnline,
            hint = stringResource(R.string.shorts_error_network_hint),
        )
        return
    }
    val (title, body) = when (failure) {
        PlayerFailure.Unsupported -> R.string.shorts_error_unsupported_title to R.string.shorts_error_unsupported_body
        PlayerFailure.Removed -> R.string.shorts_error_removed_title to R.string.shorts_error_removed_body
        else -> R.string.shorts_error_other_title to R.string.shorts_error_other_body
    }
    ODVViewerError(
        title = stringResource(title),
        body = stringResource(body),
        fileLine = fileName,
        modifier = Modifier.align(Alignment.Center),
        action = if (failure == PlayerFailure.Other) {
            { ODVViewerButton(stringResource(R.string.shorts_retry), controller::retry, filled = true) }
        } else {
            null
        },
    )
}

/** Tên tệp và thư mục chứa nó (SV-09): chữ mờ, một dòng mỗi thứ, cắt "…" khi dài; dải gradient `media.scrim` phía sau cho dễ đọc, không bấm được. */
@Composable
private fun ShortInfo(name: String, folder: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(INFO_HEIGHT)
            .background(Brush.verticalGradient(listOf(Color.Transparent, ODVMediaColors.scrim)))
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(
                name,
                style = ODVTheme.typography.bodyStrong,
                color = ODVMediaColors.onMediaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                folder,
                style = ODVTheme.typography.caption,
                color = ODVMediaColors.onMediaFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Thanh tiến độ kiêm thanh tua của Short (SV-07). Luôn có thanh mảnh [ODVViewerMiniProgress]; **chạm vào vùng thanh** hoặc **khi người dùng
 * đang tạm dừng** thì thay bằng [ShortSeekBar] (rãnh 4, núm 16, vùng chạm 24, rãnh sát đường tiếp xúc với thanh đáy). Khi đang chạm còn hiện viên thời gian
 * "đang kéo / tổng" phía trên. Kéo hoặc chạm chỉ đổi vị trí hiển thị; tua thật khi **nhả tay** ([ShortPlayerController.seekTo]), và không đổi
 * trạng thái phát/dừng. Thanh nằm trên Pager nên cú vuốt bắt đầu ở đây không làm đổi video.
 */
@Composable
private fun ShortSeek(controller: ShortPlayerController, modifier: Modifier = Modifier) {
    var touching by remember { mutableStateOf(false) }
    // Phần đang kéo (0..1); null khi không kéo thì thanh theo vị trí phát thật.
    var scrub by remember { mutableStateOf<Float?>(null) }
    val duration = controller.durationMs
    val played = scrub ?: fraction(controller.positionMs, duration)
    val seekVisible = touching || controller.userPaused
    val seekAlpha by animateFloatAsState(
        targetValue = if (seekVisible) 1f else 0f,
        animationSpec = tween(ShortsConstants.PAUSE_FADE_MS),
        label = "shortSeekAlpha",
    )
    val description = stringResource(R.string.shorts_seek_description)
    val valueDescription = stringResource(
        R.string.shorts_seek_value,
        odvFormatDuration((played * duration).toLong()),
        odvFormatDuration(duration),
    )
    Box(modifier.fillMaxWidth()) {
        ODVViewerMiniProgress(
            position = fraction(controller.positionMs, duration),
            buffered = fraction(controller.bufferedPositionMs, duration),
            modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = 1f - seekAlpha },
        )
        if (touching && duration > 0L) {
            ODVViewerPill(
                text = "${odvFormatDuration((played * duration).toLong())} / ${odvFormatDuration(duration)}",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = ODVSize.seekTouch + 8.dp),
                mono = true,
            )
        }
        // alpha 0 vẫn nhận chạm, nên chạm vào vùng này khi thanh tua đang ẩn sẽ hiện nó ngay. Không chừa lề trái phải: thanh phẳng, liền hai mép
        // màn như thanh mảnh (kiểm tay 2026-10-11).
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { alpha = seekAlpha }
                .observeTouch { isTouching ->
                    touching = isTouching
                    shortsLog.d { "[Short][Seek] ${if (isTouching) "chạm vào thanh tua" else "nhả tay khỏi thanh tua"}" }
                },
        ) {
            ShortSeekBar(
                position = played,
                buffered = fraction(controller.bufferedPositionMs, duration),
                onSeek = { if (duration > 0L) scrub = it },
                contentDescription = description,
                valueDescription = valueDescription,
                onSeekFinished = {
                    scrub?.let { controller.seekTo((it * duration).toLong()) }
                    scrub = null
                },
            )
        }
    }
}

/**
 * Báo ngón tay đang chạm vùng này hay không, **không** chiếm cử chỉ: đọc sự kiện ở lượt Initial/Final nên thanh tua bên trong vẫn nhận chạm và
 * kéo bình thường.
 */
private fun Modifier.observeTouch(onTouching: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onTouching(true)
        do {
            val event = awaitPointerEvent(PointerEventPass.Final)
        } while (event.changes.any { it.pressed })
        onTouching(false)
    }
}

private fun fraction(valueMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (valueMs.toFloat() / durationMs).coerceIn(0f, 1f)

/** Cao của dải gradient sau tên tệp (thiet-ke-ui.md mục 4.8). */
private val INFO_HEIGHT = 96.dp

/** Id Graph dài; log chỉ ghi chừng này ký tự cuối (cùng quy ước với `:core:media`, CH-06). */
private const val ID_LOG_LENGTH = 8
