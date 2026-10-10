package com.lambao.odv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Locale của ngôn ngữ app đang dùng (CD-10): số, ngày, giờ, dung lượng và thời lượng hiển thị theo quy ước của ngôn ngữ này, không
 * cố định vi-VN. Lấy từ `LocalLocale.current.platformLocale` (không dùng `Locale.getDefault()`, không tự đọc `LocalConfiguration`) nên composable tự dựng lại khi đổi ngôn ngữ trong app (Activity không bị tạo lại). Dùng
 * làm khóa cho `remember` của mọi bộ định dạng (`remember(locale) { NumberFormat.getIntegerInstance(locale) }`).
 */
@Composable
fun odvLocale(): Locale = LocalLocale.current.platformLocale

/** "1x", "0,25x" (vi) hoặc "0.25x" (en): tối đa hai chữ số thập phân, dấu thập phân theo [locale] (VD-05). */
fun odvFormatSpeed(speed: Float, locale: Locale): String =
    DecimalFormat("0.##", DecimalFormatSymbols(locale)).format(speed) + "x"
