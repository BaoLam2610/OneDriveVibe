package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.VideoStreamRepository
import com.lambao.odv.core.network.graph.GraphApi

internal class VideoStreamRepositoryImpl(
    private val api: GraphApi,
) : VideoStreamRepository {

    override suspend fun streamUrl(itemId: String): AppResult<String> = api.getDownloadUrl(itemId)
}
