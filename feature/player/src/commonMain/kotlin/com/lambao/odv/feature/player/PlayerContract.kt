package com.lambao.odv.feature.player

import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.VideoSettingOptions

/**
 * Nội dung bảng thông tin tệp (VD-17). Mọi thứ lấy từ Room nên hiện ngay và dùng được khi offline; chỉ đường dẫn thư mục
 * nạp thêm một truy vấn Room nhỏ ([folderPath], rỗng cho tới khi nạp xong hoặc khi tệp nằm ngay ở gốc).
 */
data class PlayerInfo(
    val item: DriveItem,
    val folderPath: List<FolderRef> = emptyList(),
)

/**
 * Trạng thái màn xem video (VD). Chỉ giữ thứ phải sống sót khi giao diện bị dựng lại: danh sách phát, video đang xem, vị trí để
 * xem tiếp và các lựa chọn. Thứ đổi theo từng khung hình (vị trí phát, buffer) và thứ chỉ có nghĩa với một lần hiển thị (điều khiển
 * hiện/ẩn, gợn chạm đúp, zoom, khóa thao tác) nằm ở lớp giao diện, nên State không bị ngập bởi hàng chục cập nhật mỗi giây.
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
    /** Chế độ phát khi video chạy hết (VD-13, VD-20), nhớ giữa các lần xem. */
    val playMode: PlayMode = PlayMode.AutoNext,
    /** Cách đặt video vào khung (VD-06), nhớ giữa các lần xem. */
    val videoFit: VideoFit = VideoFit.Fit,
    /** Các video đã báo lỗi trong phiên này; tự phát tiếp bỏ qua chúng và dừng khi cả danh sách đều lỗi (VD-15). */
    val failedIds: Set<String> = emptySet(),
    /** Khác null khi bảng thông tin đang mở (VD-17). */
    val info: PlayerInfo? = null,
    /** Bước tua khi chạm đúp, giây (VD-03), theo Cài đặt. */
    val seekStepSeconds: Int = VideoSettingOptions.DEFAULT_SEEK_STEP,
    /** Mở video ở hướng ngang theo Cài đặt (VD-07, VD-19); null cho tới khi đọc xong. Giao diện chỉ áp dụng một lần. */
    val openInLandscape: Boolean? = null,
) {
    private val currentIndex: Int get() = videos.indexOfFirst { it.id == currentId }

    /** Chế độ Lặp danh sách thì quay vòng nên nút Trước/Sau không bao giờ mờ (VD-10), miễn có hơn một video. */
    private val wraps: Boolean get() = playMode == PlayMode.RepeatList && videos.size > 1

    val current: DriveItem? get() = videos.getOrNull(currentIndex)
    val hasPrevious: Boolean get() = wraps || currentIndex > 0
    val hasNext: Boolean get() = wraps || currentIndex in 0 until videos.lastIndex

    /**
     * Video sẽ phát khi video này chạy hết (VD-13): Tự phát tiếp thì video sau (null nếu đang ở cuối), Lặp danh sách thì video sau
     * hoặc quay về đầu danh sách. Các chế độ còn lại là null (không tự chuyển).
     */
    val nextInQueue: DriveItem?
        get() = when (playMode) {
            PlayMode.AutoNext -> videos.getOrNull(currentIndex + 1)
            PlayMode.RepeatList -> videos.getOrNull(currentIndex + 1) ?: videos.firstOrNull()
            PlayMode.NoRepeat, PlayMode.RepeatOne -> null
        }

    /** Video kế tiếp chưa lỗi theo thứ tự phát (VD-15), hoặc null nếu không có/không áp dụng cho chế độ hiện tại. */
    internal fun nextPlayable(failed: Set<String>): DriveItem? {
        if (playMode != PlayMode.AutoNext && playMode != PlayMode.RepeatList) return null
        if (currentIndex < 0) return null
        val order = if (playMode == PlayMode.RepeatList) {
            // Một vòng đầy đủ, bắt đầu từ video ngay sau video hiện tại.
            (1 until videos.size).map { videos[(currentIndex + it) % videos.size] }
        } else {
            videos.drop(currentIndex + 1)
        }
        return order.firstOrNull { it.id !in failed }
    }
}

sealed interface PlayerIntent {
    /** Video trước (VD-10). */
    data object Previous : PlayerIntent

    /** Video sau (VD-10). */
    data object Next : PlayerIntent

    /** Video chạy hết và hết đếm ngược: sang [PlayerState.nextInQueue] (VD-13). */
    data object Advance : PlayerIntent

    /** Video [itemId] không phát được: tự phát tiếp sẽ bỏ qua nó (VD-15). */
    data class VideoFailed(val itemId: String) : PlayerIntent

    /** Có video phát được: xóa danh sách video lỗi để lỗi tạm thời (5xx, mạng chập chờn) không bị bỏ qua mãi trong phiên. */
    data object ClearFailed : PlayerIntent

    /**
     * Giao diện sắp bị gỡ: ghi lại vị trí và có đang phát không ([playing]) để dựng lại đúng chỗ và phát tiếp nếu đang phát.
     * Xuống nền thì player đã bị dừng trước đó nên [playing] là false: màn Khóa che lên rồi mở khóa vẫn nằm chờ tạm dừng.
     */
    data class SavePosition(val positionMs: Long, val playing: Boolean) : PlayerIntent

    /** Chọn tốc độ phát (VD-05). */
    data class SetSpeed(val speed: Float) : PlayerIntent

    /** Bấm nút Chế độ phát: xoay vòng 4 chế độ và nhớ lại (VD-20). */
    data object CyclePlayMode : PlayerIntent

    /** Bấm nút Khung hình: xoay vòng 3 chế độ và nhớ lại (VD-06). */
    data object CycleVideoFit : PlayerIntent

    /** Bấm nút Thông tin (VD-17). */
    data object ShowInfo : PlayerIntent

    data object HideInfo : PlayerIntent
}

sealed interface PlayerEffect {
    /** Không còn video nào để xem (video cuối bị xóa trên OneDrive qua đồng bộ, DS-06): thoát màn. */
    data object Close : PlayerEffect
}
