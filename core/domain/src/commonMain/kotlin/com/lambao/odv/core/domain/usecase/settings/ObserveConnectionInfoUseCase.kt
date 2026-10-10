package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.common.result.getOrNull
import com.lambao.odv.core.domain.model.ConnectionInfo
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Thông tin kết nối cho nhóm Kết nối của Cài đặt: UPN, Tenant ID và Client ID che bớt, thời điểm đồng bộ gần nhất (đổi theo
 * từng lần đồng bộ). Phát `null` nếu không đọc được config (chưa lưu, hoặc app đang khóa). Config chỉ được giữ trong lúc trích
 * thông tin: **Client Secret không được đưa vào [ConnectionInfo]** (CH-03, CH-06).
 */
class ObserveConnectionInfoUseCase(
    private val configs: ConfigRepository,
    private val sync: SyncRepository,
) {
    operator fun invoke(): Flow<ConnectionInfo?> = flow {
        val identity = configs.load().getOrNull()?.let { Triple(it.upn, mask(it.tenantId), mask(it.clientId)) }
        if (identity == null) {
            emit(null)
        } else {
            val (upn, tenantMasked, clientMasked) = identity
            emitAll(sync.observeState().map { ConnectionInfo(upn, tenantMasked, clientMasked, it.lastSyncedAt) })
        }
    }

    /** `a1b2c3d4-...-9f0e` thành `a1b2••••9f0e`: giữ 4 ký tự đầu và cuối; chuỗi quá ngắn thì che hết. */
    private fun mask(id: String): String =
        if (id.length > MASK_KEEP * 2) id.take(MASK_KEEP) + MASK_FILL + id.takeLast(MASK_KEEP) else MASK_FILL

    private companion object {
        const val MASK_KEEP = 4
        const val MASK_FILL = "••••"
    }
}
