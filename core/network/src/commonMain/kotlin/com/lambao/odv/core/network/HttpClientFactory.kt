package com.lambao.odv.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val NetworkJson = Json {
    ignoreUnknownKeys = true
}

/**
 * HttpClient dùng chung cho token và Graph (ADR-0006). Engine do nền tảng cung cấp qua classpath (OkHttp trên Android).
 *
 * Cố ý KHÔNG cài plugin Logging (CH-06): header `Authorization` và body request lấy token (chứa `client_secret`) không
 * được vào log. Nếu sau này cần log khi debug, phải dùng `sanitizeHeader { it == HttpHeaders.Authorization }`, chỉ bật
 * ở bản debug và không bao giờ log body request tới endpoint token.
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
}
