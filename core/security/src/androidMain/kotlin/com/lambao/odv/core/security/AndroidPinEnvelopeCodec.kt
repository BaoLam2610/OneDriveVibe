package com.lambao.odv.core.security

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import kotlinx.coroutines.CancellationException
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * [PinEnvelopeCodec] cho Android (AES-256-GCM, khóa từ [PinKeyDeriver]). Định dạng và lý do: ADR-0014.
 * Không log nội dung, PIN, khóa hay ngoại lệ gốc (CH-06).
 */
internal class AndroidPinEnvelopeCodec(
    private val deriver: PinKeyDeriver,
    private val params: KdfParams = KdfParams.Default,
) : PinEnvelopeCodec {

    private val random = SecureRandom()

    override fun isEnvelope(bytes: ByteArray): Boolean = bytes.isNotEmpty() && bytes[0] == EnvelopeFormat.MAGIC

    override suspend fun seal(pin: CharArray, plain: ByteArray, name: String): AppResult<SealedEnvelope> {
        var key: ByteArray? = null
        return try {
            val salt = ByteArray(EnvelopeFormat.SALT_BYTES).also(random::nextBytes)
            val header = header(params, salt)
            val derived = deriver.derive(pin, salt, params).also { key = it }
            val iv = ByteArray(CryptoConstants.IV_BYTES).also(random::nextBytes)
            val cipher = cipher(Cipher.ENCRYPT_MODE, derived, iv, header, name)
            val encrypted = cipher.doFinal(plain)
            AppResult.Success(SealedEnvelope(header + iv + encrypted, derived))
        } catch (e: CancellationException) {
            key?.fill(0)
            throw e
        } catch (e: Throwable) {
            key?.fill(0)
            AppResult.Failure(AppError.SecureStorage)
        }
    }

    override suspend fun open(pin: CharArray, envelope: ByteArray, name: String): EnvelopeOpen {
        val parsed = parse(envelope) ?: return EnvelopeOpen.Malformed
        val key = try {
            deriver.derive(pin, parsed.salt, parsed.params)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return EnvelopeOpen.Malformed
        }
        return decrypt(key, envelope, name)
    }

    override suspend fun openWithKey(key: ByteArray, envelope: ByteArray, name: String): EnvelopeOpen {
        if (parse(envelope) == null) return EnvelopeOpen.Malformed
        return decrypt(key.copyOf(), envelope, name)
    }

    override suspend fun resealWithKey(key: ByteArray, currentEnvelope: ByteArray, plain: ByteArray, name: String): AppResult<ByteArray> {
        if (parse(currentEnvelope) == null) return AppResult.Failure(AppError.SecureStorage)
        // Khóa phải mở được phong bì hiện tại: tránh ghi phong bì mà PIN thật không mở được (bản sao khóa bị xóa dở khi lock() chen vào,
        // hay khóa của phiên khác), vì lần mở sau sẽ ra WrongKey và tính vào bộ đếm sai (KH-02, KH-06).
        when (val check = decrypt(key.copyOf(), currentEnvelope, name)) {
            is EnvelopeOpen.Opened -> { check.plain.fill(0); check.key.fill(0) }
            else -> return AppResult.Failure(AppError.SecureStorage)
        }
        return try {
            // Cùng khóa nên IV phải mới mỗi lần (GCM); salt và tham số lấy nguyên từ header cũ để khóa phiên vẫn đúng.
            val header = currentEnvelope.copyOfRange(0, EnvelopeFormat.HEADER_BYTES)
            val iv = ByteArray(CryptoConstants.IV_BYTES).also(random::nextBytes)
            val encrypted = cipher(Cipher.ENCRYPT_MODE, key, iv, header, name).doFinal(plain)
            AppResult.Success(header + iv + encrypted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            AppResult.Failure(AppError.SecureStorage)
        }
    }

    /** Giải mã bằng [key]; [key] được trả lại trong `Opened`, còn các nhánh lỗi thì xóa luôn. */
    private fun decrypt(key: ByteArray, envelope: ByteArray, name: String): EnvelopeOpen = try {
        val header = envelope.copyOfRange(0, EnvelopeFormat.HEADER_BYTES)
        val iv = envelope.copyOfRange(EnvelopeFormat.HEADER_BYTES, EnvelopeFormat.HEADER_BYTES + CryptoConstants.IV_BYTES)
        val cipher = cipher(Cipher.DECRYPT_MODE, key, iv, header, name)
        val plain = cipher.doFinal(envelope, EnvelopeFormat.HEADER_BYTES + CryptoConstants.IV_BYTES, envelope.size - EnvelopeFormat.HEADER_BYTES - CryptoConstants.IV_BYTES)
        EnvelopeOpen.Opened(plain, key)
    } catch (e: AEADBadTagException) {
        key.fill(0)
        EnvelopeOpen.WrongKey
    } catch (e: Exception) {
        key.fill(0)
        EnvelopeOpen.Malformed
    }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray, header: ByteArray, name: String): Cipher =
        Cipher.getInstance(CryptoConstants.TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(CryptoConstants.TAG_BITS, iv))
            // AAD gồm cả header lẫn tên bí mật: sửa tham số Argon2 hay đổi tên tệp đều làm thẻ GCM sai.
            updateAAD(header)
            updateAAD(name.toByteArray(Charsets.UTF_8))
        }

    private fun header(params: KdfParams, salt: ByteArray): ByteArray =
        ByteBuffer.allocate(EnvelopeFormat.HEADER_BYTES)
            .put(EnvelopeFormat.MAGIC)
            .put(EnvelopeFormat.VERSION)
            .put(EnvelopeFormat.KDF_ARGON2ID)
            .putInt(params.memoryKib)
            .put(params.iterations.toByte())
            .put(params.parallelism.toByte())
            .put(salt)
            .array()

    private class Parsed(val params: KdfParams, val salt: ByteArray)

    /** Đọc header; null nếu không hợp lệ. Giới hạn tham số để một tệp hỏng không bắt máy cấp hàng GB RAM. */
    private fun parse(envelope: ByteArray): Parsed? {
        if (envelope.size < EnvelopeFormat.HEADER_BYTES + CryptoConstants.IV_BYTES + CryptoConstants.TAG_BITS / 8) return null
        val buffer = ByteBuffer.wrap(envelope)
        if (buffer.get() != EnvelopeFormat.MAGIC || buffer.get() != EnvelopeFormat.VERSION || buffer.get() != EnvelopeFormat.KDF_ARGON2ID) return null
        val memoryKib = buffer.getInt()
        val iterations = buffer.get().toInt() and 0xFF
        val parallelism = buffer.get().toInt() and 0xFF
        if (memoryKib !in EnvelopeFormat.MIN_MEMORY_KIB..EnvelopeFormat.MAX_MEMORY_KIB || iterations !in 1..EnvelopeFormat.MAX_ITERATIONS || parallelism !in 1..EnvelopeFormat.MAX_PARALLELISM) {
            return null
        }
        val salt = ByteArray(EnvelopeFormat.SALT_BYTES).also(buffer::get)
        return Parsed(KdfParams(memoryKib, iterations, parallelism), salt)
    }
}
