package com.lambao.odv.core.designsystem.format

import java.text.NumberFormat
import java.util.Locale

private val units = arrayOf("B", "KB", "MB", "GB", "TB")

actual fun odvFormatFileSize(bytes: Long): String {
    var value = bytes.coerceAtLeast(0).toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = if (unit == 0) 0 else 1
    }
    return "${format.format(value)} ${units[unit]}"
}
