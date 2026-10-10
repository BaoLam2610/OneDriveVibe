package com.lambao.odv.core.designsystem.format

import java.text.NumberFormat
import java.util.Locale

private val units = arrayOf("B", "KB", "MB", "GB", "TB")

actual fun odvFormatFileSize(bytes: Long, languageTag: String): String {
    var value = bytes.coerceAtLeast(0).toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    // Locale do nơi gọi truyền vào (từ `odvLocale()`, CD-10); không dùng Locale.getDefault().
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag(languageTag)).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = if (unit == 0) 0 else 1
    }
    return "${format.format(value)} ${units[unit]}"
}
