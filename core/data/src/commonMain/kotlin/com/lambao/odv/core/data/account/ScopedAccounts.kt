package com.lambao.odv.core.data.account

import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Các tài khoản trong phạm vi xem đang chọn (TA-04, TA-05), dạng danh sách drive id để các truy vấn Room lọc `accountId IN (...)`.
 * Mọi repository đọc dữ liệu dùng lớp này nên tầng domain và các màn không phải biết phạm vi (ADR-0030).
 */
internal class ScopedAccounts(
    private val accounts: AccountRepository,
    private val dao: DriveDao,
) {

    /** Drive id của các tài khoản trong phạm vi. Rỗng khi chưa có tài khoản nào (các truy vấn trả rỗng). */
    fun accountIds(): Flow<List<String>> =
        combine(accounts.observeAccounts(), accounts.observeScope()) { list, scope ->
            when (scope) {
                AccountScope.All -> list.map { it.id }
                is AccountScope.One -> listOf(scope.accountId)
            }
        }.distinctUntilChanged()

    /**
     * Như [accountIds] nhưng chỉ giữ tài khoản **đã quét xong lần đầu** (SV-01): tab Short không lấy video từ tài khoản đang quét dở,
     * vì danh sách xáo một lần (SV-02) sẽ thiếu phần chưa quét tới.
     */
    fun readyAccountIds(): Flow<List<String>> =
        combine(accountIds(), dao.observeSyncStates()) { ids, states ->
            val ready = states.filter { it.initialSyncDone }.mapTo(HashSet()) { it.accountId }
            ids.filter { it in ready }
        }.distinctUntilChanged()
}
