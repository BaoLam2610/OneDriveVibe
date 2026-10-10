package com.lambao.odv.core.domain.usecase.sync

import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.repository.SyncRepository

/**
 * Tự đồng bộ khi app quay lại foreground (DS-04, "tự đồng bộ khi mở"). Cần vì ViewModel của các tab còn sống khi process sống lâu ở
 * nền nên `syncIfStale()` lúc tạo ViewModel không chạy lại; ở chế độ không PIN không có bước mở khóa nào đánh thức nó.
 *
 * Chỉ chạy khi đã `Unlocked` và đã có config:
 * - `Unknown` (khởi động nguội): các màn tự gọi khi được tạo.
 * - `Locked` (chế độ PIN): `SyncCoordinator` tự chạy tiếp sau khi mở khóa, gọi ở đây sẽ chỉ trả `AppLocked`.
 * - chưa có config (màn Kết nối): không có gì để đồng bộ.
 * Vẫn qua kiểm tra quá 15 phút của `syncIfStale()` nên mở app liên tục không gọi Graph liên tục.
 */
class SyncOnForegroundUseCase(
    private val security: SecurityRepository,
    private val config: ConfigRepository,
    private val sync: SyncRepository,
) {
    suspend operator fun invoke() {
        if (security.lockState.value != LockState.Unlocked) return
        if (!config.hasConfig()) return
        sync.syncIfStale()
    }
}
