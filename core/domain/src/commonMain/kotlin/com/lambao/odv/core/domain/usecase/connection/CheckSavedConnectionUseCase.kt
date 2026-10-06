package com.lambao.odv.core.domain.usecase.connection

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.SavedConnection
import com.lambao.odv.core.domain.repository.ConfigRepository

/**
 * Config đã lưu trên máy ở tình trạng nào. Dùng chung cho Splash (chọn màn đầu) và Kết nối (KN-13: khôi phục sau khi hệ
 * điều hành thu hồi tiến trình thì vào thẳng Danh sách nếu config đã lưu và giải mã được).
 *
 * Config còn nhưng không giải mã được ([SavedConnection.Unusable]: khóa Keystore mất, tệp hỏng, hoặc lỗi đọc tạm thời) thì
 * người dùng sang màn Kết nối thay vì kẹt ở Danh sách với lỗi tải vĩnh viễn. **Không xóa config ở đây**: lỗi tạm thời không
 * được làm mất dữ liệu; kết nối lại sẽ ghi đè config cũ ngay khi kết nối thành công (KN-08).
 */
class CheckSavedConnectionUseCase(
    private val configs: ConfigRepository,
) {
    suspend operator fun invoke(): SavedConnection = when {
        !configs.hasConfig() -> SavedConnection.None
        configs.load() is AppResult.Failure -> SavedConnection.Unusable
        else -> SavedConnection.Usable
    }
}
