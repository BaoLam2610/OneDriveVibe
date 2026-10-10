package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.usecase.DisconnectUseCase

/**
 * Tắt bảo vệ ứng dụng (CD-03): kiểm tra [pin] rồi mã hóa lại config ở chế độ thiết bị. Thành công thì tắt luôn "Xóa dữ liệu khi nhập
 * sai quá nhiều" (CD-08), vì tùy chọn đó chỉ có nghĩa khi đang có PIN và không được tự bật lại âm thầm khi người dùng bật PIN lần sau.
 * Sai quá nhiều thì xử lý như [VerifyPinUseCase]. [pin] do người gọi sở hữu và tự xóa.
 */
class DisableProtectionUseCase(
    private val security: SecurityRepository,
    private val settings: SecuritySettings,
    private val disconnect: DisconnectUseCase,
) {
    suspend operator fun invoke(pin: CharArray): UnlockResult {
        val result = security.disableProtection(pin)
        when (result) {
            UnlockResult.Success -> settings.setWipeOnTooManyFailures(false)
            UnlockResult.Wiped -> disconnect()
            else -> Unit
        }
        return result
    }
}
