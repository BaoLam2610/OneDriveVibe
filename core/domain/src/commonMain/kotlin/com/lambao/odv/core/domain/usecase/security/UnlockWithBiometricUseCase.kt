package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase

/**
 * Mở khóa bằng sinh trắc học (KH-01). Hiện sinh trắc học không bao giờ trả [UnlockResult.Wiped] (không tính vào bộ đếm sai), nhưng
 * vẫn đi chung đường xử lý `Wiped` với [UnlockWithPinUseCase] để hai cách mở khóa luôn hành xử giống nhau.
 */
class UnlockWithBiometricUseCase(
    private val security: SecurityRepository,
    private val disconnect: DisconnectUseCase,
) {
    suspend operator fun invoke(): UnlockResult {
        val result = security.unlockWithBiometric()
        if (result == UnlockResult.Wiped) disconnect()
        return result
    }
}
