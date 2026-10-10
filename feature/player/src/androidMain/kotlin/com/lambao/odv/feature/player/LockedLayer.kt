@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lambao.odv.core.designsystem.component.ODVViewerLockHoldButton
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.icon.ODVIcon
import kotlinx.coroutines.delay

/**
 * Lớp khóa thao tác (VD-08, V7): chặn mọi chạm bên dưới. Viên thuốc "Đã khóa thao tác" và nút giữ mở khóa ẩn/hiện giống thanh điều
 * khiển video: chạm thì hiện, chạm nữa thì ẩn, tự ẩn sau [PlayerConstants.LOCK_HINT_MS]; bấm Back thì hiện. Video vẫn phát bình thường
 * phía dưới.
 */
@Composable
internal fun BoxScope.LockedLayer(landscape: Boolean, onUnlock: () -> Unit) {
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
