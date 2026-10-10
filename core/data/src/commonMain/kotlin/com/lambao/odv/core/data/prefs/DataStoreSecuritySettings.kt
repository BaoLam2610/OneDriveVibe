package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.data.PreferenceKeys
import com.lambao.odv.core.domain.model.AutoLockDelay
import com.lambao.odv.core.domain.settings.SecuritySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * [SecuritySettings] trên cùng DataStore `settings` với [DataStoreSettingsPreferences]. Giá trị lạ hoặc đọc lỗi thì dùng mặc định
 * (xóa dữ liệu tắt, tự khóa sau 1 phút, bảo vệ màn hình tắt). Không chứa bí mật: chỉ là công tắc và lựa chọn.
 */
internal class DataStoreSecuritySettings(
    private val prefs: PreferencesDataSource,
) : SecuritySettings {

    override val wipeOnTooManyFailures: Flow<Boolean> = prefs.observe { it[PreferenceKeys.WIPE_ON_TOO_MANY_FAILURES] ?: false }

    override suspend fun isWipeOnTooManyFailuresEnabled(): Boolean = wipeOnTooManyFailures.first()

    override suspend fun setWipeOnTooManyFailures(enabled: Boolean) {
        prefs.edit { it[PreferenceKeys.WIPE_ON_TOO_MANY_FAILURES] = enabled }
    }

    override val autoLockDelay: Flow<AutoLockDelay> =
        prefs.observeEnum(PreferenceKeys.AUTO_LOCK_DELAY, AutoLockDelay.OneMinute, AutoLockDelay.entries)

    override suspend fun setAutoLockDelay(delay: AutoLockDelay) {
        prefs.edit { it[PreferenceKeys.AUTO_LOCK_DELAY] = delay.name }
    }

    override val screenProtection: Flow<Boolean> = prefs.observe { it[PreferenceKeys.SCREEN_PROTECTION] ?: false }

    override suspend fun setScreenProtection(enabled: Boolean) {
        prefs.edit { it[PreferenceKeys.SCREEN_PROTECTION] = enabled }
    }
}
