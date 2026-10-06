package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Trạng thái khóa của app. Trả đúng [StateFlow] của repository, **không bọc lại**: cổng khóa trong `ODVNavDisplay` đọc `.value`
 * đồng bộ và thu bằng `collectAsState` (không gắn vòng đời) để không lộ khung hình cũ khi mở lại sau lúc bị khóa.
 */
class ObserveLockStateUseCase(
    private val security: SecurityRepository,
) {
    operator fun invoke(): StateFlow<LockState> = security.lockState
}
