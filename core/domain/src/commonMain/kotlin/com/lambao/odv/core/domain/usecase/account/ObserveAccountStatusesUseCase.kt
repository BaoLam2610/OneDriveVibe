package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.domain.model.AccountStatus
import com.lambao.odv.core.domain.model.SyncState
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Tài khoản kèm trạng thái đồng bộ riêng của từng tài khoản, cho hàng tài khoản ở Cài đặt (AC9: "Đồng bộ lúc…", "Đang lập chỉ mục,
 * đã quét N mục", lỗi TA-10) và sheet Chọn tài khoản (dòng lỗi).
 */
class ObserveAccountStatusesUseCase(
    private val accounts: AccountRepository,
    private val sync: SyncRepository,
) {
    operator fun invoke(): Flow<List<AccountStatus>> =
        combine(accounts.observeAccounts(), sync.observeAccountStates()) { list, states ->
            list.map { AccountStatus(it, states[it.id] ?: SyncState()) }
        }
}
