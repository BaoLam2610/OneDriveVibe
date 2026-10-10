package com.lambao.odv.core.security

/**
 * Tham số mật mã AES-256-GCM dùng chung cho tệp bí mật (`KeystoreSecretStore`) và phong bì PIN (`AndroidPinEnvelopeCodec`).
 * **Không được đổi giá trị:** đổi là dữ liệu đã mã hóa trên máy người dùng không giải mã được nữa (ADR-0008, ADR-0014).
 */
internal object CryptoConstants {
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val TAG_BITS = 128
    const val IV_BYTES = 12
}

/** Tệp bí mật và khóa trong Android Keystore. **Không được đổi giá trị** (xem [CryptoConstants]). */
internal object KeystoreConstants {
    const val DIRECTORY = "secrets"
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "odv_config_key"

    // Khóa bọc khóa dẫn xuất cho sinh trắc học (tạo ở bước sinh trắc, ADR-0014); xóa cùng lúc khi ngắt kết nối.
    const val BIO_KEY_ALIAS = "odv_bio_key"

    /** Đuôi tệp bí mật trên đĩa (`{tên}.bin`). Tên bộ đếm sai PIN nằm ở [SecretNames.LOCKOUT], dùng chung với `:core:data`. */
    const val FILE_EXTENSION = ".bin"
    val NAME_PATTERN = Regex("^[a-z0-9_-]{1,64}$")
}

/** Định dạng phong bì PIN (ADR-0014). **Không được đổi giá trị** (xem [CryptoConstants]). */
internal object EnvelopeFormat {
    const val MAGIC: Byte = 0xA1.toByte()
    const val VERSION: Byte = 1
    const val KDF_ARGON2ID: Byte = 1
    const val SALT_BYTES = 16
    const val HEADER_BYTES = 1 + 1 + 1 + 4 + 1 + 1 + SALT_BYTES
    const val MIN_MEMORY_KIB = 8
    const val MAX_MEMORY_KIB = 64 * 1024
    const val MAX_ITERATIONS = 10
    const val MAX_PARALLELISM = 4
}

internal object Argon2Constants {
    const val KEY_BYTES = 32
}
