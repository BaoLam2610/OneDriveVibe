package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Bảo mật đang BẬT (config ở chế độ PIN), kể cả khi đã mở khóa: để ẩn nội dung ở danh sách app gần đây ngay từ lúc bật PIN
 * (CH-05, ADR-0014).
 */
class ObserveProtectionUseCase(
    private val security: SecurityRepository,
) {
    operator fun invoke(): StateFlow<Boolean> = security.isProtected
}
