package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.data.PreferenceKeys
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PdfReadingStyle
import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.VideoSettingOptions
import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow

/**
 * [SettingsPreferences] trên DataStore Preferences, cùng cách làm với [DataStorePlayerPreferences]: giá trị lạ hoặc đọc lỗi
 * thì dùng mặc định (giao diện theo hệ thống, cả ba loại tệp, bước tua 10 giây, 1x, nhớ khung hình, hướng dọc, nhớ vị trí bật,
 * PDF cuộn dọc). Loại tệp đọc ra rỗng (tệp hỏng) cũng về cả ba để không bao giờ có màn hình trống vì lỗi lưu.
 */
internal class DataStoreSettingsPreferences(
    private val prefs: PreferencesDataSource,
) : SettingsPreferences {

    override val themeMode: Flow<ThemeMode> = prefs.observeEnum(PreferenceKeys.THEME_MODE, ThemeMode.System, ThemeMode.entries)

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit { it[PreferenceKeys.THEME_MODE] = mode.name }
    }

    override val enabledKinds: Flow<Set<MediaKind>> = prefs.observe { p ->
        val stored = p[PreferenceKeys.ENABLED_KINDS]
            ?.mapNotNull { name -> MediaKind.entries.firstOrNull { it.name == name } }
            ?.toSet()
        if (stored.isNullOrEmpty()) MediaKind.entries.toSet() else stored
    }

    override suspend fun setEnabledKinds(kinds: Set<MediaKind>) {
        // Không bao giờ lưu tập rỗng (CD-01); gọi sai thì bỏ qua.
        if (kinds.isEmpty()) return
        prefs.edit { it[PreferenceKeys.ENABLED_KINDS] = kinds.map { kind -> kind.name }.toSet() }
    }

    override val seekStepSeconds: Flow<Int> = prefs.observe { p ->
        p[PreferenceKeys.SEEK_STEP_SECONDS]?.takeIf { it in VideoSettingOptions.SEEK_STEPS } ?: VideoSettingOptions.DEFAULT_SEEK_STEP
    }

    override suspend fun setSeekStepSeconds(seconds: Int) {
        if (seconds !in VideoSettingOptions.SEEK_STEPS) return
        prefs.edit { it[PreferenceKeys.SEEK_STEP_SECONDS] = seconds }
    }

    override val shortMaxMinutes: Flow<Int> = prefs.observe { p ->
        p[PreferenceKeys.SHORT_MAX_MINUTES]
            ?.takeIf { it in VideoSettingOptions.SHORT_MIN_MINUTES..VideoSettingOptions.SHORT_MAX_MINUTES }
            ?: VideoSettingOptions.DEFAULT_SHORT_MINUTES
    }

    override suspend fun setShortMaxMinutes(minutes: Int) {
        if (minutes !in VideoSettingOptions.SHORT_MIN_MINUTES..VideoSettingOptions.SHORT_MAX_MINUTES) return
        prefs.edit { it[PreferenceKeys.SHORT_MAX_MINUTES] = minutes }
    }

    override val defaultSpeed: Flow<Float> = prefs.observe { p ->
        p[PreferenceKeys.DEFAULT_SPEED]?.takeIf { it in VideoSettingOptions.SPEEDS } ?: VideoSettingOptions.DEFAULT_SPEED
    }

    override suspend fun setDefaultSpeed(speed: Float) {
        if (speed !in VideoSettingOptions.SPEEDS) return
        prefs.edit { it[PreferenceKeys.DEFAULT_SPEED] = speed }
    }

    override val defaultVideoFit: Flow<VideoFit?> = prefs.observe { p ->
        VideoFit.entries.firstOrNull { it.name == p[PreferenceKeys.DEFAULT_VIDEO_FIT] }
    }

    override suspend fun setDefaultVideoFit(fit: VideoFit?) {
        prefs.edit {
            if (fit == null) it.remove(PreferenceKeys.DEFAULT_VIDEO_FIT) else it[PreferenceKeys.DEFAULT_VIDEO_FIT] = fit.name
        }
    }

    override val openVideoLandscape: Flow<Boolean> = prefs.observe { it[PreferenceKeys.OPEN_VIDEO_LANDSCAPE] ?: false }

    override suspend fun setOpenVideoLandscape(landscape: Boolean) {
        prefs.edit { it[PreferenceKeys.OPEN_VIDEO_LANDSCAPE] = landscape }
    }

    override val rememberVideoPosition: Flow<Boolean> = prefs.observe { it[PreferenceKeys.REMEMBER_VIDEO_POSITION] ?: true }

    override suspend fun setRememberVideoPosition(enabled: Boolean) {
        prefs.edit { it[PreferenceKeys.REMEMBER_VIDEO_POSITION] = enabled }
    }

    override val pdfReadingStyle: Flow<PdfReadingStyle> =
        prefs.observeEnum(PreferenceKeys.PDF_READING_STYLE, PdfReadingStyle.Vertical, PdfReadingStyle.entries)

    override suspend fun setPdfReadingStyle(style: PdfReadingStyle) {
        prefs.edit { it[PreferenceKeys.PDF_READING_STYLE] = style.name }
    }

    override val secretExpiryEpochDay: Flow<Long?> = prefs.observe { it[PreferenceKeys.SECRET_EXPIRY_EPOCH_DAY] }

    override suspend fun setSecretExpiryEpochDay(epochDay: Long?) {
        prefs.edit {
            if (epochDay == null) it.remove(PreferenceKeys.SECRET_EXPIRY_EPOCH_DAY) else it[PreferenceKeys.SECRET_EXPIRY_EPOCH_DAY] = epochDay
        }
    }
}
