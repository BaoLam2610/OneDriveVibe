package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AddAccountOutcome
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.repository.ConnectionRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first

/**
 * Thêm tài khoản (TA-07): kiểm tra trùng UPN ngay (không gọi mạng), kiểm tra truy cập bằng Tenant ID, Client ID, Client Secret đã lưu
 * (cùng bước 2 của KN-07), rồi lưu, **chuyển phạm vi xem sang tài khoản mới** và đưa nó lên đầu hàng đợi đồng bộ để quét lần đầu.
 * Không hỏi PIN và không hiện hộp thoại K6. Thất bại thì không lưu gì và UI giữ giá trị đã nhập.
 *
 * Trùng cũng được kiểm tra lần nữa theo drive id sau khi gọi Graph: hai UPN khác nhau (bí danh) có thể trỏ cùng một drive.
 */
class AddAccountUseCase(
    private val connection: ConnectionRepository,
    private val accounts: AccountRepository,
    private val sync: SyncRepository,
) {
    suspend operator fun invoke(upn: String): AddAccountOutcome {
        val normalized = upn.trim()
        if (accounts.observeAccounts().first().any { it.upn.equals(normalized, ignoreCase = true) }) {
            return AddAccountOutcome.AlreadyAdded
        }
        val account = when (val verified = connection.verifyAccount(normalized)) {
            is AppResult.Success -> verified.value
            is AppResult.Failure -> return AddAccountOutcome.Failed(verified.error)
        }
        val outcome = accounts.add(account)
        if (outcome is AddAccountOutcome.Added) {
            accounts.setScope(AccountScope.One(account.id))
            sync.prioritize(account.id)
        }
        return outcome
    }
}
