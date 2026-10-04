package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult

/**
 * Nguồn link phát video (VD-11, VD-14). Link do OneDrive ký sẵn và chỉ sống khoảng 1 giờ, nên trình phát không giữ nó lâu
 * mà hỏi lại qua đây mỗi lần mở kết nối. Link là bí mật (chứa `tempauth`): không ghi log, không lưu đĩa, không đưa vào khóa cache.
 */
interface VideoStreamRepository {

    /**
     * Link phát mới của [itemId]. Gọi lại là cách lấy link mới khi link cũ hết hạn; không cần token mới.
     * Lỗi: `AppError.Http(404)` khi video đã bị xóa trên OneDrive, `AppError.Network`/`Timeout` khi mất mạng,
     * `AppError.AppLocked` khi app đang bị khóa PIN (config chưa giải mã).
     */
    suspend fun streamUrl(itemId: String): AppResult<String>
}
