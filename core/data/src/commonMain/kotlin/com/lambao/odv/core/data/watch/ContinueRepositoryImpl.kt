package com.lambao.odv.core.data.watch

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.data.account.ScopedAccounts
import com.lambao.odv.core.data.drive.toDomain
import com.lambao.odv.core.database.ContinueDao
import com.lambao.odv.core.database.ContinueRow
import com.lambao.odv.core.database.ReadingProgressDao
import com.lambao.odv.core.database.WatchProgressDao
import com.lambao.odv.core.domain.model.ContinueEntry
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.repository.ContinueRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Dải "Xem tiếp / Đọc tiếp" (DS-02) từ Room. Phạm vi xem đổi (TA-06) thì truy vấn lại với danh sách tài khoản mới, nên ở Tất cả tài khoản dải
 * gộp mọi tài khoản và ở một tài khoản chỉ có mục của tài khoản đó.
 */
internal class ContinueRepositoryImpl(
    private val dao: ContinueDao,
    private val watchDao: WatchProgressDao,
    private val readingDao: ReadingProgressDao,
    private val scoped: ScopedAccounts,
    private val dispatchers: DispatcherProvider,
) : ContinueRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(enabledKinds: Set<MediaKind>): Flow<List<ContinueEntry>> {
        // Dải chỉ có video và PDF; ảnh không có tiến độ nên không đưa vào truy vấn.
        val kinds = enabledKinds.filter { it == MediaKind.Video || it == MediaKind.Pdf }.map { it.name }
        return scoped.accountIds()
            .flatMapLatest { accountIds ->
                // Truy vấn lấy ứng viên mới nhất trước (không cắt theo tài khoản); cắt "mỗi tài khoản tối đa 10" làm ở đây để không dùng
                // hàm cửa sổ SQL. Một tài khoản thì tổng 10; nhiều tài khoản (Tất cả) thì tổng tối đa 20.
                val total = if (accountIds.size > 1) MAX_ITEMS_ALL_ACCOUNTS else MAX_ITEMS_PER_ACCOUNT
                dao.observe(kinds, accountIds, CANDIDATE_LIMIT).map { rows -> limitPerAccount(rows, total).map { it.toEntry() } }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    /** [rows] đã xếp mới nhất trước: giữ mỗi tài khoản tối đa [MAX_ITEMS_PER_ACCOUNT] mục đầu, rồi tối đa [total] mục đầu của cả danh sách. */
    private fun limitPerAccount(rows: List<ContinueRow>, total: Int): List<ContinueRow> {
        val taken = HashMap<String, Int>()
        val kept = ArrayList<ContinueRow>(total)
        for (row in rows) {
            if (kept.size >= total) break
            val count = taken[row.item.accountId] ?: 0
            if (count >= MAX_ITEMS_PER_ACCOUNT) continue
            taken[row.item.accountId] = count + 1
            kept += row
        }
        return kept
    }

    // Id là duy nhất giữa hai bảng nên chỉ một bên có dòng; gọi cả hai để không phải biết loại tệp.
    override suspend fun dismiss(itemId: String) = withContext(dispatchers.io) {
        watchDao.dismiss(itemId)
        readingDao.dismiss(itemId)
    }

    private companion object {
        /** DS-02: mỗi tài khoản tối đa 10 mục. */
        const val MAX_ITEMS_PER_ACCOUNT = 10

        /** DS-02: tổng tối đa của phạm vi Tất cả tài khoản (chốt 2026-10-13): vừa đủ 10 mục cho hai tài khoản, thêm tài khoản thì mục cũ nhất rơi ra. */
        const val MAX_ITEMS_ALL_ACCOUNTS = 20

        /** Số ứng viên đọc từ Room trước khi cắt theo tài khoản; dư để tài khoản có lịch sử cũ vẫn đủ mục khi tài khoản khác xem nhiều hơn. */
        const val CANDIDATE_LIMIT = 200
    }
}

private fun ContinueRow.toEntry(): ContinueEntry {
    val domainItem = item.toDomain()
    return if (item.mediaKind == MediaKind.Pdf.name) {
        ContinueEntry.Pdf(domainItem, item.parentId, page = progressValue.toInt(), pageCount = progressTotal.toInt())
    } else {
        ContinueEntry.Video(domainItem, item.parentId, positionMs = progressValue, durationMs = progressTotal)
    }
}
