package com.lambao.odv.core.data.sync

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.data.drive.toCredentials
import com.lambao.odv.core.data.drive.toEntity
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.database.DriveItemEntity
import com.lambao.odv.core.database.SyncStateEntity
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.network.GraphApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Một lần đồng bộ delta (DB-01 → DB-05). Không giữ trạng thái giữa các lần chạy: mọi thứ cần để tiếp tục nằm trong
 * bảng `sync_state`, nên tắt app lúc nào cũng tiếp tục được.
 *
 * - Chưa có `deltaLink`: quét đầy đủ từ `/root/delta` (DB-01). Mỗi trang ghi nguyên tử cùng `pendingNextLink` (DB-04).
 * - Có `deltaLink`: chỉ lấy thay đổi từ mốc đó (DB-02).
 * - `410`: bỏ mốc rồi quét đầy đủ lại ở nền, danh sách cũ vẫn dùng được trong lúc quét (DB-03); mục không còn trên
 *   OneDrive được dọn ở trang cuối, cùng transaction với mốc mới.
 *
 * Config đọc lại ở từng trang để khi app khóa (CH-03) lần đồng bộ dừng ngay với `AppLocked`, không giữ bí mật trong
 * một coroutine chạy dài.
 */
internal class SyncEngine(
    private val api: GraphApi,
    private val configs: ConfigRepository,
    private val dao: DriveDao,
    private val clock: () -> Long,
) {

    /** [onProgress] nhận số mục đã quét trong lần quét đầy đủ (TV-06). Trả lỗi theo [AppError], không ném ngoại lệ. */
    suspend fun run(onProgress: (Int) -> Unit): AppResult<Unit> {
        var restarted = false
        while (true) {
            val result = runOnce(onProgress)
            if (result is AppResult.Failure && result.error.isResyncRequired() && !restarted) {
                restarted = true
                dropCursor()
                continue
            }
            return result
        }
    }

    private suspend fun runOnce(onProgress: (Int) -> Unit): AppResult<Unit> {
        var state = dao.getSyncState() ?: SyncStateEntity()
        val fullScan = state.deltaLink == null
        // Quét đầy đủ mới (không phải tiếp tục trang dở) thì tăng scanId và đếm lại từ 0.
        val resuming = state.pendingNextLink != null
        val scanId = if (fullScan && !resuming) state.scanId + 1 else state.scanId
        var scanned = if (fullScan && resuming) state.scannedCount else 0
        var link: String? = state.pendingNextLink ?: state.deltaLink

        while (true) {
            currentCoroutineContext().ensureActive()
            val config = when (val loaded = configs.load()) {
                is AppResult.Success -> loaded.value
                is AppResult.Failure -> return loaded
            }
            val page = when (val fetched = api.deltaPage(config.toCredentials(), link)) {
                is AppResult.Success -> fetched.value
                is AppResult.Failure -> return fetched
            }

            var rootId = state.rootId
            val upserts = ArrayList<DriveItemEntity>(page.value.size)
            val deleted = ArrayList<String>()
            for (dto in page.value) {
                when {
                    // Gốc drive không phải một mục để hiển thị: chỉ nhớ id để biết "thư mục gốc" của UI là mục nào.
                    dto.root != null -> rootId = dto.id
                    dto.deleted != null -> deleted += dto.id
                    else -> dto.toEntity(scanId)?.let(upserts::add)
                }
            }
            scanned += upserts.size

            val next = page.nextLink
            if (next != null) {
                state = state.copy(rootId = rootId, scanId = scanId, scannedCount = scanned, pendingNextLink = next)
                dao.applyPage(upserts, deleted, state, purgeScanId = null)
                onProgress(scanned)
                link = next
                continue
            }

            // Trang cuối bắt buộc có deltaLink; thiếu thì dữ liệu máy chủ bất thường, không đánh dấu hoàn tất.
            val delta = page.deltaLink ?: return AppResult.Failure(AppError.Unknown())
            state = state.copy(
                rootId = rootId,
                scanId = scanId,
                scannedCount = scanned,
                deltaLink = delta,
                pendingNextLink = null,
                initialSyncDone = true,
                lastSyncedAt = clock(),
            )
            dao.applyPage(upserts, deleted, state, purgeScanId = if (fullScan) scanId else null)
            onProgress(scanned)
            return AppResult.Success(Unit)
        }
    }

    /** Bỏ mốc và trang dở để lần chạy sau quét đầy đủ lại (DB-03); giữ nguyên `rootId`, `scanId` và dữ liệu đang có. */
    private suspend fun dropCursor() {
        val state = dao.getSyncState() ?: return
        dao.upsertSyncState(state.copy(deltaLink = null, pendingNextLink = null, scannedCount = 0))
    }

    private fun AppError.isResyncRequired(): Boolean = this is AppError.Http && status == 410
}
