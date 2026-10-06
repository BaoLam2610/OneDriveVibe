package com.lambao.odv.core.domain.usecase.sync

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.domain.model.SyncPhase
import com.lambao.odv.core.domain.model.SyncStatus
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Trạng thái đồng bộ cho banner và kéo làm mới (DS-04, TV-06). `AppLocked` không tính là lỗi đồng bộ: app vừa khóa (CH-03), màn Khóa
 * sẽ che, và `SyncCoordinator` tự chạy tiếp sau khi mở khóa.
 */
class ObserveSyncStatusUseCase(
    private val sync: SyncRepository,
) {
    operator fun invoke(): Flow<SyncStatus> = sync.observeState().map { s ->
        SyncStatus(
            isSyncing = s.phase == SyncPhase.Syncing,
            scannedCount = s.scannedCount,
            initialSyncDone = s.initialSyncDone,
            failed = s.error != null && s.error != AppError.AppLocked,
        )
    }
}
