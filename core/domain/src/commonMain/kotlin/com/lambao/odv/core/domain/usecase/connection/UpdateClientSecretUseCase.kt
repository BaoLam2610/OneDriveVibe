package com.lambao.odv.core.domain.usecase.connection

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.flatMap
import com.lambao.odv.core.common.result.getOrElse
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.ConnectionRepository

/**
 * Cập nhật Client Secret (CD-04): kiểm tra kết nối bằng secret mới trên cùng Tenant ID, Client ID, UPN (như KN-07); thành công
 * mới lưu, **thất bại thì giữ secret cũ**. Việc xác thực lại bằng PIN (P1) do màn hình làm trước khi gọi. Chế độ bảo mật (thiết
 * bị hay PIN, sinh trắc học) giữ nguyên. [newSecret] đã cắt khoảng trắng (KN-05).
 */
class UpdateClientSecretUseCase(
    private val connection: ConnectionRepository,
    private val configs: ConfigRepository,
) {
    suspend operator fun invoke(newSecret: String): AppResult<Unit> {
        val current = configs.load().getOrElse { return AppResult.Failure(it) }
        return connection.verifyConnection(current.copy(clientSecret = newSecret))
            .flatMap { configs.updateClientSecret(newSecret) }
    }
}
