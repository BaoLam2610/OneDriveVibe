package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.repository.SyncRepository

/**
 * Chọn phạm vi xem (TA-04): nhớ lựa chọn, rồi đưa tài khoản được chọn lên đầu hàng đợi đồng bộ (TA-06 bước 5). Không hỏi PIN vì
 * app đã mở khóa. Phạm vi Tất cả không cần ưu tiên tài khoản nào: các tài khoản đã được đồng bộ lần lượt như thường.
 */
class SelectAccountScopeUseCase(
    private val accounts: AccountRepository,
    private val sync: SyncRepository,
) {
    suspend operator fun invoke(scope: AccountScope) {
        accounts.setScope(scope)
        if (scope is AccountScope.One) sync.prioritize(scope.accountId)
    }
}
