package com.lambao.odv.feature.player

import com.lambao.odv.core.domain.model.DriveItem

/**
 * Trạng thái màn xem video (VD). Chỉ giữ thứ phải sống sót khi giao diện bị dựng lại: danh sách phát, video đang xem và vị
 * trí để xem tiếp. Thứ đổi theo từng khung hình (vị trí phát, buffer) và thứ chỉ có nghĩa với một lần hiển thị (điều khiển
 * hiện/ẩn, gợn chạm đúp) nằm ở lớp giao diện, nên State không bị ngập bởi hàng chục cập nhật mỗi giây.
 */
data class PlayerState(
    /** Các video để chuyển trước/sau theo ngữ cảnh mở (VD-10). */
    val videos: List<DriveItem> = emptyList(),
    /** Đã nhận danh sách đầu tiên; phân biệt "chưa nạp" với "không có video nào". */
    val isLoaded: Boolean = false,
    /** Video đang xem. */
    val currentId: String? = null,
    /**
     * Vị trí bắt đầu của video đang xem. Là 0 khi vừa mở hoặc vừa chuyển video; là vị trí đã dừng khi giao diện bị dựng lại
     * (màn Khóa che lên rồi mở khóa), để người xem về đúng chỗ.
     */
    val resumePositionMs: Long = 0L,
    /** Tự phát khi nạp. Khôi phục sau lúc giao diện bị gỡ thì theo trạng thái lúc gỡ: đang phát thì phát tiếp, đang dừng thì nằm chờ. */
    val autoPlay: Boolean = true,
    /** Tốc độ phát người xem chọn (VD-05); giữ qua các video. */
    val speed: Float = 1f,
    /** Máy có đường ra Internet; quyết định nút "Tiếp tục" của thẻ mất mạng bật hay mờ (VD-16). */
    val isOnline: Boolean = true,
) {
    private val currentIndex: Int get() = videos.indexOfFirst { it.id == currentId }

    val current: DriveItem? get() = videos.getOrNull(currentIndex)
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean get() = currentIndex in 0 until videos.lastIndex
}

sealed interface PlayerIntent {
    /** Video trước (VD-10). */
    data object Previous : PlayerIntent

    /** Video sau (VD-10). */
    data object Next : PlayerIntent

    /**
     * Giao diện sắp bị gỡ: ghi lại vị trí và có đang phát không ([playing]) để dựng lại đúng chỗ và phát tiếp nếu đang phát.
     * Xuống nền thì player đã bị dừng trước đó nên [playing] là false: màn Khóa che lên rồi mở khóa vẫn nằm chờ tạm dừng.
     */
    data class SavePosition(val positionMs: Long, val playing: Boolean) : PlayerIntent

    /** Chọn tốc độ phát (VD-05). */
    data class SetSpeed(val speed: Float) : PlayerIntent
}

sealed interface PlayerEffect {
    /** Không còn video nào để xem (video cuối bị xóa trên OneDrive qua đồng bộ, DS-06): thoát màn. */
    data object Close : PlayerEffect
}
