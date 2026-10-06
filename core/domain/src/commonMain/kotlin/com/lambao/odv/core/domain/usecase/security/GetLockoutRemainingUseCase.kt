package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.repository.SecurityRepository

/** Thời gian còn bị khóa nhập (KH-02), 0 nếu không. Tách riêng khỏi [GetLockStatusUseCase] vì đồng hồ chờ hỏi lại mỗi giây. */
class GetLockoutRemainingUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke(): Long = security.lockoutRemainingMs()
}
