package com.lambao.odv.core.security

import com.lambao.odv.core.common.result.AppResult

/**
 * Kho bí mật mã hóa trên đĩa (CH-01). Mỗi bí mật là một tệp ciphertext đặt theo [name].
 *
 * Bản Android dùng AES-GCM với khóa trong Android Keystore (khóa không xuất ra khỏi thiết bị, nên tệp lấy ra
 * ngoài máy không giải mã được). MVP2: bản iOS dùng Keychain. Lát 2 thêm khóa dẫn xuất từ PIN (Argon2id, ADR-0008).
 *
 * Mọi hàm không ném ngoại lệ (trừ hủy coroutine); lỗi trả về dạng [com.lambao.odv.core.common.error.AppError.SecureStorage].
 */
interface SecretStore {

    /** Mã hóa [plain] và ghi nguyên tử, thay bí mật cũ cùng [name]. [name] chỉ gồm `a-z`, `0-9`, `_`, `-`. */
    suspend fun write(name: String, plain: ByteArray): AppResult<Unit>

    /** Giải mã bí mật [name]. Trả `Success(null)` nếu chưa có; `Failure` nếu có mà không giải mã được. */
    suspend fun read(name: String): AppResult<ByteArray?>

    /** Bí mật [name] đã có chưa. Chỉ kiểm tra tệp, không giải mã. */
    suspend fun exists(name: String): Boolean

    /** Xóa bí mật [name]; không có thì bỏ qua. */
    suspend fun delete(name: String)
}
