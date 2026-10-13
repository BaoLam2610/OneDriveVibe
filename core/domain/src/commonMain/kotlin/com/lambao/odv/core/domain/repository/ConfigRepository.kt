package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.ConnectionProfile

/**
 * Nơi giữ config kết nối đã mã hóa (CH-01). Config được lưu ngay khi kết nối thành công, ở chế độ thiết bị (khóa
 * Keystore), trước cả hộp thoại hỏi thiết lập PIN (KN-08, ADR-0008). Config gồm phần dùng chung và danh sách tài khoản
 * ([ConnectionProfile], ADR-0030); thêm, xóa tài khoản đi qua [AccountRepository].
 *
 * Chuyển chế độ (bật/tắt/đổi PIN) và khóa/mở khóa nằm ở [SecurityRepository], không ở đây (ADR-0014). Ở chế độ PIN
 * mà app đang khóa thì [load] trả `AppError.AppLocked`.
 */
interface ConfigRepository {

    /** Đã có config lưu trên máy chưa. Không giải mã. */
    suspend fun hasConfig(): Boolean

    /**
     * Mã hóa và lưu [config] cùng [account] đầu tiên (đã xác minh bằng [ConnectionRepository.verifyConnection]) ở chế độ thiết bị, thay
     * config cũ và đặt lại phần bảo mật nếu có (kết nối mới luôn bắt đầu ở chế độ thiết bị, KN-08).
     */
    suspend fun save(config: ConnectionConfig, account: Account): AppResult<Unit>

    /** Giải mã config đã lưu. Thất bại (khóa Keystore mất, tệp hỏng) trả về lỗi, không ném ngoại lệ. */
    suspend fun load(): AppResult<ConnectionProfile>

    /**
     * Đổi **chỉ Client Secret** (CD-04), giữ nguyên Tenant ID, Client ID, danh sách tài khoản và chế độ bảo mật (thiết bị hay PIN, kể cả sinh trắc
     * học). Gọi sau khi [ConnectionRepository.verifyConnection] đã xác nhận secret mới dùng được. Thất bại thì secret cũ còn nguyên.
     * Đang khóa thì trả `AppError.AppLocked`.
     */
    suspend fun updateClientSecret(secret: String): AppResult<Unit>

    // Không có clear(): xóa config đi qua DisconnectUseCase → SecurityRepository.wipe (xóa cả khóa Keystore, bộ đếm sai,
    // token). Một hàm xóa riêng ở đây sẽ để lại khóa Keystore và dễ bị dùng nhầm.
}
