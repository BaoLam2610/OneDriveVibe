package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Đồng bộ delta OneDrive về Room (ADR-0007, DB-01 → DB-05). Việc đồng bộ chạy trong scope riêng của app, không gắn với
 * ViewModel, nên rời màn hình không làm dừng nó. Chỉ có một lần đồng bộ chạy tại một thời điểm.
 *
 * **Mọi tài khoản đều được đồng bộ, lần lượt**, tài khoản đang xem chạy trước, không phụ thuộc phạm vi xem (TA-05, ADR-0030).
 * Mốc `deltaLink`, tiến độ quét lần đầu và lần đồng bộ gần nhất là của từng tài khoản (DB-01).
 */
interface SyncRepository {

    /**
     * Trạng thái đồng bộ **của phạm vi xem đang chọn**; phát lại giá trị đã lưu ngay khi thu thập. Một tài khoản: trạng thái của nó. Tất
     * cả tài khoản: đang đồng bộ nếu có tài khoản nào đang chạy; `initialSyncDone` nếu **ít nhất một** tài khoản đã quét xong lần đầu
     * (DH-01); `lastSyncedAt` là mốc cũ nhất trong các tài khoản đã đồng bộ; `error` là lỗi của tài khoản đầu tiên bị lỗi (TA-05).
     */
    fun observeState(): Flow<SyncState>

    /** Trạng thái đồng bộ của **từng tài khoản**, khóa theo `Account.id`, cho hàng tài khoản ở Cài đặt (TA-09, TA-10). */
    fun observeAccountStates(): Flow<Map<String, SyncState>>

    /**
     * Đưa [accountId] lên đầu hàng đợi đồng bộ (TA-06): được đồng bộ ngay lượt kế tiếp. Lần đồng bộ đang chạy dở ở tài khoản khác chạy
     * tiếp đến hết rồi mới tới lượt, không bị hủy.
     */
    fun prioritize(accountId: String)

    /**
     * Yêu cầu đồng bộ **mọi tài khoản** nếu cần: quét lần đầu chưa xong, hoặc lần đồng bộ trước đã quá 15 phút (DS-04), tính riêng
     * từng tài khoản. Không chờ kết
     * quả; theo dõi qua [observeState]. Gọi khi đang đồng bộ thì bỏ qua.
     */
    fun syncIfStale()

    /** Kéo để làm mới (DS-04): đồng bộ ngay mọi tài khoản bất kể mốc thời gian. Gọi khi đang đồng bộ thì bỏ qua. */
    fun refresh()
}
