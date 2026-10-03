package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.BiometricOutcome
import com.lambao.odv.core.domain.model.BiometricUnwrap

/**
 * Cổng tới sinh trắc học của nền tảng (ADR-0014, BM-02, KH-01). Bản Android dùng `BiometricPrompt` kèm khóa Keystore
 * yêu cầu xác thực sinh trắc học (mỗi lần dùng, chỉ loại mạnh, mất hiệu lực khi đổi vân tay), nên hộp thoại nằm sau interface
 * này; domain và data không biết gì về `Activity`.
 *
 * Sinh trắc học **không thay PIN và không lưu PIN hay hash PIN** (CH-02): nó chỉ bọc **khóa dẫn xuất** từ PIN, và chỉ mở
 * được bằng sinh trắc học đã đăng ký. Mọi `ByteArray` khóa do người gọi tự xóa sau khi dùng.
 */
interface BiometricAuthenticator {

    /** Máy có sinh trắc học mạnh và đã đăng ký ít nhất một mẫu (vân tay, khuôn mặt) để dùng. */
    suspend fun isAvailable(): Boolean

    /** Đã bật: có phần bọc khóa và khóa Keystore còn hiệu lực. */
    suspend fun isEnabled(): Boolean

    /**
     * Hiện hộp thoại xác nhận rồi bọc [key] (khóa dẫn xuất từ PIN) bằng khóa Keystore sinh trắc học, thay phần bọc cũ.
     * Không giữ lại [key]. Lỗi thì không để lại phần bọc dở.
     */
    suspend fun enroll(key: ByteArray): BiometricOutcome

    /** Hiện hộp thoại sinh trắc học rồi trả khóa dẫn xuất đã bọc, hoặc lý do không có. */
    suspend fun unwrap(): BiometricUnwrap

    /** Xóa phần bọc và khóa Keystore sinh trắc học (đổi PIN, tắt bảo vệ, khóa mất hiệu lực). */
    suspend fun clear()
}
