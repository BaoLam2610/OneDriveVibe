package com.lambao.odv.core.domain.usecase.sync

import com.lambao.odv.core.domain.repository.SyncRepository

/** Kéo để làm mới (DS-04): đồng bộ ngay bất kể mốc thời gian. Gọi khi đang đồng bộ thì bỏ qua. */
class RefreshSyncUseCase(
    private val sync: SyncRepository,
) {
    operator fun invoke() = sync.refresh()
}
