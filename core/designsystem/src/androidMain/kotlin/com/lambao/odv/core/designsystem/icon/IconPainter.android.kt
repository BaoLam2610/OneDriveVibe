// Sinh bởi script, khớp enum ODVIcon.
package com.lambao.odv.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.lambao.odv.core.designsystem.R

@Composable
internal actual fun rememberIconPainter(icon: ODVIcon): Painter = painterResource(
    when (icon) {
        ODVIcon.Play -> R.drawable.ic_play
        ODVIcon.Pause -> R.drawable.ic_pause
        ODVIcon.Folder -> R.drawable.ic_folder
        ODVIcon.Image -> R.drawable.ic_image
        ODVIcon.Video -> R.drawable.ic_video
        ODVIcon.Book -> R.drawable.ic_book
        ODVIcon.Search -> R.drawable.ic_search
        ODVIcon.Tune -> R.drawable.ic_tune
        ODVIcon.Eye -> R.drawable.ic_eye
        ODVIcon.EyeOff -> R.drawable.ic_eye_off
        ODVIcon.Lock -> R.drawable.ic_lock
        ODVIcon.Sort -> R.drawable.ic_sort
        ODVIcon.Grid -> R.drawable.ic_grid
        ODVIcon.List -> R.drawable.ic_list
        ODVIcon.ArrowLeft -> R.drawable.ic_arrow_left
        ODVIcon.ChevronRight -> R.drawable.ic_chevron_right
        ODVIcon.CloudOff -> R.drawable.ic_cloud_off
        ODVIcon.Sync -> R.drawable.ic_sync
        ODVIcon.Check -> R.drawable.ic_check
        ODVIcon.Alert -> R.drawable.ic_alert
        ODVIcon.Rewind -> R.drawable.ic_rewind
        ODVIcon.Forward -> R.drawable.ic_forward
        ODVIcon.Fullscreen -> R.drawable.ic_fullscreen
        ODVIcon.Settings -> R.drawable.ic_settings
        ODVIcon.Sun -> R.drawable.ic_sun
        ODVIcon.Volume -> R.drawable.ic_volume
        ODVIcon.Unlock -> R.drawable.ic_unlock
        ODVIcon.Backspace -> R.drawable.ic_backspace
        ODVIcon.Fingerprint -> R.drawable.ic_fingerprint
        ODVIcon.Rotate -> R.drawable.ic_rotate
        ODVIcon.Replay -> R.drawable.ic_replay
        ODVIcon.Repeat -> R.drawable.ic_repeat
        ODVIcon.RepeatOne -> R.drawable.ic_repeat_one
        ODVIcon.Playlist -> R.drawable.ic_playlist
        ODVIcon.StopEnd -> R.drawable.ic_stop_end
        ODVIcon.Close -> R.drawable.ic_close
        ODVIcon.Clock -> R.drawable.ic_clock
        ODVIcon.Info -> R.drawable.ic_info
        ODVIcon.Minus -> R.drawable.ic_minus
        ODVIcon.Fit -> R.drawable.ic_fit
        ODVIcon.Prev -> R.drawable.ic_prev
        ODVIcon.Next -> R.drawable.ic_next
        ODVIcon.Short -> R.drawable.ic_short
    },
)
