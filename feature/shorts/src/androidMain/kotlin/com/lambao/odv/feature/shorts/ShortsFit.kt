package com.lambao.odv.feature.shorts

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.lambao.odv.core.domain.model.VideoMeta
import kotlin.math.roundToInt

/** Cách đặt video vào vùng hiển thị của tab Short (SV-08); khác `VideoFit` của màn Xem video và không theo cài đặt "Khung hình mặc định". */
internal enum class ShortFit { Cover, Fit }

/**
 * SV-08: chỉ video **dọc** mới có thể Cắt đầy, và chỉ khi phần bị cắt không quá [ShortsConstants.MAX_CROP_FRACTION] mỗi chiều (video dọc
 * có tỉ lệ gần tỉ lệ vùng hiển thị). Video dọc lệch tỉ lệ nhiều (vd. 3:4), video ngang và vuông thì Vừa khung, nằm giữa theo chiều dọc.
 * [videoAspect] là rộng/cao của hình sau khi giải mã (đã tính hướng xoay); 0 nghĩa là chưa biết nên giữ Vừa khung cho khỏi nháy.
 */
internal fun decideFit(videoAspect: Float, containerAspect: Float): ShortFit {
    if (videoAspect <= 0f || containerAspect <= 0f || videoAspect >= 1f) return ShortFit.Fit
    // Phóng cho phủ kín: thiếu bề ngang thì cắt chiều cao và ngược lại; phần bị cắt tính theo tỉ lệ của chiều bị cắt.
    val cropped = if (videoAspect < containerAspect) 1f - videoAspect / containerAspect else 1f - containerAspect / videoAspect
    return if (cropped <= ShortsConstants.MAX_CROP_FRACTION) ShortFit.Cover else ShortFit.Fit
}

/**
 * Tỉ lệ phỏng đoán từ dữ liệu OneDrive, chỉ để chọn khung cho thumbnail trước khi player báo kích thước thật (ADR-0024 mục 4): `width/height`
 * của Graph có thể không phản ánh cờ xoay trong tệp nên không dùng để quyết định khi đã có hình thật.
 */
internal fun VideoMeta?.guessAspect(): Float {
    val width = this?.width ?: return 0f
    val height = this.height ?: return 0f
    return if (width > 0 && height > 0) width.toFloat() / height else 0f
}

/**
 * Cắt đầy: giữ tỉ lệ [aspect] của video và phóng cho phủ kín khung, phần thừa lố ra ngoài (khung cắt bằng `clipToBounds`). Cùng cách làm với
 * màn Xem video (VD-06); giữ bản riêng vì hàm đó là `private` của `:feature:player`.
 */
internal fun Modifier.coverAspect(aspect: Float): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    if (width <= 0 || height <= 0 || aspect <= 0f) {
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
