package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.data.PreferenceKeys
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.model.CacheShares
import com.lambao.odv.core.domain.settings.CacheSettings
import kotlinx.coroutines.flow.Flow

/**
 * [CacheSettings] trên cùng DataStore `settings`. Giá trị lạ hoặc đọc lỗi thì dùng mặc định (2 GB, chia 10/30/45/15): trần cache hỏng
 * không được làm app không còn chỗ lưu hay lưu vô hạn.
 */
internal class DataStoreCacheSettings(
    private val prefs: PreferencesDataSource,
) : CacheSettings {

    override val limitGb: Flow<Int> = prefs.observe { p ->
        p[PreferenceKeys.CACHE_LIMIT_GB]?.takeIf { it in CachePolicy.MIN_LIMIT_GB..CachePolicy.MAX_LIMIT_GB } ?: CachePolicy.DEFAULT_LIMIT_GB
    }

    override suspend fun setLimitGb(gb: Int) {
        prefs.edit { it[PreferenceKeys.CACHE_LIMIT_GB] = gb.coerceIn(CachePolicy.MIN_LIMIT_GB, CachePolicy.MAX_LIMIT_GB) }
    }

    override val shares: Flow<CacheShares> = prefs.observe { p -> p[PreferenceKeys.CACHE_SHARES]?.let(::parseShares) ?: CacheShares.Default }

    override suspend fun setShares(shares: CacheShares) {
        prefs.edit { it[PreferenceKeys.CACHE_SHARES] = "${shares.thumbnail},${shares.image},${shares.video},${shares.pdf}" }
    }

    /** `10,30,45,15` thành [CacheShares]; sai số phần tử, số âm hay tổng khác 100 thì null. */
    private fun parseShares(raw: String): CacheShares? {
        val parts = raw.split(",").map { it.trim().toIntOrNull() ?: return null }
        if (parts.size != 4 || parts.any { it < 0 } || parts.sum() != CacheShares.TOTAL) return null
        return CacheShares(parts[0], parts[1], parts[2], parts[3])
    }
}
