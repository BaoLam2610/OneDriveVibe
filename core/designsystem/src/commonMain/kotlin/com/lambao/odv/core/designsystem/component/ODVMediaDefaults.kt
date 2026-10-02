package com.lambao.odv.core.designsystem.component

import androidx.compose.ui.graphics.Color
import com.lambao.odv.core.designsystem.theme.ODVMediaColors

/**
 * Bộ màu "trên màn xem" (mục 2.2, 4.1) cho các component dùng chung. Bản thân component không biết khái niệm màn xem;
 * màn xem tự truyền các bộ màu này vào tham số `colors` của component.
 */
object ODVMediaDefaults {
    /** IconButton trên media: icon trắng, nền trong suốt. */
    fun iconButtonColors() = ODVIconButtonColors(Color.Transparent, ODVMediaColors.onMedia)

    /** IconButton trên media có nền mờ (ví dụ nút Khóa thao tác). */
    fun scrimIconButtonColors() = ODVIconButtonColors(ODVMediaColors.scrim, ODVMediaColors.onMedia)

    /** ProgressBar trên media: rãnh `track`, phần đã có `accent`. */
    fun progressColors() = ODVProgressColors(ODVMediaColors.track, ODVMediaColors.accent)
}
