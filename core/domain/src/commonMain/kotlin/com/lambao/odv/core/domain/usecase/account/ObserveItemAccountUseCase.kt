package com.lambao.odv.core.domain.usecase.account

import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.ItemKey
import com.lambao.odv.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Tài khoản chứa một tệp, cho hàng "Tài khoản" ở bảng thông tin tệp (VD-17, AN-05, PD-08; thiet-ke-ui.md mục 4.5). Chỉ có giá trị khi app có từ 2 tài
 * khoản: một tài khoản thì dòng này vô nghĩa nên phát `null` và giao diện ẩn hàng. Tìm theo tiền tố của id mờ ([ItemKey]).
 */
class ObserveItemAccountUseCase(
    private val accounts: AccountRepository,
) {
    operator fun invoke(itemId: String): Flow<Account?> = accounts.observeAccounts().map { list ->
        if (list.size < MIN_ACCOUNTS) null else ItemKey.accountIdOf(itemId)?.let { id -> list.firstOrNull { it.id == id } }
    }.distinctUntilChanged()

    private companion object {
        const val MIN_ACCOUNTS = 2
    }
}
