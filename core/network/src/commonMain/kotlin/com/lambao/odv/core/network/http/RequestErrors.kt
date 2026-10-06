package com.lambao.odv.core.network.http

import com.lambao.odv.core.common.error.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

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

/** Giá trị `Retry-After` (giây) khi máy chủ gửi, null nếu không có hoặc không phải số. */
internal fun HttpResponse.retryAfterSeconds(): Long? = headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()

/** Đọc body JSON thành [T]; không đọc được thì null. Hủy coroutine được ném lại. Dùng cho body lỗi của từng dịch vụ. */
internal suspend inline fun <reified T> HttpResponse.readJsonOrNull(): T? = try {
    NetworkJson.decodeFromString<T>(bodyAsText())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
