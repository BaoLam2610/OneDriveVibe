package com.lambao.odv.core.domain.model

/** Các loại bộ nhớ đệm có trần riêng (BN-01, CD mục Bộ nhớ đệm). Thứ tự khai báo là thứ tự hiển thị ở thanh dung lượng. */
enum class CacheKind { Thumbnail, Image, Video, Pdf }

/** Giới hạn chung của bộ nhớ đệm (CD, chốt 2026-10-07): tùy chỉnh từ 1 đến 10 GB, mặc định 2 GB. */
object CachePolicy {
    const val MIN_LIMIT_GB = 1
    const val MAX_LIMIT_GB = 10
    const val DEFAULT_LIMIT_GB = 2
    const val BYTES_PER_GB = 1024L * 1024L * 1024L

    fun limitBytes(gb: Int): Long = gb.coerceIn(MIN_LIMIT_GB, MAX_LIMIT_GB) * BYTES_PER_GB
}

/**
 * Tỉ lệ chia giới hạn chung cho từng loại, **phần trăm nguyên, mỗi mốc 0 đến 100, tổng luôn 100** (CD, chốt 2026-10-07).
 * Mặc định 10 / 30 / 45 / 15: ảnh và video chiếm phần lớn, thumbnail nhỏ vì chất lượng thấp.
 */
data class CacheShares(val thumbnail: Int, val image: Int, val video: Int, val pdf: Int) {
    init {
        require(thumbnail >= 0 && image >= 0 && video >= 0 && pdf >= 0) { "Tỉ lệ không được âm" }
        require(thumbnail + image + video + pdf == TOTAL) { "Tổng tỉ lệ phải bằng $TOTAL" }
    }

    operator fun get(kind: CacheKind): Int = when (kind) {
        CacheKind.Thumbnail -> thumbnail
        CacheKind.Image -> image
        CacheKind.Video -> video
        CacheKind.Pdf -> pdf
    }

    /**
     * Đặt tỉ lệ của [kind] là [percent] (0 đến 100) và **tự tính lại ba loại còn lại** để tổng vẫn là 100: phần còn lại chia theo tỉ lệ
     * hiện có của chúng; nếu cả ba đang bằng 0 thì chia đều. Phần dư do làm tròn cộng vào loại đang lớn nhất để không âm.
     */
    fun withShare(kind: CacheKind, percent: Int): CacheShares {
        val chosen = percent.coerceIn(0, TOTAL)
        val rest = TOTAL - chosen
        val others = CacheKind.entries.filter { it != kind }
        val currentSum = others.sumOf { this[it] }
        val values = mutableMapOf<CacheKind, Int>()
        if (currentSum == 0) {
            val base = rest / others.size
            others.forEach { values[it] = base }
        } else {
            others.forEach { values[it] = this[it] * rest / currentSum }
        }
        val leftover = rest - values.values.sum()
        if (leftover != 0) {
            val largest = others.maxBy { values.getValue(it) }
            values[largest] = values.getValue(largest) + leftover
        }
        values[kind] = chosen
        return CacheShares(
            thumbnail = values.getValue(CacheKind.Thumbnail),
            image = values.getValue(CacheKind.Image),
            video = values.getValue(CacheKind.Video),
            pdf = values.getValue(CacheKind.Pdf),
        )
    }

    companion object {
        const val TOTAL = 100
        val Default = CacheShares(thumbnail = 10, image = 30, video = 45, pdf = 15)
    }
}

/** Giới hạn bộ nhớ đệm đang áp dụng: trần chung [limitBytes] chia theo [shares]. */
data class CacheBudget(val limitBytes: Long, val shares: CacheShares) {
    /** Trần của một loại = giới hạn chung × tỉ lệ của loại đó. */
    fun bytesFor(kind: CacheKind): Long = limitBytes * shares[kind] / CacheShares.TOTAL

    companion object {
        val Default = CacheBudget(CachePolicy.limitBytes(CachePolicy.DEFAULT_LIMIT_GB), CacheShares.Default)
    }
}

/** Dung lượng bộ nhớ đệm đang dùng theo loại (byte). Loại chưa có kho (vd. PDF trước Lát 8) tính 0. */
data class CacheUsage(val bytes: Map<CacheKind, Long> = emptyMap()) {
    fun of(kind: CacheKind): Long = bytes[kind] ?: 0L
    val total: Long get() = bytes.values.sum()
}
