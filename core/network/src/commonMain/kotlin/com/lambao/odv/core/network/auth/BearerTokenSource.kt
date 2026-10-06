package com.lambao.odv.core.network.auth

import com.lambao.odv.core.common.result.AppResult

/**
 * Nguồn bearer token cho một lời gọi API. Lớp cơ sở `ApiService` chỉ biết giao diện này, không biết token đến từ đâu
 * (Client Credentials của Graph hay nơi khác), nên không phụ thuộc vào credentials của dịch vụ cụ thể.
 */
interface BearerTokenSource {

    /** Token còn dùng được, hoặc lỗi nếu không lấy được (kể cả `AppError.AppLocked` khi app đang khóa). */
    suspend fun token(): AppResult<String>

    /** Bỏ đúng [rejected] vừa bị máy chủ từ chối (`401`), để lần [token] kế tiếp lấy token mới. Token khác đã làm mới thì giữ. */
    suspend fun invalidate(rejected: String)
}
