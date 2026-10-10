package com.lambao.odv.core.domain.usecase.cache

import com.lambao.odv.core.domain.hook.CacheStore

/**
 * Xóa toàn bộ bộ nhớ đệm (thumbnail, ảnh, video đã xem, PDF đã tải). **Không** xóa lịch sử xem, dữ liệu đồng bộ hay kết nối; tệp tải
 * lại khi mở. Một kho lỗi không chặn các kho còn lại (mỗi kho tự nuốt lỗi, hợp đồng [CacheStore]).
 */
class ClearCacheUseCase(
    private val stores: List<CacheStore>,
) {
    suspend operator fun invoke() = stores.forEach { it.clear() }
}
