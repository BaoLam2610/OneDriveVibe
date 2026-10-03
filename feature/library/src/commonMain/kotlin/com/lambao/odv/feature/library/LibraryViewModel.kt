package com.lambao.odv.feature.library

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.SyncPhase
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.domain.repository.NetworkMonitor
import com.lambao.odv.core.domain.repository.SyncRepository
import com.lambao.odv.core.domain.repository.UtcOffsetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Giãn cách tối thiểu giữa hai lần cập nhật số mục theo ngày: mỗi trang delta làm Room phát lại, không cần vẽ lại từng lần. */
private const val DAYS_MIN_INTERVAL_MS = 300L

/**
 * Tab Thư viện (TV-01 → TV-06). Nguồn dữ liệu duy nhất là Room (ADR-0007): ảnh và video đọc theo trang qua Paging
 * ([pages]), số mục theo ngày ([LibraryState.days]) dựng tiêu đề nhóm và độ dài lưới. Tab không gọi API; khi quét lần
 * đầu chưa xong thì hiện phần đã quét được cùng banner "Đang lập chỉ mục" (TV-06).
 *
 * Đồng bộ do [SyncRepository] lo (chạy trong scope riêng của app); ở đây chỉ yêu cầu đồng bộ nếu quá hạn khi mở tab,
 * giống tab Thư mục (DS-04).
 */
class LibraryViewModel(
    private val drives: DriveRepository,
    private val sync: SyncRepository,
    private val network: NetworkMonitor,
    utcOffset: UtcOffsetProvider,
) : BaseMviViewModel<LibraryState, LibraryIntent, LibraryEffect>(LibraryState(utcOffsetMs = utcOffset.currentOffsetMs())) {

    private val filters = state.map { it.filter }.distinctUntilChanged()

    /**
     * Ảnh và video theo bộ lọc hiện tại, mới nhất trước (TV-02). `cachedIn` giữ các trang đã nạp qua xoay màn hình và
     * khi chuyển tab; đổi bộ lọc thì nạp lại từ đầu.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pages: Flow<PagingData<DriveItem>> =
        filters.flatMapLatest { drives.libraryPages(it) }.cachedIn(viewModelScope)

    init {
        observeSync()
        observeNetwork()
        observeDays()
        sync.syncIfStale()
    }

    override fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectFilter ->
                if (intent.filter != currentState.filter) {
                    // Bỏ số mục của bộ lọc cũ ngay: lưới cũ không khớp luồng trang mới.
                    setState { copy(filter = intent.filter, days = emptyList(), daysLoaded = false) }
                }
            is LibraryIntent.Open -> sendEffect(LibraryEffect.OpenFile(intent.item))
            LibraryIntent.Refresh -> sync.refresh()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDays() {
        viewModelScope.launch {
            filters
                .flatMapLatest { drives.libraryDays(it, currentState.utcOffsetMs) }
                .conflate()
                .collect { days ->
                    setState { copy(days = days, daysLoaded = true) }
                    // Với conflate, thời gian chờ ở đây gom các lần phát dồn khi đang quét thành một.
                    delay(DAYS_MIN_INTERVAL_MS)
                }
        }
    }

    private fun observeSync() {
        viewModelScope.launch {
            sync.observeState().collect { s ->
                setState {
                    copy(
                        sync = LibrarySync(
                            isSyncing = s.phase == SyncPhase.Syncing,
                            scannedCount = s.scannedCount,
                            initialSyncDone = s.initialSyncDone,
                            // AppLocked là app vừa khóa, không phải lỗi đồng bộ: màn Khóa sẽ che (CH-03).
                            failed = s.error != null && s.error != AppError.AppLocked,
                        ),
                    )
                }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            network.isOnline.collect { online ->
                setState { copy(isOffline = !online) }
                if (online) sync.syncIfStale()
            }
        }
    }
}
