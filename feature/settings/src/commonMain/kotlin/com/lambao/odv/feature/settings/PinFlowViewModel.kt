package com.lambao.odv.feature.settings

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.PinPolicy
import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.usecase.security.ChangePinUseCase
import com.lambao.odv.core.domain.usecase.security.DisableProtectionUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockStatusUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockoutRemainingUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.domain.usecase.security.VerifyPinUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn PIN của Cài đặt (P1 → P3): xác nhận PIN hiện tại trước một thao tác nhạy cảm, và nhập PIN mới khi đổi PIN (CD-03, CD-08, CD-09).
 * Dùng chung bộ đếm sai với màn Khóa (chờ khi sai nhiều lần KH-02, xóa dữ liệu khi bật CD-08 KH-06).
 *
 * Các chữ số nằm trong [pin] (CharArray), không trong State, và bị xóa ngay khi gửi đi hoặc khi ViewModel bị hủy (CH-02). PIN hiện tại
 * đã đúng được giữ trong [held] tới khi xong việc (đổi PIN cần nó ở bước cuối, tắt bảo vệ cần nó sau hộp thoại D1) rồi xóa.
 * PIN không bao giờ vào log.
 */
class PinFlowViewModel(
    private val purpose: PinPurpose,
    private val verifyPin: VerifyPinUseCase,
    private val changePin: ChangePinUseCase,
    private val disableProtection: DisableProtectionUseCase,
    private val getLockStatus: GetLockStatusUseCase,
    private val getLockoutRemaining: GetLockoutRemainingUseCase,
    private val security: SecuritySettings,
    observeLockState: ObserveLockStateUseCase,
) : BaseMviViewModel<PinFlowState, PinFlowIntent, PinFlowEffect>(PinFlowState(purpose = purpose)) {

    private val pin = CharArray(PinPolicy.LENGTH)
    private var length = 0

    /** PIN hiện tại đã xác nhận đúng; PIN mới ở bước P2 giữ ở [first]. */
    private val held = CharArray(PinPolicy.LENGTH)
    private val first = CharArray(PinPolicy.LENGTH)
    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch { applyStatus() }
        viewModelScope.launch {
            // CH-02, CH-03: app bị khóa (rời app quá hạn tự khóa) khi đang dừng ở P2, P3 hoặc hộp thoại D1: bỏ PIN đang giữ trong bộ
            // nhớ và đưa về P1, để PIN không nằm lại suốt lúc app khóa. Sau khi mở khóa phải nhập lại từ đầu.
            observeLockState().collect { lock ->
                if (lock == LockState.Locked) {
                    clearAll()
                    setState { copy(step = PinStep.Current, entered = 0, error = null, confirmDisable = false) }
                }
            }
        }
    }

    override fun onIntent(intent: PinFlowIntent) {
        when (intent) {
            is PinFlowIntent.Digit -> onDigit(intent.digit)
            PinFlowIntent.Backspace -> onBackspace()
            PinFlowIntent.ConfirmDisable -> confirmDisable()
            PinFlowIntent.CancelDisable -> {
                clearAll()
                setState { copy(confirmDisable = false, completion = PinCompletion.Done) }
            }
        }
    }

    private fun canEdit(): Boolean = currentState.let { !it.isBusy && !it.isCoolingDown && !it.confirmDisable && it.completion == null }

    private fun onDigit(digit: Int) {
        if (!canEdit() || length >= PinPolicy.LENGTH) return
        pin[length++] = '0' + digit
        setState { copy(entered = length, error = null) }
        if (length == PinPolicy.LENGTH) onComplete()
    }

    private fun onBackspace() {
        if (!canEdit() || length == 0) return
        pin[--length] = '\u0000'
        setState { copy(entered = length, error = null) }
    }

    private fun onComplete() {
        when (currentState.step) {
            PinStep.Current -> submitCurrent()
            PinStep.New -> {
                if (PinPolicy.isWeak(pin)) {
                    // BM-06: chặn PIN dễ đoán, xóa và đặt lại.
                    clearEntry()
                    setState { copy(entered = 0, error = PinError.Weak) }
                    sendEffect(PinFlowEffect.Shake)
                } else {
                    pin.copyInto(first)
                    clearEntry()
                    setState { copy(step = PinStep.ConfirmNew, entered = 0, error = null) }
                }
            }
            PinStep.ConfirmNew -> {
                if (pin.contentEquals(first)) {
                    submitChange()
                } else {
                    clearEntry()
                    setState { copy(entered = 0, error = PinError.Mismatch) }
                    sendEffect(PinFlowEffect.Shake)
                }
            }
        }
    }

    /** P1: kiểm tra PIN hiện tại. Đúng thì làm tiếp theo mục đích. */
    private fun submitCurrent() {
        val attempt = pin.copyOf()
        clearEntry()
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            // Đúng thì [onCurrentVerified] tự xóa [attempt] (sau khi sao vào [held] nếu cần); không thì xóa ngay ở đây.
            val result = try {
                verifyPin(attempt)
            } catch (e: Throwable) {
                attempt.fill('\u0000')
                throw e
            }
            if (result == UnlockResult.Success) {
                onCurrentVerified(attempt)
            } else {
                attempt.fill('\u0000')
                handleFailure(result)
            }
        }
    }

    private suspend fun onCurrentVerified(verified: CharArray) {
        when (purpose) {
            PinPurpose.EnableWipe -> {
                verified.fill('\u0000')
                // CD-08: PIN đã xác nhận, bật tùy chọn.
                security.setWipeOnTooManyFailures(true)
                setState { copy(isBusy = false, completion = PinCompletion.Done) }
            }
            PinPurpose.DisableProtection -> {
                verified.copyInto(held)
                verified.fill('\u0000')
                // D1: hộp thoại cảnh báo trước khi tắt thật (CD-03).
                setState { copy(isBusy = false, confirmDisable = true) }
            }
            PinPurpose.UpdateSecret -> {
                verified.fill('\u0000')
                // CD-04: PIN đã xác nhận (xác thực lại); màn nhập Client Secret mở tiếp ở app, không cần giữ PIN.
                setState { copy(isBusy = false, completion = PinCompletion.Done) }
            }
            PinPurpose.ChangePin -> {
                verified.copyInto(held)
                verified.fill('\u0000')
                setState { copy(isBusy = false, step = PinStep.New, entered = 0, error = null) }
            }
        }
        applyStatus()
    }

    private fun confirmDisable() {
        if (!currentState.confirmDisable || currentState.isBusy) return
        val old = held.copyOf()
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            val result = try {
                disableProtection(old)
            } finally {
                old.fill('\u0000')
            }
            clearAll()
            when (result) {
                UnlockResult.Success -> setState { copy(isBusy = false, confirmDisable = false, completion = PinCompletion.Done) }
                else -> {
                    setState { copy(confirmDisable = false) }
                    handleFailure(result)
                }
            }
        }
    }

    /** P3 khớp: đổi PIN (CD-09). PIN cũ nằm trong [held], PIN mới trong [first]. */
    private fun submitChange() {
        val old = held.copyOf()
        val new = first.copyOf()
        clearEntry()
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            val result = try {
                changePin(old, new)
            } finally {
                old.fill('\u0000')
                new.fill('\u0000')
            }
            clearAll()
            when (result) {
                UnlockResult.Success -> setState { copy(isBusy = false, completion = PinCompletion.Done) }
                else -> {
                    // Lỗi lưu giữa chừng: config vẫn mã hóa bằng PIN cũ (CD-09). Quay về P1 để làm lại từ đầu.
                    setState { copy(step = PinStep.Current) }
                    handleFailure(result)
                }
            }
        }
    }

    private suspend fun handleFailure(result: UnlockResult) {
        when (result) {
            is UnlockResult.WrongPin -> {
                setState { copy(isBusy = false, entered = 0, error = PinError.Wrong) }
                sendEffect(PinFlowEffect.Shake)
            }
            is UnlockResult.Cooldown -> {
                setState { copy(isBusy = false, entered = 0, error = null) }
                sendEffect(PinFlowEffect.Shake)
                startCooldown(result.remainingMs)
            }
            // KH-06: VerifyPinUseCase đã xóa config, khóa và phần dữ liệu còn lại của app trước khi trả về.
            UnlockResult.Wiped -> setState { copy(isBusy = false, entered = 0, error = null, completion = PinCompletion.Disconnected) }
            UnlockResult.Failed -> setState { copy(isBusy = false, entered = 0, error = PinError.StorageFailed) }
            // App bị khóa lại giữa chừng (bấm Home): màn Khóa sẽ lên trên; ở đây chỉ cho nhập lại.
            UnlockResult.Interrupted, UnlockResult.Cancelled, UnlockResult.Success -> setState { copy(isBusy = false, entered = 0, error = null) }
        }
        applyStatus()
    }

    /** Đọc thời gian chờ và số lần sai còn lại: mở màn giữa lúc đang bị phạt (KH-02) thì hiện đồng hồ ngay. */
    private suspend fun applyStatus() {
        val status = getLockStatus()
        if (status.lockoutRemainingMs > 0 && !currentState.isCoolingDown) startCooldown(status.lockoutRemainingMs)
        val left = status.attemptsBeforeWipe?.takeIf { it <= PinFlowConstants.WARN_WHEN_ATTEMPTS_LEFT }
        setState { copy(attemptsBeforeWipe = left) }
    }

    private fun startCooldown(initialMs: Long) {
        cooldownJob?.cancel()
        setState { copy(cooldownRemainingMs = initialMs) }
        cooldownJob = viewModelScope.launch {
            var remaining = initialMs
            while (remaining > 0) {
                delay(PinFlowConstants.COOLDOWN_TICK_MS)
                // Hỏi lại repository thay vì tự trừ: chính xác khi app bị tạm dừng hay máy ngủ.
                remaining = getLockoutRemaining()
                setState { copy(cooldownRemainingMs = remaining) }
            }
        }
    }

    private fun clearEntry() {
        pin.fill('\u0000')
        length = 0
    }

    private fun clearAll() {
        clearEntry()
        held.fill('\u0000')
        first.fill('\u0000')
    }

    override fun onCleared() {
        clearAll()
        super.onCleared()
    }
}
