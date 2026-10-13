package com.lambao.odv.core.designsystem.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith

/**
 * Chuyển cảnh dùng chung (thiet-ke-ui.md mục 2.8): chỉ fade, không trượt, không scale (quy tắc "không nảy, không đàn hồi"; đổi tab
 * "cross-fade, không trượt ngang"). Mọi giá trị lấy từ [ODVDuration] và [ODVEasing]; khi hệ thống bật Giảm hiệu ứng thì rút còn
 * [ODVDuration.fast] (cross-fade ≤ 150ms).
 *
 * Chuyển màn kiểu "fade chồng": màn mới mờ dần hiện trên màn cũ còn nguyên độ đặc ([screenPush]); khi Back thì màn trên cùng mờ dần để lộ
 * màn dưới ([screenPop]). Không mờ cả hai cùng lúc vì như vậy nền cửa sổ lộ ra giữa chừng (chớp sáng khi vào màn xem nền đen).
 */
object ODVTransitions {
    private fun duration(reduceMotion: Boolean, normal: Int): Int = if (reduceMotion) ODVDuration.fast else normal

    /** Vào màn mới (thêm vào back stack, hoặc thay cả back stack như Splash sang Màn chính): màn mới fade in `base`, màn cũ đứng yên. */
    fun screenPush(reduceMotion: Boolean): ContentTransform =
        fadeIn(tween(duration(reduceMotion, ODVDuration.base), easing = ODVEasing.easeOut)) togetherWith ExitTransition.None

    /** Back (cả vuốt Back dự đoán): màn trên cùng fade out `base` ease-in, lộ màn dưới đang đứng yên. */
    fun screenPop(reduceMotion: Boolean): ContentTransform =
        EnterTransition.None togetherWith fadeOut(tween(duration(reduceMotion, ODVDuration.base), easing = ODVEasing.easeIn))

    /** Không hoạt ảnh. Dùng cho màn Khóa xuất hiện: không để lộ nội dung bên dưới dù chỉ vài khung hình (CH-03). */
    fun none(): ContentTransform = EnterTransition.None togetherWith ExitTransition.None

    /** Đổi nội dung cùng vị trí (tab, tải ↔ trống ↔ danh sách): cross-fade `fast`. */
    fun crossFade(): ContentTransform =
        fadeIn(tween(ODVDuration.fast, easing = ODVEasing.easeOut)) togetherWith fadeOut(tween(ODVDuration.fast, easing = ODVEasing.easeIn))

    /**
     * Banner xuất hiện/biến mất/đổi loại: trượt từ trên xuống kèm fade `slow` (mục 2.8 "Banner trượt vào"), biến mất fade `fast`. Chiều cao khung
     * chứa do [androidx.compose.animation.AnimatedContent] tự co giãn. Giảm hiệu ứng thì chỉ cross-fade `fast`.
     */
    fun banner(reduceMotion: Boolean): ContentTransform =
        if (reduceMotion) {
            crossFade()
        } else {
            (
                fadeIn(tween(ODVDuration.slow, easing = ODVEasing.easeOut)) +
                    slideInVertically(tween(ODVDuration.slow, easing = ODVEasing.easeOut)) { -it }
                ) togetherWith fadeOut(tween(ODVDuration.fast, easing = ODVEasing.easeIn))
        }
}
