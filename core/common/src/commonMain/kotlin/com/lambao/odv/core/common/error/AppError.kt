package com.lambao.odv.core.common.error

/**
 * Lỗi có cấu trúc dùng xuyên các tầng (ADR-0011): `domain` và `data` không chứa chuỗi hiển thị, UI tự đổi
 * [AppError] sang chuỗi theo ngôn ngữ đang dùng (KN-09, CD-10).
 *
 * Không đặt thông báo lỗi nguyên văn của máy chủ vào đây để hiển thị. Lát sau thêm loại mới khi có nghiệp vụ cần
 * phân biệt (vd. lỗi giải mã config, PIN).
 */
sealed interface AppError {

    /** Không có mạng hoặc không kết nối được tới máy chủ. Có thể thử lại. */
    data object Network : AppError

    /** Quá thời gian chờ. Có thể thử lại. */
    data object Timeout : AppError

    /**
     * Máy chủ trả lỗi HTTP.
     *
     * @param status mã HTTP (400, 401, 403, 404, 410, 429, 5xx...).
     * @param code mã lỗi máy chủ nếu có, dùng để chọn thông báo: mã Graph (`itemNotFound`, `resyncRequired`...)
     *   hoặc mã AADSTS của endpoint token (`AADSTS7000215`...). Không chứa bí mật.
     * @param retryAfterSeconds giá trị header `Retry-After` khi bị throttle (429), nếu có.
     */
    data class Http(
        val status: Int,
        val code: String? = null,
        val retryAfterSeconds: Long? = null,
    ) : AppError

    /** Lỗi không phân loại được. [cause] chỉ để ghi log khi debug, không hiển thị. */
    data class Unknown(val cause: Throwable? = null) : AppError
}
