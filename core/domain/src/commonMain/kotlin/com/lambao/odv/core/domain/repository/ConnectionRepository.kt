package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig

/** Kiểm tra kết nối OneDrive bằng config người dùng vừa nhập (màn Kết nối). */
interface ConnectionRepository {

    /**
     * Kiểm tra kết nối bằng [config] chưa lưu (KN-07): lấy token rồi gọi drive. Thành công khi cả hai trả 2xx (KN-08).
     * Không trả thông tin drive hay dung lượng (KN-12).
     * Lỗi trả về dạng [com.lambao.odv.core.common.error.AppError], UI chọn thông báo theo mã (KN-09).
     */
    suspend fun verifyConnection(config: ConnectionConfig): AppResult<Unit>
}
