package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.domain.repository.ShortRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class ShortRepositoryImpl(
    private val dao: DriveDao,
    private val dispatchers: DispatcherProvider,
) : ShortRepository {

    override fun observeVideoIds(maxDurationMs: Long): Flow<List<String>> =
        dao.observeShortVideoIds(maxDurationMs)
            .conflate()
            .distinctUntilChanged()
            .flowOn(dispatchers.default)

    override suspend fun getVideo(id: String): ShortVideo? = withContext(dispatchers.io) {
        val entity = dao.getItem(id) ?: return@withContext null
        val folderName = entity.parentId?.let { parent -> dao.refs(listOf(parent)).firstOrNull()?.name }
        ShortVideo(item = entity.toDomain(), folderName = folderName)
    }
}
