package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.BiometricOutcome
import com.lambao.odv.core.domain.repository.SecurityRepository

/** Bật mở khóa bằng sinh trắc học (BM-02, CD-02): hiện hộp thoại rồi bọc khóa phiên hiện tại. Chỉ làm được khi đã mở khóa ở chế độ PIN. */
class EnableBiometricUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke(): BiometricOutcome = security.enableBiometric()
}
