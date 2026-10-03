package com.lambao.odv.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.lambao.odv.R
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.BiometricOutcome
import com.lambao.odv.core.domain.model.BiometricUnwrap
import com.lambao.odv.core.domain.repository.BiometricAuthenticator
import com.lambao.odv.core.security.SecretStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

/**
 * [BiometricAuthenticator] cho Android (ADR-0014 mục 5). Khóa Keystore `odv_bio_key` (AES-256-GCM) chỉ dùng được sau khi
 * xác thực sinh trắc học mạnh, **mỗi lần dùng** (không có thời hạn), và mất hiệu lực khi thêm hoặc đổi sinh trắc học. Khóa
 * dẫn xuất từ PIN được bọc bằng khóa này rồi lưu ở `bio_wrap` qua [SecretStore] (đã có lớp Keystore thường bên ngoài):
 * `[độ dài IV 1 byte][IV][ciphertext + tag GCM]`. Không lưu PIN hay hash PIN (CH-02).
 *
 * Không cho phép `DEVICE_CREDENTIAL` (PIN, hình mở khóa của máy) làm phương án mở khóa: chỉ sinh trắc học mạnh, nút phụ của
 * hộp thoại là "Dùng mã PIN" của app.
 */
class AndroidBiometricAuthenticator(
    private val context: Context,
    private val secrets: SecretStore,
    private val activities: CurrentActivityHolder,
    private val dispatchers: DispatcherProvider,
) : BiometricAuthenticator {

    override suspend fun isAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun isEnabled(): Boolean = withContext(dispatchers.io) { secrets.exists(WRAP_NAME) && keyExists() }

    override suspend fun enroll(key: ByteArray): BiometricOutcome {
        if (!isAvailable()) return BiometricOutcome.Unavailable
        return try {
            val cipher = withContext(dispatchers.io) {
                // Khóa mới mỗi lần bật: phần bọc cũ (nếu có) không còn dùng được.
                deleteKey()
                createKey()
                Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, checkNotNull(secretKey())) }
            }
            val authenticated = authenticate(
                cipher,
                R.string.biometric_enroll_title,
                R.string.biometric_enroll_subtitle,
                confirmationRequired = true,
            )
            if (authenticated == null) {
                cleanup()
                return BiometricOutcome.Cancelled
            }
            val written = withContext(dispatchers.io) {
                val iv = authenticated.iv
                val encrypted = authenticated.doFinal(key)
                secrets.write(WRAP_NAME, byteArrayOf(iv.size.toByte()) + iv + encrypted)
            }
            if (written is AppResult.Success) {
                BiometricOutcome.Success
            } else {
                cleanup()
                BiometricOutcome.Failed
            }
        } catch (e: CancellationException) {
            cleanup()
            throw e
        } catch (e: Exception) {
            cleanup()
            BiometricOutcome.Failed
        }
    }

    override suspend fun unwrap(): BiometricUnwrap {
        return try {
            val stored = (secrets.read(WRAP_NAME) as? AppResult.Success)?.value ?: return BiometricUnwrap.Failed
            val ivLength = stored[0].toInt()
            if (ivLength != IV_BYTES || stored.size <= 1 + ivLength) return BiometricUnwrap.Failed
            val iv = stored.copyOfRange(1, 1 + ivLength)
            val encrypted = stored.copyOfRange(1 + ivLength, stored.size)

            val cipher = try {
                withContext(dispatchers.io) { decryptCipher(iv) }
            } catch (e: GeneralSecurityException) {
                // Đã thêm hoặc đổi sinh trắc học: khóa mất hiệu lực (KeyPermanentlyInvalidatedException). Một số máy ném
                // loại khác của họ này (UnrecoverableKeyException, InvalidKeyException) cho cùng tình huống. Xóa hẳn,
                // người dùng nhập PIN rồi bật lại.
                cleanup()
                return BiometricUnwrap.Invalidated
            }
            if (cipher == null) {
                cleanup()
                return BiometricUnwrap.Invalidated
            }
            val authenticated = authenticate(
                cipher,
                R.string.biometric_unlock_title,
                R.string.biometric_unlock_subtitle,
                confirmationRequired = false,
            ) ?: return BiometricUnwrap.Cancelled
            try {
                BiometricUnwrap.Key(withContext(dispatchers.io) { authenticated.doFinal(encrypted) })
            } catch (e: GeneralSecurityException) {
                // Phần bọc hỏng hoặc không khớp khóa (thẻ GCM sai): không cứu được, dọn để không lặp lại lỗi mỗi lần mở màn Khóa.
                cleanup()
                BiometricUnwrap.Invalidated
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BiometricUnwrap.Failed
        }
    }

    override suspend fun clear() = cleanup()

    /** Xóa phần bọc và khóa; không bị hủy giữa chừng để không để lại phần bọc mà mất khóa hoặc ngược lại. */
    private suspend fun cleanup() {
        withContext(NonCancellable + dispatchers.io) {
            try {
                deleteKey()
            } catch (e: Exception) {
                // Không để KeyStoreException thoát ra ngoài: SecurityRepository hứa không ném ngoại lệ, và đây thường chạy sau
                // khi config đã đổi xong. Lỗi xóa khóa vẫn xóa được phần bọc nên khóa còn lại vô dụng.
            }
            secrets.delete(WRAP_NAME)
        }
    }

    /** Hiện hộp thoại và trả cipher đã được xác thực, hoặc null nếu hủy, lỗi hay không có Activity để hiện. */
    private suspend fun authenticate(
        cipher: Cipher,
        @StringRes title: Int,
        @StringRes subtitle: Int,
        confirmationRequired: Boolean,
    ): Cipher? {
        val activity = activities.awaitResumed() ?: return null
        return withContext(dispatchers.main) {
            suspendCancellableCoroutine { continuation ->
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(result.cryptoObject?.cipher)
                        }

                        // Hủy, bấm "Dùng mã PIN", hệ điều hành khóa sinh trắc học tạm thời... đều quay về nhập PIN.
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(title))
                    .setSubtitle(activity.getString(subtitle))
                    .setNegativeButtonText(activity.getString(R.string.biometric_use_pin))
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    // Mở khóa: không cần bấm xác nhận thêm (nhanh). Bật: cần xác nhận để khuôn mặt thụ động không bật nhầm.
                    .setConfirmationRequired(confirmationRequired)
                    .build()
                continuation.invokeOnCancellation { activity.runOnUiThread { prompt.cancelAuthentication() } }
                // androidx.biometric chỉ ghi log rồi bỏ về, không gọi callback nào, khi Activity đã lưu trạng thái (người dùng vừa
                // bấm Home) hoặc đang đổi cấu hình: coroutine sẽ treo mãi. Kiểm tra ngay trước khi gọi để không có khe hở.
                if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
            }
        }
    }

    /** Cipher giải mã với [iv], hoặc null nếu khóa không còn. Ném `GeneralSecurityException` (thường là `KeyPermanentlyInvalidatedException`) nếu khóa mất hiệu lực. */
    private fun decryptCipher(iv: ByteArray): Cipher? {
        val key = secretKey() ?: return null
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }
    }

    private fun createKey() {
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Thời hạn 0: phải xác thực cho từng lần dùng (qua CryptoObject), chỉ sinh trắc học mạnh.
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply { init(builder.build()) }.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun secretKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun keyExists(): Boolean = keyStore().containsAlias(KEY_ALIAS)

    private fun deleteKey() {
        val store = keyStore()
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"

        // Trùng BIO_KEY_ALIAS của KeystoreSecretStore.wipeAll nên ngắt kết nối cũng xóa khóa này.
        const val KEY_ALIAS = "odv_bio_key"
        const val WRAP_NAME = "bio_wrap"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
    }
}
