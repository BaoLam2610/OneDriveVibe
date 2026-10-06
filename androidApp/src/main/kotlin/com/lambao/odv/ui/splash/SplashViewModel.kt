package com.lambao.odv.ui.splash

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.StartDestination
import com.lambao.odv.core.domain.usecase.connection.ResolveStartDestinationUseCase
import kotlinx.coroutines.launch

sealed interface SplashEffect {
    /** Chưa có config, hoặc config không dùng được: sang màn Kết nối. */
    data object NavigateToConnect : SplashEffect

    /** Đã có config: vào Danh sách. */
    data object NavigateToHome : SplashEffect
}

/**
 * Quyết định màn đầu tiên theo luồng tổng thể (đặc tả mục 2). Màn Splash không có State hay Intent; nghiệp vụ (chờ mở khóa, kiểm
 * tra config) nằm ở [ResolveStartDestinationUseCase].
 */
class SplashViewModel(
    private val resolveStartDestination: ResolveStartDestinationUseCase,
) : BaseMviViewModel<Unit, Unit, SplashEffect>(Unit) {

    init {
        viewModelScope.launch {
            val effect = when (resolveStartDestination()) {
                StartDestination.Connect -> SplashEffect.NavigateToConnect
                StartDestination.Home -> SplashEffect.NavigateToHome
            }
            sendEffect(effect)
        }
    }

    override fun onIntent(intent: Unit) = Unit
}
