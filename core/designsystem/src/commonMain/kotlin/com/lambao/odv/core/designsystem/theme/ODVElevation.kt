package com.lambao.odv.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Độ nổi (mục 2.7). Android không có bóng CSS nên dùng elevation xấp xỉ; so lại với ảnh tham chiếu khi nghiệm thu.
 * Ở theme tối gần như không thấy bóng, đúng thiết kế. Chỉ ba chỗ có bóng: ContinueCard (sm), Dialog/Snackbar/bong bóng cuộn nhanh (lg).
 */
object ODVElevation {
    /** `shadow.sm`: ContinueCard. */
    val sm = 2.dp

    /** `shadow.lg`: Dialog, Snackbar, bong bóng cuộn nhanh. */
    val lg = 12.dp
}
