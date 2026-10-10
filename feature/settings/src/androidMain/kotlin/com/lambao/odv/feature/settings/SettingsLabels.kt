package com.lambao.odv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.AppLanguage
import com.lambao.odv.core.domain.model.AutoLockDelay
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PdfReadingStyle
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.model.VideoFit

// Nhãn của các lựa chọn trong Cài đặt. Cố ý viết riêng thay vì dùng chung với màn xem video: `:feature:settings` không phụ thuộc
// `:feature:player` (feature không gọi nhau), và nhãn ở đây kèm mô tả khác với nhãn ngắn trên nút của màn xem.

@Composable
internal fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.System -> R.string.settings_option_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    },
)

/** Tên ngôn ngữ viết bằng chính ngôn ngữ đó (không dịch) để người dùng nhận ra dù đang ở ngôn ngữ nào. */
@Composable
internal fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.System -> R.string.settings_option_system
        AppLanguage.Vietnamese -> R.string.settings_language_vi
        AppLanguage.English -> R.string.settings_language_en
    },
)

@Composable
internal fun AutoLockDelay.label(): String = when (this) {
    AutoLockDelay.Immediately -> stringResource(R.string.settings_auto_lock_immediately)
    AutoLockDelay.TenSeconds -> stringResource(R.string.settings_seconds, 10)
    AutoLockDelay.ThirtySeconds -> stringResource(R.string.settings_seconds, 30)
    AutoLockDelay.OneMinute -> stringResource(R.string.settings_minutes, 1)
    AutoLockDelay.FiveMinutes -> stringResource(R.string.settings_minutes, 5)
    AutoLockDelay.FifteenMinutes -> stringResource(R.string.settings_minutes, 15)
}

@Composable
internal fun MediaKind.label(): String = stringResource(
    when (this) {
        MediaKind.Image -> R.string.settings_kind_photo
        MediaKind.Video -> R.string.settings_kind_video
        MediaKind.Pdf -> R.string.settings_kind_pdf
    },
)

@Composable
internal fun CacheKind.label(): String = stringResource(
    when (this) {
        CacheKind.Thumbnail -> R.string.settings_cache_thumbnail
        CacheKind.Image -> R.string.settings_kind_photo
        CacheKind.Video -> R.string.settings_kind_video
        CacheKind.Pdf -> R.string.settings_kind_pdf
    },
)

/** Màu của loại bộ nhớ đệm, dùng chung cho thanh dung lượng, thanh tỉ lệ và slider (thumbnail dùng `line-strong`). */
@Composable
internal fun CacheKind.color(): Color = with(ODVTheme.colors) {
    when (this@color) {
        CacheKind.Thumbnail -> lineStrong
        CacheKind.Image -> kindPhoto
        CacheKind.Video -> kindVideo
        CacheKind.Pdf -> kindPdf
    }
}

@Composable
internal fun PlayMode.label(): String = stringResource(
    when (this) {
        PlayMode.NoRepeat -> R.string.settings_mode_no_repeat
        PlayMode.AutoNext -> R.string.settings_mode_auto_next
        PlayMode.RepeatOne -> R.string.settings_mode_repeat_one
        PlayMode.RepeatList -> R.string.settings_mode_repeat_list
    },
)

@Composable
internal fun PlayMode.description(): String = stringResource(
    when (this) {
        PlayMode.NoRepeat -> R.string.settings_mode_no_repeat_desc
        PlayMode.AutoNext -> R.string.settings_mode_auto_next_desc
        PlayMode.RepeatOne -> R.string.settings_mode_repeat_one_desc
        PlayMode.RepeatList -> R.string.settings_mode_repeat_list_desc
    },
)

/** [fit] null là "nhớ lần gần nhất" (VD-06). */
@Composable
internal fun videoFitLabel(fit: VideoFit?): String = stringResource(
    when (fit) {
        null -> R.string.settings_fit_remember
        VideoFit.Fit -> R.string.settings_fit_fit
        VideoFit.Crop -> R.string.settings_fit_crop
        VideoFit.Stretch -> R.string.settings_fit_stretch
    },
)

@Composable
internal fun videoFitDescription(fit: VideoFit?): String = stringResource(
    when (fit) {
        null -> R.string.settings_fit_remember_desc
        VideoFit.Fit -> R.string.settings_fit_fit_desc
        VideoFit.Crop -> R.string.settings_fit_crop_desc
        VideoFit.Stretch -> R.string.settings_fit_stretch_desc
    },
)

@Composable
internal fun orientationLabel(landscape: Boolean): String =
    stringResource(if (landscape) R.string.settings_orientation_landscape else R.string.settings_orientation_portrait)

@Composable
internal fun PdfReadingStyle.label(): String = stringResource(
    when (this) {
        PdfReadingStyle.Vertical -> R.string.settings_pdf_vertical
        PdfReadingStyle.Horizontal -> R.string.settings_pdf_horizontal
    },
)

