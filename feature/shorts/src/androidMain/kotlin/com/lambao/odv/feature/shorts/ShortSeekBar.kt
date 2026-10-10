package com.lambao.odv.feature.shorts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVSize

/**
 * Thanh tua của tab Short (SV-07). Rãnh [ODVSize.seekTrack] 4 và núm [ODVSize.seekThumb] 16 như `ODVViewerSeekBar` của màn Xem video, nhưng:
 * **rãnh nằm sát đáy** vùng chạm, tức ngay trên đường tiếp xúc giữa khung video và thanh điều hướng đáy; **phẳng và liền hai mép màn** (không
 * chừa lề, không bo đầu); **vùng chạm cao [ODVSize.tapTarget] 48** (không phải 24) để chạm vào đáy màn không rơi sang tab của thanh đáy
 * (yêu cầu kiểm tay 2026-10-11). `ODVViewerSeekBar` đặt rãnh ở giữa vùng chạm nên rãnh nổi lên cách đường đó 10dp.
 * Núm cùng tâm với rãnh nên tràn xuống dưới đáy vài dp: màn Chính cho vùng nội dung vẽ đè lên thanh đáy (`zIndex`) để phần tràn đó thấy được.
 *
 * Chạm hoặc kéo ngang để chọn vị trí; [onSeek] gọi liên tục, [onSeekFinished] khi nhả tay. TalkBack điều chỉnh qua `setProgress`.
 *
 * @param position vị trí hiện tại trong 0..1; [buffered] phần đã tải trước trong 0..1.
 */
@Composable
internal fun ShortSeekBar(
    position: Float,
    buffered: Float,
    onSeek: (Float) -> Unit,
    contentDescription: String,
    valueDescription: String,
    modifier: Modifier = Modifier,
    onSeekFinished: () -> Unit = {},
    expanded: Float = 1f,
) {
    val seek by rememberUpdatedState(onSeek)
    val finished by rememberUpdatedState(onSeekFinished)
    val played = position.unit()
    val loaded = buffered.unit()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            // Vùng chạm 48 chứ không phải 24 như màn Xem video: thanh nằm ngay trên thanh điều hướng đáy, vùng chạm thấp thì ngón tay dễ
            // rơi xuống một tab của thanh đáy (kiểm tay 2026-10-11). Phần rãnh vẫn chỉ cao 4, sát đáy vùng chạm.
            .height(ODVSize.tapTarget)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = valueDescription
                progressBarRangeInfo = ProgressBarRangeInfo(played, 0f..1f)
                setProgress { target ->
                    seek(target.unit())
                    finished()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    seek((offset.x / size.width).unit())
                    finished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> seek((offset.x / size.width).unit()) },
                    onDragEnd = { finished() },
                    onDragCancel = { finished() },
                ) { change, _ -> seek((change.position.x / size.width).unit()) }
            },
    ) {
        // Một thanh duy nhất đổi dáng liên tục theo [expanded]: 0 là thanh mảnh `mini-progress` 3 không núm (đang phát, không chạm), 1 là thanh tua
        // `seek-track` 4 có núm 16. Trước đây hai thanh riêng mờ dần chéo nhau nên lúc chạm thấy hai thanh khác dày chồng lên nhau, hiện ra như nháy.
        val miniHeight = ODVSize.miniProgress.toPx()
        val trackHeight = miniHeight + (ODVSize.seekTrack.toPx() - miniHeight) * expanded
        // Rãnh sát đáy: mép dưới của rãnh trùng mép dưới vùng chạm, tức đường tiếp xúc với thanh đáy.
        val top = size.height - trackHeight
        // Phẳng và liền hai mép màn như thanh mảnh `ODVViewerMiniProgress` (hình chữ nhật, không bo đầu, không chừa lề trái phải).
        drawRect(ODVMediaColors.track, Offset(0f, top), Size(size.width, trackHeight))
        drawRect(ODVMediaColors.buffer, Offset(0f, top), Size(size.width * loaded, trackHeight))
        drawRect(ODVMediaColors.accent, Offset(0f, top), Size(size.width * played, trackHeight))
        // Núm giữ nguyên trong màn: ở hai đầu thì tâm núm lùi vào bằng bán kính để không bị cắt nửa; phần đã xem vẫn vẽ đúng vị trí thật.
        val radius = ODVSize.seekThumb.toPx() / 2f * expanded
        if (radius > 0.5f) {
            val thumbX = (size.width * played).coerceIn(radius, (size.width - radius).coerceAtLeast(radius))
            drawCircle(ODVMediaColors.accent, radius = radius, center = Offset(thumbX, top + trackHeight / 2f))
        }
    }
}

private fun Float.unit(): Float = if (isNaN()) 0f else coerceIn(0f, 1f)
