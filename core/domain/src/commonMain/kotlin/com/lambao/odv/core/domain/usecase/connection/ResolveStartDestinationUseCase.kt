package com.lambao.odv.core.domain.usecase.connection

import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.SavedConnection
import com.lambao.odv.core.domain.model.StartDestination
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.first

/**
 * Quyết định màn đầu tiên theo luồng tổng thể (đặc tả mục 2).
 *
 * Khi bảo mật BẬT, config chưa đọc được cho tới khi nhập đúng PIN (`load()` trả `AppLocked`). Nếu quyết định ngay lúc đó thì
 * sẽ nhầm sang màn Kết nối. Vì vậy chờ `lockState` về Unlocked (đã biết chế độ và đã mở khóa) rồi mới quyết định; màn Khóa do
 * cổng trong `ODVNavDisplay` đẩy lên (ADR-0014).
 */
class ResolveStartDestinationUseCase(
    private val security: SecurityRepository,
    private val checkSavedConnection: CheckSavedConnectionUseCase,
) {
    suspend operator fun invoke(): StartDestination {
        security.initialize()
        security.lockState.first { it == LockState.Unlocked }
        return when (checkSavedConnection()) {
            SavedConnection.Usable -> StartDestination.Home
            SavedConnection.None, SavedConnection.Unusable -> StartDestination.Connect
        }
    }
}
