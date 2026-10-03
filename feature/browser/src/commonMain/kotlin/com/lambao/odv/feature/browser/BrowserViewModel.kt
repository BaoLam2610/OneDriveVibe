package com.lambao.odv.feature.browser

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.SyncPhase
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.model.sortedFor
import com.lambao.odv.core.domain.repository.BrowserPreferences
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.domain.repository.NetworkMonitor
import com.lambao.odv.core.domain.repository.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 250L

/** Loại tệp được bật (TM-03, DS-03). Lát 1 coi cả ba loại đều bật; bật/tắt từng loại là Lát 9 (CD-01). */
private val EnabledKinds: Set<MediaKind> = MediaKind.entries.toSet()

/**
 * Tab Thư mục (TM-01 → TM-07, DS-03 → DS-06). Nguồn dữ liệu theo ADR-0007:
 * - Quét lần đầu xong ([BrowserSync.initialSyncDone]): đọc Room, tự cập nhật khi đồng bộ làm đổi dữ liệu (DB-05).
 * - Chưa xong: gọi thẳng API liệt kê thư mục (TM-07), sắp xếp tại chỗ.
 *
 * Mở màn thì yêu cầu đồng bộ nếu quá hạn (DS-04); đồng bộ chạy trong scope riêng của app nên rời màn không làm dừng nó.
 */
class BrowserViewModel(
    private val drives: DriveRepository,
    private val sync: SyncRepository,
    private val preferences: BrowserPreferences,
    private val network: NetworkMonitor,
) : BaseMviViewModel<BrowserState, BrowserIntent, BrowserEffect>(BrowserState()) {

    private val log = Logger.withTag("Browser")

    // Tăng mỗi lần "Thử lại" để nạp lại danh sách lấy từ API (Room tự cập nhật nên không cần).
    private val retryTick = MutableStateFlow(0)
    private val queryInput = MutableStateFlow("")

    private sealed interface Content {
        data object Loading : Content
        data class Loaded(val items: List<DriveItem>) : Content
        data class Failed(val error: AppError) : Content
    }

    init {
        observePreferences()
        observeSync()
        observeNetwork()
        observeContent()
        observeSearch()
        sync.syncIfStale()
    }

    override fun onIntent(intent: BrowserIntent) {
        when (intent) {
            is BrowserIntent.Open ->
                if (intent.item.isFolder) {
                    navigateTo(currentState.path + Crumb(intent.item.id, intent.item.name))
                } else {
                    sendEffect(BrowserEffect.OpenFile(intent.item))
                }
            BrowserIntent.GoUp -> if (currentState.path.isNotEmpty()) navigateTo(currentState.path.dropLast(1))
            is BrowserIntent.GoToCrumb ->
                if (intent.index in 0 until currentState.path.size) navigateTo(currentState.path.take(intent.index))
            BrowserIntent.Retry -> {
                setState { copy(isLoading = true, error = null) }
                retryTick.update { it + 1 }
                sync.syncIfStale()
            }
            BrowserIntent.Refresh -> sync.refresh()
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
            sendEffect(BrowserEffect.OpenFile(item))
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
            sync.observeState().collect { s ->
                setState {
                    copy(
                        sync = BrowserSync(
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
                // Có mạng trở lại: bù lần đồng bộ đã lỡ khi offline nếu đã quá hạn (DS-04).
                if (online) sync.syncIfStale()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeContent() {
        val folderId = state.map { it.path.lastOrNull()?.id }.distinctUntilChanged()
        val synced = state.map { it.sync.initialSyncDone }.distinctUntilChanged()
        viewModelScope.launch {
            combine(folderId, synced, retryTick) { folder, done, _ -> folder to done }
                .flatMapLatest { (folder, done) -> if (done) roomContent(folder) else apiContent(folder) }
                .collect(::applyContent)
        }
    }

    private val sortOrders: Flow<SortOrder> get() = state.map { it.sort }.distinctUntilChanged()

    /** Đọc Room (DB-05): đổi cách sắp xếp thì truy vấn lại, đồng bộ làm đổi dữ liệu thì tự phát lại. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun roomContent(folderId: String?): Flow<Content> =
        sortOrders.flatMapLatest { sort -> drives.observeChildren(folderId, sort).map { Content.Loaded(it) } }

    /**
     * TM-07: quét lần đầu chưa xong nên gọi thẳng API. Chỉ gọi mạng một lần cho mỗi thư mục; đổi cách sắp xếp chỉ sắp xếp
     * lại danh sách đã có, không gọi lại.
     */
    private fun apiContent(folderId: String?): Flow<Content> = flow {
        emit(Content.Loading)
        when (val result = drives.listChildren(folderId)) {
            is AppResult.Success -> emitAll(sortOrders.map { Content.Loaded(result.value.sortedFor(it)) })
            is AppResult.Failure -> emit(Content.Failed(result.error))
        }
    }

    private fun applyContent(content: Content) {
        when (content) {
            Content.Loading -> setState { copy(isLoading = true, error = null) }
            is Content.Loaded -> {
                val visible = content.items.toVisibleItems()
                log.d { "Thư mục: ${content.items.size} mục, hiển thị ${visible.size}" }
                setState { copy(items = visible, isLoading = false, error = null) }
            }
            is Content.Failed -> {
                val error = content.error.toBrowserError()
                log.w { "Tải thư mục thất bại: ${error.kind} code=${error.code}" }
                setState { copy(isLoading = false, error = error) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            queryInput.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    setState { copy(search = search?.copy(results = emptyList(), isSearching = false)) }
                    return@collectLatest
                }
                setState { copy(search = search?.copy(isSearching = true)) }
                // Chỉ đọc Room, không gọi API: dùng được khi offline (DS-03).
                val results = drives.search(query, EnabledKinds)
                setState { copy(search = search?.copy(results = results, isSearching = false)) }
            }
        }
    }

    /** TM-03: chỉ giữ thư mục và tệp thuộc loại được bật. Thứ tự (TM-02, TM-05) đã do nguồn dữ liệu sắp xếp. */
    private fun List<DriveItem>.toVisibleItems(): List<DriveItem> =
        filter { it.isFolder || it.mediaKind in EnabledKinds }
}
