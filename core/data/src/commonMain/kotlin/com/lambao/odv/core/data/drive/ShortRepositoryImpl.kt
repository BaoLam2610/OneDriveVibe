package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.data.account.ScopedAccounts
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.domain.repository.ShortRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class ShortRepositoryImpl(
    private val dao: DriveDao,
    private val dispatchers: DispatcherProvider,
    private val scoped: ScopedAccounts,
) : ShortRepository {

    // Chỉ các tài khoản trong phạm vi xem đã quét xong lần đầu (SV-01, TA-05).
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeVideoIds(maxDurationMs: Long): Flow<List<String>> =
        scoped.readyAccountIds()
            .flatMapLatest { accountIds -> dao.observeShortVideoIds(maxDurationMs, accountIds) }
            .conflate()
            .distinctUntilChanged()
            .flowOn(dispatchers.default)

    override suspend fun getVideo(id: String): ShortVideo? = withContext(dispatchers.io) {
        val entity = dao.getItem(id) ?: return@withContext null
        val folderName = entity.parentId?.let { parent -> dao.refs(listOf(parent)).firstOrNull()?.name }
        ShortVideo(item = entity.toDomain(), folderName = folderName)
    }
}
