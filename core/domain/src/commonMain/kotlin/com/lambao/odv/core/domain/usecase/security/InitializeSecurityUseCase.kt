package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.repository.SecurityRepository

/** Đọc chế độ bảo mật từ config rồi đặt trạng thái khóa; gọi lúc khởi động, gọi lại thì bỏ qua (ADR-0014). */
class InitializeSecurityUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke() = security.initialize()
}
