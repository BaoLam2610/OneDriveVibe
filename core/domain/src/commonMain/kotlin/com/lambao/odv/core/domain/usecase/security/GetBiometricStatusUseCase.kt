package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.BiometricStatus
import com.lambao.odv.core.domain.repository.SecurityRepository

/** Máy có sinh trắc học mạnh dùng được và app đã bật chưa: Cài đặt dùng để hiện hay ẩn công tắc "Mở khóa bằng sinh trắc học" (CD-02). */
class GetBiometricStatusUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke() = BiometricStatus(
        available = security.isBiometricAvailable(),
        enabled = security.isBiometricEnabled(),
    )
}
