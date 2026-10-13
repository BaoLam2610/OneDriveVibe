package com.lambao.odv.feature.library

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.platform.UtcOffsetProvider
import com.lambao.odv.core.domain.settings.SettingsPreferences
import com.lambao.odv.core.domain.usecase.account.ObserveAccountScopeUseCase
import com.lambao.odv.core.domain.usecase.account.ObserveAccountsUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryDaysUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryPagesUseCase
import com.lambao.odv.core.domain.usecase.library.SearchLibraryPagesUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveOnlineAndSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveSyncStatusUseCase
import com.lambao.odv.core.domain.usecase.sync.RefreshSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncIfStaleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val searchLibraryPages: SearchLibraryPagesUseCase,
    private val observeLibraryDays: ObserveLibraryDaysUseCase,
    private val observeSyncStatus: ObserveSyncStatusUseCase,
    private val observeOnlineAndSync: ObserveOnlineAndSyncUseCase,
    private val syncIfStale: SyncIfStaleUseCase,
    private val refreshSync: RefreshSyncUseCase,
    private val settings: SettingsPreferences,
    private val observeAccountScope: ObserveAccountScopeUseCase,
    private val observeAccounts: ObserveAccountsUseCase,
    utcOffset: UtcOffsetProvider,
) : BaseMviViewModel<LibraryState, LibraryIntent, LibraryEffect>(LibraryState(utcOffsetMs = utcOffset.currentOffsetMs())) {

    private val filters = state.map { it.filter }.distinctUntilChanged()

    /** Loại tệp được bật (CD-01); đổi ở Cài đặt thì lưới nạp lại ngay. */
    private val kinds = settings.enabledKinds.distinctUntilChanged()

    /**
     * Ảnh và video theo bộ lọc hiện tại, mới nhất trước (TV-02). `cachedIn` giữ các trang đã nạp qua xoay màn hình và
     * khi chuyển tab; đổi bộ lọc thì nạp lại từ đầu.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pages: Flow<PagingData<DriveItem>> =
        combine(filters, kinds) { filter, enabled -> filter to enabled }
            .flatMapLatest { (filter, enabled) -> observeLibraryPages(filter, enabled) }
            .cachedIn(viewModelScope)

    /** Chữ đang gõ ở ô tìm (TV-07); tách khỏi State để debounce trước khi đưa vào [LibrarySearchState.appliedQuery]. */
    private val queryInput = MutableStateFlow("")

    /**
     * Kết quả tìm trong Thư viện (TV-07): ảnh/video có tên khớp [LibrarySearchState.appliedQuery], theo bộ lọc đang chọn và loại tệp được bật,
     * trong phạm vi tài khoản đang xem; mới nhất trước. Từ khóa rỗng (hoặc không ở chế độ tìm) cho luồng rỗng. Lưới kết quả đọc luồng này.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchPages: Flow<PagingData<DriveItem>> =
        combine(state.map { it.search?.appliedQuery.orEmpty() }.distinctUntilChanged(), filters, kinds) { query, filter, enabled ->
            Triple(query, filter, enabled)
        }
            .flatMapLatest { (query, filter, enabled) -> searchLibraryPages(query, filter, enabled) }
            .cachedIn(viewModelScope)

    init {
        observeSearchInput()
        observeSync()
        observeNetwork()
        observeDays()
        observeEnabledKinds()
        observeScope()
        viewModelScope.launch { observeAccounts().collect { list -> setState { copy(accountNames = list.associate { it.id to it.displayName }) } } }
        syncIfStale()
    }

    override fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectFilter ->
                if (intent.filter != currentState.filter) {
                    // Không xóa số mục của bộ lọc cũ: lưới cũ giữ nguyên cho tới khi `observeDays` phát số mục mới (vài mili giây từ Room), nếu
                    // xóa thì lưới biến mất rồi hiện lại, màn khựng một nhịp. Lưới tự đưa về đầu khi bộ lọc đổi (xem ODVLibraryContent).
                    setState { copy(filter = intent.filter) }
                }
            // Mở từ kết quả tìm (TV-07) thì màn xem chỉ vuốt trong các mục khớp từ khóa đang hiện trên lưới (appliedQuery).
            is LibraryIntent.Open -> sendEffect(
                LibraryEffect.OpenFile(
                    intent.item,
                    ViewerContext.Library(currentState.filter, query = currentState.search?.appliedQuery?.takeIf { it.isNotEmpty() }),
                ),
            )
            LibraryIntent.Refresh -> refreshSync()
            LibraryIntent.OpenSearch -> setState { copy(search = LibrarySearchState()) }
            LibraryIntent.CloseSearch -> closeSearch()
            is LibraryIntent.QueryChanged -> {
                queryInput.value = intent.query
                setState { copy(search = (search ?: LibrarySearchState()).copy(query = intent.query)) }
            }
        }
    }

    private fun closeSearch() {
        queryInput.value = ""
        setState { copy(search = null) }
    }

    /** Chờ người dùng ngừng gõ rồi mới áp từ khóa (TV-07): [LibrarySearchState.appliedQuery] đổi thì [searchPages] nạp lại. */
    @OptIn(FlowPreview::class)
    private fun observeSearchInput() {
        viewModelScope.launch {
            queryInput.debounce(LibraryConstants.SEARCH_DEBOUNCE_MS).distinctUntilChanged().collect { query ->
                setState { copy(search = search?.copy(appliedQuery = query.trim())) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDays() {
        viewModelScope.launch {
            // U-L1: phạm vi nằm trong khóa khởi động lại của luồng và việc bỏ số mục cũ chạy `onStart` của chính luồng mới, nên reset và nạp là
            // một chuỗi tuần tự; trước đây reset ở collector khác có thể ghi đè kết quả của luồng nạp đến trước.
            var lastScope: AccountScope? = null
            combine(filters, kinds, observeAccountScope().distinctUntilChanged()) { filter, enabled, scope -> Triple(filter, enabled, scope) }
                .flatMapLatest { (filter, enabled, scope) ->
                    val scopeChanged = lastScope != null && lastScope != scope
                    lastScope = scope
                    observeLibraryDays(filter, enabled, currentState.utcOffsetMs)
                        .onStart { if (scopeChanged) setState { copy(days = emptyList(), daysLoaded = false) } }
                }
                .conflate()
                .collect { days ->
                    setState { copy(days = days, daysLoaded = true) }
                    // Với conflate, thời gian chờ ở đây gom các lần phát dồn khi đang quét thành một.
                    delay(LibraryConstants.DAYS_MIN_INTERVAL_MS)
                }
        }
    }

    private fun observeEnabledKinds() {
        viewModelScope.launch {
            kinds.collect { enabled ->
                // Bộ lọc đang chọn mà loại của nó vừa bị tắt hết (vd. đang ở "Ảnh" rồi tắt Ảnh) thì về "Tất cả".
                setState {
                    val valid = filter.kinds.any { it in enabled }
                    copy(enabledKinds = enabled, filter = if (valid) filter else LibraryFilter.All, daysLoaded = if (valid) daysLoaded else false)
                }
            }
        }
    }

    /**
     * Phạm vi xem đổi (TA-06): bỏ trạng thái của tab, đưa bộ lọc về "Tất cả" và xóa số mục theo ngày cũ. Lưới tự nạp lại vì kho dữ liệu
     * phát PagingData mới theo phạm vi; ở đây chỉ đảm bảo không còn tiêu đề nhóm của tài khoản cũ trong lúc nạp. Lần phát đầu không reset.
     */
    private fun observeScope() {
        viewModelScope.launch {
            var previous: AccountScope? = null
            observeAccountScope().collect { scope ->
                // Số mục theo ngày được xóa trong `observeDays` (cùng chuỗi với việc nạp, U-L1); ở đây chỉ đưa bộ lọc về "Tất cả".
                if (previous != null && previous != scope) {
                    // Đóng cả tìm kiếm (TV-07): kết quả cũ thuộc tài khoản khác.
                    queryInput.value = ""
                    setState { copy(filter = LibraryFilter.All, search = null) }
                }
                previous = scope
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
