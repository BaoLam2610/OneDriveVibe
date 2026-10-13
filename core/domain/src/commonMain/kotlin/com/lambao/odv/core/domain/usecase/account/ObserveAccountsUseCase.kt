package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

/** Danh sách tài khoản theo thứ tự thêm (TA-01): nguồn cho avatar, sheet Chọn tài khoản và nhóm Tài khoản ở Cài đặt. */
class ObserveAccountsUseCase(
    private val accounts: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = accounts.observeAccounts()
}
