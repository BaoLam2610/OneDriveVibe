package com.lambao.odv.core.network

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

internal val NetworkJson = Json {
    ignoreUnknownKeys = true
}

private const val MAX_RETRIES = 2
private const val MAX_RETRY_AFTER_SECONDS = 60L

/**
 * HttpClient dùng chung cho token và Graph (ADR-0006). Engine do nền tảng cung cấp qua classpath (OkHttp trên Android).
 *
 * Cố ý KHÔNG cài plugin Logging (CH-06): header `Authorization` và body request lấy token (chứa `client_secret`) không
 * được vào log. Lưu lượng cho màn Debug đi qua [HttpTrafficRecorder] (đã làm sạch), không qua plugin Logging.
 */
internal fun createHttpClient(): HttpClient = HttpClient {
    // 4xx/5xx không ném ngoại lệ: GraphApi tự đọc status và ánh xạ sang AppError.
    expectSuccess = false
    // Không tự theo chuyển hướng: bearer và body lấy token không được gửi sang máy chủ khác (CH-06). Các lát tải tệp
    // (downloadUrl có tempauth) dùng client riêng không mang bearer.
    followRedirects = false
    install(ContentNegotiation) { json(NetworkJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
    // Thử lại lỗi tạm thời cho cả token lẫn Graph (TK-05, TK-06): lỗi mạng, timeout, 429, 5xx. Không thử lại 4xx khác:
    // 400/401/403/404 là lỗi cấu hình hoặc dữ liệu, thử lại không đổi kết quả (TK-04). 401 do GraphApi tự xử lý
    // (lấy token mới, thử lại đúng một lần, TK-02) nên không nằm ở đây.
    install(HttpRequestRetry) {
        maxRetries = MAX_RETRIES
        retryIf { _, response -> response.status.value == 429 || response.status.value >= 500 }
        retryOnExceptionIf { _, cause ->
            cause !is CancellationException &&
                (
                    cause is HttpRequestTimeoutException ||
                        cause is ConnectTimeoutException ||
                        cause is SocketTimeoutException ||
                        cause.isNetworkFailure()
                    )
        }
        // 429/503 chờ đúng Retry-After (giới hạn 60 giây để màn hình không treo); lỗi khác chờ 1 giây rồi 2 giây.
        delayMillis(respectRetryAfterHeader = false) { retry ->
            val retryAfter = response?.headers?.get(HttpHeaders.RetryAfter)?.trim()?.toLongOrNull()
            if (retryAfter != null) retryAfter.coerceIn(1, MAX_RETRY_AFTER_SECONDS) * 1000 else 1000L shl (retry - 1)
        }
    }
}
