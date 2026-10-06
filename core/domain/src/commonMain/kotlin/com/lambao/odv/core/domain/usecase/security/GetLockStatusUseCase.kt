package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.LockStatus
import com.lambao.odv.core.domain.repository.SecurityRepository

/** Đọc một lượt những gì màn Khóa cần hiện: thời gian chờ, số lần sai nữa thì xóa (KH-06), sinh trắc học đã bật chưa. */
class GetLockStatusUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke() = LockStatus(
        lockoutRemainingMs = security.lockoutRemainingMs(),
        attemptsBeforeWipe = security.attemptsBeforeWipe(),
        biometricEnabled = security.isBiometricEnabled(),
    )
}
