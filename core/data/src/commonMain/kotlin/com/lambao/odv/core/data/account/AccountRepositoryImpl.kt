package com.lambao.odv.core.data.account

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.data.config.ConfigVault
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.hook.AccountDataCleaner
import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AddAccountOutcome
import com.lambao.odv.core.domain.model.RemoveAccountOutcome
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.settings.BrowserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Danh sách tài khoản nằm trong config đã mã hóa ([ConfigVault], CH-01), phạm vi xem nằm ở tùy chọn hiển thị
 * ([BrowserPreferences]). Thêm, xóa, cập nhật đều đi qua [ConfigVault.update] nên không ghi đè lẫn nhau và không làm lệch chế độ
 * bảo mật (ADR-0014): ở chế độ PIN config được mã hóa lại bằng khóa phiên đang giữ (TA-07, không hỏi PIN).
 *
 * [cleaners] là lambda (không phải danh sách) để Koin không vòng phụ thuộc: bản cài của việc đồng bộ cần chính [AccountRepository].
 */
internal class AccountRepositoryImpl(
    private val vault: ConfigVault,
    private val prefs: BrowserPreferences,
    private val dao: DriveDao,
    private val cleaners: () -> List<AccountDataCleaner>,
) : AccountRepository {

    private val log = Logger.withTag("Account")

    override fun observeAccounts(): Flow<List<Account>> = flow {
        // Đọc config nếu chưa có trong bộ nhớ (vd. màn được dựng lại sau khi hệ điều hành thu hồi tiến trình). Đang khóa thì lỗi AppLocked.
        vault.load()
        // Bỏ các lần phát null: khóa app xóa config khỏi bộ nhớ (CH-03) nhưng **không được** làm danh sách tài khoản và phạm vi xem "đổi", nếu
        // không mọi tab bị bỏ trạng thái mỗi lần khóa, trái KH-07 và DH-02. Hệ quả: khi đang khóa luồng giữ giá trị cũ và không phát gì tới khi mở
        // khóa (vault nạp lại config); những nơi chờ giá trị đầu (vd. `first()` của đồng bộ) tạm treo, vô hại vì đồng bộ vốn dừng khi khóa.
        emitAll(vault.profile.filterNotNull().map { it.accounts }.distinctUntilChanged())
    }

    override fun observeScope(): Flow<AccountScope> =
        combine(observeAccounts(), prefs.accountScope) { accounts, stored -> resolve(accounts, stored) }.distinctUntilChanged()

    /** Đưa phạm vi đã lưu về giá trị hợp lệ (TA-04): xem [AccountRepository.observeScope]. */
    private fun resolve(accounts: List<Account>, stored: AccountScope?): AccountScope = when {
        accounts.isEmpty() -> AccountScope.All
        stored is AccountScope.All && accounts.size >= 2 -> AccountScope.All
        stored is AccountScope.One && accounts.any { it.id == stored.accountId } -> stored
        else -> AccountScope.One(accounts.first().id)
    }

    override suspend fun setScope(scope: AccountScope) = prefs.setAccountScope(scope)

    override suspend fun add(account: Account): AddAccountOutcome {
        var duplicate = false
        val result = vault.update { profile ->
            if (profile.accounts.any { it.id == account.id || it.upn.equals(account.upn, ignoreCase = true) }) {
                duplicate = true
                profile
            } else {
                profile.copy(accounts = profile.accounts + account)
            }
        }
        return when {
            duplicate -> AddAccountOutcome.AlreadyAdded
            result is AppResult.Failure -> AddAccountOutcome.Failed(result.error)
            else -> AddAccountOutcome.Added(account)
        }
    }

    override suspend fun remove(accountId: String): RemoveAccountOutcome {
        val accounts = observeAccounts().first()
        if (accounts.size <= 1 || accounts.none { it.id == accountId }) return RemoveAccountOutcome.NotAllowed
        val scope = observeScope().first()
        if (scope is AccountScope.One && scope.accountId == accountId) return RemoveAccountOutcome.NotAllowed

        // Gỡ khỏi config trước: từ đó không ai bắt đầu việc mới cho tài khoản này; rồi các kho dọn dữ liệu của mình (dừng việc đang dở
        // rồi xóa). Config lỗi thì chưa xóa gì cả. Kiểm tra lại "không phải tài khoản duy nhất" ngay trong lambda (nguyên tử) để hai
        // lần xóa đồng thời không xóa hết tài khoản (review Lát T D-L1).
        var notAllowed = false
        val removed = vault.update { profile ->
            if (profile.accounts.size <= 1 || profile.accounts.none { it.id == accountId }) {
                notAllowed = true
                profile
            } else {
                profile.copy(accounts = profile.accounts.filterNot { it.id == accountId })
            }
        }
        if (notAllowed) return RemoveAccountOutcome.NotAllowed
        if (removed is AppResult.Failure) return RemoveAccountOutcome.Failed(removed.error)
        // Config đã đổi nên việc dọn phải chạy hết dù người gọi bị hủy (vd. rời màn), kẻo bỏ lại dữ liệu mồ côi (review Lát T D-M1).
        withContext(NonCancellable) { purgeAll(accountId) }
        return RemoveAccountOutcome.Removed
    }

    /** Chạy mọi [AccountDataCleaner] cho [accountId]; mỗi bản có try/catch riêng nên một bản lỗi không chặn các bản còn lại. */
    private suspend fun purgeAll(accountId: String) {
        for (cleaner in cleaners()) {
            try {
                cleaner.purge(accountId)
            } catch (e: Exception) {
                // Chỉ ghi tên loại ngoại lệ (CH-06).
                log.w { "Không dọn được dữ liệu của tài khoản đã xóa: ${e::class.simpleName}" }
            }
        }
    }

    override suspend fun purgeOrphanedData() {
        try {
            val known = vault.profile.value?.accounts?.map { it.id }?.toSet()
            // Chưa đọc được config (đang khóa) hoặc không có tài khoản: không đủ căn cứ để coi dữ liệu nào là mồ côi.
            if (known.isNullOrEmpty()) return
            val orphans = dao.getAccountIdsWithData().filter { it !in known }
            if (orphans.isEmpty()) return
            log.i { "Dọn dữ liệu mồ côi của ${orphans.size} tài khoản đã gỡ" }
            withContext(NonCancellable) { orphans.forEach { purgeAll(it) } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w { "Không dọn được dữ liệu mồ côi: ${e::class.simpleName}" }
        }
    }

    override suspend fun updateDetails(account: Account) {
        val current = vault.profile.value?.accounts?.firstOrNull { it.id == account.id } ?: return
        if (current == account) return
        vault.update { profile ->
            profile.copy(accounts = profile.accounts.map { if (it.id == account.id) account else it })
        }
    }
}
