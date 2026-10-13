package com.lambao.odv.feature.settings

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.CacheBudget
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.model.RemoveAccountOutcome
import com.lambao.odv.core.domain.settings.PlayerPreferences
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.settings.SettingsPreferences
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import com.lambao.odv.core.domain.usecase.account.ObserveAccountScopeUseCase
import com.lambao.odv.core.domain.usecase.account.ObserveAccountStatusesUseCase
import com.lambao.odv.core.domain.usecase.account.RemoveAccountUseCase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
    observeAccountStatuses: ObserveAccountStatusesUseCase,
    observeAccountScope: ObserveAccountScopeUseCase,
    private val removeAccount: RemoveAccountUseCase,
) : BaseMviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState(language = getLanguage())) {

    /** Chống bấm đôi công tắc sinh trắc học: hộp thoại hệ thống đang mở hoặc đang bọc khóa thì bỏ qua lần bấm tiếp (giống [SettingsState.isDisconnecting]). */
    private var isTogglingBiometric = false

    /** Mốc nhận sự kiện "đã lưu secret" chưa hiện và mốc lần gần nhất tab Cài đặt hiện ra; xem [onSecretSavedEvent] và [onScreenShown]. */
    private var secretSavedMark: TimeMark? = null
    private var shownMark: TimeMark? = null

    init {
        // Cài đặt mới sinh: sự kiện "đã lưu secret" cũ (từ banner ở Danh sách) không còn ý nghĩa.
        secretUpdateEvents.clear()
        observeDisplayAndAccounts(observeThemeMode, observeConnectionInfo, observeAccountStatuses, observeAccountScope)
        observeVideoAndPdf()
        observeSecurity(observeProtection)
        observeCache(observeCacheBudget)
        observeSecretExpiryAndEvents(observeSecretExpiry, observeSecretExpiryNotice, secretUpdateEvents)
    }

    /** Nối một luồng đọc vào State: mỗi giá trị mới đi qua [reduce]. Thay cho một `viewModelScope.launch { collect { setState } }` mỗi luồng. */
    private fun <T> Flow<T>.bindToState(reduce: SettingsState.(T) -> SettingsState) {
        viewModelScope.launch { collect { value -> setState { reduce(value) } } }
    }

    private fun observeDisplayAndAccounts(
        observeThemeMode: ObserveThemeModeUseCase,
        observeConnectionInfo: ObserveConnectionInfoUseCase,
        observeAccountStatuses: ObserveAccountStatusesUseCase,
        observeAccountScope: ObserveAccountScopeUseCase,
    ) {
        observeThemeMode().bindToState { copy(themeMode = it) }
        // M1: ObserveConnectionInfoUseCase đọc lại khi trạng thái khóa đổi, nên VM tạo lúc app khóa vẫn có nhóm Kết nối sau khi mở khóa.
        observeConnectionInfo().bindToState { copy(connection = it) }
        observeAccountStatuses().bindToState { copy(accounts = it) }
        observeAccountScope().bindToState { copy(scope = it) }
        settings.enabledKinds.bindToState { copy(enabledKinds = it) }
    }

    private fun observeVideoAndPdf() {
        settings.seekStepSeconds.bindToState { copy(seekStepSeconds = it) }
        settings.defaultSpeed.bindToState { copy(defaultSpeed = it) }
        settings.defaultVideoFit.bindToState { copy(defaultVideoFit = it) }
        settings.openVideoLandscape.bindToState { copy(openVideoLandscape = it) }
        settings.videoResumeBehavior.bindToState { copy(videoResumeBehavior = it) }
        settings.shortMaxMinutes.bindToState { copy(shortMaxMinutes = it) }
        settings.pdfReadingStyle.bindToState { copy(pdfReadingStyle = it) }
        player.playMode.bindToState { copy(playMode = it) }
    }

    private fun observeSecurity(observeProtection: ObserveProtectionUseCase) {
        security.autoLockDelay.bindToState { copy(autoLockDelay = it) }
        security.wipeOnTooManyFailures.bindToState { copy(wipeOnFailures = it) }
        security.screenProtection.bindToState { copy(screenProtection = it) }
        viewModelScope.launch {
            // Bật/tắt bảo vệ đổi trạng thái sinh trắc học (tắt PIN thì xóa phần bọc), nên đọc lại mỗi lần đổi.
            observeProtection().collect { enabled ->
                setState { copy(protectionEnabled = enabled) }
                refreshBiometric()
            }
        }
    }

    private fun observeCache(observeCacheBudget: ObserveCacheBudgetUseCase) {
        viewModelScope.launch {
            observeCacheBudget().collect { budget ->
                val gb = (budget.limitBytes / CachePolicy.BYTES_PER_GB).toInt()
                setState { copy(cacheLimitGb = gb, cacheShares = budget.shares) }
            }
        }
        viewModelScope.launch { refreshCacheUsage() }
    }

    private fun observeSecretExpiryAndEvents(
        observeSecretExpiry: ObserveSecretExpiryUseCase,
        observeSecretExpiryNotice: ObserveSecretExpiryNoticeUseCase,
        secretUpdateEvents: SecretUpdateEvents,
    ) {
        observeSecretExpiry().bindToState { copy(secretExpiryEpochDay = it) }
        observeSecretExpiryNotice().bindToState { copy(expiryNotice = it) }
        viewModelScope.launch { secretUpdateEvents.saved.collect { onSecretSavedEvent() } }
    }

    /**
     * L8: Snackbar "Đã cập nhật Client Secret" chỉ có nghĩa ngay sau khi lưu. Sự kiện đến khi tab Cài đặt không hiện (cập nhật từ banner ở
     * Danh sách) trước đây nằm trong hàng đợi effect và hiện rất muộn khi người dùng mở Cài đặt. Nay: ghi mốc; nếu tab vừa hiện lại (race
     * giữa sự kiện và [onScreenShown]) thì phát ngay, không thì để [onScreenShown] quyết định theo độ tươi của mốc.
     */
    private fun onSecretSavedEvent() {
        secretSavedMark = TimeSource.Monotonic.markNow()
        if (shownMark?.let { it.elapsedNow() < SHOWN_RACE_WINDOW } == true) deliverSecretSaved()
    }

    /** Gọi mỗi khi tab Cài đặt hiện ra (intent [SettingsIntent.Refresh]): phát Snackbar nếu secret vừa lưu cách đây không lâu, bỏ sự kiện cũ. */
    private fun onScreenShown() {
        shownMark = TimeSource.Monotonic.markNow()
        if (secretSavedMark?.let { it.elapsedNow() < SECRET_SAVED_FRESH } == true) deliverSecretSaved() else secretSavedMark = null
    }

    private fun deliverSecretSaved() {
        secretSavedMark = null
        sendEffect(SettingsEffect.ShowSecretSaved)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ShowSheet -> setState { copy(sheet = intent.sheet) }
            SettingsIntent.DismissSheet -> setState { copy(sheet = null) }
            SettingsIntent.DismissDialog -> setState { copy(dialog = null, pendingLimitGb = null, removeCandidate = null) }
            SettingsIntent.Refresh -> {
                onScreenShown()
                viewModelScope.launch {
                    refreshBiometric()
                    refreshCacheUsage()
                }
            }
            is SettingsIntent.Display -> onDisplayIntent(intent)
            is SettingsIntent.VideoPdf -> onVideoPdfIntent(intent)
            is SettingsIntent.Security -> onSecurityIntent(intent)
            is SettingsIntent.Cache -> onCacheIntent(intent)
            is SettingsIntent.Account -> onAccountIntent(intent)
        }
    }

    private fun onDisplayIntent(intent: SettingsIntent.Display) {
        when (intent) {
            is SettingsIntent.SelectTheme -> choose { setThemeMode(intent.mode) }
            is SettingsIntent.SelectLanguage -> {
                // Cập nhật State trước: AppCompat áp dụng ngay khi đổi, ViewModel hiện đúng giá trị mới.
                setState { copy(language = intent.language, sheet = null) }
                setLanguage(intent.language)
            }
            is SettingsIntent.ToggleKind ->
                // Luật "không tắt loại cuối" nằm ở UseCase và nó đọc-sửa-ghi nguyên tử trong kho (L7: chạm nhanh hai chip không mất cập
                // nhật). Giá trị đích tính từ State hiện tại; bị từ chối thì State không đổi và chip vẫn bật.
                viewModelScope.launch { toggleFileKind(intent.kind, intent.kind !in currentState.enabledKinds) }
        }
    }

    private fun onVideoPdfIntent(intent: SettingsIntent.VideoPdf) {
        when (intent) {
            is SettingsIntent.SelectSeekStep -> choose { settings.setSeekStepSeconds(intent.seconds) }
            // O6: nháp nằm trong bảng, chỉ ghi khi bấm "Áp dụng" (cùng kiểu bảng Giới hạn tối đa).
            is SettingsIntent.ApplyShortMax -> choose { settings.setShortMaxMinutes(intent.minutes) }
            is SettingsIntent.SelectSpeed -> choose { settings.setDefaultSpeed(intent.speed) }
            is SettingsIntent.SelectVideoFit -> choose { settings.setDefaultVideoFit(intent.fit) }
            is SettingsIntent.SelectOrientation -> choose { settings.setOpenVideoLandscape(intent.landscape) }
            is SettingsIntent.SelectPlayMode -> choose { player.setPlayMode(intent.mode) }
            // Như các lựa chọn khác trong sheet: lưu rồi đóng sheet.
            is SettingsIntent.SelectResumeBehavior -> choose { settings.setVideoResumeBehavior(intent.behavior) }
            is SettingsIntent.SelectPdfStyle -> choose { settings.setPdfReadingStyle(intent.style) }
        }
    }

    private fun onSecurityIntent(intent: SettingsIntent.Security) {
        when (intent) {
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
            is SettingsIntent.SetScreenProtection -> viewModelScope.launch { security.setScreenProtection(intent.enabled) }
        }
    }

    private fun onCacheIntent(intent: SettingsIntent.Cache) {
        when (intent) {
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
        }
    }

    private fun onAccountIntent(intent: SettingsIntent.Account) {
        when (intent) {
            SettingsIntent.UpdateSecret ->
                sendEffect(if (currentState.protectionEnabled) SettingsEffect.OpenPin(PinPurpose.UpdateSecret) else SettingsEffect.OpenSecretForm)
            is SettingsIntent.SaveSecretExpiry -> {
                setState { copy(sheet = null) }
                viewModelScope.launch { setSecretExpiry(intent.epochDay) }
            }
            SettingsIntent.OpenAccountPicker -> sendEffect(SettingsEffect.OpenAccountPicker)
            SettingsIntent.AddAccount -> sendEffect(SettingsEffect.OpenAddAccount)
            is SettingsIntent.AskRemoveAccount -> askRemoveAccount(intent.accountId)
            SettingsIntent.ConfirmRemoveAccount -> confirmRemoveAccount()
            SettingsIntent.AskDisconnect -> setState { copy(dialog = SettingsDialog.DisconnectStep1) }
            SettingsIntent.ContinueDisconnect -> setState { copy(dialog = SettingsDialog.DisconnectStep2) }
            SettingsIntent.ConfirmDisconnect -> disconnectNow()
        }
    }

    /**
     * Xóa tài khoản (TA-08): hỏi AC8 nếu được phép; không thì Snackbar giải thích. Tài khoản đang xem (phạm vi một tài khoản) và tài khoản
     * duy nhất không xóa được; ở phạm vi Tất cả mọi tài khoản đều xóa được (khi còn từ 2).
     */
    private fun askRemoveAccount(accountId: String) {
        val state = currentState
        val target = state.accounts.firstOrNull { it.account.id == accountId } ?: return
        when {
            state.accounts.size <= 1 -> sendEffect(SettingsEffect.ShowAccountMessage(AccountMessage.CannotRemoveLast))
            (state.scope as? AccountScope.One)?.accountId == accountId ->
                sendEffect(SettingsEffect.ShowAccountMessage(AccountMessage.CannotRemoveCurrent))
            else -> setState { copy(dialog = SettingsDialog.RemoveAccount, removeCandidate = target) }
        }
    }

    private fun confirmRemoveAccount() {
        val target = currentState.removeCandidate ?: return
        if (currentState.isRemovingAccount) return
        setState { copy(isRemovingAccount = true) }
        viewModelScope.launch {
            val outcome = removeAccount(target.account.id)
            setState { copy(isRemovingAccount = false, dialog = null, removeCandidate = null) }
            // Xóa tài khoản dọn thumbnail, ảnh, video, PDF của nó (TA-08): đo lại để dung lượng ở nhóm Bộ nhớ đệm đổi ngay, không phải
            // chuyển tab mới thấy (kiểm tay Lát 9, mục 8).
            if (outcome == RemoveAccountOutcome.Removed) refreshCacheUsage()
            sendEffect(
                SettingsEffect.ShowAccountMessage(
                    when (outcome) {
                        RemoveAccountOutcome.Removed -> AccountMessage.Removed(target.account.email)
                        // U-L10: NotAllowed không nói lý do; suy từ state (không xóa được thì danh sách chưa đổi): chỉ còn một tài khoản
                        // thì "không xóa được tài khoản cuối", ngược lại là tài khoản đang xem.
                        RemoveAccountOutcome.NotAllowed ->
                            if (currentState.accounts.size <= 1) AccountMessage.CannotRemoveLast else AccountMessage.CannotRemoveCurrent
                        is RemoveAccountOutcome.Failed -> AccountMessage.RemoveFailed
                    },
                ),
            )
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
        // L2: bấm đôi khi hộp thoại hệ thống đang mở sẽ chạy enableBiometric hai lần; cờ này bỏ qua lần bấm sau tới khi lần đầu xong.
        if (isTogglingBiometric) return
        isTogglingBiometric = true
        viewModelScope.launch {
            try {
                if (enable) enableBiometric() else disableBiometric()
                refreshBiometric()
            } finally {
                isTogglingBiometric = false
            }
        }
    }

    /**
     * Đổi giới hạn chung. Hạ giới hạn mà **bất kỳ loại nào** đang dùng vượt trần riêng mới của nó (giới hạn mới × tỉ lệ của loại đó, đo lại
     * cho chắc) thì hỏi D6 vì sẽ dọn ngay tệp lâu không dùng (CD-07, chốt 2026-10-12: không chỉ khi tổng vượt); không thì đổi luôn.
     * Tăng giới hạn không bao giờ hỏi. Đổi tỉ lệ chia vẫn dọn không hỏi.
     */
    private fun applyCacheLimit(gb: Int) {
        setState { copy(sheet = null) }
        viewModelScope.launch {
            val usage = getCacheUsage()
            setState { copy(cacheUsage = usage) }
            val state = currentState
            val newBudget = CacheBudget(CachePolicy.limitBytes(gb), state.cacheShares)
            val lowering = gb < state.cacheLimitGb
            val anyOverShare = CacheKind.entries.any { usage.of(it) > newBudget.bytesFor(it) }
            if (lowering && anyOverShare) {
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

    private companion object {
        /** Secret lưu cách đây chưa tới ngần này thì coi là "vừa lưu" và hiện Snackbar khi tab Cài đặt hiện ra (L8). */
        val SECRET_SAVED_FRESH = 5.seconds

        /** Tab Cài đặt vừa hiện trong khoảng này thì sự kiện "đã lưu" đến sau vẫn được phát ngay (tránh race với [onScreenShown]). */
        val SHOWN_RACE_WINDOW = 1.seconds
    }
}
