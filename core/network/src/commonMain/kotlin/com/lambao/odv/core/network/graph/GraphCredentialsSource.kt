package com.lambao.odv.core.network.graph

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.network.auth.GraphCredentials

/**
 * Nguồn credentials của người dùng hiện tại cho [GraphApi]. `:core:network` không biết config nằm ở đâu hay được mã hóa
 * thế nào (ADR-0008); `:core:data` cài đặt interface này từ config đã lưu. Gọi **mỗi lần** có lời gọi Graph để khi app khóa
 * (CH-03) lời gọi kế tiếp dừng ngay với `AppError.AppLocked`, không giữ bí mật trong một coroutine chạy dài.
 */
fun interface GraphCredentialsSource {
    suspend fun current(): AppResult<GraphCredentials>
}
