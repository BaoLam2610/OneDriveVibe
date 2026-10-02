package com.lambao.odv.core.network

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.network.dto.GraphErrorEnvelopeDto
import com.lambao.odv.core.network.dto.TokenErrorDto
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

private val aadstsPattern = Regex("AADSTS[0-9]+")

/** Ánh xạ ngoại lệ khi gửi request sang [AppError]. Hủy coroutine được ném lại nguyên vẹn. */
internal fun Throwable.toAppError(): AppError {
    if (this is CancellationException) throw this
    return when {
        this is HttpRequestTimeoutException || this is ConnectTimeoutException || this is SocketTimeoutException ->
            AppError.Timeout
        isNetworkFailure() -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

/** Lỗi từ Graph: lấy `error.code` (vd. `itemNotFound`) và `Retry-After`. Không đọc `message` của máy chủ (KN-09). */
internal suspend fun HttpResponse.toGraphError(): AppError.Http {
    val code = readJsonOrNull<GraphErrorEnvelopeDto>()?.error?.code
    return AppError.Http(status.value, code, retryAfterSeconds())
}

/**
 * Lỗi từ endpoint token: mã `AADSTS` lấy từ `error_codes`, rồi tiền tố trong `error_description`, rồi `error`
 * (onedrive-graph-api.md 1.4). Không bao giờ đưa nguyên body vào lỗi hay log (CH-06).
 */
internal suspend fun HttpResponse.toTokenError(): AppError.Http {
    val dto = readJsonOrNull<TokenErrorDto>()
    val code = dto?.errorCodes?.firstOrNull()?.let { "AADSTS$it" }
        ?: dto?.errorDescription?.let { aadstsPattern.find(it)?.value }
        ?: dto?.error
    return AppError.Http(status.value, code, retryAfterSeconds())
}

private fun HttpResponse.retryAfterSeconds(): Long? = headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()

private suspend inline fun <reified T> HttpResponse.readJsonOrNull(): T? = try {
    NetworkJson.decodeFromString<T>(bodyAsText())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
