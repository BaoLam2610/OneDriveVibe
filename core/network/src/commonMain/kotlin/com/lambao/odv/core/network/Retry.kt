package com.lambao.odv.core.network

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MAX_ATTEMPTS = 3
private const val MAX_RETRY_AFTER_SECONDS = 60L

/**
 * Thử lại lỗi tạm thời (TK-05, TK-06): lỗi mạng, timeout, `429`, `5xx`.
 * `429`/`503` chờ đúng `Retry-After` (giới hạn 60 giây để màn hình không treo); lỗi khác chờ 1 giây rồi 2 giây.
 * Không thử lại `4xx` khác: `400`/`401`/`403`/`404` là lỗi cấu hình hoặc dữ liệu, thử lại không đổi kết quả (TK-04).
 */
internal suspend fun <T> withRetry(block: suspend () -> AppResult<T>): AppResult<T> {
    var attempt = 1
    while (true) {
        val result = block()
        val error = (result as? AppResult.Failure)?.error
        if (error == null || attempt >= MAX_ATTEMPTS || !error.isTransient()) return result
        delay(error.retryDelay(attempt))
        attempt++
    }
}

private fun AppError.isTransient(): Boolean = when (this) {
    AppError.Network, AppError.Timeout -> true
    is AppError.Http -> status == 429 || status >= 500
    else -> false
}

private fun AppError.retryDelay(attempt: Int): Duration {
    val retryAfter = (this as? AppError.Http)?.retryAfterSeconds
    return if (retryAfter != null) retryAfter.coerceIn(1, MAX_RETRY_AFTER_SECONDS).seconds else (1L shl (attempt - 1)).seconds
}
