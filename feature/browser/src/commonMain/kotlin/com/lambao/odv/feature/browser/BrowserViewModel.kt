package com.lambao.odv.feature.browser

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.FolderContent
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.settings.BrowserPreferences
import com.lambao.odv.core.domain.usecase.folder.ObserveFolderContentUseCase
import com.lambao.odv.core.domain.usecase.folder.SearchDriveUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveOnlineAndSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveSyncStatusUseCase
import com.lambao.odv.core.domain.usecase.sync.RefreshSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncIfStaleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tab Thư mục (TM-01 → TM-07, DS-03 → DS-06). Nguồn dữ liệu theo ADR-0007:
 * - Quét lần đầu xong ([SyncStatus.initialSyncDone]): đọc Room, tự cập nhật khi đồng bộ làm đổi dữ liệu (DB-05).
 * - Chưa xong: gọi thẳng API liệt kê thư mục (TM-07), sắp xếp tại chỗ.
 *
 * Mở màn thì yêu cầu đồng bộ nếu quá hạn (DS-04); đồng bộ chạy trong scope riêng của app nên rời màn không làm dừng nó.
 */
class BrowserViewModel(
    private val observeFolderContent: ObserveFolderContentUseCase,
    private val searchDrive: SearchDriveUseCase,
    private val observeSyncStatus: ObserveSyncStatusUseCase,
    private val observeOnlineAndSync: ObserveOnlineAndSyncUseCase,
    private val syncIfStale: SyncIfStaleUseCase,
    private val refreshSync: RefreshSyncUseCase,
    private val preferences: BrowserPreferences,
) : BaseMviViewModel<BrowserState, BrowserIntent, BrowserEffect>(BrowserState()) {

    private val log = Logger.withTag("Browser")

    // Tăng mỗi lần "Thử lại" để nạp lại danh sách lấy từ API (Room tự cập nhật nên không cần).
    private val retryTick = MutableStateFlow(0)
    private val queryInput = MutableStateFlow("")

    init {
        observePreferences()
        observeSync()
        observeNetwork()
        observeContent()
        observeSearch()
        syncIfStale()
    }

    override fun onIntent(intent: BrowserIntent) {
        when (intent) {
            is BrowserIntent.Open ->
                if (intent.item.isFolder) {
                    navigateTo(currentState.path + Crumb(intent.item.id, intent.item.name))
                } else {
                    sendEffect(BrowserEffect.OpenFile(intent.item, ViewerContext.Folder(currentState.path.lastOrNull()?.id, currentState.sort)))
                }
            BrowserIntent.GoUp -> if (currentState.path.isNotEmpty()) navigateTo(currentState.path.dropLast(1))
            is BrowserIntent.GoToCrumb ->
                if (intent.index in 0 until currentState.path.size) navigateTo(currentState.path.take(intent.index))
            BrowserIntent.Retry -> {
                setState { copy(isLoading = true, error = null) }
                retryTick.update { it + 1 }
                syncIfStale()
            }
            BrowserIntent.Refresh -> refreshSync()
            BrowserIntent.OpenSortSheet -> setState { copy(isSortSheetOpen = true) }
            BrowserIntent.DismissSortSheet -> setState { copy(isSortSheetOpen = false) }
            is BrowserIntent.SelectSort -> selectSort(intent.order)
            BrowserIntent.ToggleViewMode -> toggleViewMode()
            BrowserIntent.OpenSearch -> setState { copy(search = SearchState()) }
            BrowserIntent.CloseSearch -> closeSearch()
            is BrowserIntent.QueryChanged -> {
                setState { copy(search = (search ?: SearchState()).copy(query = intent.query)) }
                queryInput.value = intent.query
            }
            is BrowserIntent.OpenSearchResult -> openSearchResult(intent)
        }
    }

    private fun navigateTo(path: List<Crumb>) {
        if (path.lastOrNull()?.id == currentState.path.lastOrNull()?.id) {
            // Cùng thư mục (vd. chạm kết quả tìm trùng thư mục đang mở): luồng nạp lọc theo id nên sẽ không phát lại,
            // đặt isLoading = true ở đây sẽ treo vòng quay mãi. Chỉ cập nhật đường dẫn.
            setState { copy(path = path) }
            return
        }
        setState { copy(path = path, items = emptyList(), isLoading = true, error = null) }
    }

    private fun selectSort(order: SortOrder) {
        // Cập nhật ngay để giao diện không chờ ổ đĩa; lưu lỗi thì chỉ mất lựa chọn ở lần mở sau (TM-05).
        setState { copy(sort = order, isSortSheetOpen = false) }
        viewModelScope.launch { preferences.setSortOrder(order) }
    }

    private fun toggleViewMode() {
        val next = if (currentState.viewMode == ViewMode.List) ViewMode.Grid else ViewMode.List
        setState { copy(viewMode = next) }
        viewModelScope.launch { preferences.setViewMode(next) }
    }

    private fun closeSearch() {
        queryInput.value = ""
        setState { copy(search = null) }
    }

    private fun openSearchResult(intent: BrowserIntent.OpenSearchResult) {
        val item = intent.result.item
        if (!item.isFolder) {
            // Tệp từ kết quả tìm: vuốt trong thư mục chứa nó (parentPath rỗng = gốc), không phải thư mục đang mở.
            sendEffect(BrowserEffect.OpenFile(item, ViewerContext.Folder(intent.result.parentPath.lastOrNull()?.id, currentState.sort)))
            return
        }
        closeSearch()
        navigateTo(intent.result.parentPath.map { Crumb(it.id, it.name) } + Crumb(item.id, item.name))
    }

    private fun observePreferences() {
        viewModelScope.launch { preferences.sortOrder.collect { order -> setState { copy(sort = order) } } }
        viewModelScope.launch { preferences.viewMode.collect { mode -> setState { copy(viewMode = mode) } } }
    }

    private fun observeSync() {
        viewModelScope.launch {
            // AppLocked là app vừa khóa, không phải lỗi đồng bộ: ObserveSyncStatusUseCase đã loại (CH-03).
            observeSyncStatus().collect { status -> setState { copy(sync = status) } }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            // Có mạng trở lại: use case bù lần đồng bộ đã lỡ khi offline nếu đã quá hạn (DS-04).
            observeOnlineAndSync().collect { online -> setState { copy(isOffline = !online) } }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeContent() {
        val folderId = state.map { it.path.lastOrNull()?.id }.distinctUntilChanged()
        val synced = state.map { it.sync.initialSyncDone }.distinctUntilChanged()
        viewModelScope.launch {
            combine(folderId, synced, retryTick) { folder, done, _ -> folder to done }
                .flatMapLatest { (folder, done) -> observeFolderContent(folder, done, sortOrders, BrowserConstants.ENABLED_KINDS) }
                .collect(::applyContent)
        }
    }

    private val sortOrders: Flow<SortOrder> get() = state.map { it.sort }.distinctUntilChanged()

    private fun applyContent(content: FolderContent) {
        when (content) {
            FolderContent.Loading -> setState { copy(isLoading = true, error = null) }
            is FolderContent.Loaded -> {
                log.d { "Thư mục: hiển thị ${content.items.size} mục" }
                setState { copy(items = content.items, isLoading = false, error = null) }
            }
            is FolderContent.Failed -> {
                val error = content.error.toBrowserError()
                log.w { "Tải thư mục thất bại: ${error.kind} code=${error.code}" }
                setState { copy(isLoading = false, error = error) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            queryInput.debounce(BrowserConstants.SEARCH_DEBOUNCE_MS).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    setState { copy(search = search?.copy(results = emptyList(), isSearching = false)) }
                    return@collectLatest
                }
                setState { copy(search = search?.copy(isSearching = true)) }
                // Chỉ đọc Room, không gọi API: dùng được khi offline (DS-03).
                val results = searchDrive(query, BrowserConstants.ENABLED_KINDS)
                setState { copy(search = search?.copy(results = results, isSearching = false)) }
            }
        }
    }
}
