package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase

/**
 * Mở khóa bằng PIN (KH-01). Đạt ngưỡng sai của CD-08 thì repository đã xóa phần bảo mật và config; ở đây chạy tiếp
 * [DisconnectUseCase] (idempotent) để dữ liệu còn lại của app (Room, cache, cài đặt) cũng được xóa trước khi trả [UnlockResult.Wiped]
 * (KH-06). [pin] do người gọi sở hữu và tự xóa sau khi dùng.
 */
class UnlockWithPinUseCase(
    private val security: SecurityRepository,
    private val disconnect: DisconnectUseCase,
) {
    suspend operator fun invoke(pin: CharArray): UnlockResult {
        val result = security.unlockWithPin(pin)
        if (result == UnlockResult.Wiped) disconnect()
        return result
    }
}
