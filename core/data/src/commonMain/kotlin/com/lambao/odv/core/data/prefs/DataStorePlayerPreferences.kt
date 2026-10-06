package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.data.PreferenceKeys
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.settings.PlayerPreferences
import kotlinx.coroutines.flow.Flow

/**
 * [PlayerPreferences] trên DataStore Preferences, cùng cách làm với [DataStoreBrowserPreferences]: giá trị lạ hoặc đọc lỗi thì
 * dùng mặc định ([PlayMode.AutoNext], [VideoFit.Fit]).
 */
internal class DataStorePlayerPreferences(
    private val prefs: PreferencesDataSource,
) : PlayerPreferences {

    override val playMode: Flow<PlayMode> = prefs.observeEnum(PreferenceKeys.PLAY_MODE, PlayMode.AutoNext, PlayMode.entries)

    override val videoFit: Flow<VideoFit> = prefs.observeEnum(PreferenceKeys.VIDEO_FIT, VideoFit.Fit, VideoFit.entries)

    override suspend fun setPlayMode(mode: PlayMode) {
        prefs.edit { it[PreferenceKeys.PLAY_MODE] = mode.name }
    }

    override suspend fun setVideoFit(fit: VideoFit) {
        prefs.edit { it[PreferenceKeys.VIDEO_FIT] = fit.name }
    }
}
