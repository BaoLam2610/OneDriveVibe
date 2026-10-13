package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.ConnectionConfig

/** Kiểm tra kết nối OneDrive bằng config người dùng vừa nhập (màn Kết nối). */
interface ConnectionRepository {

    /**
     * Kiểm tra kết nối bằng [config] chưa lưu (KN-07): lấy token rồi gọi drive. Thành công khi cả hai trả 2xx (KN-08), trả về
     * [Account] dựng từ drive id và `owner` (TA-02). Không đọc hay trả dung lượng (KN-12).
     * Lỗi trả về dạng [com.lambao.odv.core.common.error.AppError], UI chọn thông báo theo mã (KN-09).
     */
    suspend fun verifyConnection(config: ConnectionConfig): AppResult<Account>

    /**
     * Kiểm tra một [upn] mới bằng Tenant ID, Client ID, Client Secret **đã lưu** (TA-07): chỉ gọi drive của UPN đó với token đang có.
     * Đang khóa thì trả `AppError.AppLocked` (CH-03).
     */
    suspend fun verifyAccount(upn: String): AppResult<Account>
}
