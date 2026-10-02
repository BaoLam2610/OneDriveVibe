package com.lambao.odv.core.network

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.network.dto.TokenResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.http.takeFrom
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val TOKEN_ENDPOINT = "https://login.microsoftonline.com"
private const val GRAPH_SCOPE = "https://graph.microsoft.com/.default"
private val REFRESH_MARGIN = 5.minutes

/**
 * Lấy và giữ access token Client Credentials (ADR-0005).
 *
 * - Token **chỉ nằm trong bộ nhớ**, không ghi xuống đĩa (CH-03).
 * - Làm mới chủ động khi còn dưới 5 phút là hết hạn (TK-01).
 * - Nhiều yêu cầu cùng lúc cần token mới thì chỉ một yêu cầu gọi endpoint, số còn lại chờ rồi dùng chung kết quả (TK-03):
 *   [Mutex] giữ suốt lúc gọi nên các yêu cầu đến sau thấy token đã có trong cache.
 * - Lỗi mạng/`5xx`/`429` thử lại có backoff (TK-05, TK-06). Lỗi secret sai hay hết hạn (AADSTS7000215, AADSTS7000222)
 *   trả về ngay, không thử lại (TK-04).
 */
class TokenProvider internal constructor(
    private val http: HttpClient,
    private val recorder: HttpTrafficRecorder? = null,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private class Cached(val owner: GraphCredentials, val value: String, val expiresAt: TimeMark)

    private val mutex = Mutex()
    // @Volatile: clear() ghi không qua mutex để khóa app xóa token ngay, không phải chờ lần lấy token đang retry (CH-03).
    @Volatile
    private var cached: Cached? = null

    // Tăng mỗi lần clear(): lần lấy token đang bay lúc khóa app không được ghi token vào cache sau khi đã xóa.
    @Volatile
    private var generation = 0

    /** Token còn dùng được cho [credentials]; gọi endpoint nếu chưa có, sắp hết hạn, hoặc credentials đã đổi. */
    suspend fun token(credentials: GraphCredentials): AppResult<String> = mutex.withLock {
        val startGeneration = generation
        val current = cached
        if (current != null && current.owner.sameApp(credentials) && !(current.expiresAt - REFRESH_MARGIN).hasPassedNow()) {
            return@withLock AppResult.Success(current.value)
        }
        // Lấy token mới thất bại thì giữ nguyên cache cũ: token cũ còn hạn vẫn dùng được cho lần gọi sau.
        when (val fetched = fetch(credentials)) {
            is AppResult.Success -> {
                if (startGeneration == generation) cached = Cached(credentials, fetched.value.value, fetched.value.expiresAt)
                AppResult.Success(fetched.value.value)
            }
            is AppResult.Failure -> fetched
        }
    }

    /** Bỏ token đang giữ, ví dụ khi Graph trả `401` (TK-02). Lần gọi [token] kế tiếp sẽ lấy token mới. */
    suspend fun invalidate(rejectedToken: String) = mutex.withLock {
        // Chỉ bỏ đúng token vừa bị từ chối: nếu yêu cầu khác đã lấy token mới rồi thì giữ nguyên (TK-03).
        if (cached?.value == rejectedToken) cached = null
    }

    /** Xóa token khỏi bộ nhớ khi khóa app hoặc ngắt kết nối (CH-03). */
    fun clear() {
        generation++
        cached = null
    }

    private class Fetched(val value: String, val expiresAt: TimeMark)

    private suspend fun fetch(credentials: GraphCredentials): AppResult<Fetched> {
        val started = timeSource.markNow()
        val response = try {
            http.post {
                url {
                    takeFrom(TOKEN_ENDPOINT)
                    appendPathSegments(credentials.tenantId, "oauth2", "v2.0", "token")
                }
                setBody(
                    FormDataContent(
                        parameters {
                            append("client_id", credentials.clientId)
                            append("client_secret", credentials.clientSecret)
                            append("scope", GRAPH_SCOPE)
                            append("grant_type", "client_credentials")
                        },
                    ),
                )
            }
        } catch (e: Throwable) {
            val error = e.toAppError()
            recorder?.record(
                failedTrafficEntry(
                    "POST",
                    "$TOKEN_ENDPOINT/${credentials.tenantId}/oauth2/v2.0/token",
                    started.elapsedNow().inWholeMilliseconds,
                    error::class.simpleName.orEmpty(),
                ),
            )
            return AppResult.Failure(error)
        }
        // includeBody = false: body request chứa client_secret, body response chứa access_token (CH-06).
        recorder?.record(response.toTrafficEntry(started.elapsedNow().inWholeMilliseconds, includeBody = false))
        if (!response.status.isSuccess()) return AppResult.Failure(response.toTokenError())
        return try {
            val dto = response.body<TokenResponseDto>()
            AppResult.Success(Fetched(dto.accessToken, timeSource.markNow() + dto.expiresInSeconds.seconds))
        } catch (e: Throwable) {
            AppResult.Failure(e.toAppError())
        }
    }

    // Token cấp cho (tenant, client, secret); UPN không ảnh hưởng token.
    private fun GraphCredentials.sameApp(other: GraphCredentials) =
        tenantId == other.tenantId && clientId == other.clientId && clientSecret == other.clientSecret
}
