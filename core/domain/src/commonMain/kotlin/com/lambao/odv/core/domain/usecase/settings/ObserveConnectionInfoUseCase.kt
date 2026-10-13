package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.common.result.getOrNull
import com.lambao.odv.core.domain.model.ConnectionInfo
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phần dùng chung của kết nối cho nhóm "Tài khoản và kết nối" của Cài đặt: Tenant ID và Client ID che bớt. Phát `null` nếu không đọc
 * được config (chưa lưu, hoặc app đang khóa). Config chỉ được giữ trong lúc trích thông tin: **Client Secret không được đưa vào
 * [ConnectionInfo]** (CH-03, CH-06). Danh sách tài khoản và trạng thái đồng bộ từng tài khoản là việc của [ObserveAccountStatusesUseCase].
 */
class ObserveConnectionInfoUseCase(
    private val configs: ConfigRepository,
    private val security: SecurityRepository,
) {
    /**
     * Đọc lại mỗi khi trạng thái khóa đổi (StateFlow nên đã bỏ trùng): ViewModel Cài đặt có thể được tạo lúc app đang khóa (`load()`
     * lỗi `AppLocked` → `null`); khi mở khóa phải đọc lại, nếu không nhóm Kết nối mất mãi.
     */
    operator fun invoke(): Flow<ConnectionInfo?> = security.lockState.map {
        configs.load().getOrNull()?.let { ConnectionInfo(mask(it.tenantId), mask(it.clientId)) }
    }

    /** `a1b2c3d4-...-9f0e` thành `a1b2••••9f0e`: giữ 4 ký tự đầu và cuối; chuỗi quá ngắn thì che hết. */
    private fun mask(id: String): String =
        if (id.length > MASK_KEEP * 2) id.take(MASK_KEEP) + MASK_FILL + id.takeLast(MASK_KEEP) else MASK_FILL

    private companion object {
        const val MASK_KEEP = 4
        const val MASK_FILL = "••••"
    }
}
