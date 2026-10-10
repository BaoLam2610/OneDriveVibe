package com.lambao.odv.feature.shorts

import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.domain.model.VideoSettingOptions

enum class ShortsPhase {
    /** Chưa dựng xong danh sách (lần đầu mở tab, hoặc vừa bị bỏ trạng thái do tắt Video, DH-01). */
    Loading,

    /** Không có video nào thỏa SV-01 (SV-15). */
    Empty,
    Ready,
}

/**
 * Ảnh chụp trạng thái để khôi phục sau khi hệ điều hành thu hồi tiến trình (DH-02, ADR-0024 mục 5): chỉ seed, chỉ số và vị trí phát chứ
 * không giữ cả danh sách. Màn giữ nó bằng `rememberSaveable` và đưa lại qua [ShortsIntent.Enter].
 */
data class ShortsSnapshot(val seed: Long, val maxMinutes: Int, val index: Int, val positionMs: Long)

/**
 * Trạng thái tab Short (mục 3.4.4). [ids] là danh sách **đã xáo** (SV-02); [videos] chỉ chứa chi tiết các video quanh trang hiện tại.
 * [generation] tăng mỗi lần dựng/xáo lại danh sách để Pager dựng lại từ đầu và player nạp lại.
 */
data class ShortsState(
    val phase: ShortsPhase = ShortsPhase.Loading,
    val ids: List<String> = emptyList(),
    val index: Int = 0,
    val maxMinutes: Int = VideoSettingOptions.DEFAULT_SHORT_MINUTES,
    val seed: Long = 0L,
    val generation: Int = 0,
    val videos: Map<String, ShortVideo> = emptyMap(),
    /** Vị trí cần nạp cho video hiện tại ở lần nạp đầu của [generation]; khác 0 chỉ khi khôi phục (ms). */
    val resumePositionMs: Long = 0L,
    /** Vừa khôi phục sau khi tiến trình bị thu hồi: video nằm chờ ở trạng thái tạm dừng (DH-06, SV-11). */
    val startPaused: Boolean = false,
)

sealed interface ShortsIntent {
    /**
     * Tab Short vào màn hình (mỗi lần dựng lại). Lần đầu thì dựng danh sách mới (SV-02), hoặc khôi phục từ [restore] nếu có; các lần sau
     * giữ nguyên thứ tự (DH-02) trừ khi "Thời lượng tối đa của Short" đã đổi thì lọc lại và xáo mới (SV-16).
     */
    data class Enter(val restore: ShortsSnapshot?) : ShortsIntent

    /** Người dùng vuốt tới video ở [index] (SV-04). */
    data class PageSettled(val index: Int) : ShortsIntent

    /** Kéo xuống ở video đầu tiên để xáo lại toàn bộ danh sách (SV-03); ở video khác thì không làm gì. */
    data object Reshuffle : ShortsIntent

    /** Đã dùng xong [ShortsState.resumePositionMs] và [ShortsState.startPaused] cho lần nạp đầu. */
    data object RestoreConsumed : ShortsIntent
}

sealed interface ShortsEffect {
    /** Vừa xáo lại danh sách: hiện viên "Đã xáo lại" 2 giây (SV-03). */
    data object Reshuffled : ShortsEffect
}
