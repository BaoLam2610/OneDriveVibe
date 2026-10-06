package com.lambao.odv.core.network.graph

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.getOrElse
import com.lambao.odv.core.network.GraphConstants
import com.lambao.odv.core.network.auth.BearerTokenSource
import com.lambao.odv.core.network.auth.GraphCredentials
import com.lambao.odv.core.network.auth.TokenProvider
import com.lambao.odv.core.network.graph.dto.ChildrenPageDto
import com.lambao.odv.core.network.graph.dto.DeltaPageDto
import com.lambao.odv.core.network.graph.dto.DriveDto
import com.lambao.odv.core.network.graph.dto.DriveItemDto
import com.lambao.odv.core.network.http.ApiService
import com.lambao.odv.core.network.http.httpsLocationOrNull
import com.lambao.odv.core.network.http.isRedirect
import com.lambao.odv.core.network.http.toAppError
import com.lambao.odv.core.network.traffic.HttpTrafficRecorder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException

/**
 * Microsoft Graph, chỉ đọc (ADR-0006). Mọi đường dẫn đi qua `/users/{upn}/drive` vì app-only không có `/me` (ADR-0005).
 * Token, thử lại `401`, ghi lưu lượng nằm ở [ApiService]; lớp này chỉ biết các endpoint của OneDrive và dạng lỗi của Graph.
 * Credentials đến từ [GraphCredentialsSource] ở mỗi lời gọi nên nơi gọi không phải load config rồi truyền vào; ngoại lệ duy nhất
 * là [getDrive] (kiểm tra kết nối KN-07) vì dùng config chưa lưu.
 */
class GraphApi internal constructor(
    http: HttpClient,
    private val tokens: TokenProvider,
    recorder: HttpTrafficRecorder? = null,
    /** Client không bearer, dành cho URL đã ký sẵn do Graph trả (thumbnail). Xem `createDownloadHttpClient` (package `http`). */
    private val signedDownloads: HttpClient,
    /** Credentials của người dùng hiện tại, lấy lại ở mỗi lời gọi (trừ [getDrive], nhận credentials chưa lưu). */
    private val credentialsSource: GraphCredentialsSource,
) : ApiService(http, recorder, GraphConstants.BASE_URL) {

    override suspend fun mapError(response: HttpResponse): AppError = response.toGraphError()

    /**
     * Cầu nối từ credentials của một lời gọi sang [BearerTokenSource] mà [ApiService] cần. `ApiService` không biết
     * [GraphCredentials]; chỉ lớp này biết token Graph được lấy theo credentials nào.
     */
    private fun GraphCredentials.bearer(): BearerTokenSource = object : BearerTokenSource {
        override suspend fun token(): AppResult<String> = tokens.token(this@bearer)
        override suspend fun invalidate(rejected: String) = tokens.invalidate(rejected)
    }

    /** Kiểm tra kết nối: `GET /users/{upn}/drive?$select=id,driveType` (KN-07). Không xin `quota` (KN-12). */
    suspend fun getDrive(credentials: GraphCredentials): AppResult<DriveDto> =
        authorizedGet(credentials.bearer()) {
            url { appendDrivePath(credentials) }
            parameter("\$select", GraphConstants.DRIVE_SELECT)
        }.decode()

    /**
     * Mọi mục con của thư mục [folderId] (null = gốc), gom đủ các trang `@odata.nextLink`.
     * Chỉ theo `nextLink` trỏ về Graph: bearer không được gửi tới máy chủ khác. Nếu một trang trỏ ra ngoài thì bỏ cả
     * kết quả (không trả dữ liệu dở dang) và báo lỗi.
     */
    suspend fun listChildren(folderId: String?): AppResult<List<DriveItemDto>> {
        val credentials = credentialsSource.current().getOrElse { return AppResult.Failure(it) }
        val items = mutableListOf<DriveItemDto>()
        var nextLink: String? = null
        do {
            val link = nextLink
            val response = authorizedGet(credentials.bearer()) {
                if (link == null) {
                    url {
                        appendDrivePath(credentials)
                        if (folderId == null) appendPathSegments("root", "children") else appendPathSegments("items", folderId, "children")
                    }
                    parameter("\$select", GraphConstants.CHILDREN_SELECT)
                    parameter("\$top", GraphConstants.CHILDREN_PAGE_SIZE)
                } else {
                    url.takeFrom(link)
                }
            }
            val page = response.decode<ChildrenPageDto>().getOrElse { return AppResult.Failure(it) }
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
     * bộ sau giữ cùng tập trường. Đổi [GraphConstants.DELTA_SELECT] thì mốc cũ vẫn chạy với tập trường cũ cho tới lần quét đầy đủ sau.
     */
    suspend fun deltaPage(link: String?): AppResult<DeltaPageDto> {
        if (link != null && !isOwnUrl(link)) return AppResult.Failure(AppError.Unknown())
        val credentials = credentialsSource.current().getOrElse { return AppResult.Failure(it) }
        return authorizedGet(credentials.bearer()) {
            if (link == null) {
                url {
                    appendDrivePath(credentials)
                    appendPathSegments("root", "delta")
                }
                parameter("\$select", GraphConstants.DELTA_SELECT)
                parameter("\$top", GraphConstants.DELTA_PAGE_SIZE)
            } else {
                url.takeFrom(link)
            }
        }.decode()
    }

    /**
     * Ảnh thumbnail của [itemId] ở kích thước [size] (tên có sẵn như `medium` hoặc tùy chỉnh như `c300x300_crop`).
     * Graph thường trả `302` sang URL đã ký trên CDN: bearer chỉ gửi tới Graph, bước hai gọi URL đó bằng client không
     * bearer. Graph trả thẳng `200` thì dùng luôn nội dung. Bước một vẫn được ghi vào màn Debug như mọi request Graph
     * (bản debug, ADR-0013); bước hai (ảnh nhị phân, URL đã ký) thì không.
     * Tệp không có thumbnail trả `AppError.Http(404, ...)` để nơi gọi giữ ô giữ chỗ.
     */
    suspend fun fetchThumbnail(itemId: String, size: String): AppResult<ByteArray> {
        val credentials = credentialsSource.current().getOrElse { return AppResult.Failure(it) }
        val response = authorizedGet(credentials.bearer(), acceptRedirect = true) {
            url {
                appendDrivePath(credentials)
                appendPathSegments("items", itemId, "thumbnails", "0", size, "content")
            }
        }.getOrElse { return AppResult.Failure(it) }
        if (!response.isRedirect()) return response.readBytes()
        // Chỉ theo chuyển hướng tới HTTPS: URL do máy chủ trả nên coi là không tin cậy.
        val location = response.httpsLocationOrNull() ?: return AppResult.Failure(AppError.Unknown())
        return try {
            signedDownloads.get(location).readBytes()
        } catch (e: Throwable) {
            AppResult.Failure(e.toAppError())
        }
    }

    /**
     * Thông tin ảnh để hiện ở bảng thông tin (AN-05): kích thước, thiết bị chụp, ngày. Lấy theo yêu cầu thay vì đồng bộ
     * hàng loạt vì chỉ cần khi người dùng mở bảng cho đúng một ảnh (quyết định 2026-10-04, không đổi schema Room).
     */
    suspend fun getItemInfo(itemId: String): AppResult<DriveItemDto> {
        val credentials = credentialsSource.current().getOrElse { return AppResult.Failure(it) }
        return authorizedGet(credentials.bearer()) {
            url {
                appendDrivePath(credentials)
                appendPathSegments("items", itemId)
            }
            parameter("\$select", GraphConstants.ITEM_INFO_SELECT)
        }.decode()
    }

    /**
     * Tải nội dung gốc của [itemId] theo luồng, từ byte [offset] (BN-03). `GET /items/{id}/content` trả `302` sang URL đã
     * ký; bước hai tải bằng client không bearer với header `Range` nên bearer không rời Graph.
     *
     * [onStart] gọi một lần khi có phản hồi: `resumed` là true khi máy chủ trả `206` (tiếp tục từ [offset]), false khi trả
     * `200` (bỏ qua Range, nơi gọi phải ghi lại từ đầu); `totalBytes` là dung lượng cả tệp nếu biết. [onBytes] nhận từng
     * khúc đã đọc (`length` byte đầu của mảng, mảng được dùng lại nên phải ghi ngay). Đã đủ [offset] mà máy chủ trả `416`
     * thì lỗi `Http(416)`: nơi gọi tải lại từ đầu.
     */
    suspend fun downloadContent(
        itemId: String,
        offset: Long,
        onStart: suspend (resumed: Boolean, totalBytes: Long?) -> Unit,
        onBytes: suspend (buffer: ByteArray, length: Int) -> Unit,
    ): AppResult<Unit> {
        val credentials = credentialsSource.current().getOrElse { return AppResult.Failure(it) }
        val redirect = authorizedGet(credentials.bearer(), acceptRedirect = true) {
            url {
                appendDrivePath(credentials)
                appendPathSegments("items", itemId, "content")
            }
        }.getOrElse { return AppResult.Failure(it) }
        // Graph luôn trả 302 cho /content; không thì không có URL ký để tải theo luồng.
        if (!redirect.isRedirect()) return AppResult.Failure(AppError.Unknown())
        val location = redirect.httpsLocationOrNull() ?: return AppResult.Failure(AppError.Unknown())
        return try {
            signedDownloads.prepareGet(location) {
                if (offset > 0) header(HttpHeaders.Range, "bytes=$offset-")
                // Ảnh gốc lớn tải lâu hơn 30 giây là bình thường: bỏ trần tổng, chỉ giữ trần im lặng của socket.
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
            }.execute { response ->
                if (!response.status.isSuccess()) return@execute AppResult.Failure(response.toGraphError())
                val resumed = response.status.value == 206
                val total = if (resumed) {
                    response.headers[HttpHeaders.ContentRange]?.substringAfterLast('/')?.toLongOrNull()
                } else {
                    response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                }
                onStart(resumed, total)
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(GraphConstants.DOWNLOAD_CHUNK_BYTES)
                while (true) {
                    val read = channel.readAvailable(buffer)
                    if (read == -1) break
                    onBytes(buffer, read)
                }
                AppResult.Success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            AppResult.Failure(e.toAppError())
        }
    }

    /**
     * Link tải đã ký sẵn của [itemId] (`@microsoft.graph.downloadUrl`, sống khoảng 1 giờ) để đưa vào trình phát video
     * (VD-11, VD-14). Gọi lại hàm này là cách lấy link mới khi link cũ hết hạn: không lấy lại access token.
     * Link chứa `tempauth` nên coi là bí mật: không ghi log (CH-06). Chỉ nhận `https` vì giá trị do máy chủ trả.
     * Tệp đã bị xóa trả `AppError.Http(404, ...)`.
     */
    suspend fun getDownloadUrl(itemId: String): AppResult<String> {
        val credentials = credentialsSource.current().getOrElse { return AppResult.Failure(it) }
        // Dùng `/content` (302 → link ký ở header Location) thay vì `$select=@microsoft.graph.downloadUrl`: cách này đã chạy ở
        // ảnh gốc (`downloadContent`), còn `$select` có chứa `@` đã trả phản hồi không có link khi thử ở Lát 6 (log 2026-10-04).
        // Không gọi link ở đây: chỉ đọc header, nên không tải byte nào của video.
        val response = authorizedGet(credentials.bearer(), acceptRedirect = true) {
            url {
                appendDrivePath(credentials)
                appendPathSegments("items", itemId, "content")
            }
        }.getOrElse { return AppResult.Failure(it) }
        // Chỉ nhận chuyển hướng tới HTTPS: URL do máy chủ trả nên coi là không tin cậy.
        val location = response.httpsLocationOrNull()
        if (!response.isRedirect() || location == null) {
            // Không ghi giá trị: link chứa tempauth (CH-06). Chỉ ghi mã trạng thái và có/không có Location.
            val hasLocation = response.headers[HttpHeaders.Location] != null
            Logger.withTag("GraphApi").w { "getDownloadUrl: không có chuyển hướng hợp lệ (HTTP ${response.status.value}, Location=$hasLocation)" }
            return AppResult.Failure(AppError.Unknown())
        }
        return AppResult.Success(location)
    }

    private suspend fun HttpResponse.readBytes(): AppResult<ByteArray> =
        if (status.isSuccess()) {
            try {
                AppResult.Success(body<ByteArray>())
            } catch (e: Throwable) {
                AppResult.Failure(e.toAppError())
            }
        } else {
            AppResult.Failure(toGraphError())
        }

    private fun URLBuilder.appendDrivePath(credentials: GraphCredentials) {
        takeFrom(baseUrl)
        appendPathSegments("users", credentials.upn, "drive")
    }
}
