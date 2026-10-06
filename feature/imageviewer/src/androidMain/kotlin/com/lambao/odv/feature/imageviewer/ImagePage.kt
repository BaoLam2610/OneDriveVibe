package com.lambao.odv.feature.imageviewer

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.designsystem.component.ODVSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ThumbnailSize
import com.lambao.odv.core.domain.model.thumbnailSource
import com.lambao.odv.core.domain.model.OriginalImageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import java.io.File
import kotlin.math.roundToInt

/** Kết quả đọc thử đầu tệp ảnh gốc: có giải mã được kích thước hay không (AN-07) và có phải GIF (AN-06). */
private sealed interface ImageProbe {
    data object Invalid : ImageProbe
    data class Valid(val isGif: Boolean) : ImageProbe
}

/**
 * Một trang của màn xem ảnh (AN-01 → AN-07):
 * 1. Thumbnail lớn (không cắt) hiện ngay làm nền.
 * 2. Ảnh gốc tải về (xem `OriginalImageRepository`); khi sẵn sàng thì hiện đè lên thumbnail, thumbnail mờ đi trong
 *    `duration.fast` (AN-01). Trong lúc tải có viên thuốc "Đang tải ảnh gốc" nhỏ dưới ảnh, không che ảnh.
 * 3. Tệp không đọc được kích thước thì là lỗi định dạng: biểu tượng lỗi và tên tệp, không nút (AN-07).
 *
 * Chỉ trang đang hiện được dựng nên chỉ ảnh này tải; rời trang thì dừng tải nhưng giữ phần đã có (BN-03).
 *
 * @param onZoomedChange báo khi ảnh đang được zoom để màn chứa khóa vuốt ngang (AN-03: khi zoom, vuốt chỉ di chuyển ảnh).
 */
@Composable
internal fun ImagePage(
    item: DriveItem,
    originalOf: (DriveItem) -> Flow<OriginalImageState>,
    onTap: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val original by remember(item.id, item.cTag) { originalOf(item) }
        .collectAsStateWithLifecycle(initialValue = OriginalImageState.Downloading(0L, null))
    val path = (original as? OriginalImageState.Ready)?.path
    val probe by produceState<ImageProbe?>(initialValue = null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) { probeImage(path) }
    }
    var originalShown by remember(item.id, item.cTag) { mutableStateOf(false) }
    var zoomFraction by remember(item.id, item.cTag) { mutableFloatStateOf(0f) }
    val zoomed = zoomFraction > ImageViewerConstants.ZOOMED_THRESHOLD
    LaunchedEffect(zoomed) { onZoomedChange(zoomed) }

    val currentProbe = probe
    // Chạm một lần (AN-04): lớp zoom tự bắt chạm khi đã hiện; trước đó (đang tải) và ở trang lỗi thì trang tự bắt.
    val pageTap = if (currentProbe is ImageProbe.Valid) Modifier else Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
    Box(modifier.fillMaxSize().then(pageTap)) {
        if (currentProbe != ImageProbe.Invalid) {
            val thumbnailAlpha by animateFloatAsState(if (originalShown) 0f else 1f, tween(ODVDuration.fast), label = "thumbnail")
            AsyncImage(
                model = item.thumbnailSource(ThumbnailSize.Viewer),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(thumbnailAlpha),
                contentScale = ContentScale.Fit,
            )
        }

        when (currentProbe) {
            is ImageProbe.Valid -> {
                val request = rememberOriginalRequest(path.orEmpty())
                if (currentProbe.isGif) {
                    GifLayer(request, item.name, onTap, { originalShown = true }, { zoomFraction = it })
                } else {
                    ZoomableLayer(request, item.name, onTap, { originalShown = true }, { zoomFraction = it })
                }
            }
            ImageProbe.Invalid -> ODVViewerError(
                title = stringResource(R.string.image_viewer_error_title),
                body = stringResource(R.string.image_viewer_error_body),
                fileLine = item.name,
                modifier = Modifier.align(Alignment.Center),
            )
            null -> Unit
        }

        if (zoomed) {
            val percent = ((1f + zoomFraction * (ImageViewerConstants.MAX_ZOOM - 1f)) * 100f).roundToInt()
            ODVViewerPill(
                text = stringResource(R.string.image_viewer_zoom_percent, percent),
                mono = true,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
            )
            Text(
                stringResource(R.string.image_viewer_zoom_hint),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp),
                style = ODVTheme.typography.caption,
                color = ODVMediaColors.onMediaMuted,
            )
        } else {
            StatusPill(original, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp))
        }
    }
}

@Composable
private fun StatusPill(state: OriginalImageState, modifier: Modifier) {
    when (state) {
        is OriginalImageState.Downloading -> LoadingPill(stringResource(R.string.image_viewer_loading_original), modifier)
        is OriginalImageState.Failed -> {
            val offline = state.error is AppError.Network || state.error is AppError.Timeout
            ODVViewerPill(
                text = stringResource(if (offline) R.string.image_viewer_offline else R.string.image_viewer_load_failed),
                icon = if (offline) ODVIcon.CloudOff else ODVIcon.Alert,
                live = true,
                modifier = modifier,
            )
        }
        is OriginalImageState.Ready -> Unit
    }
}

/** Viên thuốc nhỏ có spinner 14 và chữ (thiet-ke-ui.md mục 4.5), cùng kiểu với `ODVViewerPill`. */
@Composable
private fun LoadingPill(text: String, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(ODVMediaColors.pill, ODVTheme.shapes.full)
            .border(1.dp, ODVMediaColors.onMedia.copy(alpha = 0.13f), ODVTheme.shapes.full)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ODVSpinner(size = 14.dp, color = ODVMediaColors.accent, trackColor = ODVMediaColors.track)
        Text(text, style = ODVTheme.typography.meta, color = ODVMediaColors.onMedia, maxLines = 1)
    }
}

/**
 * Ảnh thường: Telephoto lo pinch, chạm đúp (zoom tới điểm chạm rồi về vừa khung), kéo khi đang zoom (AN-02) và cắt vùng
 * cho ảnh độ phân giải rất lớn để không tràn bộ nhớ (AN-06).
 */
@Composable
private fun ZoomableLayer(
    request: ImageRequest,
    name: String,
    onTap: () -> Unit,
    onShown: () -> Unit,
    onZoomFraction: (Float) -> Unit,
) {
    val state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = ImageViewerConstants.MAX_ZOOM)))
    ZoomableAsyncImage(
        model = request,
        contentDescription = name,
        state = state,
        modifier = Modifier.fillMaxSize(),
        onClick = { onTap() },
    )
    val shown = state.isImageDisplayed
    LaunchedEffect(shown) { if (shown) onShown() }
    val fraction = state.zoomableState.zoomFraction ?: 0f
    LaunchedEffect(fraction) { onZoomFraction(fraction) }
}

/**
 * GIF động (AN-06): Coil phát hoạt ảnh nhờ decoder đăng ký trong `ImageLoader`. Vẫn zoom/pan được nhưng không cắt vùng
 * (GIF hiếm khi lớn đến mức cần).
 */
@Composable
private fun GifLayer(
    request: ImageRequest,
    name: String,
    onTap: () -> Unit,
    onShown: () -> Unit,
    onZoomFraction: (Float) -> Unit,
) {
    val zoomable = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = ImageViewerConstants.MAX_ZOOM))
    AsyncImage(
        model = request,
        contentDescription = name,
        contentScale = ContentScale.Fit,
        onSuccess = { success ->
            val image = success.result.image
            zoomable.setContentLocation(
                ZoomableContentLocation.scaledInsideAndCenterAligned(Size(image.width.toFloat(), image.height.toFloat())),
            )
            onShown()
        },
        modifier = Modifier.fillMaxSize().zoomable(zoomable, onClick = { onTap() }),
    )
    val fraction = zoomable.zoomFraction ?: 0f
    LaunchedEffect(fraction) { onZoomFraction(fraction) }
}

/**
 * Yêu cầu tải tệp ảnh gốc cục bộ. Tắt cache bộ nhớ của Coil: ảnh gốc to và đã nằm trên đĩa (cache riêng), còn cache bộ nhớ
 * của app cỡ cho thumbnail nên một ảnh gốc giải mã sẽ đẩy cả lưới thumbnail ra.
 */
@Composable
private fun rememberOriginalRequest(path: String): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(path) {
        ImageRequest.Builder(context).data(File(path)).memoryCachePolicy(CachePolicy.DISABLED).build()
    }
}

/** Đọc kích thước không giải mã điểm ảnh: rẻ, và cho biết tệp có phải ảnh app mở được không (AN-07). */
private fun probeImage(path: String): ImageProbe {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    return if (options.outWidth > 0 && options.outHeight > 0) {
        ImageProbe.Valid(isGif = options.outMimeType == "image/gif")
    } else {
        ImageProbe.Invalid
    }
}
