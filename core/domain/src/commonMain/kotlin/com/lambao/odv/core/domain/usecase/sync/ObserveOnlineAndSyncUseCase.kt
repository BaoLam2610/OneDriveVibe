package com.lambao.odv.core.domain.usecase.sync

import com.lambao.odv.core.domain.platform.NetworkMonitor
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Máy có mạng hay không (DS-05), kèm một tác dụng phụ **chỉ khi được thu thập**: mỗi lần có mạng (kể cả lần phát đầu) thì bù lần
 * đồng bộ đã lỡ nếu đã quá hạn (DS-04). Cố ý không đặt trong `SyncCoordinator`: coordinator chạy từ lúc mở app nên sẽ đồng bộ cả khi
 * đang ở màn Khóa (vô ích vì config chưa giải mã); ở đây chỉ chạy khi một màn đang quan sát.
 */
class ObserveOnlineAndSyncUseCase(
    private val network: NetworkMonitor,
    private val sync: SyncRepository,
) {
    operator fun invoke(): Flow<Boolean> = network.isOnline.onEach { online -> if (online) sync.syncIfStale() }
}
