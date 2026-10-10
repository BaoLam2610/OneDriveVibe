@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
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
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.thumbnailSource
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
                ShortsPager(state, controller, reshuffledShown, onIntent, onPositionChanged)
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
) {
    val ids = state.ids
    val currentOnIntent by rememberUpdatedState(onIntent)
    val currentOnPosition by rememberUpdatedState(onPositionChanged)
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
    LaunchedEffect(controller, currentVideo?.item?.id, state.generation) {
        val player = controller ?: return@LaunchedEffect
        val video = currentVideo ?: return@LaunchedEffect
        val firstOfGeneration = player.loadedGeneration != state.generation
        shortsLog.d {
            "[Short][UI] quyết định nạp: trang=$settled id=${video.item.id.takeLast(ID_LOG_LENGTH)} đang nạp=${player.loadedId?.takeLast(ID_LOG_LENGTH)} " +
                "gen=${state.generation}/${player.loadedGeneration} → ${if (player.loadedId != video.item.id || firstOfGeneration) "nạp mới" else "phát tiếp"}"
        }
        if (player.loadedId != video.item.id || firstOfGeneration) {
            // Chỉ lần nạp đầu của một đợt danh sách mới dùng vị trí và trạng thái khôi phục (DH-06, SV-11).
            val restoring = firstOfGeneration && state.startPaused
            player.load(
                item = video.item,
                generation = state.generation,
                startPositionMs = if (firstOfGeneration) state.resumePositionMs else 0L,
                autoPlay = !restoring,
            )
            if (firstOfGeneration) currentOnIntent(ShortsIntent.RestoreConsumed)
        } else {
            player.resume()
        }
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
            controller.resume()
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
            canPull = { !pagerState.canScrollBackward },
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

        // SV-07: thanh tiến độ mỏng sát mép trên thanh đáy, chỉ để xem.
        val loaded = controller?.takeIf { it.loadedId == currentId }
        if (loaded != null) {
            ODVViewerMiniProgress(
                position = fraction(loaded.positionMs, loaded.durationMs),
                buffered = fraction(loaded.bufferedPositionMs, loaded.durationMs),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // SH6: vòng xáo lại trượt xuống theo ngón tay khi kéo ở video đầu (SV-03).
        if (reshuffle.pull > 0f) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
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

/**
 * Một trang video. Bề mặt phát chỉ dựng ở trang đang hiện và đúng video player đang nạp; thumbnail phủ lên tới khi có khung hình đầu để
 * không để màn đen (SH5). Khung hình theo [decideFit] (SV-08), tên tệp và thư mục chứa nó luôn hiện ở góc dưới trái (SV-09).
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
            // Lỗi codec/đã xóa thì không phủ thumbnail lên thẻ lỗi; mất mạng vẫn giữ để có hình nền.
            if (!rendered && (failure == null || failure == PlayerFailure.Network)) {
                // key theo id: AsyncImage giữ ảnh cũ trong lúc tải ảnh mới nên cần key để thumbnail video trước không hiện lại.
                key(item.id) {
                    AsyncImage(
                        model = item.thumbnailSource(ThumbnailSize.Viewer),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (fit == ShortFit.Cover) ContentScale.Crop else ContentScale.Fit,
                    )
                }
            }
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
            actionEnabled = true,
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

private fun fraction(valueMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (valueMs.toFloat() / durationMs).coerceIn(0f, 1f)

/** Cao của dải gradient sau tên tệp (thiet-ke-ui.md mục 4.8). */
private val INFO_HEIGHT = 96.dp

/** Id Graph dài; log chỉ ghi chừng này ký tự cuối (cùng quy ước với `:core:media`, CH-06). */
private const val ID_LOG_LENGTH = 8
