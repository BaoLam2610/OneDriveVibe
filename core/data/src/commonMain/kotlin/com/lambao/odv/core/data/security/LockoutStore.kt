package com.lambao.odv.core.data.security

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.security.BootAwareClock
import com.lambao.odv.core.security.SecretStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * Bộ đếm sai bền (KH-02, ADR-0014). [failures] là số lần sai liên tiếp; [lastAttemptMs] là mốc lần thử cuối theo
 * `elapsedRealtime` (không đổi theo giờ hệ thống); [bootCount] là số lần khởi động máy lúc đó.
 */
@Serializable
internal data class LockoutRecord(
    val failures: Int = 0,
    val lastAttemptMs: Long = 0,
    val bootCount: Int = -1,
)

/**
 * Lưu [LockoutRecord] trong [SecretStore] (mã hóa bằng khóa Keystore, không dùng PIN, ghi nguyên tử) nên tắt/mở lại
 * app vẫn tính đủ thời gian chờ (KH-02). Người gọi phải [recordAttempt] **trước** khi thử giải mã và [clear] khi đúng.
 */
internal class LockoutStore(
    private val secrets: SecretStore,
    private val clock: BootAwareClock,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Mọi thao tác đọc-sửa-ghi đi qua mutex này: màn Khóa hỏi [remainingMs] (có thể ghi mốc reboot) trong khi một lần
    // thử PIN đang [recordAttempt] thì không được ghi đè lẫn nhau và làm mất một lần sai.
    private val mutex = Mutex()

    /** Ghi một lần thử mới. Trả về (trước, sau), hoặc null nếu không ghi được (khi đó không được thử PIN). */
    suspend fun recordAttempt(): Pair<LockoutRecord, LockoutRecord>? = mutex.withLock {
        val before = load()
        val after = LockoutRecord(before.failures + 1, clock.elapsedRealtimeMs(), clock.bootCount())
        if (save(after)) before to after else null
    }

    /** Hoàn tác [recordAttempt] khi lần thử không phải PIN sai (tệp hỏng): không phạt oan. */
    suspend fun rollback(previous: LockoutRecord) {
        mutex.withLock {
            if (previous.failures == 0) secrets.delete(NAME) else save(previous)
        }
    }

    suspend fun clear() = mutex.withLock { secrets.delete(NAME) }

    /** Số lần sai liên tiếp hiện tại (KH-06). */
    suspend fun failures(): Int = mutex.withLock { load().failures }

    /**
     * Thời gian còn bị khóa nhập, 0 nếu không (KH-02). Khi phát hiện máy đã khởi động lại (số lần khởi động đổi, hoặc
     * `elapsedRealtime` nhỏ hơn mốc đã lưu) thì tính lại thời gian chờ từ đầu và lưu mốc mới (ADR-0014).
     */
    suspend fun remainingMs(): Long = mutex.withLock {
        val record = load()
        val penalty = penaltyMs(record.failures)
        if (penalty == 0L) return@withLock 0L
        val now = clock.elapsedRealtimeMs()
        val boot = clock.bootCount()
        val rebooted = record.bootCount != boot || now < record.lastAttemptMs
        if (rebooted) {
            save(record.copy(lastAttemptMs = now, bootCount = boot))
            return@withLock penalty
        }
        (record.lastAttemptMs + penalty - now).coerceIn(0, penalty)
    }

    private suspend fun load(): LockoutRecord {
        val bytes = (secrets.read(NAME) as? AppResult.Success)?.value ?: return LockoutRecord()
        return try {
            json.decodeFromString(LockoutRecord.serializer(), bytes.decodeToString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LockoutRecord()
        }
    }

    private suspend fun save(record: LockoutRecord): Boolean =
        secrets.write(NAME, json.encodeToString(LockoutRecord.serializer(), record).encodeToByteArray()) is AppResult.Success

    companion object {
        const val NAME = "lock_state"
        private const val FREE_ATTEMPTS = 5
        private const val BASE_PENALTY_MS = 30_000L
        private const val MAX_PENALTY_MS = 60 * 60 * 1000L

        /** KH-02: từ lần sai thứ 5 chờ 30 giây, mỗi lần sai tiếp theo gấp đôi, trần 1 giờ. */
        fun penaltyMs(failures: Int): Long {
            if (failures < FREE_ATTEMPTS) return 0
            // Chặn số mũ để không tràn: 30 giây × 2^7 đã vượt trần 1 giờ.
            val doublings = min(failures - FREE_ATTEMPTS, 7)
            return min(BASE_PENALTY_MS shl doublings, MAX_PENALTY_MS)
        }
    }
}
