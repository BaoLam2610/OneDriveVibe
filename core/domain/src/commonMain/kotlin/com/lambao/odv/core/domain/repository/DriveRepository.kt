package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SearchResult
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/** Đọc drive OneDrive qua Graph (chỉ đọc, Files.Read.All). */
interface DriveRepository {

    /**
     * Kiểm tra kết nối bằng [config] chưa lưu (KN-07): lấy token rồi gọi drive. Thành công khi cả hai trả 2xx (KN-08).
     * Không trả thông tin drive hay dung lượng (KN-12).
     * Lỗi trả về dạng [com.lambao.odv.core.common.error.AppError], UI chọn thông báo theo mã (KN-09).
     */
    suspend fun verifyConnection(config: ConnectionConfig): AppResult<Unit>

    /**
     * Liệt kê toàn bộ mục con của thư mục [folderId] (null = thư mục gốc), gom mọi trang. Dùng config đã lưu.
     * Gọi thẳng API, không qua Room: chỉ dùng cho TM-07, khi quét lần đầu chưa xong ([SyncState.initialSyncDone] false).
     */
    suspend fun listChildren(folderId: String?): AppResult<List<DriveItem>>

    /**
     * Mục con của thư mục [folderId] (null = thư mục gốc) đọc từ Room, tự phát lại khi đồng bộ làm đổi dữ liệu (DB-05).
     * Sắp xếp theo [sort] với thư mục trước tệp (TM-02, TM-05). Chưa lọc theo loại tệp: nơi gọi lọc (TM-03).
     * Rỗng khi chưa đồng bộ xong lần đầu hoặc chưa biết id thư mục gốc.
     */
    fun observeChildren(folderId: String?, sort: SortOrder): Flow<List<DriveItem>>

    /**
     * Tìm theo tên tệp/thư mục trên dữ liệu đã đồng bộ, không gọi API, dùng được khi offline (DS-03). Không phân biệt
     * hoa thường và dấu tiếng Việt. Chỉ trả thư mục và tệp thuộc [kinds] (loại tệp được bật). Tối đa [limit] kết quả,
     * thư mục trước rồi theo tên. [query] rỗng thì trả danh sách rỗng.
     */
    suspend fun search(query: String, kinds: Set<MediaKind>, limit: Int = SEARCH_LIMIT): List<SearchResult>

    companion object {
        const val SEARCH_LIMIT = 100
    }
}
