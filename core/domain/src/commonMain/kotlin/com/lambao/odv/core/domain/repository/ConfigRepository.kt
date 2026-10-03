package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig

/**
 * Nơi giữ config kết nối đã mã hóa (CH-01). Config được lưu ngay khi kết nối thành công, ở chế độ thiết bị (khóa
 * Keystore), trước cả hộp thoại hỏi thiết lập PIN (KN-08, ADR-0008).
 *
 * Chuyển chế độ (bật/tắt/đổi PIN) và khóa/mở khóa nằm ở [SecurityRepository], không ở đây (ADR-0014). Ở chế độ PIN
 * mà app đang khóa thì [load] trả `AppError.AppLocked`.
 */
interface ConfigRepository {

    /** Đã có config lưu trên máy chưa. Không giải mã. */
    suspend fun hasConfig(): Boolean

    /**
     * Mã hóa và lưu [config] ở chế độ thiết bị, thay config cũ và đặt lại phần bảo mật nếu có (kết nối mới luôn bắt đầu
     * ở chế độ thiết bị, KN-08).
     */
    suspend fun save(config: ConnectionConfig): AppResult<Unit>

    /** Giải mã config đã lưu. Thất bại (khóa Keystore mất, tệp hỏng) trả về lỗi, không ném ngoại lệ. */
    suspend fun load(): AppResult<ConnectionConfig>

    /** Xóa config đã lưu và bản trong bộ nhớ. */
    suspend fun clear()
}
