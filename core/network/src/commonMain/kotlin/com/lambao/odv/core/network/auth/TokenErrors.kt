package com.lambao.odv.core.network.auth

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.network.http.readJsonOrNull
import com.lambao.odv.core.network.http.retryAfterSeconds
import io.ktor.client.statement.HttpResponse

private val aadstsPattern = Regex("AADSTS[0-9]+")

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
