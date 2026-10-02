package com.lambao.odv.feature.auth.connect

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.feature.auth.PendingConnection
import kotlinx.coroutines.launch

class ConnectViewModel(
    private val drives: DriveRepository,
    private val pending: PendingConnection,
) : BaseMviViewModel<ConnectState, ConnectIntent, ConnectEffect>(ConnectState()) {

    override fun onIntent(intent: ConnectIntent) {
        when (intent) {
            is ConnectIntent.FieldChanged -> onFieldChanged(intent.field, intent.value)
            is ConnectIntent.ToggleReveal -> setState {
                copy(revealed = if (intent.field in revealed) revealed - intent.field else revealed + intent.field)
            }
            ConnectIntent.HideAll -> setState { copy(revealed = emptySet()) }
            ConnectIntent.Connect -> connect()
            ConnectIntent.DismissFailure -> setState { copy(failure = null) }
            ConnectIntent.DismissConnected -> setState { copy(connected = null) }
            ConnectIntent.EditSecret -> {
                setState { copy(failure = null) }
                sendEffect(ConnectEffect.FocusField(ConnectField.ClientSecret))
            }
            ConnectIntent.Continue -> {
                // KN-08: config chưa được lưu, giữ trong bộ nhớ cho tới khi hoàn tất bước bảo mật.
                pending.hold(currentState.toConfig())
                setState { copy(connected = null) }
                sendEffect(ConnectEffect.NavigateToSecuritySetup)
            }
        }
    }

    private fun onFieldChanged(field: ConnectField, raw: String) {
        // KN-05: bỏ khoảng trắng và xuống dòng ở đầu/cuối giá trị dán vào.
        val value = raw.trim()
        setState {
            when (field) {
                ConnectField.TenantId -> copy(tenantId = value)
                ConnectField.ClientId -> copy(clientId = value)
                ConnectField.ClientSecret -> copy(clientSecret = value)
                ConnectField.Upn -> copy(upn = value)
            }
        }
    }

    private fun connect() {
        val state = currentState
        if (!state.canConnect) return
        setState { copy(isConnecting = true, failure = null, revealed = emptySet()) }
        viewModelScope.launch {
            when (val result = drives.verifyConnection(state.toConfig())) {
                is AppResult.Success -> setState {
                    copy(isConnecting = false, connected = ConnectedDrive(result.value, state.upn))
                }
                is AppResult.Failure -> setState {
                    // KN-09: giữ nguyên giá trị đã nhập để sửa.
                    copy(isConnecting = false, failure = result.error.toConnectFailure())
                }
            }
        }
    }
}
