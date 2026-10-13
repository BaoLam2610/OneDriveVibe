package com.lambao.odv.feature.browser

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.ContinueEntry
import com.lambao.odv.core.domain.model.FolderContent
import com.lambao.odv.core.domain.model.ItemKey
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.settings.BrowserPreferences
import com.lambao.odv.core.domain.settings.SettingsPreferences
import com.lambao.odv.core.domain.usecase.account.ObserveAccountScopeUseCase
import com.lambao.odv.core.domain.usecase.account.ObserveAccountStatusesUseCase
import com.lambao.odv.core.domain.usecase.folder.ObserveFolderContentUseCase
import com.lambao.odv.core.domain.usecase.folder.SearchDriveUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveOnlineAndSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveSyncStatusUseCase
import com.lambao.odv.core.domain.usecase.sync.RefreshSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncIfStaleUseCase
import com.lambao.odv.core.domain.usecase.watch.DismissContinueItemUseCase
import com.lambao.odv.core.domain.usecase.watch.ObserveContinueItemsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val settings: SettingsPreferences,
    private val observeAccountScope: ObserveAccountScopeUseCase,
    private val observeAccountStatuses: ObserveAccountStatusesUseCase,
    private val observeContinueItems: ObserveContinueItemsUseCase,
    private val dismissContinueItem: DismissContinueItemUseCase,
) : BaseMviViewModel<BrowserState, BrowserIntent, BrowserEffect>(BrowserState()) {

    private val log = Logger.withTag("Browser")

    // Tăng mỗi lần "Thử lại" để nạp lại danh sách lấy từ API (Room tự cập nhật nên không cần).
    private val retryTick = MutableStateFlow(0)
    private val queryInput = MutableStateFlow("")

    init {
        observePreferences()
        observeAccounts()
        observeContinue()
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
            is BrowserIntent.OpenAccount -> openAccount(intent.accountId)
            is BrowserIntent.OpenContinue -> openContinue(intent.entry)
            is BrowserIntent.DismissContinue -> viewModelScope.launch {
                try {
                    dismissContinueItem(intent.itemId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Không ẩn được chỉ làm thẻ còn nằm ở dải; không đáng làm sập tab (viewModelScope không có handler).
                    log.w { "Không ẩn được mục khỏi dải Xem tiếp: ${e::class.simpleName}" }
                }
            }
        }
    }

    /**
     * Mở tệp từ dải Xem tiếp / Đọc tiếp (DS-02): video vào thẳng vị trí đã dừng ([ViewerContext.Folder.resumeFromSaved]), danh sách phát là
     * các video cùng thư mục chứa nó theo kiểu sắp xếp đang chọn (VD-10). PDF tự mở lại đúng trang đã lưu (PD-05).
     */
    private fun openContinue(entry: ContinueEntry) {
        sendEffect(BrowserEffect.OpenFile(entry.item, ViewerContext.Folder(entry.parentId, currentState.sort, resumeFromSaved = true)))
    }

    /**
     * Mở gốc drive của một tài khoản từ ô ở D10 (TA-05): đường dẫn bắt đầu bằng một [Crumb] là gốc đó. Chưa biết gốc (trang đồng bộ đầu
     * tiên chưa ghi) thì chưa mở được, bỏ qua.
     */
    private fun openAccount(accountId: String) {
        val status = currentState.accounts.firstOrNull { it.account.id == accountId } ?: return
        val root = status.sync.rootFolderId
        if (root == null) {
            // U-M3: chưa có gốc (trang đồng bộ đầu tiên của tài khoản này chưa ghi): báo cho người dùng thay vì bỏ qua im lặng.
            sendEffect(BrowserEffect.AccountNotReady(status.account.displayName))
            return
        }
        navigateTo(listOf(Crumb(root, status.account.displayName)))
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
        // CD-01: bật/tắt loại tệp ở Cài đặt; ViewModel sống trong back stack nên đổi xong quay về là danh sách đã lọc lại.
        viewModelScope.launch { settings.enabledKinds.collect { kinds -> setState { copy(enabledKinds = kinds) } } }
    }

    /**
     * Danh sách tài khoản và phạm vi xem. Khi **phạm vi đổi** (TA-06) thì bỏ trạng thái của tab: về gốc, đóng tìm kiếm và sheet sắp xếp,
     * xóa danh sách cũ để không còn mục của tài khoản khác trên màn trong lúc nạp. Lần phát đầu chỉ ghi nhận phạm vi, không reset.
     */
    private fun observeAccounts() {
        viewModelScope.launch {
            var previous: AccountScope? = null
            observeAccountScope().collect { scope ->
                val changed = previous != null && previous != scope
                previous = scope
                if (changed) {
                    queryInput.value = ""
                    // items/isLoading do luồng nạp tự đặt khi khởi động lại theo phạm vi mới (observeContent, U-M2/U-L1), không đặt ở đây.
                    setState { copy(scope = scope, path = emptyList(), error = null, search = null, isSortSheetOpen = false) }
                } else {
                    setState { copy(scope = scope) }
                }
            }
        }
        viewModelScope.launch { observeAccountStatuses().collect { list -> setState { copy(accounts = list) } } }
    }

    /**
     * Dải Xem tiếp / Đọc tiếp (DS-02). Phạm vi xem đổi (TA-06) thì repository tự truy vấn lại với danh sách tài khoản mới; loại tệp đang bật
     * đọc thẳng từ Cài đặt (như [observeContent]) để không chớp loại đã tắt.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeContinue() {
        viewModelScope.launch {
            settings.enabledKinds.distinctUntilChanged()
                .flatMapLatest { kinds -> observeContinueItems(kinds) }
                .collect { entries -> setState { copy(continueItems = entries) } }
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
            // Có mạng trở lại: use case bù lần đồng bộ đã lỡ khi offline nếu đã quá hạn (DS-04).
            observeOnlineAndSync().collect { online -> setState { copy(isOffline = !online) } }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeContent() {
        // U-M2: phạm vi nằm trong khóa khởi động lại (cùng thư mục, cùng cờ "đã quét" nhưng đổi phạm vi vẫn phải nạp lại, nếu không isLoading kẹt).
        // U-M3: cờ đọc Room hay gọi API tính theo ĐÚNG tài khoản chứa thư mục (xem [folderSynced]). `scope == null` là chưa biết nên chưa nạp.
        val key = state.filter { it.scope != null }
            .map { Triple(it.path.lastOrNull()?.id, it.scope, it.folderSynced()) }
            .distinctUntilChanged()
        // Đọc thẳng từ Cài đặt chứ không qua State: State khởi đầu bằng "cả ba loại" nên đi qua đó sẽ truy vấn một lần với loại đã tắt
        // (tệp PDF chớp lên) rồi mới truy vấn lại với giá trị thật.
        val kinds = settings.enabledKinds.distinctUntilChanged()
        viewModelScope.launch {
            // U-L1: reset khi phạm vi đổi chạy `onStart` của chính luồng nạp mới (tuần tự với kết quả của nó), thay vì đặt isLoading/items từ
            // collector phạm vi khác, vốn có thể ghi đè kết quả luồng nạp đến trước.
            var lastScope: AccountScope? = null
            combine(key, kinds, retryTick) { k, enabled, _ -> k to enabled }
                .flatMapLatest { (k, enabled) ->
                    val (folder, scope, done) = k
                    val scopeChanged = lastScope != null && lastScope != scope
                    lastScope = scope
                    observeFolderContent(folder, done, sortOrders, enabled)
                        .onStart { if (scopeChanged) setState { copy(items = emptyList(), isLoading = true, error = null) } }
                }
                .collect(::applyContent)
        }
    }

    /**
     * Cờ "đã quét xong lần đầu" dùng để chọn Room (DB-05) hay API (TM-07). Ở phạm vi Tất cả, `sync.initialSyncDone` gộp nghĩa là "ít nhất một
     * tài khoản xong" nên thư mục của tài khoản chưa quét phải tra cờ riêng của tài khoản chứa nó (tiền tố id, ADR-0030); chưa tra được thì
     * dùng cờ gộp. Phạm vi một tài khoản giữ hành vi cũ.
     */
    private fun BrowserState.folderSynced(): Boolean {
        val accountId = path.lastOrNull()?.id?.let(ItemKey::accountIdOf)
        if (scope == AccountScope.All && accountId != null) {
            accounts.firstOrNull { it.account.id == accountId }?.let { return it.sync.initialSyncDone }
        }
        return sync.initialSyncDone
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
                val results = searchDrive(query, currentState.enabledKinds)
                setState { copy(search = search?.copy(results = results, isSearching = false)) }
            }
        }
    }
}
