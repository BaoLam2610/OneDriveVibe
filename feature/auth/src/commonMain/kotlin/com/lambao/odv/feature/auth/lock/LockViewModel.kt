package com.lambao.odv.feature.auth.lock

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.LockStatus
import com.lambao.odv.core.domain.model.PinPolicy
import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockStatusUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockoutRemainingUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.domain.usecase.security.UnlockWithBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.UnlockWithPinUseCase
import com.lambao.odv.feature.auth.LockConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn Khóa (KH-01 → KH-06, CH-03). Mở khóa bằng PIN 6 số hoặc sinh trắc học (nếu đã bật), chờ khi sai nhiều lần, Quên mã PIN.
 *
 * Các chữ số nằm trong [pin] (CharArray) chứ không trong State, và bị xóa ngay khi gửi đi hoặc khi ViewModel bị hủy
 * (CH-02). Kết thúc màn đi qua `State.completion` (không phải Effect) để không kẹt ở màn Khóa nếu UI đang xoay màn hình.
 */
class LockViewModel(
    private val observeLockState: ObserveLockStateUseCase,
    private val getLockStatus: GetLockStatusUseCase,
    private val getLockoutRemaining: GetLockoutRemainingUseCase,
    private val unlockWithPin: UnlockWithPinUseCase,
    private val unlockWithBiometric: UnlockWithBiometricUseCase,
    private val disconnect: DisconnectUseCase,
) : BaseMviViewModel<LockUiState, LockIntent, LockEffect>(LockUiState()) {

    private val pin = CharArray(PinPolicy.LENGTH)
    private var length = 0
    private var cooldownJob: Job? = null

    init {
        // Đã mở khóa nhưng app bị khóa lại trước khi UI kịp bỏ màn Khóa (bấm Home ngay sau khi nhập PIN): hủy kết quả
        // "đã mở khóa" để màn Khóa cho nhập lại, thay vì bỏ màn Khóa khi app đang khóa.
        viewModelScope.launch {
            observeLockState().collect { lock ->
                if (lock == LockState.Locked && currentState.completion == LockCompletion.Unlocked) {
                    setState { copy(completion = null) }
                }
            }
        }
        viewModelScope.launch {
            // Mở lại màn Khóa giữa lúc đang bị phạt (tắt rồi mở app, KH-02): hiện đồng hồ ngay.
            val status = getLockStatus()
            val remaining = status.lockoutRemainingMs
            if (remaining > 0) startCooldown(remaining)
            applyStatus(status)
            // KH-01: đã bật sinh trắc học thì tự hiện hộp thoại ngay khi mở màn Khóa; không hiện khi đang bị khóa tạm (KH-02).
            if (remaining <= 0) useBiometric()
        }
    }

    override fun onIntent(intent: LockIntent) {
        when (intent) {
            is LockIntent.Digit -> onDigit(intent.digit)
            LockIntent.Backspace -> onBackspace()
            LockIntent.UseBiometric -> useBiometric()
            LockIntent.ForgotClicked -> if (!currentState.isBusy) setState { copy(forgot = ForgotStep.Warn) }
            LockIntent.ForgotContinue -> setState { copy(forgot = ForgotStep.Confirm) }
            LockIntent.ForgotDismiss -> setState { copy(forgot = ForgotStep.None) }
            LockIntent.ForgotConfirm -> forgetPinAndDisconnect()
        }
    }

    private fun canEdit(): Boolean = currentState.let { !it.isBusy && !it.isCoolingDown && it.completion == null }

    private fun onDigit(digit: Int) {
        if (!canEdit() || length >= PinPolicy.LENGTH) return
        pin[length++] = '0' + digit
        setState { copy(entered = length, error = null) }
        if (length == PinPolicy.LENGTH) submit()
    }

    private fun onBackspace() {
        if (!canEdit() || length == 0) return
        pin[--length] = '\u0000'
        setState { copy(entered = length, error = null) }
    }

    /** Hiện hộp thoại sinh trắc học của hệ thống; xong thì xử lý như kết quả PIN. Hủy thì ở lại để nhập PIN. */
    private fun useBiometric() {
        if (!canEdit() || !currentState.biometricEnabled) return
        setState { copy(isBusy = true, error = null) }
        viewModelScope.launch {
            try {
                handle(unlockWithBiometric())
            } finally {
                // Hủy coroutine hoặc hộp thoại không hiện được (Activity bị hủy khi xoay màn hình, ViewModel vẫn sống) mà không
                // bỏ cờ này thì bàn phím và nút Quên PIN bị khóa mãi.
                setState { copy(isBusy = false) }
            }
        }
    }

    private fun submit() {
        // Bản sao để xóa PIN trong ViewModel ngay; bản sao này xóa ngay sau khi repository dùng xong.
        val attempt = pin.copyOf()
        pin.fill('\u0000')
        length = 0
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            val result = try {
                unlockWithPin(attempt)
            } finally {
                attempt.fill('\u0000')
            }
            handle(result)
        }
    }

    private suspend fun handle(result: UnlockResult) {
        when (result) {
            UnlockResult.Success -> setState { copy(isBusy = false, entered = 0, error = null, completion = LockCompletion.Unlocked) }
            is UnlockResult.WrongPin -> {
                setState { copy(isBusy = false, entered = 0, error = LockError.WrongPin) }
                sendEffect(LockEffect.Shake)
            }
            is UnlockResult.Cooldown -> {
                setState { copy(isBusy = false, entered = 0, error = null) }
                sendEffect(LockEffect.Shake)
                startCooldown(result.remainingMs)
            }
            // KH-06: UnlockWithPinUseCase đã xóa config, khóa và phần dữ liệu còn lại của app (Room, cache, cài đặt) trước khi trả về.
            UnlockResult.Wiped ->
                setState { copy(isBusy = false, entered = 0, error = null, completion = LockCompletion.Disconnected) }
            UnlockResult.Failed -> setState { copy(isBusy = false, entered = 0, error = LockError.StorageFailed) }
            // App bị khóa lại giữa lúc giải mã: không phải lỗi, chỉ cho nhập lại.
            UnlockResult.Interrupted -> setState { copy(isBusy = false, entered = 0, error = null) }
            // Hủy hộp thoại sinh trắc học, hoặc nó không còn dùng được (đổi vân tay): về nhập PIN, không phải lỗi.
            UnlockResult.Cancelled -> setState { copy(isBusy = false, entered = 0, error = null) }
        }
        applyStatus(getLockStatus())
    }

    private fun startCooldown(initialMs: Long) {
        cooldownJob?.cancel()
        setState { copy(cooldownRemainingMs = initialMs, cooldownTotalMs = initialMs) }
        cooldownJob = viewModelScope.launch {
            var remaining = initialMs
            while (remaining > 0) {
                delay(LockConstants.COOLDOWN_TICK_MS)
                // Hỏi lại repository thay vì tự trừ: chính xác khi app bị tạm dừng hay máy ngủ.
                remaining = getLockoutRemaining()
                setState { copy(cooldownRemainingMs = remaining) }
            }
        }
    }

    private fun applyStatus(status: LockStatus) {
        val left = status.attemptsBeforeWipe?.takeIf { it <= LockConstants.WARN_WHEN_ATTEMPTS_LEFT }
        setState { copy(attemptsBeforeWipe = left, biometricEnabled = status.biometricEnabled) }
    }

    private fun forgetPinAndDisconnect() {
        setState { copy(forgot = ForgotStep.None, isBusy = true) }
        viewModelScope.launch {
            disconnect()
            setState { copy(isBusy = false, completion = LockCompletion.Disconnected) }
        }
    }

    override fun onCleared() {
        pin.fill('\u0000')
        super.onCleared()
    }
}
