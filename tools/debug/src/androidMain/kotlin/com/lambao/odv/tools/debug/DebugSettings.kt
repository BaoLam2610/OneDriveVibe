package com.lambao.odv.tools.debug

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.lambao.odv.core.designsystem.component.ODVSecureWindowPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cài đặt của công cụ debug, lưu trong SharedPreferences `odv_debug` (hiện ở tab Lưu trữ) để sống qua lần mở app sau.
 *
 * - [maskTraffic]: che Authorization, token, client_secret, downloadUrl khi hiển thị log API. Mặc định TẮT (hiện đầy đủ,
 *   ADR-0013).
 * - [thumbnailScalePercent]: tỉ lệ chất lượng thumbnail so với cỡ mặc định (Lát 4); chỉ có ở bản debug.
 */
object DebugSettings {
    private const val PREFS = "odv_debug"
    // Khóa cũ của ghi đè FLAG_SECURE riêng của Debug (đã bỏ, ADR-0020); xóa đi khi cài đặt để SharedPreferences không giữ rác.
    private const val LEGACY_KEY_SECURE = "secure_mode"
    private const val KEY_MASK = "mask_traffic"
    private const val KEY_THUMBNAIL_SCALE = "thumbnail_scale_percent"

    /** Các tỉ lệ chất lượng thumbnail cho chọn (phần trăm so với cỡ mặc định); 100 là mặc định của app. */
    val thumbnailScales: List<Int> = listOf(50, 75, 100, 125, 150)

    private var prefs: SharedPreferences? = null


    private val _maskTraffic = MutableStateFlow(false)
    val maskTraffic: StateFlow<Boolean> = _maskTraffic.asStateFlow()

    private val _thumbnailScale = MutableStateFlow(100)

    /** Tỉ lệ chất lượng thumbnail (50 → 150, mặc định 100). `DebugTools` ở app cấp giá trị này cho `ThumbnailQuality`. */
    val thumbnailScalePercent: StateFlow<Int> = _thumbnailScale.asStateFlow()

    /** Gọi một lần ở Application.onCreate: nạp cài đặt và đăng ký Window của mọi Activity mới cho FLAG_SECURE (theo cài đặt "Bảo vệ màn hình" của người dùng). */
    fun install(application: Application) {
        val stored = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = stored
        if (stored.contains(LEGACY_KEY_SECURE)) stored.edit { remove(LEGACY_KEY_SECURE) }
        _maskTraffic.value = stored.getBoolean(KEY_MASK, false)
        _thumbnailScale.value = stored.getInt(KEY_THUMBNAIL_SCALE, 100).takeIf { it in thumbnailScales } ?: 100
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            // Đăng ký Window của mọi Activity (DebugActivity, gallery) để "Bảo vệ màn hình" toàn app áp dụng cho cả những Activity
            // không có màn nào gọi ODVSecureWindow.
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ODVSecureWindowPolicy.track(activity.window)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun setThumbnailScale(percent: Int) {
        if (percent !in thumbnailScales) return
        _thumbnailScale.value = percent
        prefs?.edit {
            putInt(KEY_THUMBNAIL_SCALE, percent)
        }
    }

    fun setMaskTraffic(mask: Boolean) {
        _maskTraffic.value = mask
        prefs?.edit {
            putBoolean(KEY_MASK, mask)
        }
    }
}
