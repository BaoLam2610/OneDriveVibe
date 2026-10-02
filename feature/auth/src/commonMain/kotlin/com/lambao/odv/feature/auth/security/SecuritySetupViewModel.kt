package com.lambao.odv.feature.auth.security

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.feature.auth.PendingConnection
import kotlinx.coroutines.launch

class SecuritySetupViewModel(
    private val configs: ConfigRepository,
    private val pending: PendingConnection,
) : BaseMviViewModel<SecuritySetupState, SecuritySetupIntent, SecuritySetupEffect>(SecuritySetupState()) {

    init {
        if (pending.config == null) resolveMissingConnection()
    }

    override fun onIntent(intent: SecuritySetupIntent) {
        when (intent) {
            SecuritySetupIntent.Complete -> complete()
            SecuritySetupIntent.DismissSaveFailure -> setState { copy(saveFailed = false) }
        }
    }

    private fun complete() {
        val config = pending.config
        if (config == null) {
            resolveMissingConnection()
            return
        }
        if (currentState.isSaving) return
        setState { copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            when (configs.save(config)) {
                is AppResult.Success -> {
                    pending.clear()
                    setState { copy(isSaving = false) }
                    sendEffect(SecuritySetupEffect.NavigateToHome)
                }
                is AppResult.Failure -> setState { copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    /**
     * Không còn config chờ lưu: hoặc tiến trình bị thu hồi giữa chừng (về Kết nối), hoặc config đã lưu xong mà effect
     * điều hướng bị mất lúc xoay màn hình (vào Danh sách). Phân biệt bằng việc đã có config trên máy chưa.
     */
    private fun resolveMissingConnection() {
        viewModelScope.launch {
            sendEffect(
                if (configs.hasConfig()) SecuritySetupEffect.NavigateToHome else SecuritySetupEffect.NavigateToConnect,
            )
        }
    }
}
