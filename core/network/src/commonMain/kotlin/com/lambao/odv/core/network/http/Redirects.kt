package com.lambao.odv.core.network.http

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

private const val HTTPS_PREFIX = "https://"

/** Phản hồi là chuyển hướng 3xx. Client Graph không tự theo (CH-06), nên nơi gọi tự quyết định có theo hay không. */
internal fun HttpResponse.isRedirect(): Boolean = status.value in 300..399

/**
 * `Location` của chuyển hướng nếu là `https`, không thì null. URL do máy chủ trả (link ký của thumbnail, ảnh gốc, video) nên
 * coi là không tin cậy: chỉ nhận `https`, và bearer không bao giờ được gửi theo (CH-06).
 */
internal fun HttpResponse.httpsLocationOrNull(): String? =
    headers[HttpHeaders.Location]?.takeIf { it.startsWith(HTTPS_PREFIX) }
