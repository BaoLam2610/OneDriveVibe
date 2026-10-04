package com.lambao.odv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.VideoFit

/** Nhãn ngắn của chế độ phát (VD-20), dùng cho viên thuốc khi đổi và cho mô tả TalkBack. */
@Composable
internal fun PlayMode.label(): String = stringResource(
    when (this) {
        PlayMode.NoRepeat -> R.string.player_mode_no_repeat
        PlayMode.AutoNext -> R.string.player_mode_auto_next
        PlayMode.RepeatOne -> R.string.player_mode_repeat_one
        PlayMode.RepeatList -> R.string.player_mode_repeat_list
    },
)

/** Icon của chế độ phát trên nút (thiet-ke-ui.md mục 3.1). */
internal fun PlayMode.icon(): ODVIcon = when (this) {
    PlayMode.NoRepeat -> ODVIcon.StopEnd
    PlayMode.AutoNext -> ODVIcon.Playlist
    PlayMode.RepeatOne -> ODVIcon.RepeatOne
    PlayMode.RepeatList -> ODVIcon.Repeat
}

/** Nhãn ngắn của khung hình (VD-06): "Vừa khung", "Cắt đầy", "Kéo giãn". */
@Composable
internal fun VideoFit.label(): String = stringResource(
    when (this) {
        VideoFit.Fit -> R.string.player_fit_fit
        VideoFit.Crop -> R.string.player_fit_crop
        VideoFit.Stretch -> R.string.player_fit_stretch
    },
)
