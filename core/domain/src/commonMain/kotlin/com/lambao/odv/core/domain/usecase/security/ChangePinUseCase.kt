package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase

/**
 * Đổi mã PIN (CD-09): kiểm tra [oldPin] rồi mã hóa lại config bằng [newPin], ghi tệp tạm rồi thay nên lỗi giữa chừng không làm mất
 * config. Mở khóa sinh trắc học bị tắt sau khi đổi (khóa dẫn xuất mới); người dùng bật lại ở Cài đặt. Cả hai PIN do người gọi sở hữu
 * và tự xóa. Sai quá nhiều thì xử lý như [VerifyPinUseCase].
 */
class ChangePinUseCase(
    private val security: SecurityRepository,
    private val disconnect: DisconnectUseCase,
) {
    suspend operator fun invoke(oldPin: CharArray, newPin: CharArray): UnlockResult {
        val result = security.changePin(oldPin, newPin)
        if (result == UnlockResult.Wiped) disconnect()
        return result
    }
}
