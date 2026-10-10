package com.lambao.odv.core.domain.usecase.cache

import com.lambao.odv.core.domain.model.CacheBudget
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.settings.CacheSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Giới hạn bộ nhớ đệm đang áp dụng (trần chung và tỉ lệ chia), đổi theo Cài đặt. */
class ObserveCacheBudgetUseCase(
    private val settings: CacheSettings,
) {
    operator fun invoke(): Flow<CacheBudget> =
        combine(settings.limitGb, settings.shares) { gb, shares -> CacheBudget(CachePolicy.limitBytes(gb), shares) }
}
