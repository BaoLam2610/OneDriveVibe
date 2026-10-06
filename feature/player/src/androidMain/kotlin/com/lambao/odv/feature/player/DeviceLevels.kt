package com.lambao.odv.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/**
 * Độ sáng của cửa sổ và âm lượng nhạc của máy cho cử chỉ vuốt dọc (VD-04). Mức là số thực 0..1.
 *
 * Độ sáng chỉ đổi cho cửa sổ của app (`screenBrightness`), không đụng cài đặt hệ thống, và được trả về mặc định khi rời màn
 * ([restoreBrightness]). Âm lượng đổi thẳng âm lượng nhạc của máy nên giữ nguyên sau khi rời màn (như mọi trình phát video).
 */
internal class DeviceLevels(
    private val activity: Activity?,
    private val context: Context,
    private val audio: AudioManager,
) {
    /** Độ sáng hiện tại: của cửa sổ nếu app đang ghi đè, không thì độ sáng hệ thống. */
    fun brightness(): Float {
        val override = activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (override >= 0f) return override
        val system = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / PlayerConstants.MAX_SYSTEM_BRIGHTNESS
        } catch (e: Settings.SettingNotFoundException) {
            PlayerConstants.DEFAULT_BRIGHTNESS
        }
        return system.coerceIn(0f, 1f)
    }

    fun setBrightness(level: Float) {
        val window = activity?.window ?: return
        val attributes = window.attributes
        // Không cho về 0 tuyệt đối: một số máy tắt hẳn màn hình, người xem không biết đường vuốt lại.
        attributes.screenBrightness = level.coerceIn(PlayerConstants.MIN_BRIGHTNESS, 1f)
        window.attributes = attributes
    }

    fun restoreBrightness() {
        val window = activity?.window ?: return
        val attributes = window.attributes
        attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attributes
    }

    fun volume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat() else 0f
    }

    fun setVolume(level: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Cờ 0: không hiện thanh âm lượng của hệ thống vì app có HUD riêng.
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level.coerceIn(0f, 1f) * max).roundToInt(), 0)
    }
}

/** [DeviceLevels] gắn với vòng đời của màn: rời màn thì trả độ sáng về mặc định của hệ thống. */
@Composable
internal fun rememberDeviceLevels(): DeviceLevels {
    val context = LocalContext.current
    val levels = remember(context) {
        DeviceLevels(context.findHostActivity(), context, context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
    }
    DisposableEffect(levels) { onDispose { levels.restoreBrightness() } }
    return levels
}

internal tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
