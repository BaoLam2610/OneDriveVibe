package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase

/**
 * Kiểm tra PIN hiện tại trước một thao tác nhạy cảm ở Cài đặt (CD-03, CD-04, CD-08, CD-09) mà không đổi trạng thái khóa. Dùng chung
 * bộ đếm sai với màn Khóa, nên sai nhiều lần cũng bị chờ (KH-02) hoặc bị xóa dữ liệu khi bật CD-08 (KH-06): khi đó chạy tiếp
 * [DisconnectUseCase] (idempotent) để phần còn lại của app cũng được xóa, như [UnlockWithPinUseCase]. [pin] do người gọi sở hữu và tự xóa.
 */
class VerifyPinUseCase(
    private val security: SecurityRepository,
    private val disconnect: DisconnectUseCase,
) {
    suspend operator fun invoke(pin: CharArray): UnlockResult {
        val result = security.verifyPin(pin)
        if (result == UnlockResult.Wiped) disconnect()
        return result
    }
}
