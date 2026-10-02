package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig

/**
 * Nơi giữ config kết nối đã mã hóa (CH-01). Config chỉ được lưu sau khi hoàn tất bước bảo mật (KN-08).
 *
 * Lát 1 chỉ có chế độ bảo mật TẮT (khóa Keystore). Lát 2 thêm tham số chế độ bảo mật kèm PIN cho [save].
 */
interface ConfigRepository {

    /** Đã có config lưu trên máy chưa. Không giải mã. */
    suspend fun hasConfig(): Boolean

    /** Mã hóa và lưu [config], thay config cũ nếu có. */
    suspend fun save(config: ConnectionConfig): AppResult<Unit>

    /** Giải mã config đã lưu. Thất bại (khóa Keystore mất, tệp hỏng) trả về lỗi, không ném ngoại lệ. */
    suspend fun load(): AppResult<ConnectionConfig>

    /** Xóa config đã lưu và bản trong bộ nhớ. */
    suspend fun clear()
}
