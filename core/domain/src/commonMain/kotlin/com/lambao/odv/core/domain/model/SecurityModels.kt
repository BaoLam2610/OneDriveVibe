package com.lambao.odv.core.domain.model

/** Chế độ bảo vệ config (ADR-0008, CH-01). */
enum class SecurityMode {
    /** Bảo mật tắt: config chỉ mã hóa bằng khóa Keystore, không cần PIN để mở app. Mặc định sau khi kết nối. */
    DEVICE,

    /** Bảo mật bật: config thêm một lớp mã hóa bằng khóa dẫn xuất từ PIN (ADR-0014). */
    PIN,
}

/**
 * Trạng thái khóa của app, chỉ nằm trong bộ nhớ. Khởi động nguội ở chế độ PIN luôn là [Locked] nên khôi phục back stack
 * sau process death không bỏ qua được màn Khóa (ADR-0014).
 */
enum class LockState {
    /** Chưa biết (đang khởi tạo): UI chưa được vẽ nội dung nào. */
    Unknown,

    /** Chế độ thiết bị, chưa có config, hoặc đã nhập đúng PIN. */
    Unlocked,

    /** Chế độ PIN và chưa nhập PIN: config và token không có trong bộ nhớ (CH-03). */
    Locked,
}

/** Kết quả một lần nhập PIN (mở khóa, kiểm tra PIN hiện tại, đổi PIN, tắt bảo vệ). Mọi nhánh ngoài [Success] đều giữ config nguyên. */
sealed interface UnlockResult {
    data object Success : UnlockResult

    /**
     * PIN sai. [failures] là số lần sai liên tiếp. [attemptsBeforeWipe] là số lần còn lại trước khi xóa dữ liệu khi
     * bật tùy chọn CD-08, `null` nếu tùy chọn tắt (KH-06).
     */
    data class WrongPin(val failures: Int, val attemptsBeforeWipe: Int?) : UnlockResult

    /** Đang bị khóa nhập vì sai nhiều lần (KH-02): chưa thử giải mã, chưa tính thêm lần sai. */
    data class Cooldown(val remainingMs: Long) : UnlockResult

    /** Đạt ngưỡng sai của CD-08: đã xóa dữ liệu như ngắt kết nối (KH-06). */
    data object Wiped : UnlockResult

    /** Lỗi lưu trữ (khóa Keystore mất, tệp hỏng): không phải PIN sai và không tính vào bộ đếm. */
    data object Failed : UnlockResult

    /**
     * PIN đúng nhưng app bị khóa lại ngay trong lúc giải mã (người dùng bấm Home): không mở khóa, không phải lỗi. Giữ màn
     * Khóa để nhập lại, không hiện thông báo lỗi.
     */
    data object Interrupted : UnlockResult

    /**
     * Người dùng hủy hộp thoại sinh trắc học, hoặc sinh trắc học không còn dùng được (đổi vân tay, phần bọc cũ): không phải
     * lỗi, quay về nhập PIN. Màn Khóa cập nhật lại phím sinh trắc học theo `SecurityRepository.isBiometricEnabled`.
     */
    data object Cancelled : UnlockResult
}

/** Kết quả bật sinh trắc học (BM-02). */
enum class BiometricOutcome {
    Success,

    /** Người dùng hủy hộp thoại: có thể thử lại hoặc bỏ qua. */
    Cancelled,

    /** Máy không có hoặc chưa đăng ký sinh trắc học mạnh, hoặc app chưa ở trạng thái đã mở khóa. */
    Unavailable,

    /** Lỗi khi tạo khóa hoặc ghi phần bọc. Không để lại phần bọc dở. */
    Failed,
}

/** Kết quả lấy khóa dẫn xuất qua sinh trắc học (KH-01). */
sealed interface BiometricUnwrap {
    /** Có khóa. Người nhận tự xóa [key] sau khi dùng. */
    class Key(val key: ByteArray) : BiometricUnwrap

    /** Người dùng hủy, hoặc hộp thoại không hiện được (app đang ở nền, khóa tạm bởi hệ điều hành). */
    data object Cancelled : BiometricUnwrap

    /** Khóa Keystore mất hiệu lực (đổi hoặc thêm vân tay): đã xóa phần bọc, quay về PIN. */
    data object Invalidated : BiometricUnwrap

    data object Failed : BiometricUnwrap
}

/** Luật PIN (BM-02, BM-06). */
object PinPolicy {
    const val LENGTH = 6

    /**
     * Số lần sai liên tiếp thì xóa dữ liệu như ngắt kết nối khi người dùng bật "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08,
     * KH-06). Ngưỡng là luật cố định; thứ người dùng quyết định chỉ là bật hay tắt (`SecuritySettings`).
     */
    const val WIPE_AFTER_FAILURES = 10

    /** Đúng 6 chữ số. */
    fun isWellFormed(pin: CharArray): Boolean = pin.size == LENGTH && pin.all { it in '0'..'9' }

    /**
     * BM-06: PIN dễ đoán. Gồm 6 số giống nhau, dãy tăng hoặc giảm liên tiếp (`123456`, `654321`), và dạng lặp 2 số
     * hoặc 3 số (`121212`, `123123`). Giả định [pin] đã [isWellFormed].
     */
    fun isWeak(pin: CharArray): Boolean {
        if (pin.size != LENGTH) return false
        val digits = IntArray(LENGTH) { pin[it] - '0' }
        val ascending = (1 until LENGTH).all { digits[it] - digits[it - 1] == 1 }
        val descending = (1 until LENGTH).all { digits[it] - digits[it - 1] == -1 }
        // Mẫu lặp chu kỳ 1 (giống nhau hết) đã nằm trong chu kỳ 2 nên không cần kiểm tra riêng.
        val repeatsTwo = (2 until LENGTH).all { digits[it] == digits[it - 2] }
        val repeatsThree = (3 until LENGTH).all { digits[it] == digits[it - 3] }
        return ascending || descending || repeatsTwo || repeatsThree
    }
}

/**
 * Trạng thái màn Khóa tại một thời điểm (KH-01, KH-02, KH-06): thời gian còn bị khóa nhập, số lần sai nữa thì xóa dữ liệu
 * (`null` nếu tùy chọn tắt), và sinh trắc học đã bật chưa.
 */
data class LockStatus(
    val lockoutRemainingMs: Long,
    val attemptsBeforeWipe: Int?,
    val biometricEnabled: Boolean,
)

/** Kết quả bật bảo vệ bằng PIN (BM-02, BM-04). */
enum class ProtectionSetupResult {
    /** PIN đã bật và máy dùng được sinh trắc học: hỏi người dùng có bật không (B6). */
    OfferBiometric,

    /** PIN đã bật, không có gì để hỏi thêm. */
    Done,

    /** Không bật được; config giữ nguyên ở chế độ thiết bị (BM-04). */
    Failed,
}

/**
 * Thời gian chờ trước khi tự khóa khi rời app (CH-03, CD, mục Bảo mật). Tính từ lúc cả app xuống nền; quay lại trước mốc này thì
 * không khóa. Mặc định [OneMinute] (đặc tả); [Immediately] là hành vi trước Lát 7c. Thứ tự khai báo là thứ tự trong bảng chọn;
 * giá trị lưu bằng tên nên không đổi tên.
 */
enum class AutoLockDelay(val millis: Long) {
    Immediately(0L),
    TenSeconds(10_000L),
    ThirtySeconds(30_000L),
    OneMinute(60_000L),
    FiveMinutes(5 * 60_000L),
    FifteenMinutes(15 * 60_000L),
}

/** Máy có sinh trắc học mạnh dùng được và đã bật cho app chưa (CD-02): nhóm Bảo mật của Cài đặt dựa vào đây để hiện công tắc. */
data class BiometricStatus(val available: Boolean, val enabled: Boolean)
