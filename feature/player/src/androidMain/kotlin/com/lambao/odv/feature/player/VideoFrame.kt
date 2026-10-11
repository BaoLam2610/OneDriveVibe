@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import com.lambao.odv.core.designsystem.component.ODVViewerMiniProgress
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.thumbnailSource
import com.lambao.odv.core.media.PlayerFailure
import kotlin.math.roundToInt

/** Gợn tua đang hiện: [seconds] là tổng đã cộng dồn, [tick] đổi mỗi lần chạm để hẹn giờ tắt tính lại. */
internal data class SeekFeedback(val rightSide: Boolean, val seconds: Int, val tick: Int)

/**
 * Khung video: bề mặt phát (TextureView để zoom bằng graphicsLayer, VD-18), thumbnail phủ lên tới khi có khung hình đầu tiên
 * (hết màn đen lúc mở), gợn chạm đúp và thanh tiến độ mảnh khi điều khiển ẩn. [fit] quyết định cách đặt video vào khung (VD-06).
 * Zoom chỉ áp lên video (và thumbnail), không áp lên gợn chạm đúp hay thanh tiến độ.
 */
@Composable
internal fun VideoFrame(
    controller: VideoPlayerController,
    item: DriveItem,
    fit: VideoFit,
    zoom: ZoomState,
    seekFeedback: SeekFeedback?,
    centerFeedback: CenterFeedback?,
    onCenterFeedbackFinished: () -> Unit,
    showMiniProgress: Boolean,
    portraitFit: Boolean,
) {
    val failure = controller.failure.takeIf { controller.failedItemId == item.id }
    // Hướng dọc + Vừa khung: khung 16:9 căn giữa (thiet-ke-ui.md mục 4.5). Cắt đầy / Kéo giãn / hướng ngang: lấp cả màn.
    // Khung chỉ là bố cục lúc 1x; vùng cắt và lớp zoom là cả màn hình nên zoom tràn ra ngoài khung 16:9 (VD-18).
    val frameModifier = if (portraitFit) Modifier.fillMaxWidth().aspectRatio(PlayerConstants.FRAME_ASPECT) else Modifier.fillMaxSize()
    Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { zoom.updateViewport(it) }, contentAlignment = Alignment.Center) {
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
            Box(frameModifier, contentAlignment = Alignment.Center) {
                val rendered = controller.hasRenderedFirstFrame(item.id)
                PlayerSurface(
                    player = controller.player,
                    modifier = when {
                        aspect <= 0f || fit == VideoFit.Stretch -> Modifier.fillMaxSize()
                        fit == VideoFit.Crop -> Modifier.coverAspect(aspect)
                        else -> Modifier.aspectRatio(aspect)
                    }
                        // Hình chữ nhật video thật, để chặn kéo lố vào vùng viền đen khi zoom.
                        .onSizeChanged { zoom.updateContent(it) }
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
        }
        // Lớp phủ nằm theo khung bố cục (16:9 ở hướng dọc + Vừa khung), không đi qua lớp zoom: vùng ripple (nửa trái/phải) kèm sóng lan,
        // biểu tượng Phát/Tạm dừng giữa khung và thanh tiến độ mảnh.
        Box(frameModifier) {
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
