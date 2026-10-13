package com.lambao.odv.core.domain.model

/**
 * Chế độ phát khi video chạy hết (VD-13, VD-20). Thứ tự khai báo là thứ tự xoay vòng của nút Chế độ phát. Mặc định [AutoNext].
 */
enum class PlayMode {
    /** Dừng ở cuối, hiện nút Phát lại (VD-21). */
    NoRepeat,

    /** Đếm ngược 5 giây rồi sang video sau; hết video cuối danh sách thì dừng. */
    AutoNext,

    /** Phát lại từ đầu ngay, không đếm ngược. */
    RepeatOne,

    /** Như [AutoNext] nhưng hết video cuối thì quay về video đầu danh sách. */
    RepeatList,
    ;

    /** Chế độ kế tiếp khi bấm nút Chế độ phát (xoay vòng). */
    fun next(): PlayMode = entries[(ordinal + 1) % entries.size]
}

/** Cách đặt video vào khung (VD-06). Thứ tự khai báo là thứ tự xoay vòng của nút Khung hình. Mặc định [Fit]. */
enum class VideoFit {
    /** Giữ tỉ lệ, có viền đen nếu khác tỉ lệ khung. */
    Fit,

    /** Giữ tỉ lệ, phóng cho đầy khung, phần thừa bị cắt. */
    Crop,

    /** Kéo cho đầy khung, hình có thể bị méo. */
    Stretch,
    ;

    fun next(): VideoFit = entries[(ordinal + 1) % entries.size]
}

/**
 * Cách xử lý khi mở lại video đã xem dở (VD-12), chọn ở Cài đặt › Video. Chỉ áp cho video mở từ Thư mục hoặc Thư viện; mở từ dải
 * "Xem tiếp" thì luôn vào đúng vị trí đã dừng (DS-02). Mặc định [Ask].
 */
enum class ResumeBehavior {
    /** Hiện hộp thoại "Xem tiếp từ mm:ss?" (Xem tiếp / Xem từ đầu). */
    Ask,

    /** Vào thẳng vị trí đã dừng, không hỏi. */
    Resume,

    /** Phát từ đầu, không hỏi; vị trí đã lưu bị xóa. */
    StartOver,
}
