package com.lambao.odv.core.data.sync

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.database.DatabaseCompactor
import com.lambao.odv.core.data.drive.toAccount
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.database.SyncStateEntity
import com.lambao.odv.core.domain.hook.AccountDataCleaner
import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.SyncPhase
import com.lambao.odv.core.domain.model.SyncState
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import com.lambao.odv.core.data.SyncConstants
import com.lambao.odv.core.network.graph.GraphApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
 * **Nhiều tài khoản (ADR-0030, TA-05):** một lượt đồng bộ đi qua mọi tài khoản **lần lượt** (không song song, để không giành băng thông
 * và không vượt giới hạn tốc độ của Graph); tài khoản được ưu tiên ([prioritize]) rồi tài khoản đang xem chạy trước, các tài khoản
 * còn lại theo thứ tự thêm. Một tài khoản lỗi không chặn các tài khoản sau. Cũng là [AccountDataCleaner]: xóa tài khoản thì dừng
 * đồng bộ của nó rồi xóa dữ liệu của nó (TA-08).
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
    private val accounts: AccountRepository,
    private val api: GraphApi,
) : SyncRepository, ConnectionResetter, AccountDataCleaner {

    private class RunState(val syncing: Boolean = false, val scanned: Int = 0, val error: AppError? = null)

    /** Lần chạy đang diễn ra của một tài khoản; [done] hoàn tất khi lần chạy kết thúc (kể cả lỗi) để xóa tài khoản chờ nó dừng hẳn. */
    private class ActiveRun(val accountId: String, val done: CompletableDeferred<Unit>)

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Scope riêng cho việc theo dõi khóa: reset() hủy mọi con của [scope], nếu theo dõi nằm trong đó thì sau Ngắt kết nối hoặc
    // Quên PIN tính năng "chạy tiếp sau khi mở khóa" sẽ chết tới khi khởi động lại app.
    private val watcher = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Giữ suốt một lượt đồng bộ (mọi tài khoản): tryLock thất bại nghĩa là đang chạy, yêu cầu mới bị bỏ qua (single-flight), trừ
    // [prioritize] vì nó đặt [rerunRequested].
    private val running = Mutex()

    // Đặt trong lúc reset(): không cho lần đồng bộ mới chen vào giữa lúc dừng và lúc xóa (CD-05).
    @Volatile
    private var resetting = false

    // Trạng thái chạy của từng tài khoản, khóa là drive id. Tài khoản chưa chạy lần nào không có mặt.
    private val runtime = MutableStateFlow<Map<String, RunState>>(emptyMap())
    private val log = Logger.withTag("Sync")

    // Lần đồng bộ gần nhất dừng vì app bị khóa (AppLocked). Dùng khi chưa có dòng sync_state để biết có việc dở cần chạy tiếp
    // (khóa xảy ra trước khi trang đầu tiên kịp ghi).
    @Volatile
    private var interruptedByLock = false

    // Chốt chặn: chạy tiếp ngay (không chờ chuyển trạng thái) tối đa một lần cho mỗi lần khóa, để nếu bất biến "không có AppLocked
    // khi đang Unlocked" bị phá thì không thành vòng lặp nóng. Đặt lại khi mở khóa hoặc khi một lần chạy kết thúc không bị cắt.
    @Volatile
    private var immediateResumeUsed = false

    // Tài khoản đang bị xóa (TA-08): lần chạy của nó dừng ở trang kế và không ai được bắt đầu lần chạy mới cho nó.
    @Volatile
    private var removing: Set<String> = emptySet()
    private val purgeLock = Mutex()

    @Volatile
    private var active: ActiveRun? = null

    // Đã dọn dữ liệu mồ côi của tài khoản đã gỡ trong lần chạy app này chưa.
    @Volatile
    private var orphansChecked = false

    // Tài khoản được yêu cầu chạy trước (TA-06, tài khoản mới thêm); bị xóa ngay khi được chọn.
    @Volatile
    private var priorityId: String? = null

    // Có yêu cầu đến khi đang chạy ([prioritize]): chạy thêm một lượt sau khi lượt này xong, để tài khoản mới thêm không phải chờ 15 phút.
    @Volatile
    private var rerunRequested = false

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

    override fun observeAccountStates(): Flow<Map<String, SyncState>> =
        combine(dao.observeSyncStates(), runtime) { rows, runs ->
            val byId = rows.associateBy { it.accountId }
            (byId.keys + runs.keys).associateWith { id -> toSyncState(byId[id], runs[id]) }
        }

    override fun observeState(): Flow<SyncState> =
        combine(observeAccountStates(), accounts.observeAccounts(), accounts.observeScope()) { states, list, scope ->
            val ids = when (scope) {
                AccountScope.All -> list.map { it.id }
                is AccountScope.One -> listOf(scope.accountId)
            }
            aggregate(ids, ids.map { states[it] ?: SyncState() }, single = scope is AccountScope.One)
        }.distinctUntilChanged()

    /**
     * Gộp trạng thái các tài khoản trong phạm vi xem (TA-05). Một tài khoản: giữ nguyên. Nhiều tài khoản: đang đồng bộ nếu có tài khoản
     * nào đang chạy; xong lần đầu nếu **ít nhất một** tài khoản xong (mục Short, DH-01); mốc đồng bộ là mốc cũ nhất; lỗi là lỗi chung
     * của tài khoản đầu tiên bị lỗi. Lỗi `403`/`404` là của riêng từng tài khoản (TA-10) nên không đưa vào đây: hàng tài khoản đó tự báo
     * lỗi qua `AccountStatus.inaccessible`, các tài khoản khác chạy bình thường và không có banner "đồng bộ lỗi" cho cả app.
     */
    private fun aggregate(ids: List<String>, states: List<SyncState>, single: Boolean): SyncState {
        if (states.isEmpty()) return SyncState()
        if (single) return states.first()
        val syncing = states.firstOrNull { it.phase == SyncPhase.Syncing }
        // Tài khoản đang quét lần đầu (D7): banner "Đang lập chỉ mục" ghi tên nó và số mục của nó, kể cả khi tài khoản khác đã xong.
        val scanningIndex = states.indexOfFirst { it.phase == SyncPhase.Syncing && !it.initialSyncDone }
        val scanning = states.getOrNull(scanningIndex)
        return SyncState(
            phase = if (syncing != null) SyncPhase.Syncing else SyncPhase.Idle,
            scanningAccountId = if (scanning != null) ids[scanningIndex] else null,
            scannedCount = (scanning ?: syncing)?.scannedCount ?: states.maxOf { it.scannedCount },
            initialSyncDone = states.any { it.initialSyncDone },
            lastSyncedAt = states.mapNotNull { it.lastSyncedAt }.minOrNull(),
            error = states.firstNotNullOfOrNull { s -> s.error?.takeUnless { it.isAccountSpecific() } },
        )
    }

    private fun AppError.isAccountSpecific(): Boolean = this is AppError.Http && (status == 403 || status == 404)

    private fun toSyncState(row: SyncStateEntity?, run: RunState?): SyncState {
        val syncing = run?.syncing == true
        return SyncState(
            phase = if (syncing) SyncPhase.Syncing else SyncPhase.Idle,
            scannedCount = if (syncing) maxOf(run?.scanned ?: 0, row?.scannedCount ?: 0) else row?.scannedCount ?: 0,
            initialSyncDone = row?.initialSyncDone ?: false,
            lastSyncedAt = row?.lastSyncedAt,
            error = run?.error,
            rootFolderId = row?.rootId,
        )
    }

    override fun syncIfStale() = launchSync(force = false)

    override fun refresh() = launchSync(force = true)

    override fun prioritize(accountId: String) {
        priorityId = accountId
        rerunRequested = true
        launchSync(force = false)
    }

    private fun launchSync(force: Boolean) {
        if (resetting || !running.tryLock()) return
        // ATOMIC: thân coroutine luôn chạy tới điểm treo đầu tiên kể cả khi reset() hủy nó trước khi kịp bắt đầu, nhờ đó `finally` mở
        // khóa [running] luôn chạy (nếu không mutex kẹt và Ngắt kết nối treo, review Lát T D-M4).
        scope.launch(start = CoroutineStart.ATOMIC) {
            var interrupted = false
            try {
                // Một lần mỗi lần chạy app, khi config đã đọc được: dọn dữ liệu mồ côi của tài khoản đã gỡ nhưng chưa dọn xong (D-M1).
                // Đọc danh sách trước để vault nạp config (lỗi AppLocked rơi xuống catch bên dưới, lần sau thử lại).
                if (!orphansChecked) {
                    accounts.observeAccounts().first()
                    accounts.purgeOrphanedData()
                    orphansChecked = true
                }
                var forceThisPass = force
                do {
                    rerunRequested = false
                    interrupted = runPass(forceThisPass)
                    forceThisPass = false
                } while (!interrupted && rerunRequested && !resetting)
            } catch (e: CancellationException) {
                runtime.value = emptyMap()
                throw e
            } catch (e: Exception) {
                // Lỗi ngoài dự kiến (Room, ổ đĩa đầy...): bắt ở đây vì scope không có handler, để rơi ra là app crash và
                // trạng thái kẹt ở "đang đồng bộ". Chỉ ghi tên loại ngoại lệ, không ghi nội dung (CH-06).
                log.w { "Đồng bộ lỗi ngoài dự kiến: ${e::class.simpleName}" }
                // Chỉ tài khoản đang chạy dở mới nhận lỗi; tài khoản đã xong giữ nguyên kết quả của mình (review Lát T D-L4).
                runtime.update { runs ->
                    runs.mapValues { (_, run) -> if (run.syncing) RunState(error = AppError.Unknown(e)) else run }
                }
            } finally {
                priorityId = null
                running.unlock()
            }
            if (interrupted) onInterruptedByLock() else immediateResumeUsed = false
        }
    }

    /**
     * Một lượt: đi qua các tài khoản lần lượt, mỗi tài khoản đúng một lần. Trả `true` nếu bị cắt vì app khóa. Tài khoản kế tiếp được
     * chọn lại **sau mỗi lần chạy** ([pickNext]) nên tài khoản vừa thêm, vừa được ưu tiên hay vừa bị xóa trong lúc chạy được tính đến.
     */
    private suspend fun runPass(force: Boolean): Boolean {
        val handled = HashSet<String>()
        while (!resetting) {
            val account = pickNext(handled, force) ?: return security.lockState.value == LockState.Locked
            handled += account.id
            val done = CompletableDeferred<Unit>()
            active = ActiveRun(account.id, done)
            try {
                setRun(account.id, RunState(syncing = true))
                val result = engine.run(account.id, account.upn, shouldStop = { account.id in removing }) { scanned ->
                    setRun(account.id, RunState(syncing = true, scanned = scanned))
                }
                if (account.id in removing) {
                    // Đang bị xóa giữa chừng: không ghi lỗi hay trạng thái nào cho tài khoản sắp biến mất.
                    runtime.update { it - account.id }
                    continue
                }
                val error = (result as? AppResult.Failure)?.error
                setRun(account.id, RunState(error = error))
                if (result is AppResult.Success) refreshDetails(account)
                if (error == AppError.AppLocked) return true
            } catch (e: CancellationException) {
                runtime.update { it - account.id }
                throw e
            } catch (e: Exception) {
                log.w { "Đồng bộ một tài khoản lỗi ngoài dự kiến: ${e::class.simpleName}" }
                setRun(account.id, RunState(error = AppError.Unknown(e)))
            } finally {
                active = null
                done.complete(Unit)
            }
        }
        return false
    }

    private fun setRun(accountId: String, state: RunState) = runtime.update { it + (accountId to state) }

    /**
     * Tài khoản kế tiếp cần chạy: tài khoản được ưu tiên, rồi tài khoản đang xem (TA-05), rồi các tài khoản còn lại theo thứ tự thêm.
     * Bỏ tài khoản đã chạy trong lượt này, đang bị xóa, hoặc chưa quá hạn (khi không ép làm mới, DS-04).
     */
    private suspend fun pickNext(handled: Set<String>, force: Boolean): Account? {
        val list = accounts.observeAccounts().first()
        if (list.isEmpty()) return null
        val viewing = (accounts.observeScope().first() as? AccountScope.One)?.accountId
        val priority = priorityId
        val ordered = buildList {
            list.firstOrNull { it.id == priority }?.let(::add)
            list.firstOrNull { it.id == viewing }?.let(::add)
            addAll(list)
        }.distinctBy { it.id }
        val next = ordered.firstOrNull { it.id !in handled && it.id !in removing && (force || isStale(it.id)) }
        if (next != null && next.id == priority) priorityId = null
        return next
    }

    /** Làm mới tên hiển thị và email sau mỗi lần đồng bộ thành công (TA-02). Lỗi bỏ qua: không ảnh hưởng dữ liệu đã đồng bộ. */
    private suspend fun refreshDetails(account: Account) {
        val drive = (api.getDrive(account.upn) as? AppResult.Success)?.value ?: return
        // Response thiếu owner: giữ tên và email đã có, không ghi đè bằng tên dự phòng dựng từ UPN (review Lát T D-L6).
        if (drive.owner?.user == null) return
        val fresh = drive.toAccount(account.upn)
        // Khác drive id nghĩa là UPN nay trỏ drive khác: không đụng tới tài khoản đã lưu.
        if (fresh.id == account.id) accounts.updateDetails(fresh)
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
        if (!interrupted && dao.getSyncStates().isEmpty()) return
        interruptedByLock = false
        log.i { "Đã mở khóa: kiểm tra đồng bộ (bị cắt bởi khóa: $interrupted)" }
        launchSync(force = false)
    }

    private suspend fun isStale(accountId: String): Boolean {
        val state = dao.getSyncState(accountId) ?: return true
        if (!state.initialSyncDone || state.pendingNextLink != null) return true
        val last = state.lastSyncedAt ?: return true
        // Đồng hồ máy chỉnh lùi (tuổi âm) cũng coi là quá hạn để không kẹt với mốc ở tương lai.
        return (clock.now().toEpochMilliseconds() - last) !in 0..SyncConstants.STALE_AFTER_MS
    }

    override suspend fun purge(accountId: String) = purgeLock.withLock {
        removing = removing + accountId
        try {
            if (priorityId == accountId) priorityId = null
            // Chờ lần chạy đang dở của tài khoản này dừng hẳn rồi mới xóa, để nó không ghi lại dữ liệu sau khi xóa (TA-08).
            active?.takeIf { it.accountId == accountId }?.done?.await()
            dao.clearAccount(accountId)
            compactQuietly()
            runtime.update { it - accountId }
        } finally {
            removing = removing - accountId
        }
    }

    override suspend fun reset() {
        resetting = true
        try {
            // Dừng mọi lần đồng bộ đang chạy (không dựa vào một biến job có thể chưa kịp gán) rồi mới xóa.
            scope.coroutineContext[Job]?.children?.toList()?.forEach { it.cancelAndJoin() }
            running.withLock {
                dao.clearAll()
                // DELETE chỉ đánh dấu trang trống nên tên tệp cũ còn trong odv.db và WAL; thu gọn để dữ liệu thật sự mất (CD-05).
                compactQuietly()
            }
            interruptedByLock = false
            priorityId = null
            rerunRequested = false
            removing = emptySet()
            runtime.value = emptyMap()
        } finally {
            resetting = false
        }
    }

    /**
     * Thu gọn odv.db để tên tệp đã xóa thật sự mất (CD-05, TA-08). Lỗi ở đây không được chặn việc xóa config và khóa (hợp đồng
     * [ConnectionResetter]), chỉ ghi tên loại ngoại lệ (CH-06).
     */
    private suspend fun compactQuietly() {
        try {
            compactor.compact()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w { "Không thu gọn được odv.db sau khi xóa: ${e::class.simpleName}" }
        }
    }
}
