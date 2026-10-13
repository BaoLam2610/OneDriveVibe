package com.lambao.odv.core.domain.usecase.connection

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.flatMap
import com.lambao.odv.core.common.result.getOrElse
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.first

/**
 * Cập nhật Client Secret (CD-04): kiểm tra kết nối bằng secret mới trên cùng Tenant ID, Client ID (như KN-07) với **tài khoản đang
 * xem**, hoặc tài khoản đầu danh sách khi đang ở phạm vi Tất cả (TA-11); thành công mới lưu và áp cho mọi tài khoản, **thất bại thì
 * giữ secret cũ**. Việc xác thực lại bằng PIN (P1) do màn hình làm trước khi gọi. Chế độ bảo mật (thiết
 * bị hay PIN, sinh trắc học) giữ nguyên. [newSecret] đã cắt khoảng trắng (KN-05).
 */
class UpdateClientSecretUseCase(
    private val connection: ConnectionRepository,
    private val configs: ConfigRepository,
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(newSecret: String): AppResult<Unit> {
        val current = configs.load().getOrElse { return AppResult.Failure(it) }
        val scope = accounts.observeScope().first()
        val probe = (current.accounts.firstOrNull { scope is AccountScope.One && it.id == scope.accountId } ?: current.accounts.firstOrNull())
            ?: return AppResult.Failure(AppError.SecureStorage)
        return connection.verifyConnection(current.configFor(probe.upn).copy(clientSecret = newSecret))
            .flatMap { configs.updateClientSecret(newSecret) }
    }
}
