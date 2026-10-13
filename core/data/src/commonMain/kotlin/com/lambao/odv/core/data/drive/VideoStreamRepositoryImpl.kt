package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.getOrElse
import com.lambao.odv.core.data.account.AccountDirectory
import com.lambao.odv.core.domain.repository.VideoStreamRepository
import com.lambao.odv.core.network.graph.GraphApi

internal class VideoStreamRepositoryImpl(
    private val api: GraphApi,
    private val directory: AccountDirectory,
) : VideoStreamRepository {

    override suspend fun streamUrl(itemId: String): AppResult<String> {
        val target = directory.resolve(itemId).getOrElse { return AppResult.Failure(it) }
        return api.getDownloadUrl(target.upn, target.graphId)
    }
}
