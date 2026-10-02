// Sinh từ .claude/docs/odv-tokens.json. Token đổi thì sửa file JSON trước, rồi cập nhật file này cho khớp.
package com.lambao.odv.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable

/** Thời lượng, đơn vị mili giây (mục 2.8). */
object ODVDuration {
    const val fast: Int = 150
    const val base: Int = 250
    const val slow: Int = 400
    /** VD-01 */
    const val controlsAutoHide: Int = 3000
    const val snackbar: Int = 3000
    /** VD-13 */
    const val autoplayCountdown: Int = 5000
    /** Giữ để mở khóa thao tác (đề xuất) */
    const val lockHold: Int = 1000
    /** Nhãn Cắt đầy, Lặp một video (VD-06, VD-20) */
    const val toastLabel: Int = 2000
}

/** Easing (mục 2.8). Không nảy, không đàn hồi. */
object ODVEasing {
    /** Xuất hiện */
    val easeOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Biến mất */
    val easeIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

/**
 * Chuyển động theo cài đặt hệ thống.
 *
 * Khi [reduceMotion] bật: bỏ scale, trượt, gợn, mở rộng thumbnail; thay bằng cross-fade ≤ 150ms hoặc hiện ngay.
 * Giữ thanh tiến độ và vòng đếm ngược vì đó là thông tin (mục 2.8).
 */
@Immutable
class ODVMotion(val reduceMotion: Boolean)

/** Độ mờ (mục 2.9). */
object ODVOpacity {
    const val disabled: Float = 0.38f
    /** TM-03 */
    const val unsupported: Float = 0.5f
    const val pressedScale: Float = 0.98f

    /** Ô nhập bị tắt (mục 4.1). Không có trong odv-tokens.json. */
    const val fieldDisabled: Float = 0.6f
}
