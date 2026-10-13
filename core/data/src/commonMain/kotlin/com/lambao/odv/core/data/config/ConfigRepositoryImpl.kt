package com.lambao.odv.core.data.config

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.data.security.LockoutStore
import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.ConnectionProfile
import com.lambao.odv.core.domain.repository.ConfigRepository

/**
 * Lấy và lưu config. Việc mã hóa, cache và chế độ PIN nằm ở [ConfigVault]; phần khóa/mở khóa ở `SecurityRepositoryImpl`
 * (ADR-0014). Lớp này luôn lưu ở chế độ thiết bị (KN-08).
 */
internal class ConfigRepositoryImpl(
    private val vault: ConfigVault,
    private val lockout: LockoutStore,
) : ConfigRepository {

    override suspend fun hasConfig(): Boolean = vault.exists()

    override suspend fun save(config: ConnectionConfig, account: Account): AppResult<Unit> {
        val profile = ConnectionProfile(config.tenantId, config.clientId, config.clientSecret, listOf(account))
        val result = vault.saveDevice(profile)
        // Kết nối mới thay hẳn config cũ nên bộ đếm sai của PIN cũ không còn ý nghĩa.
        if (result is AppResult.Success) lockout.clear()
        return result
    }

    override suspend fun load(): AppResult<ConnectionProfile> = vault.load()

    override suspend fun updateClientSecret(secret: String): AppResult<Unit> =
        vault.update { it.copy(clientSecret = secret) }
}
