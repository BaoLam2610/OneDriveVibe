package com.lambao.odv.core.domain.usecase.shorts

import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.repository.SyncRepository
import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Mục Short có hiện trên thanh điều hướng đáy không (DH-01): loại Video đang bật (CD-01) **và** đồng bộ lần đầu đã xong (DB-01). Quét lại
 * toàn bộ do `410` (DB-03) không làm ẩn lại vì `initialSyncDone` được giữ `true` trong lúc đó.
 */
class ObserveShortTabAvailableUseCase(
    private val settings: SettingsPreferences,
    private val sync: SyncRepository,
) {
    operator fun invoke(): Flow<Boolean> = combine(
        settings.enabledKinds.map { MediaKind.Video in it },
        sync.observeState().map { it.initialSyncDone },
    ) { videoEnabled, synced -> videoEnabled && synced }.distinctUntilChanged()
}
