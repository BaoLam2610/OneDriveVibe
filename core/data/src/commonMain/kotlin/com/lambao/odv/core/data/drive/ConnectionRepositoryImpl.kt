package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.map
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.repository.ConnectionRepository
import com.lambao.odv.core.network.graph.GraphApi

internal class ConnectionRepositoryImpl(
    private val api: GraphApi,
) : ConnectionRepository {

    override suspend fun verifyConnection(config: ConnectionConfig): AppResult<Unit> =
        api.getDrive(config.toCredentials()).map { }
}
