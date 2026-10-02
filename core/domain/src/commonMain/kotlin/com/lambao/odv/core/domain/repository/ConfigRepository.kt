package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.ConnectionConfig

/**
 * Nơi giữ config kết nối đã mã hóa (CH-01). Config được lưu ngay khi kết nối thành công, ở chế độ thiết bị (khóa
 * Keystore), trước cả hộp thoại hỏi thiết lập PIN (KN-08, ADR-0008).
 *
 * Lát 2 thêm thao tác mã hóa lại config bằng khóa dẫn xuất từ PIN (BM-04, CD-02, CD-09).
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
