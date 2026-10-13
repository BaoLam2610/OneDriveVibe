package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

/** Phạm vi xem đang chọn, đã đưa về giá trị hợp lệ (TA-04). Màn nào đọc theo phạm vi thì bỏ trạng thái cũ khi giá trị này đổi (TA-06). */
class ObserveAccountScopeUseCase(
    private val accounts: AccountRepository,
) {
    operator fun invoke(): Flow<AccountScope> = accounts.observeScope()
}
