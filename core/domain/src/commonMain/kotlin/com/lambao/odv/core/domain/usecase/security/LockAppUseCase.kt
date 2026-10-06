package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.repository.SecurityRepository

/**
 * Khóa app (CH-03): xóa config đã giải mã, khóa phiên và token khỏi bộ nhớ. Chỉ có tác dụng ở chế độ PIN. **Không suspend**
 * vì gọi đồng bộ ngay lúc app xuống nền, không được chờ gì.
 */
class LockAppUseCase(
    private val security: SecurityRepository,
) {
    operator fun invoke() = security.lock()
}
