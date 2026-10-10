package com.lambao.odv.core.data.sync

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.database.DatabaseCompactor
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.SyncPhase
import com.lambao.odv.core.domain.model.SyncState
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import com.lambao.odv.core.data.SyncConstants
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
import kotlin.time.Clock

/**
 * Điều phối đồng bộ: giữ scope riêng của app (không chết theo ViewModel), bảo đảm một lần chạy tại một thời điểm và
 * công bố [SyncState]. Cũng là [ConnectionResetter] của Room: ngắt kết nối thì dừng đồng bộ rồi xóa dữ liệu (CD-05).
 *
 * Tự chạy tiếp sau khi mở khóa (DS-04: "tự đồng bộ khi mở, sau khi mở khóa"): khóa app xóa config khỏi bộ nhớ (CH-03) nên một lần
 * đồng bộ đang chạy dừng với `AppLocked`; các màn cố ý không báo lỗi này, và ViewModel của chúng vẫn sống qua lần khóa nên
 * không ai gọi lại [syncIfStale]. Nếu không có bước này, đồng bộ lần đầu bị bỏ dở và banner "đang lập chỉ mục" biến mất cho tới
 * khi một màn khác được tạo mới (xem Nhật ký 2026-10-07).
 */
internal class SyncCoordinator(
    private val engine: SyncEngine,
    private val dao: DriveDao,
    dispatchers: DispatcherProvider,
    private val clock: Clock,
    private val security: SecurityRepository,
    private val compactor: DatabaseCompactor,
) : SyncRepository, ConnectionResetter {

    private class RunState(val syncing: Boolean = false, val scanned: Int = 0, val error: AppError? = null)

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Scope riêng cho việc theo dõi khóa: reset() hủy mọi con của [scope], nếu theo dõi nằm trong đó thì sau Ngắt kết nối hoặc
    // Quên PIN tính năng "chạy tiếp sau khi mở khóa" sẽ chết tới khi khởi động lại app.
    private val watcher = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Giữ suốt một lần đồng bộ: tryLock thất bại nghĩa là đang chạy, yêu cầu mới bị bỏ qua (single-flight).
    private val running = Mutex()

    // Đặt trong lúc reset(): không cho lần đồng bộ mới chen vào giữa lúc dừng và lúc xóa (CD-05).
    @Volatile
    private var resetting = false
    private val runtime = MutableStateFlow(RunState())
    private val log = Logger.withTag("Sync")

    // Lần đồng bộ gần nhất dừng vì app bị khóa (AppLocked). Dùng khi chưa có dòng sync_state để biết có việc dở cần chạy tiếp
    // (khóa xảy ra trước khi trang đầu tiên kịp ghi).
    @Volatile
    private var interruptedByLock = false

    // Chốt chặn: chạy tiếp ngay (không chờ chuyển trạng thái) tối đa một lần cho mỗi lần khóa, để nếu bất biến "không có AppLocked
    // khi đang Unlocked" bị phá thì không thành vòng lặp nóng. Đặt lại khi mở khóa hoặc khi một lần chạy kết thúc không bị cắt.
    @Volatile
    private var immediateResumeUsed = false

    init {
        watcher.launch {
            var previous: LockState? = null
            security.lockState.collect { state ->
                // Chỉ chuyển Khóa → Mở khóa: lần phát đầu (Unknown/Unlocked lúc khởi động) đã có màn gọi syncIfStale lúc được tạo.
                if (previous == LockState.Locked && state == LockState.Unlocked) {
                    immediateResumeUsed = false
                    resumeAfterUnlock()
                }
                previous = state
            }
        }
    }

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
            var interrupted = false
            try {
                if (!force && !isStale()) return@launch
                runtime.value = RunState(syncing = true)
                val result = engine.run { scanned -> runtime.update { RunState(syncing = true, scanned = scanned) } }
                val error = (result as? AppResult.Failure)?.error
                interrupted = error == AppError.AppLocked
                runtime.value = RunState(error = error)
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
            if (interrupted) onInterruptedByLock() else immediateResumeUsed = false
        }
    }

    private fun onInterruptedByLock() {
        interruptedByLock = true
        log.i { "Đồng bộ dừng vì app bị khóa, sẽ chạy tiếp sau khi mở khóa" }
        // Đã mở khóa lại ngay trước khi cờ trên kịp đặt: không còn chuyển trạng thái nào để đánh thức, nên chạy tiếp luôn.
        if (security.lockState.value == LockState.Unlocked && !immediateResumeUsed) {
            immediateResumeUsed = true
            scope.launch { resumeAfterUnlock() }
        }
    }

    /**
     * Sau khi mở khóa: chạy tiếp nếu lần đồng bộ trước bị khóa cắt ngang, hoặc đã từng đồng bộ (kiểm tra quá 15 phút ở
     * [isStale], DS-04). Chưa từng có gì (vừa Quên mã PIN xong, Room đã xóa) thì không có gì để chạy tiếp.
     */
    private suspend fun resumeAfterUnlock() {
        val interrupted = interruptedByLock
        if (!interrupted && dao.getSyncState() == null) return
        interruptedByLock = false
        log.i { "Đã mở khóa: kiểm tra đồng bộ (bị cắt bởi khóa: $interrupted)" }
        launchSync(force = false)
    }

    private suspend fun isStale(): Boolean {
        val state = dao.getSyncState() ?: return true
        if (!state.initialSyncDone || state.pendingNextLink != null) return true
        val last = state.lastSyncedAt ?: return true
        // Đồng hồ máy chỉnh lùi (tuổi âm) cũng coi là quá hạn để không kẹt với mốc ở tương lai.
        return (clock.now().toEpochMilliseconds() - last) !in 0..SyncConstants.STALE_AFTER_MS
    }

    override suspend fun reset() {
        resetting = true
        try {
            // Dừng mọi lần đồng bộ đang chạy (không dựa vào một biến job có thể chưa kịp gán) rồi mới xóa.
            scope.coroutineContext[Job]?.children?.toList()?.forEach { it.cancelAndJoin() }
            running.withLock {
                dao.clearAll()
                // DELETE chỉ đánh dấu trang trống nên tên tệp cũ còn trong odv.db và WAL; thu gọn để dữ liệu thật sự mất (CD-05).
                // Lỗi ở đây không được chặn việc xóa config và khóa (hợp đồng ConnectionResetter), chỉ ghi tên loại ngoại lệ (CH-06).
                try {
                    compactor.compact()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w { "Không thu gọn được odv.db sau khi xóa: ${e::class.simpleName}" }
                }
            }
            interruptedByLock = false
            runtime.value = RunState()
        } finally {
            resetting = false
        }
    }
}
