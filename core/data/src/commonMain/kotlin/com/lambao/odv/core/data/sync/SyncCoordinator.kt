package com.lambao.odv.core.data.sync

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.SyncPhase
import com.lambao.odv.core.domain.model.SyncState
import com.lambao.odv.core.domain.repository.ConnectionResetter
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/** Quá ngần này kể từ lần đồng bộ trọn vẹn trước thì tự đồng bộ khi mở app (DS-04). */
private const val STALE_AFTER_MS = 15L * 60 * 1000

/**
 * Điều phối đồng bộ: giữ scope riêng của app (không chết theo ViewModel), bảo đảm một lần chạy tại một thời điểm và
 * công bố [SyncState]. Cũng là [ConnectionResetter] của Room: ngắt kết nối thì dừng đồng bộ rồi xóa dữ liệu (CD-05).
 */
internal class SyncCoordinator(
    private val engine: SyncEngine,
    private val dao: DriveDao,
    dispatchers: DispatcherProvider,
    private val clock: () -> Long,
) : SyncRepository, ConnectionResetter {

    private class RunState(val syncing: Boolean = false, val scanned: Int = 0, val error: AppError? = null)

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Giữ suốt một lần đồng bộ: tryLock thất bại nghĩa là đang chạy, yêu cầu mới bị bỏ qua (single-flight).
    private val running = Mutex()

    // Đặt trong lúc reset(): không cho lần đồng bộ mới chen vào giữa lúc dừng và lúc xóa (CD-05).
    @Volatile
    private var resetting = false
    private val runtime = MutableStateFlow(RunState())
    private val log = Logger.withTag("Sync")

    override fun observeState(): Flow<SyncState> = combine(dao.observeSyncState(), runtime) { row, rt ->
        SyncState(
            phase = if (rt.syncing) SyncPhase.Syncing else SyncPhase.Idle,
            scannedCount = if (rt.syncing) maxOf(rt.scanned, row?.scannedCount ?: 0) else row?.scannedCount ?: 0,
            initialSyncDone = row?.initialSyncDone ?: false,
            lastSyncedAt = row?.lastSyncedAt,
            error = rt.error,
        )
    }

    override fun syncIfStale() = launchSync(force = false)

    override fun refresh() = launchSync(force = true)

    private fun launchSync(force: Boolean) {
        if (resetting || !running.tryLock()) return
        scope.launch {
            try {
                if (!force && !isStale()) return@launch
                runtime.value = RunState(syncing = true)
                val result = engine.run { scanned -> runtime.update { RunState(syncing = true, scanned = scanned) } }
                runtime.value = RunState(error = (result as? AppResult.Failure)?.error)
            } catch (e: CancellationException) {
                runtime.value = RunState()
                throw e
            } catch (e: Exception) {
                // Lỗi ngoài dự kiến (Room, ổ đĩa đầy...): bắt ở đây vì scope không có handler, để rơi ra là app crash và
                // trạng thái kẹt ở "đang đồng bộ". Chỉ ghi tên loại ngoại lệ, không ghi nội dung (CH-06).
                log.w { "Đồng bộ lỗi ngoài dự kiến: ${e::class.simpleName}" }
                runtime.value = RunState(error = AppError.Unknown(e))
            } finally {
                running.unlock()
            }
        }
    }

    private suspend fun isStale(): Boolean {
        val state = dao.getSyncState() ?: return true
        if (!state.initialSyncDone || state.pendingNextLink != null) return true
        val last = state.lastSyncedAt ?: return true
        // Đồng hồ máy chỉnh lùi (tuổi âm) cũng coi là quá hạn để không kẹt với mốc ở tương lai.
        return (clock() - last) !in 0..STALE_AFTER_MS
    }

    override suspend fun reset() {
        resetting = true
        try {
            // Dừng mọi lần đồng bộ đang chạy (không dựa vào một biến job có thể chưa kịp gán) rồi mới xóa.
            scope.coroutineContext[Job]?.children?.toList()?.forEach { it.cancelAndJoin() }
            running.withLock { dao.clearAll() }
            runtime.value = RunState()
        } finally {
            resetting = false
        }
    }
}
