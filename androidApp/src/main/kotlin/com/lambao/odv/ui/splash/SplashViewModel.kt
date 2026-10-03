package com.lambao.odv.ui.splash

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashEffect {
    /** Chưa có config: sang màn Kết nối. */
    data object NavigateToConnect : SplashEffect

    /** Đã có config: vào Danh sách. */
    data object NavigateToHome : SplashEffect
}

/**
 * Quyết định màn đầu tiên theo luồng tổng thể (đặc tả mục 2). Màn Splash không có State hay Intent.
 *
 * Khi bảo mật BẬT, config chưa đọc được cho tới khi nhập đúng PIN (`load()` trả `AppLocked`). Nếu quyết định ngay lúc đó thì
 * sẽ nhầm sang màn Kết nối. Vì vậy chờ [SecurityRepository.lockState] về Unlocked (đã biết chế độ và đã mở khóa) rồi mới
 * quyết định; màn Khóa do cổng trong `ODVNavDisplay` đẩy lên (ADR-0014).
 */
class SplashViewModel(
    private val configs: ConfigRepository,
    private val security: SecurityRepository,
) : BaseMviViewModel<Unit, Unit, SplashEffect>(Unit) {

    init {
        viewModelScope.launch {
            security.initialize()
            security.lockState.first { it == LockState.Unlocked }
            val destination = when {
                !configs.hasConfig() -> SplashEffect.NavigateToConnect
                // Config còn nhưng không giải mã được (khóa Keystore mất, tệp hỏng, hoặc lỗi đọc tạm thời): sang Kết nối
                // thay vì kẹt ở Danh sách với lỗi tải vĩnh viễn. KHÔNG xóa config ở đây: lỗi tạm thời không được làm mất
                // dữ liệu; kết nối lại sẽ ghi đè config cũ ngay khi kết nối thành công (KN-08). Đóng app khi hộp thoại KN-13 đang hiện
                // thì lần mở sau vào thẳng Danh sách ở chế độ thiết bị (config đã lưu).
                configs.load() is AppResult.Failure -> SplashEffect.NavigateToConnect
                else -> SplashEffect.NavigateToHome
            }
            sendEffect(destination)
        }
    }

    override fun onIntent(intent: Unit) = Unit
}
