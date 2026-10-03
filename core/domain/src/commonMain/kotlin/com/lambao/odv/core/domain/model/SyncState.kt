package com.lambao.odv.core.domain.model

import com.lambao.odv.core.common.error.AppError

enum class SyncPhase {
    /** Không đồng bộ. Lần gần nhất thành công hay chưa xem [SyncState.lastSyncedAt] và [SyncState.error]. */
    Idle,

    /** Đang đồng bộ (quét lần đầu hoặc lấy thay đổi). */
    Syncing,
}

/**
 * Trạng thái đồng bộ delta (DB-01 → DB-05) để UI hiện banner, quyết định đọc Room hay gọi API (TM-07) và tự đồng bộ
 * khi quá 15 phút (DS-04).
 */
data class SyncState(
    val phase: SyncPhase = SyncPhase.Idle,
    /** Số mục đã quét trong lần quét đầy đủ đang chạy hoặc dở dang ("đã quét N mục", TV-06). */
    val scannedCount: Int = 0,
    /**
     * Đã quét xong toàn bộ drive ít nhất một lần. Chỉ khi đó Room mới đủ dữ liệu để làm nguồn duy nhất của tab Thư mục
     * và để áp dụng TM-04. Giữ `true` khi quét lại vì `410` (DB-03): danh sách cũ vẫn dùng được.
     */
    val initialSyncDone: Boolean = false,
    /** Mốc (epoch mili giây) lần đồng bộ trọn vẹn gần nhất; null nếu chưa có. */
    val lastSyncedAt: Long? = null,
    /** Lỗi của lần đồng bộ gần nhất; null nếu lần đó thành công hoặc chưa chạy. UI chọn thông báo theo mã. */
    val error: AppError? = null,
)
