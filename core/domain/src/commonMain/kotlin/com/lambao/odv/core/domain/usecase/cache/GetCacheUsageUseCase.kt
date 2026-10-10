package com.lambao.odv.core.domain.usecase.cache

import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.model.CacheUsage

/** Đo dung lượng đang dùng theo loại (thumbnail, ảnh, video, PDF). Nhiều kho cùng loại thì cộng lại. */
class GetCacheUsageUseCase(
    private val stores: List<CacheStore>,
) {
    suspend operator fun invoke(): CacheUsage =
        CacheUsage(stores.groupBy { it.kind }.mapValues { (_, same) -> same.sumOf { it.usedBytes() } })
}
