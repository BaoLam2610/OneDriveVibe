package com.lambao.odv.feature.library

import android.text.format.DateFormat
import com.lambao.odv.core.domain.DomainConstants
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.dayNumberOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Cách dàn lưới Thư viện, dựng hoàn toàn từ số mục theo ngày ([LibraryDay]) nên biết trước độ dài và vị trí mọi tiêu đề
 * nhóm mà không cần nạp hết ảnh (TV-01, TV-04). Lưới gồm, theo thứ tự ngày mới nhất trước, một tiêu đề rồi các ô của ngày
 * đó. "Vị trí media" là chỉ số của ô trong luồng phân trang (đã xếp theo ngày giảm dần, cùng thứ tự với [days]).
 */
internal class LibraryLayout(val days: List<LibraryDay>) {

    /** Chỉ số trong lưới của tiêu đề từng ngày. */
    private val headerIndex = IntArray(days.size)

    /** Số ô media đứng trước từng ngày. */
    private val mediaBefore = IntArray(days.size)

    /** Tổng số phần tử của lưới (tiêu đề và ô). */
    val total: Int

    init {
        var layout = 0
        var media = 0
        days.forEachIndexed { i, day ->
            headerIndex[i] = layout
            mediaBefore[i] = media
            layout += 1 + day.count
            media += day.count
        }
        total = layout
    }

    /** Chỉ số (trong [days]) của ngày chứa phần tử [index] của lưới. Chỉ gọi khi [total] > 0. */
    fun dayIndexAt(index: Int): Int {
        val target = index.coerceIn(0, total - 1)
        var low = 0
        var high = days.lastIndex
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (headerIndex[mid] <= target) low = mid else high = mid - 1
        }
        return low
    }

    fun isHeader(index: Int): Boolean = headerIndex[dayIndexAt(index)] == index

    /** Vị trí của ô ở [index] trong luồng phân trang. Chỉ gọi khi [index] không phải tiêu đề. */
    fun mediaPositionAt(index: Int): Int {
        val day = dayIndexAt(index)
        return mediaBefore[day] + (index - headerIndex[day] - 1)
    }
}

/**
 * Định dạng ngày cho Thư viện theo ngôn ngữ đang dùng (TV-01, CD-10): "23 tháng 5, 2026" / "May 23, 2026" cho tiêu đề
 * nhóm và nhãn TalkBack, "tháng 5 năm 2026" / "May 2026" cho bong bóng cuộn nhanh. Số ngày đã cộng độ lệch múi giờ nên
 * định dạng ở UTC. Không an toàn đa luồng: chỉ dùng trên luồng chính.
 */
internal class LibraryDateFormatter(locale: Locale) {

    private val dayFormat = formatter(locale, "dMMMMy")
    private val monthFormat = formatter(locale, "MMMMy")

    fun day(dayNumber: Long): String = dayFormat.format(noonOf(dayNumber))

    fun month(dayNumber: Long): String = monthFormat.format(noonOf(dayNumber))

    /** Ngày của một mục ([epochMs]) theo cùng cách nhóm của lưới. */
    fun day(epochMs: Long, utcOffsetMs: Long): String = day(dayNumberOf(epochMs, utcOffsetMs))

    // Giữa trưa để không lệch ngày dù có sai số giờ.
    private fun noonOf(dayNumber: Long) = Date(dayNumber * DomainConstants.MS_PER_DAY + DomainConstants.MS_PER_DAY / 2)

    private fun formatter(locale: Locale, skeleton: String) =
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}
