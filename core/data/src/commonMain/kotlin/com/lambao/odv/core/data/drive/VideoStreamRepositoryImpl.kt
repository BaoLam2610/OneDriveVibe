package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.VideoStreamRepository
import com.lambao.odv.core.network.GraphApi

internal class VideoStreamRepositoryImpl(
    private val api: GraphApi,
    private val configs: ConfigRepository,
) : VideoStreamRepository {

    override suspend fun streamUrl(itemId: String): AppResult<String> {
        val config = when (val loaded = configs.load()) {
            is AppResult.Success -> loaded.value
            is AppResult.Failure -> return loaded
        }
        return api.getDownloadUrl(config.toCredentials(), itemId)
    }
}
