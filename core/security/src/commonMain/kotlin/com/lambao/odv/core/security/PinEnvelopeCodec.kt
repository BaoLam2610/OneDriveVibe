package com.lambao.odv.core.security

import com.lambao.odv.core.common.result.AppResult

/** Tham số Argon2id. Lưu trong header phong bì để sau này nâng cấp mà vẫn đọc được tệp cũ (ADR-0014). */
data class KdfParams(val memoryKib: Int, val iterations: Int, val parallelism: Int) {
    companion object {
        /** m = 32 MiB, t = 3, p = 1. Đo trên máy yếu: quá 700 ms thì hạ [iterations] xuống 2 (ADR-0014). */
        val Default = KdfParams(memoryKib = 32 * 1024, iterations = 3, parallelism = 1)
    }
}

/**
 * Dẫn xuất khóa 32 byte từ mã PIN bằng Argon2id (CH-07). Đặt sau interface để đổi thư viện được (tech-stack §5.4).
 * Việc nặng (hàng trăm ms): phải chạy ngoài luồng chính. Không giữ lại [pin]; người gọi tự xóa mảng sau khi dùng.
 */
interface PinKeyDeriver {
    suspend fun derive(pin: CharArray, salt: ByteArray, params: KdfParams): ByteArray
}

/** Phong bì vừa tạo. [key] là khóa dẫn xuất (dùng làm khóa phiên hoặc bọc bằng sinh trắc học); người gọi tự xóa khi xong. */
class SealedEnvelope(val bytes: ByteArray, val key: ByteArray)

sealed interface EnvelopeOpen {
    /** Mở được. [key] là khóa dẫn xuất; người gọi tự xóa [plain] và [key] khi xong. */
    class Opened(val plain: ByteArray, val key: ByteArray) : EnvelopeOpen

    /** Thẻ GCM sai: PIN (hoặc khóa) không đúng. Chỉ kết quả này mới tính vào bộ đếm sai (KH-02). */
    data object WrongKey : EnvelopeOpen

    /** Không phải phong bì hợp lệ (sai magic, thiếu byte, tham số vô lý). Không tính vào bộ đếm. */
    data object Malformed : EnvelopeOpen
}

/**
 * Phong bì PIN: lớp mã hóa trong của config ở chế độ PIN (ADR-0014). Định dạng:
 * `[0xA1][ver][kdf][m KiB u32][t u8][p u8][salt 16][IV 12][ciphertext + tag GCM]`, AAD = header + tên bí mật.
 * Không lưu PIN hay hash PIN (CH-02): PIN đúng hay sai là mở được phong bì hay không.
 */
interface PinEnvelopeCodec {

    /** Byte đầu của [bytes] có phải phong bì PIN không (khác `{` của JSON chế độ thiết bị). Không giải mã. */
    fun isEnvelope(bytes: ByteArray): Boolean

    /** Tạo phong bì mới cho [plain] bằng [pin], salt ngẫu nhiên mới. [name] là tên bí mật, đưa vào AAD. */
    suspend fun seal(pin: CharArray, plain: ByteArray, name: String): AppResult<SealedEnvelope>

    /** Mở [envelope] bằng [pin]: dẫn xuất khóa theo tham số trong header rồi giải mã. */
    suspend fun open(pin: CharArray, envelope: ByteArray, name: String): EnvelopeOpen

    /** Mở bằng khóa đã dẫn xuất sẵn (đường sinh trắc học, bỏ qua Argon2id). */
    suspend fun openWithKey(key: ByteArray, envelope: ByteArray, name: String): EnvelopeOpen
}

/**
 * Đồng hồ chịu được việc chỉnh giờ hệ thống và khởi động lại (KH-02, ADR-0014): `elapsedRealtime` không đổi theo giờ
 * hệ thống nhưng về 0 khi khởi động lại, nên đi kèm số lần khởi động để phát hiện việc đó.
 */
interface BootAwareClock {
    fun elapsedRealtimeMs(): Long

    /** Số lần khởi động máy; `-1` nếu hệ thống không cung cấp (khi đó chỉ phát hiện reboot qua `elapsedRealtime` quay ngược). */
    fun bootCount(): Int
}
