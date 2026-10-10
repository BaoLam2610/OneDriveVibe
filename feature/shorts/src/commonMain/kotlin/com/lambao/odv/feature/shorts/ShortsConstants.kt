package com.lambao.odv.feature.shorts

import co.touchlab.kermit.Logger

/**
 * Hằng số của `:feature:shorts`. [TAG] dùng chung thẻ "ODVPlayer" với `:core:media` và `:feature:player` để tab Log của màn Debug
 * (ADR-0012) lọc "Player" thấy trọn đường đi của một lần phát ở cả Short lẫn màn Xem video. Quy tắc log (CH-06): không ghi link phát,
 * không ghi message ngoại lệ mạng; chỉ ghi id rút gọn, mã và thời gian.
 */
internal object ShortsConstants {
    const val TAG = "ODVPlayer"

    /** Không có ảnh chụp trạng thái để khôi phục (chưa từng mở tab Short). */
    const val NO_SEED = Long.MIN_VALUE

    /** Cửa sổ nạp thông tin video quanh trang hiện tại: trang trước, hiện tại, trang sau (preload ở 8c cũng dùng ±1, ADR-0024). */
    const val NEIGHBOR_RADIUS = 1

    /** Số seed thử tối đa khi xáo lại để video đầu mới khác video đang xem (SV-03). */
    const val RESHUFFLE_SEED_ATTEMPTS = 8

    /**
     * SV-08: video dọc có phần bị cắt không quá chừng này mỗi chiều thì Cắt đầy, vượt thì Vừa khung. Đề xuất ~20%, chốt lại khi thử máy
     * (đặc tả mục 7, điểm mở về ngưỡng "tỉ lệ gần").
     */
    const val MAX_CROP_FRACTION = 0.2f

    /** Nhịp đọc vị trí phát để vẽ thanh tiến độ (SV-07) và lưu vị trí khôi phục. */
    const val PROGRESS_TICK_MS = 250L

    /** Viên "Đã xáo lại" hiện chừng này (`duration.toast-label`, SV-03). */
    const val LABEL_MS = 2000L

    /** Kéo xuống ở video đầu quá chừng này (dp) rồi thả thì xáo lại (SV-03). */
    const val RESHUFFLE_PULL_DP = 96

    /** Cuộn về đầu khi chạm lại tab Short: 320ms + 90ms × ln(1 + số trang), tối đa 900ms (DH-04, 2026-10-11). */
    const val SCROLL_TOP_BASE_MS = 320f
    const val SCROLL_TOP_LOG_MS = 90f
    const val SCROLL_TOP_MAX_MS = 900

    /** Chờ chừng này mới hiện spinner tải: video đã được tải trước thường lên hình trong vài trăm ms, hiện spinner ngay chỉ gây nháy. */
    const val SPINNER_DELAY_MS = 400L

    /** Vòng tạm dừng mờ vào/ra (`duration.fast`, SV-06). */
    const val PAUSE_FADE_MS = 150

    /** Số lần tự chặn bộ giải mã vừa chết rồi phát lại cho một video (bug Dolby Vision trên MediaTek, như màn Xem video). */
    const val MAX_DECODER_RETRIES = 2
}

internal val shortsLog: Logger = Logger.withTag(ShortsConstants.TAG)
