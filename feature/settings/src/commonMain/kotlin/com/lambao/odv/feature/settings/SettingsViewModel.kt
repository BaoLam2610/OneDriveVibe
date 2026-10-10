package com.lambao.odv.feature.settings

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.settings.PlayerPreferences
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.settings.SettingsPreferences
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import com.lambao.odv.core.domain.usecase.cache.ClearCacheUseCase
import com.lambao.odv.core.domain.usecase.cache.GetCacheUsageUseCase
import com.lambao.odv.core.domain.usecase.cache.ObserveCacheBudgetUseCase
import com.lambao.odv.core.domain.usecase.cache.SetCacheLimitUseCase
import com.lambao.odv.core.domain.usecase.cache.SetCacheSharesUseCase
import com.lambao.odv.core.domain.usecase.security.DisableBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.EnableBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.GetBiometricStatusUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.settings.GetLanguageUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveConnectionInfoUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryNoticeUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.lambao.odv.core.domain.usecase.settings.SetLanguageUseCase
import com.lambao.odv.core.domain.usecase.settings.SetSecretExpiryUseCase
import com.lambao.odv.core.domain.usecase.settings.SetThemeModeUseCase
import com.lambao.odv.core.domain.usecase.settings.ToggleFileKindUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * ViewModel màn Cài đặt (MVI, ADR-0002). Nói chuyện với UseCase (ADR-0016); các tùy chọn đơn giản (không có luật) đọc/ghi thẳng qua
 * [SettingsPreferences], [SecuritySettings] và [PlayerPreferences], là interface cài đặt mà feature được dùng trực tiếp (ADR-0016,
 * mục Đính chính). Chế độ phát dùng chung [PlayerPreferences] với nút Chế độ phát ở màn xem video (VD-20).
 *
 * Các thao tác cần PIN (tắt bảo vệ, đổi PIN, bật xóa dữ liệu) **không** làm ở đây: ViewModel chỉ phát [SettingsEffect.OpenPin] và
 * màn PIN riêng ([PinFlowViewModel]) giữ PIN trong bộ nhớ ngắn nhất có thể (CH-02).
 */
class SettingsViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    observeConnectionInfo: ObserveConnectionInfoUseCase,
    observeProtection: ObserveProtectionUseCase,
    observeSecretExpiry: ObserveSecretExpiryUseCase,
    observeSecretExpiryNotice: ObserveSecretExpiryNoticeUseCase,
    private val setSecretExpiry: SetSecretExpiryUseCase,
    private val disconnect: DisconnectUseCase,
    secretUpdateEvents: SecretUpdateEvents,
    getLanguage: GetLanguageUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val toggleFileKind: ToggleFileKindUseCase,
    private val getBiometricStatus: GetBiometricStatusUseCase,
    observeCacheBudget: ObserveCacheBudgetUseCase,
    private val getCacheUsage: GetCacheUsageUseCase,
    private val clearCache: ClearCacheUseCase,
    private val setCacheLimit: SetCacheLimitUseCase,
    private val setCacheShares: SetCacheSharesUseCase,
    private val enableBiometric: EnableBiometricUseCase,
    private val disableBiometric: DisableBiometricUseCase,
    private val settings: SettingsPreferences,
    private val security: SecuritySettings,
    private val player: PlayerPreferences,
) : BaseMviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState(language = getLanguage())) {

    init {
        // Cài đặt mới sinh: sự kiện "đã lưu secret" cũ (từ banner ở Danh sách) không còn ý nghĩa.
        secretUpdateEvents.clear()
        viewModelScope.launch { observeThemeMode().collect { mode -> setState { copy(themeMode = mode) } } }
        viewModelScope.launch { observeConnectionInfo().collect { info -> setState { copy(connection = info) } } }
        viewModelScope.launch { settings.enabledKinds.collect { kinds -> setState { copy(enabledKinds = kinds) } } }
        viewModelScope.launch { settings.seekStepSeconds.collect { value -> setState { copy(seekStepSeconds = value) } } }
        viewModelScope.launch { settings.defaultSpeed.collect { value -> setState { copy(defaultSpeed = value) } } }
        viewModelScope.launch { settings.defaultVideoFit.collect { value -> setState { copy(defaultVideoFit = value) } } }
        viewModelScope.launch { settings.openVideoLandscape.collect { value -> setState { copy(openVideoLandscape = value) } } }
        viewModelScope.launch { settings.rememberVideoPosition.collect { value -> setState { copy(rememberVideoPosition = value) } } }
        viewModelScope.launch { settings.shortMaxMinutes.collect { value -> setState { copy(shortMaxMinutes = value) } } }
        viewModelScope.launch { settings.pdfReadingStyle.collect { value -> setState { copy(pdfReadingStyle = value) } } }
        viewModelScope.launch { player.playMode.collect { value -> setState { copy(playMode = value) } } }
        viewModelScope.launch { security.autoLockDelay.collect { value -> setState { copy(autoLockDelay = value) } } }
        viewModelScope.launch { security.wipeOnTooManyFailures.collect { value -> setState { copy(wipeOnFailures = value) } } }
        viewModelScope.launch { security.screenProtection.collect { value -> setState { copy(screenProtection = value) } } }
        viewModelScope.launch {
            observeCacheBudget().collect { budget ->
                val gb = (budget.limitBytes / CachePolicy.BYTES_PER_GB).toInt()
                setState { copy(cacheLimitGb = gb, cacheShares = budget.shares) }
            }
        }
        viewModelScope.launch { refreshCacheUsage() }
        viewModelScope.launch { secretUpdateEvents.saved.collect { sendEffect(SettingsEffect.ShowSecretSaved) } }
        viewModelScope.launch { observeSecretExpiry().collect { day -> setState { copy(secretExpiryEpochDay = day) } } }
        viewModelScope.launch { observeSecretExpiryNotice().collect { notice -> setState { copy(expiryNotice = notice) } } }
        viewModelScope.launch {
            // Bật/tắt bảo vệ đổi trạng thái sinh trắc học (tắt PIN thì xóa phần bọc), nên đọc lại mỗi lần đổi.
            observeProtection().collect { enabled ->
                setState { copy(protectionEnabled = enabled) }
                refreshBiometric()
            }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ShowSheet -> setState { copy(sheet = intent.sheet) }
            SettingsIntent.DismissSheet -> setState { copy(sheet = null) }
            is SettingsIntent.SelectTheme -> choose { setThemeMode(intent.mode) }
            is SettingsIntent.SelectLanguage -> {
                // Cập nhật State trước: AppCompat áp dụng ngay khi đổi, ViewModel hiện đúng giá trị mới.
                setState { copy(language = intent.language, sheet = null) }
                setLanguage(intent.language)
            }
            is SettingsIntent.ToggleKind ->
                // Luật "không tắt loại cuối" nằm ở UseCase; bị từ chối thì State không đổi và chip vẫn bật.
                viewModelScope.launch { toggleFileKind(intent.kind, intent.kind !in currentState.enabledKinds) }
            is SettingsIntent.SelectSeekStep -> choose { settings.setSeekStepSeconds(intent.seconds) }
            // O6: nháp nằm trong bảng, chỉ ghi khi bấm "Áp dụng" (cùng kiểu bảng Giới hạn tối đa).
            is SettingsIntent.ApplyShortMax -> choose { settings.setShortMaxMinutes(intent.minutes) }
            is SettingsIntent.SelectSpeed -> choose { settings.setDefaultSpeed(intent.speed) }
            is SettingsIntent.SelectVideoFit -> choose { settings.setDefaultVideoFit(intent.fit) }
            is SettingsIntent.SelectOrientation -> choose { settings.setOpenVideoLandscape(intent.landscape) }
            is SettingsIntent.SelectPlayMode -> choose { player.setPlayMode(intent.mode) }
            is SettingsIntent.SetRememberPosition -> viewModelScope.launch { settings.setRememberVideoPosition(intent.enabled) }
            is SettingsIntent.SelectPdfStyle -> choose { settings.setPdfReadingStyle(intent.style) }
            SettingsIntent.Refresh -> viewModelScope.launch {
                refreshBiometric()
                refreshCacheUsage()
            }
            is SettingsIntent.ToggleProtection ->
                sendEffect(if (intent.enable) SettingsEffect.OpenSecuritySetup else SettingsEffect.OpenPin(PinPurpose.DisableProtection))
            SettingsIntent.ChangePin -> sendEffect(SettingsEffect.OpenPin(PinPurpose.ChangePin))
            is SettingsIntent.ToggleBiometric -> toggleBiometric(intent.enable)
            is SettingsIntent.SelectAutoLock -> choose { security.setAutoLockDelay(intent.delay) }
            is SettingsIntent.ToggleWipe ->
                if (intent.enable) setState { copy(dialog = SettingsDialog.WipeOnFailures) } else viewModelScope.launch { security.setWipeOnTooManyFailures(false) }
            SettingsIntent.ConfirmWipeDialog -> {
                setState { copy(dialog = null) }
                sendEffect(SettingsEffect.OpenPin(PinPurpose.EnableWipe))
            }
            SettingsIntent.DismissDialog -> setState { copy(dialog = null, pendingLimitGb = null) }
            is SettingsIntent.ApplyCacheLimit -> applyCacheLimit(intent.gb)
            SettingsIntent.ConfirmShrinkCache -> {
                val gb = currentState.pendingLimitGb
                setState { copy(dialog = null, pendingLimitGb = null) }
                if (gb != null) viewModelScope.launch { setLimitAndRefresh(gb) }
            }
            is SettingsIntent.ApplyCacheShares -> {
                setState { copy(sheet = null) }
                viewModelScope.launch {
                    setCacheShares(intent.shares)
                    refreshCacheUsage()
                }
            }
            SettingsIntent.AskClearCache -> setState { copy(dialog = SettingsDialog.ClearCache) }
            SettingsIntent.ConfirmClearCache -> {
                setState { copy(dialog = null) }
                viewModelScope.launch {
                    clearCache()
                    refreshCacheUsage()
                }
            }
            is SettingsIntent.SetScreenProtection -> viewModelScope.launch { security.setScreenProtection(intent.enabled) }
            SettingsIntent.UpdateSecret ->
                sendEffect(if (currentState.protectionEnabled) SettingsEffect.OpenPin(PinPurpose.UpdateSecret) else SettingsEffect.OpenSecretForm)
            is SettingsIntent.SaveSecretExpiry -> {
                setState { copy(sheet = null) }
                viewModelScope.launch { setSecretExpiry(intent.epochDay) }
            }
            SettingsIntent.AskDisconnect -> setState { copy(dialog = SettingsDialog.DisconnectStep1) }
            SettingsIntent.ContinueDisconnect -> setState { copy(dialog = SettingsDialog.DisconnectStep2) }
            SettingsIntent.ConfirmDisconnect -> disconnectNow()
        }
    }

    /**
     * Ngắt kết nối (CD-05), sau hai hộp thoại xác nhận: xóa dữ liệu rồi báo app về màn Kết nối. Chặn bấm đôi bằng
     * [SettingsState.isDisconnecting]; `DisconnectUseCase` tự chạy NonCancellable nên rời màn giữa chừng cũng không xóa dở.
     */
    private fun disconnectNow() {
        if (currentState.isDisconnecting) return
        setState { copy(isDisconnecting = true) }
        viewModelScope.launch {
            val done = try {
                disconnect()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            // Lỗi thì mở khóa nút để thử lại, không để D4 kẹt.
            setState { copy(isDisconnecting = false, dialog = if (done) null else dialog) }
            if (done) sendEffect(SettingsEffect.Disconnected)
        }
    }

    /** Chạm một lựa chọn trong sheet: đóng sheet ngay rồi ghi (không có nút Lưu); giá trị mới tới qua luồng đọc ở [init]. */
    private fun choose(write: suspend () -> Unit) {
        setState { copy(sheet = null) }
        viewModelScope.launch { write() }
    }

    /**
     * Bật: hiện hộp thoại sinh trắc học của hệ thống rồi bọc khóa phiên (CD-02); hủy hay lỗi thì công tắc giữ nguyên (đọc lại để chắc).
     * Tắt: xóa phần bọc ngay, PIN vẫn dùng được.
     */
    private fun toggleBiometric(enable: Boolean) {
        viewModelScope.launch {
            if (enable) enableBiometric() else disableBiometric()
            refreshBiometric()
        }
    }

    /**
     * Đổi giới hạn chung. Dung lượng đang dùng (đo lại cho chắc) vượt giới hạn mới thì hỏi D6 vì sẽ dọn ngay tệp lâu không dùng
     * (CD-07); chưa vượt thì đổi luôn. UseCase vẫn dọn từng loại theo trần mới của nó dù tổng chưa vượt.
     */
    private fun applyCacheLimit(gb: Int) {
        setState { copy(sheet = null) }
        viewModelScope.launch {
            val usage = getCacheUsage()
            setState { copy(cacheUsage = usage) }
            if (usage.total > CachePolicy.limitBytes(gb)) {
                setState { copy(dialog = SettingsDialog.ShrinkCache, pendingLimitGb = gb) }
            } else {
                setLimitAndRefresh(gb)
            }
        }
    }

    private suspend fun setLimitAndRefresh(gb: Int) {
        setCacheLimit(gb)
        refreshCacheUsage()
    }

    private suspend fun refreshCacheUsage() {
        val usage = getCacheUsage()
        setState { copy(cacheUsage = usage) }
    }

    private suspend fun refreshBiometric() {
        val status = getBiometricStatus()
        setState { copy(biometric = status) }
    }
}
