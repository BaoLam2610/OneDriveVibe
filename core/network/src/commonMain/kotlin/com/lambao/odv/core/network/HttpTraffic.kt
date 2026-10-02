package com.lambao.odv.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

/**
 * Một request/response để gỡ lỗi trong màn Debug (ADR-0013). Chứa dữ liệu **đầy đủ, chưa che**: header `Authorization`,
 * body request lấy token (có `client_secret`), body response (có `access_token`, `downloadUrl`...). Vì vậy:
 * - chỉ bản debug có [HttpTrafficRecorder] cài đặt; bản release không ghi gì;
 * - chỉ nằm trong bộ nhớ, không ghi xuống đĩa, không vào Logcat;
 * - màn Debug mặc định hiện đầy đủ theo yêu cầu, có công tắc che lại: dùng [masked].
 */
class HttpTrafficEntry(
    val method: String,
    val url: String,
    /** Null khi lỗi trước khi nhận được phản hồi (mất mạng, timeout). */
    val status: Int?,
    val durationMs: Long,
    val requestHeaders: List<Pair<String, String>>,
    /** Body request đã gửi: `form-urlencoded` với endpoint token, null với GET. */
    val requestBody: String?,
    val responseHeaders: List<Pair<String, String>>,
    val responseBody: String?,
    /** Tên loại lỗi (vd. `Network`, `Timeout`), không chứa nội dung ngoại lệ. */
    val error: String?,
)

/**
 * Nơi nhận lưu lượng mạng để gỡ lỗi. Chỉ bản debug cài đặt (module `:tools:debug`); bản release không có bản cài nên
 * `:core:network` không ghi gì và không tốn công dựng bản ghi.
 */
interface HttpTrafficRecorder {
    fun record(entry: HttpTrafficEntry)
}

// Giới hạn bộ nhớ cho một body (ký tự). Response một trang 200 mục chỉ cỡ trăm KB; vượt thì cắt và ghi rõ.
private const val MAX_BODY_CHARS = 1_000_000

// Không dùng dấu gạch ngược để tránh lỗi escape trong chuỗi Kotlin.
// Dừng ở `&`, dấu nháy kép hoặc khoảng trắng: URL nằm trong chuỗi JSON thì dấu `"` đóng chuỗi phải còn, nếu không body
// mất khả năng parse thành JSON ở màn chi tiết.
private val secretQueryParam = Regex("""([?&](?:tempauth|sig|access_token)=)[^&"\s]*""")
private val secretFormField = Regex("((?:client_secret|access_token|refresh_token|id_token)=)[^&]*")
private val secretJsonValue =
    Regex("""("(?:@microsoft\.graph\.downloadUrl(?:NoAuth)?|access_token|refresh_token|client_secret|id_token)"\s*:\s*)"[^"]*"""")

private fun maskText(text: String): String = text
    .replace(secretJsonValue, "\$1\"***\"")
    .replace(secretFormField, "\$1***")
    .replace(secretQueryParam, "\$1***")

/**
 * Bản đã che của [HttpTrafficEntry]: `Authorization` thành `***`, `client_secret`/token/`downloadUrl`/`tempauth` thành `***`
 * trong URL, body và header. Dùng khi người dùng bật công tắc che trong màn Debug.
 */
fun HttpTrafficEntry.masked() = HttpTrafficEntry(
    method = method,
    url = maskText(url),
    status = status,
    durationMs = durationMs,
    requestHeaders = requestHeaders.map { (name, value) ->
        name to if (name.equals(HttpHeaders.Authorization, ignoreCase = true) || name.equals("Cookie", ignoreCase = true)) "***" else maskText(value)
    },
    requestBody = requestBody?.let(::maskText),
    responseHeaders = responseHeaders.map { (name, value) ->
        name to if (name.equals("Set-Cookie", ignoreCase = true)) "***" else maskText(value)
    },
    responseBody = responseBody?.let(::maskText),
    error = error,
)

/** Chỉ URL đã che. Dùng ở danh sách API: không cần che cả body lớn của từng dòng như [masked]. */
fun HttpTrafficEntry.maskedUrl(): String = maskText(url)

/** Dựng bản ghi đầy đủ từ phản hồi. [requestBody] là body đã gửi (null nếu không có). */
internal suspend fun HttpResponse.toTrafficEntry(durationMs: Long, requestBody: String?): HttpTrafficEntry {
    val body = try {
        bodyAsText().let { if (it.length > MAX_BODY_CHARS) it.take(MAX_BODY_CHARS) + "\n… (cắt, còn ${it.length - MAX_BODY_CHARS} ký tự)" else it }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    return HttpTrafficEntry(
        method = request.method.value,
        url = request.url.toString(),
        status = status.value,
        durationMs = durationMs,
        requestHeaders = request.headers.entries().flatMap { (name, values) -> values.map { name to it } },
        requestBody = requestBody,
        responseHeaders = headers.entries().flatMap { (name, values) -> values.map { name to it } },
        responseBody = body,
        error = null,
    )
}

/** Bản ghi cho request lỗi trước khi có phản hồi. */
internal fun failedTrafficEntry(method: String, url: String, durationMs: Long, errorName: String, requestBody: String? = null) =
    HttpTrafficEntry(
        method = method,
        url = url,
        status = null,
        durationMs = durationMs,
        requestHeaders = emptyList(),
        requestBody = requestBody,
        responseHeaders = emptyList(),
        responseBody = null,
        error = errorName,
    )
