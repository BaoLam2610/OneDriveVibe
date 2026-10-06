package com.lambao.odv.core.data.config

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.map
import com.lambao.odv.core.data.drive.toCredentials
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.network.auth.GraphCredentials
import com.lambao.odv.core.network.graph.GraphCredentialsSource

/**
 * Cấp credentials cho `GraphApi` từ config đã lưu. Nơi duy nhất trong data đổi lỗi đọc config (kể cả `AppError.AppLocked`
 * khi app đang khóa, CH-03) thành kết quả của một lời gọi Graph, nên các repository không phải tự load config nữa.
 */
internal class ConfigCredentialsSource(
    private val configs: ConfigRepository,
) : GraphCredentialsSource {

    override suspend fun current(): AppResult<GraphCredentials> = configs.load().map { it.toCredentials() }
}
