package com.lambao.odv.core.domain.usecase.security

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ProtectionSetupResult
import com.lambao.odv.core.domain.repository.SecurityRepository

/**
 * Bật bảo vệ bằng PIN (BM-02, BM-04). Thành công thì nếu máy dùng được sinh trắc học mạnh, màn Thiết lập hỏi tiếp (B6); lỗi thì
 * config giữ nguyên ở chế độ thiết bị. [pin] do người gọi sở hữu và tự xóa sau khi dùng.
 */
class EnableProtectionUseCase(
    private val security: SecurityRepository,
) {
    suspend operator fun invoke(pin: CharArray): ProtectionSetupResult = when (security.enableProtection(pin)) {
        is AppResult.Success ->
            if (security.isBiometricAvailable()) ProtectionSetupResult.OfferBiometric else ProtectionSetupResult.Done
        is AppResult.Failure -> ProtectionSetupResult.Failed
    }
}
