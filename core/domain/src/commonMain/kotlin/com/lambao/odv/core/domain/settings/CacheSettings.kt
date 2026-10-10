package com.lambao.odv.core.domain.settings

import com.lambao.odv.core.domain.model.CacheShares
import kotlinx.coroutines.flow.Flow

/**
 * Giới hạn bộ nhớ đệm do người dùng đặt ở Cài đặt (CD, mục Bộ nhớ đệm), lưu trên máy và bị xóa khi Ngắt kết nối (CD-05). Đổi giá trị
 * chỉ lưu; việc dọn ngay theo giới hạn mới (CD-07) do UseCase `SetCacheLimitUseCase` / `SetCacheSharesUseCase` làm.
 */
interface CacheSettings {
    /** Giới hạn chung, GB, từ `CachePolicy.MIN_LIMIT_GB` đến `MAX_LIMIT_GB`; mặc định `DEFAULT_LIMIT_GB`. Giá trị lạ về mặc định. */
    val limitGb: Flow<Int>

    suspend fun setLimitGb(gb: Int)

    /** Tỉ lệ chia theo loại, tổng 100; giá trị lạ hoặc hỏng về [CacheShares.Default]. */
    val shares: Flow<CacheShares>

    suspend fun setShares(shares: CacheShares)
}
