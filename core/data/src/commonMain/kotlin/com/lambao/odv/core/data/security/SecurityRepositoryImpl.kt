package com.lambao.odv.core.data.security

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.data.config.ConfigVault
import com.lambao.odv.core.domain.model.BiometricOutcome
import com.lambao.odv.core.domain.model.BiometricUnwrap
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.SecurityMode
import com.lambao.odv.core.domain.model.UnlockResult
import com.lambao.odv.core.domain.repository.BiometricAuthenticator
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.network.TokenProvider
import com.lambao.odv.core.security.EnvelopeOpen
import com.lambao.odv.core.security.PinEnvelopeCodec
import com.lambao.odv.core.security.SecretStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Khóa, mở khóa, đổi chế độ và đếm lần sai (ADR-0008, ADR-0014). Không giữ PIN và không lưu hash PIN (CH-02): PIN đúng
 * hay sai là mở được phong bì PIN hay không.
 *
 * Mọi thao tác dùng PIN chạy tuần tự qua [mutex] nên hai lần nhập không thể cùng chạy Argon2id và cùng ghi bộ đếm.
 * [lock] thì không qua [mutex] (gọi ngay lúc app xuống nền, không được chờ Argon2id); thay vào đó mỗi thao tác đọc
 * `epoch` của [ConfigVault] trước và `adopt` kiểm tra lại, nên khóa xen giữa luôn thắng.
 *
 * @param wipeAfterFailures ngưỡng sai liên tiếp để xóa dữ liệu theo CD-08 (10), `null` nếu tùy chọn đang tắt. Lát 2
 *   chưa có cài đặt nên mặc định tắt; Lát 9 nối với `SettingsRepository`.
 */
internal class SecurityRepositoryImpl(
    private val vault: ConfigVault,
    private val codec: PinEnvelopeCodec,
    private val lockout: LockoutStore,
    private val secrets: SecretStore,
    private val tokens: TokenProvider,
    private val biometric: BiometricAuthenticator,
    private val wipeAfterFailures: () -> Int? = { null },
) : SecurityRepository {

    private val mutex = Mutex()

    override val lockState: StateFlow<LockState> get() = vault.lockState

    override val isProtected: StateFlow<Boolean> get() = vault.protectionEnabled

    override suspend fun initialize() = vault.initialize()

    override suspend fun mode(): SecurityMode {
        vault.initialize()
        return if (vault.pinMode) SecurityMode.PIN else SecurityMode.DEVICE
    }

    override suspend fun enableProtection(pin: CharArray): AppResult<Unit> = mutex.withLock {
        vault.initialize()
        if (vault.pinMode) return@withLock AppResult.Failure(AppError.SecureStorage)
        val epoch = vault.currentEpoch()
        val config = when (val loaded = vault.load()) {
            is AppResult.Success -> loaded.value
            is AppResult.Failure -> return@withLock loaded
        }
        val plain = vault.encode(config)
        val envelope = try {
            codec.seal(pin, plain, ConfigVault.NAME)
        } finally {
            plain.fill(0)
        }
        val sealed = when (envelope) {
            is AppResult.Success -> envelope.value
            is AppResult.Failure -> return@withLock envelope
        }
        // Ghi nguyên tử qua tệp tạm: lỗi giữa chừng thì tệp cũ (chế độ thiết bị) còn nguyên (BM-04).
        val written = vault.writeRaw(sealed.bytes)
        if (written is AppResult.Failure) {
            sealed.key.fill(0)
            return@withLock written
        }
        // Nếu app bị khóa giữa chừng, adopt hoàn tác và để Locked (tệp đã ở chế độ PIN nên vẫn đúng).
        adoptOrClearToken(config, sealed.key, epoch)
        lockout.clear()
        AppResult.Success(Unit)
    }

    override suspend fun unlockWithPin(pin: CharArray): UnlockResult = mutex.withLock {
        vault.initialize()
        if (!vault.pinMode) return@withLock UnlockResult.Success
        val epoch = vault.currentEpoch()
        when (val attempt = attempt(pin)) {
            is Attempt.Rejected -> attempt.result
            is Attempt.Opened -> {
                val config = vault.decode(attempt.plain)
                attempt.plain.fill(0)
                when {
                    config == null -> {
                        attempt.key.fill(0)
                        UnlockResult.Failed
                    }
                    // false: app đã bị khóa lại trong lúc giải mã (xuống nền). Giữ Locked, không coi là mở khóa.
                    adoptOrClearToken(config, attempt.key, epoch) -> UnlockResult.Success
                    else -> UnlockResult.Interrupted
                }
            }
        }
    }

    override suspend fun isBiometricAvailable(): Boolean = biometric.isAvailable()

    override suspend fun isBiometricEnabled(): Boolean = vault.pinMode && biometric.isEnabled()

    override suspend fun enableBiometric(): BiometricOutcome {
        vault.initialize()
        // Chỉ khi đang ở chế độ PIN và đã mở khóa mới có khóa phiên để bọc; đang khóa thì không có.
        val key = vault.copySessionKey() ?: return BiometricOutcome.Unavailable
        return try {
            biometric.enroll(key)
        } finally {
            key.fill(0)
        }
    }

    override suspend fun unlockWithBiometric(): UnlockResult {
        vault.initialize()
        if (!vault.pinMode) return UnlockResult.Success
        // Vân tay không được né việc chống đoán PIN: đang bị khóa nhập thì phải đợi hết giờ (KH-02).
        val remaining = lockout.remainingMs()
        if (remaining > 0) return UnlockResult.Cooldown(remaining)
        val epoch = vault.currentEpoch()
        // Hộp thoại sinh trắc học chạy NGOÀI mutex: nó có thể chờ người dùng lâu, và không được chặn nhập PIN hay khóa.
        val unwrapped = when (val result = biometric.unwrap()) {
            is BiometricUnwrap.Key -> result
            // Đã tự xóa phần bọc ở bên cài đặt; về nhập PIN.
            BiometricUnwrap.Cancelled, BiometricUnwrap.Invalidated -> return UnlockResult.Cancelled
            BiometricUnwrap.Failed -> return UnlockResult.Failed
        }
        return try {
            mutex.withLock { openWithBiometricKey(unwrapped.key, epoch) }
        } finally {
            unwrapped.key.fill(0)
        }
    }

    private suspend fun openWithBiometricKey(key: ByteArray, epoch: Int): UnlockResult {
        // Kiểm tra lại trong mutex: hộp thoại có thể mở lâu, và một lần nhập sai PIN trong lúc đó có thể đã bật khóa tạm.
        val remaining = lockout.remainingMs()
        if (remaining > 0) return UnlockResult.Cooldown(remaining)
        val envelope = (vault.readRaw() as? AppResult.Success)?.value
        if (envelope == null || !codec.isEnvelope(envelope)) return UnlockResult.Failed
        return when (val opened = codec.openWithKey(key, envelope, ConfigVault.NAME)) {
            is EnvelopeOpen.Opened -> {
                val config = vault.decode(opened.plain)
                opened.plain.fill(0)
                when {
                    config == null -> {
                        opened.key.fill(0)
                        UnlockResult.Failed
                    }
                    // false: app bị khóa lại trong lúc xác thực; giữ Locked, không coi là mở khóa.
                    adoptOrClearToken(config, opened.key, epoch) -> UnlockResult.Success
                    else -> UnlockResult.Interrupted
                }
            }
            // Khóa bọc không còn khớp phong bì (PIN đã đổi ở nơi khác, tệp cũ): xóa và để người dùng nhập PIN.
            EnvelopeOpen.WrongKey, EnvelopeOpen.Malformed -> {
                biometric.clear()
                UnlockResult.Cancelled
            }
        }
    }

    // Đọc bộ đếm có thể ghi lại mốc khi phát hiện reboot nên LockoutStore tự tuần tự hóa (không cần chờ Argon2id ở đây).
    override suspend fun lockoutRemainingMs(): Long = lockout.remainingMs()

    override suspend fun attemptsBeforeWipe(): Int? {
        val threshold = wipeAfterFailures() ?: return null
        return (threshold - lockout.failures()).coerceAtLeast(0)
    }

    override fun lock() {
        if (vault.lock()) tokens.clear()
    }

    override suspend fun verifyPin(pin: CharArray): UnlockResult = mutex.withLock {
        vault.initialize()
        if (!vault.pinMode) return@withLock UnlockResult.Success
        when (val attempt = attempt(pin)) {
            is Attempt.Rejected -> attempt.result
            is Attempt.Opened -> {
                attempt.plain.fill(0)
                attempt.key.fill(0)
                UnlockResult.Success
            }
        }
    }

    override suspend fun changePin(oldPin: CharArray, newPin: CharArray): UnlockResult = mutex.withLock {
        vault.initialize()
        if (!vault.pinMode) return@withLock UnlockResult.Failed
        val epoch = vault.currentEpoch()
        when (val attempt = attempt(oldPin)) {
            is Attempt.Rejected -> attempt.result
            is Attempt.Opened -> {
                attempt.key.fill(0)
                val config = vault.decode(attempt.plain)
                val envelope = try {
                    if (config == null) null else codec.seal(newPin, attempt.plain, ConfigVault.NAME)
                } finally {
                    attempt.plain.fill(0)
                }
                if (config == null || envelope !is AppResult.Success) return@withLock UnlockResult.Failed
                val sealed = envelope.value
                // Tệp tạm rồi thay (CD-09): lỗi giữa chừng thì config vẫn mã hóa bằng PIN cũ.
                if (vault.writeRaw(sealed.bytes) is AppResult.Failure) {
                    sealed.key.fill(0)
                    return@withLock UnlockResult.Failed
                }
                adoptOrClearToken(config, sealed.key, epoch)
                // PIN mới có salt và khóa dẫn xuất mới: phần bọc sinh trắc học cũ không còn mở được phong bì. Xóa, người dùng
                // bật lại khi muốn (CD-09).
                biometric.clear()
                UnlockResult.Success
            }
        }
    }

    override suspend fun disableProtection(pin: CharArray): UnlockResult = mutex.withLock {
        vault.initialize()
        if (!vault.pinMode) return@withLock UnlockResult.Success
        when (val attempt = attempt(pin)) {
            is Attempt.Rejected -> attempt.result
            is Attempt.Opened -> {
                attempt.key.fill(0)
                val config = vault.decode(attempt.plain)
                // Plaintext của phong bì chính là JSON chế độ thiết bị: ghi lại thẳng qua lớp Keystore.
                val written = if (config == null) null else vault.writeRaw(attempt.plain)
                attempt.plain.fill(0)
                if (config == null || written !is AppResult.Success) return@withLock UnlockResult.Failed
                vault.setDeviceMode(config)
                lockout.clear()
                // Hết chế độ PIN thì phần bọc khóa dẫn xuất không còn ý nghĩa (CD-03).
                biometric.clear()
                UnlockResult.Success
            }
        }
    }

    /**
     * [ConfigVault.adopt] kèm dọn token khi bị hoàn tác. [lock] không làm gì ở chế độ thiết bị nên nếu app xuống nền
     * giữa lúc đang bật PIN, token chưa bị xóa; khi adopt hoàn tác thì phải xóa ở đây (CH-03).
     */
    private fun adoptOrClearToken(config: ConnectionConfig, key: ByteArray, epoch: Int): Boolean {
        val adopted = vault.adopt(config, key, epoch)
        if (!adopted) tokens.clear()
        return adopted
    }

    override suspend fun wipe() = mutex.withLock { wipeLocked() }

    /**
     * Không bị hủy giữa chừng ([NonCancellable]): nhập sai quá nhiều (KH-06) hay Quên PIN (KH-03) phải xóa trọn dù
     * ViewModel vừa bị clear. Thứ tự: tệp và khóa (`SecretStore.wipeAll`), rồi bộ nhớ, rồi token.
     */
    private suspend fun wipeLocked() = withContext(NonCancellable) {
        secrets.wipeAll()
        vault.forget()
        tokens.clear()
    }

    private sealed interface Attempt {
        /** Mở được: người nhận tự xóa [plain] và [key] khi xong, hoặc giao [key] cho vault. */
        class Opened(val plain: ByteArray, val key: ByteArray) : Attempt
        class Rejected(val result: UnlockResult) : Attempt
    }

    /**
     * Một lần thử PIN (KH-02): kiểm tra thời gian chờ, **ghi bộ đếm trước**, rồi mới chạy Argon2id. Ghi trước để tắt app
     * giữa lúc đang giải mã cũng tính là một lần sai. Đúng thì xóa bộ đếm. Gọi trong [mutex].
     */
    private suspend fun attempt(pin: CharArray): Attempt {
        val remaining = lockout.remainingMs()
        if (remaining > 0) return Attempt.Rejected(UnlockResult.Cooldown(remaining))

        // Kiểm tra lớp Keystore trước khi tính lần thử: khóa Keystore mất hay tệp hỏng không phải PIN sai.
        val envelope = (vault.readRaw() as? AppResult.Success)?.value
        if (envelope == null || !codec.isEnvelope(envelope)) return Attempt.Rejected(UnlockResult.Failed)

        val (previous, current) = lockout.recordAttempt() ?: return Attempt.Rejected(UnlockResult.Failed)
        return when (val opened = codec.open(pin, envelope, ConfigVault.NAME)) {
            is EnvelopeOpen.Opened -> {
                try {
                    lockout.clear()
                } catch (e: Throwable) {
                    // Bị hủy giữa chừng: không để plaintext và khóa nằm lại trong bộ nhớ.
                    opened.plain.fill(0)
                    opened.key.fill(0)
                    throw e
                }
                Attempt.Opened(opened.plain, opened.key)
            }
            EnvelopeOpen.WrongKey -> Attempt.Rejected(onWrongPin(current.failures))
            EnvelopeOpen.Malformed -> {
                lockout.rollback(previous)
                Attempt.Rejected(UnlockResult.Failed)
            }
        }
    }

    private suspend fun onWrongPin(failures: Int): UnlockResult {
        val threshold = wipeAfterFailures()
        if (threshold != null && failures >= threshold) {
            // KH-06: như ngắt kết nối. Chỉ xóa phần bảo mật và config ở đây; phần dữ liệu khác của app do nơi gọi chạy
            // tiếp `DisconnectUseCase` (idempotent) khi nhận [UnlockResult.Wiped]. Nếu process chết trước đó thì dữ
            // liệu của các lát sau (Room, cache) sẽ mồ côi: lát nào thêm dữ liệu phải tự dọn khi không còn config.
            wipeLocked()
            return UnlockResult.Wiped
        }
        val remaining = lockout.remainingMs()
        return if (remaining > 0) UnlockResult.Cooldown(remaining) else UnlockResult.WrongPin(failures, threshold?.minus(failures))
    }
}
