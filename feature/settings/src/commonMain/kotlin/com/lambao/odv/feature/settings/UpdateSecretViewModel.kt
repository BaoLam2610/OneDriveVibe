package com.lambao.odv.feature.settings

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.usecase.connection.UpdateClientSecretUseCase
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel màn Cập nhật Client Secret (CD-04). Việc xác thực lại bằng PIN đã làm ở màn PIN (P1) trước khi tới đây. Kiểm tra kết nối
 * bằng secret mới rồi mới lưu; thất bại thì config cũ còn nguyên. Không log giá trị ô nhập (CH-06).
 */
class UpdateSecretViewModel(
    private val updateClientSecret: UpdateClientSecretUseCase,
    private val events: SecretUpdateEvents,
) : BaseMviViewModel<UpdateSecretState, UpdateSecretIntent, UpdateSecretEffect>(UpdateSecretState()) {

    override fun onIntent(intent: UpdateSecretIntent) {
        when (intent) {
            // KN-05: bỏ khoảng trắng và xuống dòng ở đầu/cuối giá trị dán vào.
            is UpdateSecretIntent.SecretChanged -> setState { copy(secret = intent.value.trim(), failure = null) }
            UpdateSecretIntent.ToggleReveal -> setState { copy(revealed = !revealed) }
            UpdateSecretIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val state = currentState
        // Guard bấm đôi: đang kiểm tra thì bỏ qua.
        if (!state.canSubmit) return
        setState { copy(isChecking = true, failure = null) }
        viewModelScope.launch {
            // NonCancellable: rời màn giữa chừng không được để tệp đã ghi mà bộ nhớ chưa khớp (giống DisconnectUseCase).
            when (val result = withContext(NonCancellable) { updateClientSecret(state.secret) }) {
                is AppResult.Success -> {
                    setState { copy(isChecking = false, secret = "") }
                    events.notifySaved()
                    sendEffect(UpdateSecretEffect.Saved)
                }
                is AppResult.Failure -> setState { copy(isChecking = false, failure = result.error.toSecretFailure()) }
            }
        }
    }
}
