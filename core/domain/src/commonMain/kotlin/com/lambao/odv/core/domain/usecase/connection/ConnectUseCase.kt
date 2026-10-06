package com.lambao.odv.core.domain.usecase.connection

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.flatMap
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.ConnectionRepository

/**
 * Kết nối OneDrive bằng [ConnectionConfig] người dùng vừa nhập: kiểm tra kết nối (KN-07), thành công thì lưu config ngay ở
 * chế độ thiết bị, trước cả hộp thoại hỏi thiết lập PIN (KN-08, KN-13). Thất bại thì không lưu gì (KN-09).
 */
class ConnectUseCase(
    private val connection: ConnectionRepository,
    private val configs: ConfigRepository,
) {
    suspend operator fun invoke(config: ConnectionConfig): AppResult<Unit> =
        connection.verifyConnection(config).flatMap { configs.save(config) }
}
