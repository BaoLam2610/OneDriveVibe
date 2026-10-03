package com.lambao.odv.core.security

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import kotlinx.coroutines.withContext
import java.nio.CharBuffer

/** [PinKeyDeriver] bằng `argon2kt` (Argon2id native). Chạy trên dispatcher mặc định vì tốn CPU và 32 MiB RAM. */
internal class Argon2PinKeyDeriver(
    private val dispatchers: DispatcherProvider,
) : PinKeyDeriver {

    // lazy: nạp thư viện native chỉ khi thật sự dùng PIN, để lỗi ABI không làm app crash ngay ở chế độ thiết bị (ADR-0014).
    private val argon2 by lazy { Argon2Kt() }

    override suspend fun derive(pin: CharArray, salt: ByteArray, params: KdfParams): ByteArray =
        withContext(dispatchers.default) {
            val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(pin))
            val password = ByteArray(buffer.remaining()).also { buffer.get(it) }
            // Xóa cả vùng đệm tạm lẫn bản sao mật khẩu ngay khi dùng xong (CH-02: PIN không nằm lâu trong bộ nhớ).
            buffer.array().fill(0)
            try {
                argon2.hash(
                    mode = Argon2Mode.ARGON2_ID,
                    password = password,
                    salt = salt,
                    tCostInIterations = params.iterations,
                    mCostInKibibyte = params.memoryKib,
                    parallelism = params.parallelism,
                    hashLengthInBytes = KEY_BYTES,
                ).rawHashAsByteArray()
            } finally {
                password.fill(0)
            }
        }

    private companion object {
        const val KEY_BYTES = 32
    }
}
