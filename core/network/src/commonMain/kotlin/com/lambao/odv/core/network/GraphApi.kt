package com.lambao.odv.core.network

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.network.dto.ChildrenPageDto
import com.lambao.odv.core.network.dto.DeltaPageDto
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
 * Số mục mỗi trang delta. Đo thực tế trên drive khoảng 3.200 mục: `200` ra 16 trang (~8 giây vì các trang nối đuôi nhau,
 * mỗi trang ~450 ms), `1000` ra 4 trang (~4,5 giây, mỗi trang 175 đến 210 KB, 1,2 đến 1,4 giây). Graph chấp nhận 1000 nên
 * không bị cắt về 200. Đánh đổi: mỗi transaction Room lớn hơn và tắt app giữa chừng mất tối đa một trang lớn hơn (DB-04).
 * `children` giữ [CHILDREN_PAGE_SIZE] vì chưa đo.
 */
private const val DELTA_PAGE_SIZE = 1000

/**
 * Đúng các trường `DriveItemDto` đọc khi đồng bộ (bỏ `createdBy`, `lastModifiedBy`, `shared`... để giảm payload và thời
 * gian parse trên drive lớn). `deleted` và `root` phải có mặt: thiếu thì không nhận ra mục bị xóa (DS-06) hay thư mục gốc.
 */
private const val DELTA_SELECT =
    "id,name,size,folder,file,video,package,deleted,root,parentReference,cTag," +
        "lastModifiedDateTime,createdDateTime,fileSystemInfo,photo"

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

    /**
     * Một trang delta (DB-01, DB-02, DB-04). [link] null = bắt đầu quét lại từ `/root/delta`; không null = `nextLink`
     * (tiếp tục trang dở) hoặc `deltaLink` (lần đồng bộ sau). [link] đọc từ CSDL nên coi như không tin cậy: chỉ gửi
     * bearer khi nó trỏ về Graph. Mốc hết hiệu lực trả `AppError.Http(410, "resyncRequired")` (DB-03).
     *
     * `$select` chỉ gắn ở request đầu: Graph mang nó sẵn trong `nextLink`/`deltaLink` nên các trang sau và các lần đồng
     * bộ sau giữ cùng tập trường. Đổi [DELTA_SELECT] thì mốc cũ vẫn chạy với tập trường cũ cho tới lần quét đầy đủ sau.
     */
    suspend fun deltaPage(credentials: GraphCredentials, link: String?): AppResult<DeltaPageDto> {
        if (link != null && !isOwnUrl(link)) return AppResult.Failure(AppError.Unknown())
        return authorizedGet(credentials) {
            if (link == null) {
                url {
                    appendDrivePath(credentials)
                    appendPathSegments("root", "delta")
                }
                parameter("\$select", DELTA_SELECT)
                parameter("\$top", DELTA_PAGE_SIZE)
            } else {
                url.takeFrom(link)
            }
        }.decode()
    }

    private fun URLBuilder.appendDrivePath(credentials: GraphCredentials) {
        takeFrom(baseUrl)
        appendPathSegments("users", credentials.upn, "drive")
    }
}
