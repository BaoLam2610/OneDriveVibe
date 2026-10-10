@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.ThumbnailSource
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

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
    // Hướng khi mở video theo Cài đặt (mặc định dọc): áp dụng đúng một lần khi đọc xong, không ép lại khi xoay tay hay đổi video.
    var orientationApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.openInLandscape) {
        val open = state.openInLandscape
        if (!orientationApplied && open != null) {
            orientationApplied = true
            requestedLandscape = open
        }
    }
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
