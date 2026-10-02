package com.lambao.odv.core.network

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlin.time.TimeSource

/**
 * Lớp cơ sở cho mọi service gọi một API cần Bearer token (hiện chỉ có [GraphApi]). Gom phần dùng chung để service con
 * chỉ còn lo đường dẫn và kiểu dữ liệu:
 *
 * - Gắn `Authorization: Bearer` từ [TokenProvider]; bearer không bao giờ vào log (CH-06).
 * - `401`: bỏ đúng token vừa bị từ chối, lấy token mới và thử lại đúng một lần (TK-02, TK-03).
 * - Lỗi tạm thời (mạng, `429`, `5xx`) do plugin HttpRequestRetry của client lo (xem [createHttpClient], TK-05, TK-06).
 * - Ghi lưu lượng đầy đủ, chưa che vào [HttpTrafficRecorder] nếu có (chỉ bản debug, ADR-0013).
 * - Đọc status và ánh xạ lỗi sang [AppError]; không để lộ nội dung lỗi của máy chủ.
 *
 * [baseUrl] là gốc duy nhất mà bearer được phép gửi tới; dùng [isOwnUrl] trước khi theo một URL do máy chủ trả về.
 */
abstract class ApiService internal constructor(
    private val http: HttpClient,
    private val tokens: TokenProvider,
    private val recorder: HttpTrafficRecorder?,
    protected val baseUrl: String,
) {

    /** URL có thuộc [baseUrl] không. Bearer chỉ được gửi tới URL thỏa điều kiện này (vd. `@odata.nextLink`). */
    protected fun isOwnUrl(url: String): Boolean = url.startsWith("$baseUrl/")

    /**
     * GET có token. [configure] dựng phần đường dẫn và tham số của request; không cần gắn `Authorization`.
     * Chỉ trả [HttpResponse] 2xx; mọi lỗi là [AppResult.Failure].
     */
    protected suspend fun authorizedGet(
        credentials: GraphCredentials,
        configure: HttpRequestBuilder.() -> Unit,
    ): AppResult<HttpResponse> {
        var tokenRefreshed = false
        while (true) {
            val token = when (val result = tokens.token(credentials)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> return result
            }
            val result = executeGet(token, configure)
            val unauthorized = result is AppResult.Failure && (result.error as? AppError.Http)?.status == 401
            if (unauthorized && !tokenRefreshed) {
                tokenRefreshed = true
                tokens.invalidate(token)
                continue
            }
            return result
        }
    }

    private suspend fun executeGet(token: String, configure: HttpRequestBuilder.() -> Unit): AppResult<HttpResponse> {
        val started = TimeSource.Monotonic.markNow()
        val response = try {
            http.get {
                configure()
                bearerAuth(token)
            }
        } catch (e: Throwable) {
            val error = e.toAppError()
            // Dựng lại URL thật chỉ khi lỗi (configure không có tác dụng phụ) để màn Debug thấy đúng request đã thất bại.
            val url = HttpRequestBuilder().apply(configure).url.buildString()
            recorder?.record(failedTrafficEntry("GET", url, started.elapsedNow().inWholeMilliseconds, error::class.simpleName.orEmpty()))
            return AppResult.Failure(error)
        }
        recorder?.record(response.toTrafficEntry(started.elapsedNow().inWholeMilliseconds, requestBody = null))
        return if (response.status.isSuccess()) AppResult.Success(response) else AppResult.Failure(response.toGraphError())
    }

    /** Đọc body JSON của phản hồi thành công thành [T]; lỗi parse trở thành [AppResult.Failure]. */
    internal suspend inline fun <reified T> AppResult<HttpResponse>.decode(): AppResult<T> = when (this) {
        is AppResult.Failure -> this
        is AppResult.Success -> try {
            AppResult.Success(value.body<T>())
        } catch (e: Throwable) {
            AppResult.Failure(e.toAppError())
        }
    }
}
