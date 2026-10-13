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
    /**
     * Id (đã có tiền tố tài khoản) của thư mục gốc drive của tài khoản này, để mở "gốc của một tài khoản" từ ô tài khoản ở phạm vi Tất cả
     * (D10, TA-05). Null cho tới khi trang đồng bộ đầu tiên được ghi; ở phạm vi gộp luôn null.
     */
    val rootFolderId: String? = null,
    /**
     * Drive id của tài khoản **đang quét lần đầu**, chỉ đặt ở trạng thái gộp của phạm vi Tất cả tài khoản, để banner "Đang lập chỉ mục" ghi tên
     * tài khoản và hiện cho tới khi mọi tài khoản quét xong (D7, TA-05). Ở phạm vi một tài khoản luôn null (đã rõ là tài khoản nào).
     */
    val scanningAccountId: String? = null,
)

/**
 * Phần của [SyncState] mà các màn Thư mục và Thư viện cần để hiện banner và kéo làm mới (DS-04, TV-06). Đã áp quy tắc "app vừa khóa
 * không phải lỗi đồng bộ" (CH-03): màn Khóa sẽ che, và đồng bộ tự chạy tiếp sau khi mở khóa.
 */
data class SyncStatus(
    val isSyncing: Boolean = false,
    /** Số mục đã quét khi quét lần đầu ("đã quét N mục"). */
    val scannedCount: Int = 0,
    /** Đã quét xong drive ít nhất một lần: từ đó danh sách đọc từ Room, trước đó gọi thẳng API (TM-07). */
    val initialSyncDone: Boolean = false,
    /** Lần đồng bộ gần nhất lỗi (không tính app đang khóa). */
    val failed: Boolean = false,
    /** Tài khoản đang quét lần đầu ở phạm vi Tất cả (xem [SyncState.scanningAccountId]); null ở phạm vi một tài khoản. */
    val scanningAccountId: String? = null,
)
