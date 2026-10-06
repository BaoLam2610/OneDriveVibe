package com.lambao.odv.feature.auth.connect

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.SavedConnection
import com.lambao.odv.core.domain.usecase.connection.CheckSavedConnectionUseCase
import com.lambao.odv.core.domain.usecase.connection.ConnectUseCase
import kotlinx.coroutines.launch

class ConnectViewModel(
    private val connect: ConnectUseCase,
    private val checkSavedConnection: CheckSavedConnectionUseCase,
) : BaseMviViewModel<ConnectState, ConnectIntent, ConnectEffect>(ConnectState()) {

    // Chỉ log kết quả, tuyệt đối không log giá trị các ô hay config (CH-06).
    private val log = Logger.withTag("Connect")

    init {
        // KN-13: hệ điều hành thu hồi tiến trình khi K6 hoặc màn Thiết lập bảo mật đang mở thì back stack được khôi phục
        // nhưng State này thì không. Config đã lưu (KN-08) nên vào thẳng Danh sách. Phải giải mã được: Splash cũng đưa
        // người dùng về Kết nối khi config còn nhưng hỏng, lúc đó phải ở lại form để kết nối lại.
        viewModelScope.launch {
            if (checkSavedConnection() == SavedConnection.Usable) {
                sendEffect(ConnectEffect.NavigateToHome)
            }
        }
    }

    override fun onIntent(intent: ConnectIntent) {
        when (intent) {
            is ConnectIntent.FieldChanged -> onFieldChanged(intent.field, intent.value)
            is ConnectIntent.ToggleReveal -> setState {
                copy(revealed = if (intent.field in revealed) revealed - intent.field else revealed + intent.field)
            }
            ConnectIntent.HideAll -> setState { copy(revealed = emptySet()) }
            ConnectIntent.Connect -> onConnect()
            ConnectIntent.DismissFailure -> setState { copy(failure = null) }
            ConnectIntent.EditSecret -> {
                setState { copy(failure = null) }
                sendEffect(ConnectEffect.FocusField(ConnectField.ClientSecret))
            }
            // Cả hai nút giữ showPinPrompt = true: màn Kết nối sắp rời back stack (Để sau) hoặc bị màn khác che
            // (Thiết lập mã PIN), và Back từ màn Thiết lập bảo mật phải thấy lại hộp thoại (BM-08).
            ConnectIntent.SetupPin -> sendEffect(ConnectEffect.NavigateToSecuritySetup)
            ConnectIntent.SkipPin -> sendEffect(ConnectEffect.NavigateToHome)
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

    private fun onConnect() {
        val state = currentState
        if (!state.canConnect) return
        setState { copy(isConnecting = true, failure = null, revealed = emptySet()) }
        viewModelScope.launch {
            val config = state.toConfig()
            // KN-08: kết nối thành công thì ConnectUseCase đã lưu config ngay (chế độ thiết bị); rồi mới hỏi thiết lập PIN (KN-13).
            when (val result = connect(config)) {
                is AppResult.Success -> {
                    log.i { "Kết nối thành công, đã lưu config" }
                    setState { copy(isConnecting = false, showPinPrompt = true) }
                }
                is AppResult.Failure -> {
                    val failure = result.error.toConnectFailure()
                    log.w { "Kết nối thất bại: ${failure.kind} code=${failure.code}" }
                    // KN-09: giữ nguyên giá trị đã nhập để sửa; không lưu config.
                    setState { copy(isConnecting = false, failure = failure) }
                }
            }
        }
    }
}
