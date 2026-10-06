package com.lambao.odv.core.network.graph

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.network.graph.dto.GraphErrorEnvelopeDto
import com.lambao.odv.core.network.http.readJsonOrNull
import com.lambao.odv.core.network.http.retryAfterSeconds
import io.ktor.client.statement.HttpResponse

/** Lỗi từ Graph: lấy `error.code` (vd. `itemNotFound`) và `Retry-After`. Không đọc `message` của máy chủ (KN-09). */
internal suspend fun HttpResponse.toGraphError(): AppError.Http {
    val code = readJsonOrNull<GraphErrorEnvelopeDto>()?.error?.code
    return AppError.Http(status.value, code, retryAfterSeconds())
}
