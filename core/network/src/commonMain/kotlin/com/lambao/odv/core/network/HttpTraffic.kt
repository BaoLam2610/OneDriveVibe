package com.lambao.odv.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

/**
 * Một request/response đã được **làm sạch** để hiện trong màn Debug (ADR-0012). Mọi trường đều an toàn để hiển thị:
 * không có `Authorization`, không có body của endpoint token, không có `downloadUrl`/`tempauth` (CH-06).
 */
class HttpTrafficEntry(
    val method: String,
    val url: String,
    /** Null khi lỗi trước khi nhận được phản hồi (mất mạng, timeout). */
    val status: Int?,
    val durationMs: Long,
    val requestHeaders: List<Pair<String, String>>,
    val responseHeaders: List<Pair<String, String>>,
    /** Chỉ có với Graph, đã che và cắt; luôn null với endpoint token. */
    val responseBody: String?,
    /** Tên loại lỗi (vd. `Network`, `Timeout`), không chứa nội dung ngoại lệ. */
    val error: String?,
)

/**
 * Nơi nhận lưu lượng mạng để gỡ lỗi. Chỉ bản debug cài đặt (module `:tools:debug`); bản release không có bản cài nên
 * `:core:network` không ghi gì và không tốn công làm sạch.
 */
interface HttpTrafficRecorder {
    fun record(entry: HttpTrafficEntry)
}

private const val MAX_BODY_CHARS = 16 * 1024

private val visibleRequestHeaders = setOf(HttpHeaders.Accept, HttpHeaders.ContentType)
private val visibleResponseHeaders =
    setOf(HttpHeaders.ContentType, HttpHeaders.ContentLength, HttpHeaders.RetryAfter, HttpHeaders.Date, "request-id", "client-request-id")

// Không dùng dấu gạch ngược để tránh lỗi escape trong chuỗi Kotlin.
private val secretQueryParam = Regex("([?&](?:tempauth|sig|access_token)=)[^&]*")
private val secretJsonValue =
    Regex("""("(?:@microsoft\.graph\.downloadUrl(?:NoAuth)?|access_token|client_secret|id_token)"\s*:\s*)"[^"]*"""")

/** Che giá trị nhạy cảm trong URL (query `tempauth`, `sig`, `access_token`). */
internal fun sanitizeUrl(url: String): String = url.replace(secretQueryParam, "\$1***")

/** Che `downloadUrl` và token trong body JSON rồi cắt ngắn. */
internal fun sanitizeBody(body: String): String {
    // Che cả URL nhúng trong các trường khác (thumbnail, webUrl có tempauth/sig) trước khi cắt ngắn.
    val masked = body.replace(secretJsonValue, "\$1\"***\"").replace(secretQueryParam, "\$1***")
    return if (masked.length > MAX_BODY_CHARS) masked.take(MAX_BODY_CHARS) + "\n… (cắt, còn ${masked.length - MAX_BODY_CHARS} ký tự)" else masked
}

/**
 * Dựng bản ghi từ phản hồi. [includeBody] chỉ bật cho Graph; endpoint token tuyệt đối không ghi body vì nó chứa
 * `client_secret` (request) và `access_token` (response).
 */
internal suspend fun HttpResponse.toTrafficEntry(durationMs: Long, includeBody: Boolean): HttpTrafficEntry {
    val body = if (includeBody) {
        try {
            sanitizeBody(bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
    return HttpTrafficEntry(
        method = request.method.value,
        url = sanitizeUrl(request.url.toString()),
        status = status.value,
        durationMs = durationMs,
        requestHeaders = request.headers.entries().flatMap { (name, values) ->
            when {
                name.equals(HttpHeaders.Authorization, ignoreCase = true) -> listOf(name to "***")
                visibleRequestHeaders.any { it.equals(name, ignoreCase = true) } -> values.map { name to it }
                else -> emptyList()
            }
        },
        responseHeaders = headers.entries().flatMap { (name, values) ->
            if (visibleResponseHeaders.any { it.equals(name, ignoreCase = true) }) values.map { name to it } else emptyList()
        },
        responseBody = body,
        error = null,
    )
}

/** Bản ghi cho request lỗi trước khi có phản hồi. [url] đã là chuỗi đã làm sạch. */
internal fun failedTrafficEntry(method: String, url: String, durationMs: Long, errorName: String) = HttpTrafficEntry(
    method = method,
    url = sanitizeUrl(url),
    status = null,
    durationMs = durationMs,
    requestHeaders = emptyList(),
    responseHeaders = emptyList(),
    responseBody = null,
    error = errorName,
)
