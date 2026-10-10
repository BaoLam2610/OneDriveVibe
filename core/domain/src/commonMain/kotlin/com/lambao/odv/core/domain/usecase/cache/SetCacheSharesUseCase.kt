package com.lambao.odv.core.domain.usecase.cache

import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.model.CacheBudget
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.model.CacheShares
import com.lambao.odv.core.domain.settings.CacheSettings
import kotlinx.coroutines.flow.first

/** Đổi tỉ lệ chia giới hạn cho từng loại (tổng 100, bảo đảm bởi [CacheShares]) rồi dọn ngay loại nào đang vượt trần mới (CD-07). */
class SetCacheSharesUseCase(
    private val settings: CacheSettings,
    private val stores: List<CacheStore>,
) {
    suspend operator fun invoke(shares: CacheShares) {
        settings.setShares(shares)
        trimToBudget(stores, CacheBudget(CachePolicy.limitBytes(settings.limitGb.first()), shares))
    }
}

/** Dọn từng kho về trần của loại đó. Dùng chung cho [SetCacheLimitUseCase] và [SetCacheSharesUseCase]. */
internal suspend fun trimToBudget(stores: List<CacheStore>, budget: CacheBudget) {
    stores.forEach { it.trimTo(budget.bytesFor(it.kind)) }
}
