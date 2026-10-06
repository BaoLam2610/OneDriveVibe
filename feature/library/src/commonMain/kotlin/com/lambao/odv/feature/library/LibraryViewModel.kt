package com.lambao.odv.feature.library

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.platform.UtcOffsetProvider
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryDaysUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryPagesUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveOnlineAndSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveSyncStatusUseCase
import com.lambao.odv.core.domain.usecase.sync.RefreshSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncIfStaleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Tab Thư viện (TV-01 → TV-06). Nguồn dữ liệu duy nhất là Room (ADR-0007): ảnh và video đọc theo trang qua Paging
 * ([pages]), số mục theo ngày ([LibraryState.days]) dựng tiêu đề nhóm và độ dài lưới. Tab không gọi API; khi quét lần
 * đầu chưa xong thì hiện phần đã quét được cùng banner "Đang lập chỉ mục" (TV-06).
 *
 * Đồng bộ do [SyncRepository] lo (chạy trong scope riêng của app); ở đây chỉ yêu cầu đồng bộ nếu quá hạn khi mở tab,
 * giống tab Thư mục (DS-04).
 */
class LibraryViewModel(
    private val observeLibraryPages: ObserveLibraryPagesUseCase,
    private val observeLibraryDays: ObserveLibraryDaysUseCase,
    private val observeSyncStatus: ObserveSyncStatusUseCase,
    private val observeOnlineAndSync: ObserveOnlineAndSyncUseCase,
    private val syncIfStale: SyncIfStaleUseCase,
    private val refreshSync: RefreshSyncUseCase,
    utcOffset: UtcOffsetProvider,
) : BaseMviViewModel<LibraryState, LibraryIntent, LibraryEffect>(LibraryState(utcOffsetMs = utcOffset.currentOffsetMs())) {

    private val filters = state.map { it.filter }.distinctUntilChanged()

    /**
     * Ảnh và video theo bộ lọc hiện tại, mới nhất trước (TV-02). `cachedIn` giữ các trang đã nạp qua xoay màn hình và
     * khi chuyển tab; đổi bộ lọc thì nạp lại từ đầu.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pages: Flow<PagingData<DriveItem>> =
        filters.flatMapLatest { observeLibraryPages(it) }.cachedIn(viewModelScope)

    init {
        observeSync()
        observeNetwork()
        observeDays()
        syncIfStale()
    }

    override fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectFilter ->
                if (intent.filter != currentState.filter) {
                    // Bỏ số mục của bộ lọc cũ ngay: lưới cũ không khớp luồng trang mới.
                    setState { copy(filter = intent.filter, days = emptyList(), daysLoaded = false) }
                }
            is LibraryIntent.Open -> sendEffect(LibraryEffect.OpenFile(intent.item, ViewerContext.Library(currentState.filter)))
            LibraryIntent.Refresh -> refreshSync()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDays() {
        viewModelScope.launch {
            filters
                .flatMapLatest { observeLibraryDays(it, currentState.utcOffsetMs) }
                .conflate()
                .collect { days ->
                    setState { copy(days = days, daysLoaded = true) }
                    // Với conflate, thời gian chờ ở đây gom các lần phát dồn khi đang quét thành một.
                    delay(LibraryConstants.DAYS_MIN_INTERVAL_MS)
                }
        }
    }

    private fun observeSync() {
        viewModelScope.launch {
            // AppLocked là app vừa khóa, không phải lỗi đồng bộ: ObserveSyncStatusUseCase đã loại (CH-03).
            observeSyncStatus().collect { status -> setState { copy(sync = status) } }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            // Có mạng trở lại: use case bù lần đồng bộ đã lỡ nếu đã quá hạn (DS-04).
            observeOnlineAndSync().collect { online -> setState { copy(isOffline = !online) } }
        }
    }
}
