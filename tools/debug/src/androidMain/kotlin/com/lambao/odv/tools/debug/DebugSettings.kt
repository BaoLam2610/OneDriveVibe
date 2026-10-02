package com.lambao.odv.tools.debug

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.lambao.odv.core.designsystem.component.ODVSecureMode
import com.lambao.odv.core.designsystem.component.ODVSecureWindowPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cài đặt của công cụ debug, lưu trong SharedPreferences `odv_debug` (hiện ở tab Lưu trữ) để sống qua lần mở app sau.
 *
 * - [secureMode]: FLAG_SECURE toàn app (ByDesign / AlwaysOn / AlwaysOff). Áp dụng ngay cho mọi cửa sổ.
 * - [maskTraffic]: che Authorization, token, client_secret, downloadUrl khi hiển thị log API. Mặc định TẮT (hiện đầy đủ,
 *   ADR-0013).
 */
object DebugSettings {
    private const val PREFS = "odv_debug"
    private const val KEY_SECURE = "secure_mode"
    private const val KEY_MASK = "mask_traffic"

    private var prefs: SharedPreferences? = null

    private val _secureMode = MutableStateFlow(ODVSecureMode.ByDesign)
    val secureMode: StateFlow<ODVSecureMode> = _secureMode.asStateFlow()

    private val _maskTraffic = MutableStateFlow(false)
    val maskTraffic: StateFlow<Boolean> = _maskTraffic.asStateFlow()

    /** Gọi một lần ở Application.onCreate: nạp cài đặt, áp dụng FLAG_SECURE và theo dõi mọi Activity mới. */
    fun install(application: Application) {
        val stored = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = stored
        _secureMode.value = stored.getString(KEY_SECURE, null)
            ?.let { name -> ODVSecureMode.entries.firstOrNull { it.name == name } }
            ?: ODVSecureMode.ByDesign
        _maskTraffic.value = stored.getBoolean(KEY_MASK, false)
        ODVSecureWindowPolicy.setMode(_secureMode.value)
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            // Đăng ký Window của mọi Activity (MainActivity, DebugActivity, gallery) để chế độ toàn app áp dụng cho cả
            // những Activity không có màn nào gọi ODVSecureWindow.
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

    fun setSecureMode(mode: ODVSecureMode) {
        _secureMode.value = mode
        prefs?.edit {
            putString(KEY_SECURE, mode.name)
        }
        ODVSecureWindowPolicy.setMode(mode)
    }

    fun setMaskTraffic(mask: Boolean) {
        _maskTraffic.value = mask
        prefs?.edit {
            putBoolean(KEY_MASK, mask)
        }
    }
}
