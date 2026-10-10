@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.media.PlayerFailure
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.designsystem.component.ODVSidePanel
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.component.ODVViewerHud
import com.lambao.odv.core.designsystem.component.ODVViewerNetworkNotice
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVViewerResetZoomButton
import com.lambao.odv.core.designsystem.component.ODVViewerTopBar
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvFormatSpeed
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.DriveItem
import kotlin.math.roundToInt

/** HUD độ sáng/âm lượng đang hiện; [tick] đổi mỗi lần vuốt để hẹn giờ ẩn tính lại. */
internal data class SwipeHud(val kind: SwipeKind, val level: Float, val tick: Int)

/** Viên thuốc nhãn đổi khung hình / chế độ phát (VD-06, VD-20), hiện [PlayerConstants.LABEL_MS]. */
internal data class LabelPill(val text: String, val icon: ODVIcon?, val tick: Int)

/**
 * Lớp điều khiển phủ lên video (thanh trên có Thông tin và Khóa, cụm giữa, thanh dưới), hiện/ẩn theo [visible]. Không giữ state:
 * mọi thay đổi đi qua lambda về [PlayerLayer]. [showPlaybackControls] tắt khi có lỗi (chỉ còn thanh trên).
 */
@Composable
internal fun PlayerControlsOverlay(
    visible: Boolean,
    controller: VideoPlayerController,
    state: PlayerState,
    current: DriveItem,
    playState: ODVPlayState,
    landscape: Boolean,
    showPlaybackControls: Boolean,
    scrubState: () -> Float?,
    onScrub: (Float?) -> Unit,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onLock: () -> Unit,
    onOpenSpeed: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onCycleFit: () -> Unit,
    onRotate: () -> Unit,
    onResetZoom: (() -> Unit)?,
    onIntent: (PlayerIntent) -> Unit,
    onTouch: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
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
                        onInfo,
                        colors = ODVMediaDefaults.iconButtonColors(),
                    )
                    ODVIconButton(
                        ODVIcon.Lock,
                        stringResource(R.string.player_lock),
                        onLock,
                        colors = ODVMediaDefaults.iconButtonColors(),
                    )
                },
            )
            if (showPlaybackControls) {
                CenterControls(controller, state, playState, landscape, onIntent, onTouch)
                BottomControls(
                    controller = controller,
                    state = state,
                    duration = controller.durationMs,
                    landscape = landscape,
                    scrubState = scrubState,
                    onScrub = onScrub,
                    onOpenSpeed = onOpenSpeed,
                    onCyclePlayMode = onCyclePlayMode,
                    onCycleFit = onCycleFit,
                    onRotate = onRotate,
                    onResetZoom = onResetZoom,
                    onTouch = onTouch,
                )
            }
        }
    }
}

/**
 * Các lớp phản hồi nhỏ: nhãn đổi khung hình / chế độ phát, HUD độ sáng/âm lượng, viên 2x khi giữ, nút "Đặt lại zoom" hướng dọc
 * ([showResetZoom]). Thứ tự vẽ giữ như trước khi tách.
 */
@Composable
internal fun BoxScope.PlayerFeedbackOverlays(
    labelPill: LabelPill?,
    hud: SwipeHud?,
    boosting: Boolean,
    showResetZoom: Boolean,
    onResetZoom: () -> Unit,
) {
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
            text = odvFormatSpeed(PlayerConstants.BOOST_SPEED, odvLocale()),
            mono = true,
            live = true,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp),
        )
    }

    // Nút "Đặt lại zoom" kèm dòng gợi ý (VD-18) chỉ hiện khi đang zoom VÀ điều khiển đang hiện: lúc xem thì không che hình.
    // Hướng dọc: cách đáy 112 (thiet-ke-ui.md mục 4.5). Hướng ngang: chiều cao thấp, mọi chỗ trống đều trùng cụm giữa hoặc thanh
    // trên, nên nút nằm ngay trong hàng nút của thanh dưới (BottomControls, tham số onResetZoom), trước nút Tốc độ.
    if (showResetZoom) {
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 112.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ODVViewerResetZoomButton(stringResource(R.string.player_zoom_reset), onResetZoom)
            Text(stringResource(R.string.player_zoom_hint), style = ODVTheme.typography.caption, color = ODVMediaColors.onMediaMuted)
        }
    }
}

/** Thẻ "Tiếp theo sau 5 giây" (VD-13, VD-15): cùng vị trí cho cả thẻ phát tiếp và thẻ bỏ qua video lỗi. */
@Composable
internal fun BoxScope.PlayerNextUpCard(
    nextTitle: String,
    landscape: Boolean,
    onFinished: () -> Unit,
    onCancel: () -> Unit,
) {
    NextUpCard(
        nextTitle = nextTitle,
        onFinished = onFinished,
        onCancel = onCancel,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = if (landscape) 88.dp else 112.dp),
    )
}

/** Bảng Tốc độ và bảng Thông tin tệp (VD-17: hướng dọc là bottom sheet, hướng ngang là bảng bên phải). */
@Composable
internal fun PlayerSheets(
    speed: Float,
    speedSheetOpen: Boolean,
    info: PlayerInfo?,
    landscape: Boolean,
    onIntent: (PlayerIntent) -> Unit,
    onDismissSpeed: () -> Unit,
) {
    if (speedSheetOpen) {
        SpeedSheet(
            current = speed,
            onSelect = { onIntent(PlayerIntent.SetSpeed(it)) },
            onDismiss = onDismissSpeed,
        )
    }

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

/** Thẻ lỗi (V9, V10): mất mạng có nút Tiếp tục (VD-16), còn lại báo rõ nguyên nhân thay vì để màn hình đen (VD-15). */
@Composable
internal fun BoxScope.FailureOverlay(
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
