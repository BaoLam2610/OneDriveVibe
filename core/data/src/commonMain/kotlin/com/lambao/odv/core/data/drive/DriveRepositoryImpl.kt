package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.map
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveInfo
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.network.GraphApi

internal class DriveRepositoryImpl(
    private val api: GraphApi,
    private val configs: ConfigRepository,
) : DriveRepository {

    override suspend fun verifyConnection(config: ConnectionConfig): AppResult<DriveInfo> =
        api.getDrive(config.toCredentials()).map { it.toDomain() }

    override suspend fun listChildren(folderId: String?): AppResult<List<DriveItem>> {
        val config = when (val loaded = configs.load()) {
            is AppResult.Success -> loaded.value
            is AppResult.Failure -> return loaded
        }
        return api.listChildren(config.toCredentials(), folderId).map { items -> items.mapNotNull { it.toDomain() } }
    }
}
