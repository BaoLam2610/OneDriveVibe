package com.lambao.odv.core.data.config

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.security.PinEnvelopeCodec
import com.lambao.odv.core.security.SecretStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * Config dạng lưu: JSON. `toString` che toàn bộ vì có Client Secret (CH-06).
 * Ở chế độ thiết bị đây là plaintext của lớp mã hóa Keystore; ở chế độ PIN nó nằm trong phong bì PIN (ADR-0014).
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

/**
 * Giữ tệp config (`connection_config`), bản đã giải mã trong bộ nhớ, khóa phiên và [lockState]. Dùng chung cho
 * `ConfigRepositoryImpl` (lấy config) và `SecurityRepositoryImpl` (khóa, mở khóa, đổi chế độ) để hai bên không lệch
 * nhau về tệp lẫn bộ nhớ (ADR-0014).
 *
 * Chế độ nhận biết bằng byte đầu của plaintext lớp ngoài: `{` là JSON (chế độ thiết bị), `0xA1` là phong bì PIN.
 */
internal class ConfigVault(
    private val secrets: SecretStore,
    private val codec: PinEnvelopeCodec,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    /** Bản đã giải mã trong bộ nhớ. Xóa khi khóa app hoặc ngắt kết nối (CH-03). */
    @Volatile
    private var cache: ConnectionConfig? = null

    /** Khóa dẫn xuất từ PIN của phiên hiện tại (chỉ có ở chế độ PIN khi đã mở khóa). Xóa bằng 0 khi khóa. */
    @Volatile
    private var sessionKey: ByteArray? = null

    /** Config đang ở chế độ PIN. Chỉ đúng sau [initialize]. */
    @Volatile
    var pinMode: Boolean = false
        private set

    private val _lockState = MutableStateFlow(LockState.Unknown)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    /**
     * Thế hệ khóa: [lock] luôn tăng số này **trước** khi xóa gì. Thao tác dùng PIN (chạy Argon2id mất cả giây) đọc số này
     * trước khi bắt đầu và nhờ [adopt] kiểm tra lại sau khi ghi xong: nếu app đã bị khóa giữa chừng (ON_STOP) thì [adopt]
     * hoàn tác thay vì để app nằm nền trong trạng thái đã mở khóa (CH-03).
     */
    private val epoch = MutableStateFlow(0)

    /** Đã đọc được tệp config thành công và chốt chế độ. Lỗi đọc tạm thời thì chưa chốt để [initialize] thử lại. */
    @Volatile
    private var settled = false

    fun currentEpoch(): Int = epoch.value

    suspend fun exists(): Boolean = secrets.exists(NAME)

    /** Đọc tệp config đã qua lớp Keystore: `Success(null)` nếu chưa có. Không đụng tới bộ nhớ hay trạng thái khóa. */
    suspend fun readRaw(): AppResult<ByteArray?> = mutex.withLock { secrets.read(NAME) }

    /** Ghi nguyên tử (tệp tạm rồi thay). Không đụng tới bộ nhớ hay trạng thái khóa. */
    suspend fun writeRaw(bytes: ByteArray): AppResult<Unit> = mutex.withLock { secrets.write(NAME, bytes) }

    /**
     * Đọc chế độ từ tệp rồi đặt [lockState]. Đọc thành công thì chốt và các lần sau bỏ qua. Đọc lỗi (khóa Keystore mất,
     * IO tạm thời) thì tạm đặt Unlocked để Splash đưa người dùng về Kết nối, nhưng **chưa chốt**: lần gọi sau đọc lại,
     * vì nếu tệp thực ra là phong bì PIN thì app phải khóa chứ không được coi là chế độ thiết bị mãi.
     */
    suspend fun initialize() {
        if (settled) return
        when (val result = readRaw()) {
            is AppResult.Failure -> {
                if (_lockState.value == LockState.Unknown) {
                    pinMode = false
                    _lockState.value = LockState.Unlocked
                }
            }
            is AppResult.Success -> {
                if (settled) return
                val bytes = result.value
                if (bytes != null && codec.isEnvelope(bytes)) {
                    pinMode = true
                    _lockState.value = LockState.Locked
                } else {
                    pinMode = false
                    _lockState.value = LockState.Unlocked
                }
                settled = true
            }
        }
    }

    suspend fun load(): AppResult<ConnectionConfig> {
        cache?.let { return AppResult.Success(it) }
        val bytes = when (val result = readRaw()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(AppError.SecureStorage)
        }
        // Phong bì PIN mà chưa có khóa phiên: đang khóa. Không thử giải mã (không có PIN).
        if (codec.isEnvelope(bytes)) return AppResult.Failure(AppError.AppLocked)
        val config = decode(bytes) ?: return AppResult.Failure(AppError.SecureStorage)
        cache = config
        return AppResult.Success(config)
    }

    /** Ghi [config] ở chế độ thiết bị và bỏ mọi thứ của chế độ PIN trong bộ nhớ (kết nối mới, KN-08). */
    suspend fun saveDevice(config: ConnectionConfig): AppResult<Unit> {
        val written = writeRaw(encode(config))
        if (written is AppResult.Success) setDeviceMode(config)
        return written
    }

    /** Sau khi tệp đã ở chế độ thiết bị: giữ [config] trong bộ nhớ, bỏ khóa phiên, mở khóa. */
    fun setDeviceMode(config: ConnectionConfig) {
        cache = config
        discardKey()
        pinMode = false
        settled = true
        _lockState.value = LockState.Unlocked
    }

    /**
     * Sau khi tệp đã ở chế độ PIN và PIN đã được xác nhận: nhận [config] và [key] (vault giữ [key], không sao chép).
     * [expectedEpoch] là [currentEpoch] đọc trước khi bắt đầu thao tác. Nếu app đã bị [lock] từ đó thì hoàn tác, xóa
     * [key] và để Locked; trả về `false`. Phần ghi diễn ra trước, kiểm tra thế hệ sau cùng (xem [epoch]).
     */
    fun adopt(config: ConnectionConfig, key: ByteArray, expectedEpoch: Int): Boolean {
        discardKey()
        sessionKey = key
        cache = config
        pinMode = true
        settled = true
        _lockState.value = LockState.Unlocked
        if (epoch.value == expectedEpoch) return true
        cache = null
        discardKey()
        _lockState.value = LockState.Locked
        return false
    }

    /**
     * CH-03: bỏ config và khóa phiên khỏi bộ nhớ, đặt Locked. Chỉ xóa khi ở chế độ PIN, nhưng **luôn** tăng [epoch] để
     * hủy mọi thao tác PIN đang dở (kể cả bật PIN lần đầu). Trả về có thực sự khóa hay không.
     */
    fun lock(): Boolean {
        epoch.update { it + 1 }
        if (!pinMode) return false
        cache = null
        discardKey()
        _lockState.value = LockState.Locked
        return true
    }

    /** Xóa tệp config và mọi thứ trong bộ nhớ. */
    suspend fun clear() {
        mutex.withLock { secrets.delete(NAME) }
        forget()
    }

    /** Chỉ bỏ phần trong bộ nhớ (tệp đã bị xóa ở nơi khác, ví dụ `SecretStore.wipeAll`). */
    fun forget() {
        epoch.update { it + 1 }
        cache = null
        discardKey()
        pinMode = false
        settled = true
        _lockState.value = LockState.Unlocked
    }

    fun encode(config: ConnectionConfig): ByteArray =
        json.encodeToString(StoredConfig.serializer(), config.toStored()).encodeToByteArray()

    /** Null nếu không phải JSON config hợp lệ. */
    fun decode(bytes: ByteArray): ConnectionConfig? = try {
        json.decodeFromString(StoredConfig.serializer(), bytes.decodeToString()).toDomain()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun discardKey() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    private fun ConnectionConfig.toStored() = StoredConfig(tenantId, clientId, clientSecret, upn)

    private fun StoredConfig.toDomain() = ConnectionConfig(tenantId, clientId, clientSecret, upn)

    companion object {
        /** Tên bí mật của config; cũng là AAD của phong bì PIN. */
        const val NAME = "connection_config"
    }
}
