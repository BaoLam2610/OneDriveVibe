package com.lambao.odv.core.data.config

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.network.TokenProvider
import com.lambao.odv.core.security.SecretStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val CONFIG_SECRET = "connection_config"

/**
 * Config dạng lưu: JSON rồi mã hóa AES-GCM bằng khóa Keystore qua [SecretStore] (CH-01, chế độ thiết bị: config được lưu ở chế độ này ngay sau khi kết nối, KN-08).
 * Lát 2 thêm khóa dẫn xuất từ PIN (ADR-0008). `toString` che toàn bộ vì có Client Secret (CH-06).
 */
@Serializable
private class StoredConfig(
    val tenantId: String,
    val clientId: String,
    val clientSecret: String,
    val upn: String,
) {
    override fun toString(): String = "StoredConfig(***)"
}

internal class ConfigRepositoryImpl(
    private val secrets: SecretStore,
    private val tokens: TokenProvider,
) : ConfigRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    /** Bản đã giải mã trong bộ nhớ. Phải xóa khi khóa app hoặc ngắt kết nối (CH-03, Lát 2). */
    private var cache: ConnectionConfig? = null

    override suspend fun hasConfig(): Boolean = secrets.exists(CONFIG_SECRET)

    override suspend fun save(config: ConnectionConfig): AppResult<Unit> = mutex.withLock {
        val bytes = json.encodeToString(StoredConfig.serializer(), config.toStored()).encodeToByteArray()
        when (val result = secrets.write(CONFIG_SECRET, bytes)) {
            is AppResult.Success -> {
                cache = config
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun load(): AppResult<ConnectionConfig> = mutex.withLock {
        cache?.let { return@withLock AppResult.Success(it) }
        when (val result = secrets.read(CONFIG_SECRET)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val bytes = result.value ?: return@withLock AppResult.Failure(AppError.SecureStorage)
                try {
                    val config = json.decodeFromString(StoredConfig.serializer(), bytes.decodeToString()).toDomain()
                    cache = config
                    AppResult.Success(config)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppResult.Failure(AppError.SecureStorage)
                }
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            secrets.delete(CONFIG_SECRET)
            cache = null
        }
        tokens.clear()
    }

    private fun ConnectionConfig.toStored() = StoredConfig(tenantId, clientId, clientSecret, upn)

    private fun StoredConfig.toDomain() = ConnectionConfig(tenantId, clientId, clientSecret, upn)
}
