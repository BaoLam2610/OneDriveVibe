package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.repository.SecurityRepository

/** Tắt mở khóa bằng sinh trắc học (CD-02). PIN vẫn dùng được; không cần xác nhận. */
class DisableBiometricUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke() = security.disableBiometric()
}
