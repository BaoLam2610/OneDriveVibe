package com.lambao.odv.feature.auth.security

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.BiometricOutcome
import com.lambao.odv.core.domain.model.PinPolicy
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.launch

/**
 * Thiết lập mã PIN lần đầu (BM-01 → BM-08): đặt PIN, nhập lại, rồi mã hóa lại config đã lưu ở chế độ thiết bị (BM-04).
 *
 * Các chữ số nằm trong [current] và [first] (CharArray), không trong State, và bị xóa ngay khi không còn cần (CH-02).
 * PIN không bao giờ vào log. Kết thúc đi qua `State.isDone` (không phải Effect) để không kẹt khi xoay màn hình.
 */
class SecuritySetupViewModel(
    private val security: SecurityRepository,
) : BaseMviViewModel<SetupUiState, SetupIntent, SetupEffect>(SetupUiState()) {

    private val current = CharArray(PinPolicy.LENGTH)
    private var length = 0

    /** PIN đã nhập ở bước đặt, giữ để so với bước nhập lại. */
    private val first = CharArray(PinPolicy.LENGTH)

    override fun onIntent(intent: SetupIntent) {
        when (intent) {
            is SetupIntent.Digit -> onDigit(intent.digit)
            SetupIntent.Backspace -> onBackspace()
            SetupIntent.BackToCreate -> backToCreate()
            SetupIntent.DismissSaveFailure -> setState { copy(saveFailed = false) }
            SetupIntent.EnableBiometric -> enableBiometric()
            SetupIntent.SkipBiometric -> if (!currentState.isEnrollingBiometric) setState { copy(isDone = true) }
        }
    }

    /** Chỉ nhập PIN ở hai bước đầu; đang hoàn tất, đang hỏi sinh trắc học hoặc đã xong thì bỏ qua phím. */
    private fun canEdit(): Boolean = currentState.let { !it.isDone && (it.step == SetupStep.Create || it.step == SetupStep.Confirm) }

    private fun enableBiometric() {
        if (currentState.isEnrollingBiometric || currentState.step != SetupStep.OfferBiometric) return
        setState { copy(isEnrollingBiometric = true) }
        viewModelScope.launch {
            val outcome = try {
                security.enableBiometric()
            } finally {
                // Kể cả khi bị hủy: không bỏ cờ này thì hai nút ở B6 bị khóa mãi.
                setState { copy(isEnrollingBiometric = false) }
            }
            // Hủy hộp thoại: ở lại B6 để thử lại hoặc chọn "Để sau". Thành công, hoặc lỗi và không dùng được (PIN đã bật nên
            // không mất gì; bật lại ở Cài đặt, CD-02): vào Danh sách thay vì để người dùng kẹt ở B6 không có thông báo.
            if (outcome != BiometricOutcome.Cancelled) setState { copy(isDone = true) }
        }
    }

    private fun onDigit(digit: Int) {
        if (!canEdit() || length >= PinPolicy.LENGTH) return
        current[length++] = '0' + digit
        setState { copy(entered = length, error = null) }
        // BM-05: đủ 6 số thì tự chuyển bước, không cần bấm nút.
        if (length == PinPolicy.LENGTH) onComplete()
    }

    private fun onBackspace() {
        if (!canEdit() || length == 0) return
        current[--length] = '\u0000'
        setState { copy(entered = length, error = null) }
    }

    private fun onComplete() {
        when (currentState.step) {
            SetupStep.Create -> {
                if (PinPolicy.isWeak(current)) {
                    // BM-06: chặn PIN dễ đoán, xóa và đặt lại.
                    clearCurrent()
                    setState { copy(entered = 0, error = SetupError.Weak) }
                    sendEffect(SetupEffect.Shake)
                } else {
                    current.copyInto(first)
                    clearCurrent()
                    setState { copy(step = SetupStep.Confirm, entered = 0, error = null) }
                }
            }
            SetupStep.Confirm -> {
                if (current.contentEquals(first)) {
                    finish()
                } else {
                    clearCurrent()
                    setState { copy(entered = 0, error = SetupError.Mismatch) }
                    sendEffect(SetupEffect.Shake)
                }
            }
            SetupStep.Finishing, SetupStep.OfferBiometric -> Unit
        }
    }

    private fun backToCreate() {
        if (currentState.step != SetupStep.Confirm) return
        clearAll()
        setState { copy(step = SetupStep.Create, entered = 0, error = null) }
    }

    private fun finish() {
        val pin = current.copyOf()
        clearCurrent()
        setState { copy(step = SetupStep.Finishing, entered = PinPolicy.LENGTH, error = null) }
        viewModelScope.launch {
            val result = try {
                security.enableProtection(pin)
            } finally {
                pin.fill('\u0000')
                first.fill('\u0000')
            }
            when (result) {
                // BM-02: PIN đã bật. Nếu máy hỗ trợ sinh trắc học thì hỏi (B6), không thì xong luôn.
                is AppResult.Success ->
                    if (security.isBiometricAvailable()) setState { copy(step = SetupStep.OfferBiometric) } else setState { copy(isDone = true) }
                // BM-04: lỗi thì config giữ nguyên ở chế độ thiết bị; quay về bước đặt PIN để thử lại.
                is AppResult.Failure -> setState { copy(step = SetupStep.Create, entered = 0, saveFailed = true) }
            }
        }
    }

    private fun clearCurrent() {
        current.fill('\u0000')
        length = 0
    }

    private fun clearAll() {
        clearCurrent()
        first.fill('\u0000')
    }

    override fun onCleared() {
        clearAll()
        super.onCleared()
    }
}
