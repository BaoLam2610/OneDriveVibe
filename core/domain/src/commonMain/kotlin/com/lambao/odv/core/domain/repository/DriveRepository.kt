package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveItem

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
     * Gọi thẳng API, chưa qua Room: chỉ dùng cho TM-07; từ Lát 3 UI đọc Room.
     */
    suspend fun listChildren(folderId: String?): AppResult<List<DriveItem>>
}
