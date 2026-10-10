package com.lambao.odv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryNoticeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Thông tin cho banner "Client Secret sắp hết hạn" ở đầu màn Danh sách (CD-06, Q4). Chỉ đọc hai luồng nên không cần Intent hay Effect:
 * [notice] có giá trị thì hiện banner; [protectionEnabled] cho biết nút "Cập nhật" phải qua bước nhập PIN (CD-04) hay vào thẳng form.
 */
class HomeBannerViewModel(
    observeNotice: ObserveSecretExpiryNoticeUseCase,
    observeProtection: ObserveProtectionUseCase,
) : ViewModel() {
    val notice: StateFlow<SecretExpiryNotice?> =
        observeNotice().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val protectionEnabled: StateFlow<Boolean> =
        observeProtection().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
