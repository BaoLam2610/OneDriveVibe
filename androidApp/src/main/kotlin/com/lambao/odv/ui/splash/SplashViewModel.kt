package com.lambao.odv.ui.splash

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.ConfigRepository
import kotlinx.coroutines.launch

sealed interface SplashEffect {
    /** Chưa có config: sang màn Kết nối. */
    data object NavigateToConnect : SplashEffect

    /** Đã có config: vào Danh sách. */
    data object NavigateToHome : SplashEffect
}

/**
 * Quyết định màn đầu tiên theo luồng tổng thể (đặc tả mục 2). Màn Splash không có State hay Intent.
 * Lát 2 thêm nhánh "có config + bảo mật BẬT → màn Khóa".
 */
class SplashViewModel(
    private val configs: ConfigRepository,
) : BaseMviViewModel<Unit, Unit, SplashEffect>(Unit) {

    init {
        viewModelScope.launch {
            val destination = when {
                !configs.hasConfig() -> SplashEffect.NavigateToConnect
                // Config còn nhưng không giải mã được (khóa Keystore mất, tệp hỏng, hoặc lỗi đọc tạm thời): sang Kết nối
                // thay vì kẹt ở Danh sách với lỗi tải vĩnh viễn. KHÔNG xóa config ở đây: lỗi tạm thời không được làm mất
                // dữ liệu; kết nối lại sẽ ghi đè config cũ khi hoàn tất bước bảo mật. Lát 2 (bảo mật BẬT) đổi bước này thành màn Khóa.
                configs.load() is AppResult.Failure -> SplashEffect.NavigateToConnect
                else -> SplashEffect.NavigateToHome
            }
            sendEffect(destination)
        }
    }

    override fun onIntent(intent: Unit) = Unit
}
