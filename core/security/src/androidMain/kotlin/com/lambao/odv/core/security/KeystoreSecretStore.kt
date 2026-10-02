package com.lambao.odv.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [SecretStore] cho Android: AES-256-GCM, khóa trong Android Keystore (CH-01, ADR-0008).
 *
 * Định dạng tệp: `[1 byte độ dài IV][IV][ciphertext + tag GCM]`. Tên bí mật được đưa vào AAD của GCM để không thể
 * đổi tên tệp này thành tệp khác mà vẫn giải mã được. Ghi qua tệp tạm rồi đổi tên nên không bao giờ để lại tệp ghi dở.
 *
 * Không log nội dung, tên khóa hay ngoại lệ gốc (CH-06): mọi lỗi gộp thành [AppError.SecureStorage].
 */
internal class KeystoreSecretStore(
    context: Context,
    private val dispatchers: DispatcherProvider,
) : SecretStore {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY)
    private val mutex = Mutex()

    override suspend fun write(name: String, plain: ByteArray): AppResult<Unit> = guarded {
        val target = fileFor(name)
        directory.mkdirs()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, checkNotNull(key(createIfMissing = true)))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        val temp = File(directory, "$name.tmp")
        temp.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(encrypted)
            out.fd.sync()
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Không đổi tên được tệp tạm")
        }
    }

    override suspend fun read(name: String): AppResult<ByteArray?> = guarded {
        val file = fileFor(name)
        if (!file.exists()) return@guarded null
        val bytes = file.readBytes()
        val ivLength = bytes[0].toInt()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Khóa mất (đặt lại màn hình khóa, khôi phục máy): không tạo khóa mới ngầm, để đọc thất bại rõ ràng thay vì sai thẻ GCM.
        val secretKey = checkNotNull(key(createIfMissing = false))
        require(ivLength == IV_BYTES)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, bytes, 1, ivLength))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        cipher.doFinal(bytes, 1 + ivLength, bytes.size - 1 - ivLength)
    }

    override suspend fun exists(name: String): Boolean = withContext(dispatchers.io) { fileFor(name).exists() }

    override suspend fun delete(name: String) {
        withContext(dispatchers.io) {
            mutex.withLock { fileFor(name).delete() }
        }
    }

    /** Chạy [block] trên luồng IO, tuần tự hóa truy cập, gộp mọi lỗi (trừ hủy coroutine) thành [AppError.SecureStorage]. */
    private suspend fun <T> guarded(block: () -> T): AppResult<T> = withContext(dispatchers.io) {
        mutex.withLock {
            try {
                AppResult.Success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.Failure(AppError.SecureStorage)
            }
        }
    }

    private fun fileFor(name: String): File {
        require(NAME_PATTERN.matches(name)) { "Tên bí mật không hợp lệ" }
        return File(directory, "$name.bin")
    }

    private fun key(createIfMissing: Boolean): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        if (!createIfMissing) return null
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val DIRECTORY = "secrets"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "odv_config_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        val NAME_PATTERN = Regex("^[a-z0-9_-]{1,64}$")
    }
}
