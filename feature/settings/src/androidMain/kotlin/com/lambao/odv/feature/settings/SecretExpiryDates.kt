package com.lambao.odv.feature.settings

import java.text.DateFormat
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Tiện ích ngày hết hạn Client Secret (CD-06, CD-10), tách khỏi ConnectionGroup.kt (L10) để file đó chỉ còn UI. Hành vi giữ nguyên.

/**
 * Mẫu ngày ngắn của ngôn ngữ đang dùng nhưng năm luôn 4 chữ số (vi: `dd/MM/yyyy`, en-US: `M/d/yyyy`), để "30/11/2026" không bị hiểu nhầm
 * như `yy` và để nhập tay không mơ hồ.
 */
internal fun datePattern(locale: Locale): String {
    val base = (DateFormat.getDateInstance(DateFormat.SHORT, locale) as? java.text.SimpleDateFormat)?.toPattern() ?: "dd/MM/yyyy"
    return if ("yyyy" in base) base else base.replace(Regex("y+"), "yyyy")
}

// Ngày hết hạn là ngày lịch, không có giờ: coi như 00:00 UTC để epoch day không lệch theo múi giờ. Dùng SimpleDateFormat/Calendar thay vì
// java.time vì minSdk 24 và dự án chưa bật core library desugaring.
private const val MILLIS_PER_DAY = 86_400_000L
private const val MIN_EPOCH_DAY = 10_957L // 2000-01-01, loại năm gõ thiếu như "26"
private const val MAX_EPOCH_DAY = 47_482L // 2099-12-31

private fun dateFormat(locale: Locale) = java.text.SimpleDateFormat(datePattern(locale), locale).apply {
    isLenient = false
    timeZone = TimeZone.getTimeZone("UTC")
}

internal fun formatEpochDay(epochDay: Long, locale: Locale): String = dateFormat(locale).format(Date(epochDay * MILLIS_PER_DAY))

/** Epoch day của chuỗi ngày; null nếu sai định dạng, ngày không có thật (vd. 31/02) hoặc ngoài năm 2000 đến 2099. */
internal fun parseEpochDay(text: String, locale: Locale): Long? {
    val trimmed = text.trim()
    val position = ParsePosition(0)
    val date = dateFormat(locale).parse(trimmed, position) ?: return null
    if (position.index != trimmed.length) return null
    return (date.time / MILLIS_PER_DAY).takeIf { it in MIN_EPOCH_DAY..MAX_EPOCH_DAY }
}

/** Epoch day của hôm nay (theo ngày lịch địa phương) cộng [months] tháng. */
internal fun epochDayFromToday(months: Int): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        add(Calendar.MONTH, months)
    }
    return utc.timeInMillis / MILLIS_PER_DAY
}
