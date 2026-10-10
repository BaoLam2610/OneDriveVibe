package com.lambao.odv.core.domain.usecase.cache

import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.model.CacheBudget
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.settings.CacheSettings
import kotlinx.coroutines.flow.first

/**
 * Đổi giới hạn chung (CD, từ 1 đến 10 GB) rồi **dọn ngay** theo giới hạn mới nếu dung lượng đang vượt (CD-07): tệp lâu không dùng
 * nhất bị xóa trước. Giá trị ngoài khoảng được ép vào khoảng.
 */
class SetCacheLimitUseCase(
    private val settings: CacheSettings,
    private val stores: List<CacheStore>,
) {
    suspend operator fun invoke(gb: Int) {
        val limit = gb.coerceIn(CachePolicy.MIN_LIMIT_GB, CachePolicy.MAX_LIMIT_GB)
        settings.setLimitGb(limit)
        trimToBudget(stores, CacheBudget(CachePolicy.limitBytes(limit), settings.shares.first()))
    }
}
