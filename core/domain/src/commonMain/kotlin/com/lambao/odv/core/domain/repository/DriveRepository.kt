package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.model.ImageInfo
import com.lambao.odv.core.domain.model.ViewerContext
import androidx.paging.PagingData
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
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

    /**
     * Mọi ảnh và video thuộc [filter] trong drive (mọi thư mục), mới nhất trước theo [libraryDate] (TV-01, TV-02), đọc từ
     * Room theo trang nên drive lớn không nạp hết vào bộ nhớ. Tự làm mới khi đồng bộ đổi dữ liệu (DB-05).
     */
    fun libraryPages(filter: LibraryFilter): Flow<PagingData<DriveItem>>

    /**
     * Số mục theo từng ngày của Thư viện, mới nhất trước, nhóm theo [dayNumberOf] với độ lệch [utcOffsetMs]. Dùng cho
     * số mục ở tiêu đề nhóm (TV-01) và cuộn nhanh (TV-04).
     */
    fun libraryDays(filter: LibraryFilter, utcOffsetMs: Long): Flow<List<LibraryDay>>

    /**
     * Các ảnh để vuốt trước/sau trong màn xem ảnh (AN-03), đúng thứ tự người dùng thấy ở nơi mở: tab Thư mục theo
     * [ViewerContext.Folder.sort], tab Thư viện mới nhất trước. Chỉ gồm ảnh (video và PDF có màn xem riêng). Đọc từ Room
     * nên dùng được offline, tự phát lại khi đồng bộ đổi dữ liệu (ảnh bị xóa trên OneDrive tự biến mất, DS-06).
     */
    fun observeViewerImages(context: ViewerContext): Flow<List<DriveItem>>

    /**
     * Kích thước ảnh và thiết bị chụp cho bảng thông tin (AN-05). Gọi Graph theo yêu cầu cho một ảnh (không lưu Room),
     * nên offline trả lỗi và UI chỉ hiện các dòng đã có trong Room.
     */
    suspend fun getImageInfo(itemId: String): AppResult<ImageInfo>

    /**
     * Các thư mục từ cấp dưới gốc xuống thư mục chứa [itemId], đọc từ Room (bảng thông tin: đường dẫn thư mục, AN-05).
     * Rỗng nếu tệp nằm ngay ở gốc hoặc chưa có trong Room.
     */
    suspend fun folderPathOf(itemId: String): List<FolderRef>

    companion object {
        const val SEARCH_LIMIT = 100
    }
}
