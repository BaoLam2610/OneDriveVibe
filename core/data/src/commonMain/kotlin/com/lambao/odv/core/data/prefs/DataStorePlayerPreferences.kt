package com.lambao.odv.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.repository.PlayerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.enums.enumEntries

private val PLAY_MODE = stringPreferencesKey("play_mode")
private val VIDEO_FIT = stringPreferencesKey("video_fit")

/**
 * [PlayerPreferences] trên DataStore Preferences, cùng cách làm với [DataStoreBrowserPreferences]: giá trị lạ hoặc đọc lỗi thì
 * dùng mặc định ([PlayMode.AutoNext], [VideoFit.Fit]); ghi lỗi bị bỏ qua (mất lựa chọn lần này là đủ).
 */
internal class DataStorePlayerPreferences(
    private val store: DataStore<Preferences>,
) : PlayerPreferences {

    private val data: Flow<Preferences> = store.data.catch { emit(emptyPreferences()) }

    override val playMode: Flow<PlayMode> = data
        .map { prefs -> prefs[PLAY_MODE].toEnum(PlayMode.AutoNext) }
        .distinctUntilChanged()

    override val videoFit: Flow<VideoFit> = data
        .map { prefs -> prefs[VIDEO_FIT].toEnum(VideoFit.Fit) }
        .distinctUntilChanged()

    override suspend fun setPlayMode(mode: PlayMode) {
        write { it[PLAY_MODE] = mode.name }
    }

    override suspend fun setVideoFit(fit: VideoFit) {
        write { it[VIDEO_FIT] = fit.name }
    }

    private suspend fun write(block: (MutablePreferences) -> Unit) {
        try {
            store.edit(block)
        } catch (e: IOException) {
            // Chỉ là tùy chọn hiển thị: không ghi được thì giữ lựa chọn trong phiên, không báo lỗi.
        }
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumEntries<E>().firstOrNull { it.name == this } ?: default
