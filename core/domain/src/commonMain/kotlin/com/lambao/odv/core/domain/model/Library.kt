package com.lambao.odv.core.domain.model

/** Chip lọc của tab Thư viện (TV-03). PDF không bao giờ có trong Thư viện (TV-05). */
enum class LibraryFilter(val kinds: Set<MediaKind>) {
    All(setOf(MediaKind.Image, MediaKind.Video)),
    Photos(setOf(MediaKind.Image)),
    Videos(setOf(MediaKind.Video)),
}

/**
 * Ngày dùng để xếp và nhóm trong Thư viện (TV-02): ngày chụp, rồi ngày tạo tệp gốc (đã rơi về ngày tải lên khi thiếu,
 * xem mapper), cuối cùng ngày sửa; không có gì thì 0 (cuối danh sách). Epoch mili giây.
 */
fun libraryDateOf(takenAt: Long?, createdAt: Long?, modifiedAt: Long?): Long = takenAt ?: createdAt ?: modifiedAt ?: 0L

/** Ngày của một mục trong Thư viện theo [libraryDateOf]. */
val DriveItem.libraryDate: Long get() = libraryDateOf(takenAt, createdAt, modifiedAt)

private const val MS_PER_DAY = 86_400_000L

/**
 * Số ngày (từ 1970-01-01) của [epochMs] ở múi giờ có độ lệch [utcOffsetMs]. Nhóm ngày trong SQL và trong giao diện dùng
 * đúng công thức này với cùng một độ lệch nên đầu nhóm và tiêu đề luôn khớp nhau. Dùng phép chia cắt về 0 (không phải
 * `floorDiv`) vì SQLite chia số nguyên như vậy; hai bên chỉ khác với tổng âm, tức mục không có ngày ở múi giờ phía tây UTC.
 */
fun dayNumberOf(epochMs: Long, utcOffsetMs: Long): Long = (epochMs + utcOffsetMs) / MS_PER_DAY

/** Số mục của một ngày trong Thư viện (tiêu đề nhóm, TV-01) và vị trí của nhóm trong danh sách (cuộn nhanh, TV-04). */
data class LibraryDay(val dayNumber: Long, val count: Int)
