package com.lambao.odv.core.network

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.network.dto.ChildrenPageDto
import com.lambao.odv.core.network.dto.DriveDto
import com.lambao.odv.core.network.dto.DriveItemDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom

private const val GRAPH_BASE = "https://graph.microsoft.com/v1.0"
private const val CHILDREN_SELECT = "id,name,size,folder,file,video,package"
private const val CHILDREN_PAGE_SIZE = 200

/**
 * Gọi Microsoft Graph, chỉ đọc (ADR-0006). Mọi đường dẫn đi qua `/users/{upn}/drive` vì app-only không có `/me` (ADR-0005).
 *
 * - Tự gắn `Authorization: Bearer` từ [TokenProvider]; bearer không bao giờ vào log (CH-06).
 * - `401`: bỏ token, lấy token mới và thử lại đúng một lần (TK-02).
 * - Lỗi tạm thời (mạng, `429`, `5xx`) thử lại có backoff theo [withRetry] (TK-05, TK-06).
 */
class GraphApi internal constructor(
    private val http: HttpClient,
    private val tokens: TokenProvider,
) {

    /** Kiểm tra kết nối: `GET /users/{upn}/drive?$select=id,driveType,quota` (KN-07). */
    suspend fun getDrive(credentials: GraphCredentials): AppResult<DriveDto> =
        get(credentials) {
            url { appendDrivePath(credentials) }
            parameter("\$select", "id,driveType,quota")
        }.decode()

    /**
     * Mọi mục con của thư mục [folderId] (null = gốc), gom đủ các trang `@odata.nextLink`.
     * Chỉ theo `nextLink` trỏ về Graph: bearer không được gửi tới máy chủ khác.
     */
    suspend fun listChildren(credentials: GraphCredentials, folderId: String?): AppResult<List<DriveItemDto>> {
        val items = mutableListOf<DriveItemDto>()
        var nextLink: String? = null
        do {
            val link = nextLink
            val response = get(credentials) {
                if (link == null) {
                    url {
                        appendDrivePath(credentials)
                        if (folderId == null) appendPathSegments("root", "children") else appendPathSegments("items", folderId, "children")
                    }
                    parameter("\$select", CHILDREN_SELECT)
                    parameter("\$top", CHILDREN_PAGE_SIZE)
                } else {
                    url.takeFrom(link)
                }
            }
            val page = when (val decoded = response.decode<ChildrenPageDto>()) {
                is AppResult.Success -> decoded.value
                is AppResult.Failure -> return decoded
            }
            items += page.value
            nextLink = page.nextLink
            if (nextLink != null && !nextLink.startsWith("$GRAPH_BASE/")) return AppResult.Failure(AppError.Unknown())
        } while (nextLink != null)
        return AppResult.Success(items)
    }

    private fun URLBuilder.appendDrivePath(credentials: GraphCredentials) {
        takeFrom(GRAPH_BASE)
        appendPathSegments("users", credentials.upn, "drive")
    }

    /** GET có token, thử lại `401` một lần và lỗi tạm thời theo chính sách. Chỉ trả [HttpResponse] 2xx. */
    private suspend fun get(
        credentials: GraphCredentials,
        build: HttpRequestBuilder.() -> Unit,
    ): AppResult<HttpResponse> {
        var tokenRefreshed = false
        while (true) {
            val token = when (val result = tokens.token(credentials)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> return result
            }
            val result = withRetry { send(token, build) }
            val unauthorized = result is AppResult.Failure && (result.error as? AppError.Http)?.status == 401
            if (unauthorized && !tokenRefreshed) {
                tokenRefreshed = true
                tokens.invalidate(token)
                continue
            }
            return result
        }
    }

    private suspend fun send(token: String, build: HttpRequestBuilder.() -> Unit): AppResult<HttpResponse> {
        val response = try {
            http.get {
                build()
                bearerAuth(token)
            }
        } catch (e: Throwable) {
            return AppResult.Failure(e.toAppError())
        }
        return if (response.status.isSuccess()) AppResult.Success(response) else AppResult.Failure(response.toGraphError())
    }

    private suspend inline fun <reified T> AppResult<HttpResponse>.decode(): AppResult<T> = when (this) {
        is AppResult.Failure -> this
        is AppResult.Success -> try {
            AppResult.Success(value.body<T>())
        } catch (e: Throwable) {
            AppResult.Failure(e.toAppError())
        }
    }
}
