package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Đồng bộ delta OneDrive về Room (ADR-0007, DB-01 → DB-05). Việc đồng bộ chạy trong scope riêng của app, không gắn với
 * ViewModel, nên rời màn hình không làm dừng nó. Chỉ có một lần đồng bộ chạy tại một thời điểm.
 */
interface SyncRepository {

    /** Trạng thái đồng bộ; phát lại giá trị đã lưu ngay khi thu thập. */
    fun observeState(): Flow<SyncState>

    /**
     * Yêu cầu đồng bộ nếu cần: quét lần đầu chưa xong, hoặc lần đồng bộ trước đã quá 15 phút (DS-04). Không chờ kết
     * quả; theo dõi qua [observeState]. Gọi khi đang đồng bộ thì bỏ qua.
     */
    fun syncIfStale()

    /** Kéo để làm mới (DS-04): đồng bộ ngay bất kể mốc thời gian. Gọi khi đang đồng bộ thì bỏ qua. */
    fun refresh()
}
