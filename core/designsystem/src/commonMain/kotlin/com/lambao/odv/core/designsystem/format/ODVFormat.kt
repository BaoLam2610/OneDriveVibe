package com.lambao.odv.core.designsystem.format

/**
 * Dung lượng theo quy ước ngôn ngữ đang dùng: "4,9 MB" (vi), "4.9 MB" (en); đơn vị cách 1024; bỏ phần thập phân khi tròn
 * ("1 TB"). Số theo locale của app (CD-10), nên gọi trong composable để đổi ngôn ngữ cập nhật ngay.
 */
expect fun odvFormatFileSize(bytes: Long, languageTag: String): String

/** Thời lượng dạng đồng hồ "1:26:02" hoặc "18:20" (không đệm số 0 ở giờ). Không phụ thuộc ngôn ngữ. */
fun odvFormatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "${minutes}:$ss"
}
