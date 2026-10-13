package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.domain.model.RemoveAccountOutcome
import com.lambao.odv.core.domain.repository.AccountRepository

/**
 * Xóa tài khoản khỏi app (TA-08). Chỉ xóa dữ liệu trên máy, không đụng gì trên OneDrive. Không xóa được tài khoản đang xem (trừ khi
 * đang ở phạm vi Tất cả) và tài khoản duy nhất; xóa xong còn một tài khoản thì phạm vi tự về tài khoản đó.
 */
class RemoveAccountUseCase(
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(accountId: String): RemoveAccountOutcome = accounts.remove(accountId)
}
