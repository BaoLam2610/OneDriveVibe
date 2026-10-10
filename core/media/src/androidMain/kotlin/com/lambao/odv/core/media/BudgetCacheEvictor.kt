@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.core.media

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import java.util.TreeSet

/**
 * Bộ dọn cache video theo LRU như `LeastRecentlyUsedCacheEvictor` của Media3, nhưng **trần đọc lúc cần** ([maxBytes]) để người dùng đổi
 * giới hạn bộ nhớ đệm ở Cài đặt (CD) mà không phải mở lại cache; bản gốc chốt trần lúc dựng. Thuật toán giữ nguyên: theo dõi từng
 * đoạn (span) theo lần chạm cuối, vượt trần thì xóa đoạn lâu không chạm nhất trước.
 *
 * `SimpleCache` gọi các hàm này trong khối đồng bộ của chính nó nên không cần khóa riêng; [trimTo] gọi từ ngoài phải tự đồng bộ trên cache.
 */
internal class BudgetCacheEvictor(
    private val maxBytes: () -> Long,
) : CacheEvictor {

    private val leastRecentlyUsed = TreeSet<CacheSpan>(Comparator { lhs, rhs -> compareSpans(lhs, rhs) })
    private var currentSize = 0L

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() = Unit

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != C.LENGTH_UNSET.toLong()) evict(cache, length, maxBytes())
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evict(cache, 0L, maxBytes())
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    /** Dọn ngay cho tới khi không quá [limit] (CD-07). Gọi trong `synchronized(cache)`. */
    fun trimTo(cache: Cache, limit: Long) = evict(cache, 0L, limit)

    private fun evict(cache: Cache, requiredSpace: Long, limit: Long) {
        while (currentSize + requiredSpace > limit && leastRecentlyUsed.isNotEmpty()) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }
}

/** Đoạn chạm lâu hơn đứng trước. Cùng thời điểm chạm thì dùng thứ tự mặc định của CacheSpan để TreeSet không coi hai đoạn khác nhau là một. */
private fun compareSpans(lhs: CacheSpan, rhs: CacheSpan): Int {
    if (lhs.lastTouchTimestamp == rhs.lastTouchTimestamp) return lhs.compareTo(rhs)
    return if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) -1 else 1
}
