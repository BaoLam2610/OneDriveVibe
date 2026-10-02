package com.lambao.odv.core.network

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.network.dto.ChildrenPageDto
import com.lambao.odv.core.network.dto.DriveDto
import com.lambao.odv.core.network.dto.DriveItemDto
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom

private const val GRAPH_BASE = "https://graph.microsoft.com/v1.0"
private const val CHILDREN_SELECT = "id,name,size,folder,file,video,package"
private const val CHILDREN_PAGE_SIZE = 200

/**
 * Microsoft Graph, chỉ đọc (ADR-0006). Mọi đường dẫn đi qua `/users/{upn}/drive` vì app-only không có `/me` (ADR-0005).
 * Token, thử lại `401`, ghi lưu lượng và ánh xạ lỗi nằm ở [ApiService]; lớp này chỉ biết các endpoint của OneDrive.
 */
class GraphApi internal constructor(
    http: HttpClient,
    tokens: TokenProvider,
    recorder: HttpTrafficRecorder? = null,
) : ApiService(http, tokens, recorder, GRAPH_BASE) {

    /** Kiểm tra kết nối: `GET /users/{upn}/drive?$select=id,driveType` (KN-07). Không xin `quota` (KN-12). */
    suspend fun getDrive(credentials: GraphCredentials): AppResult<DriveDto> =
        authorizedGet(credentials) {
            url { appendDrivePath(credentials) }
            parameter("\$select", "id,driveType")
        }.decode()

    /**
     * Mọi mục con của thư mục [folderId] (null = gốc), gom đủ các trang `@odata.nextLink`.
     * Chỉ theo `nextLink` trỏ về Graph: bearer không được gửi tới máy chủ khác. Nếu một trang trỏ ra ngoài thì bỏ cả
     * kết quả (không trả dữ liệu dở dang) và báo lỗi.
     */
    suspend fun listChildren(credentials: GraphCredentials, folderId: String?): AppResult<List<DriveItemDto>> {
        val items = mutableListOf<DriveItemDto>()
        var nextLink: String? = null
        do {
            val link = nextLink
            val response = authorizedGet(credentials) {
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
            if (nextLink != null && !isOwnUrl(nextLink)) return AppResult.Failure(AppError.Unknown())
        } while (nextLink != null)
        return AppResult.Success(items)
    }

    private fun URLBuilder.appendDrivePath(credentials: GraphCredentials) {
        takeFrom(baseUrl)
        appendPathSegments("users", credentials.upn, "drive")
    }
}
