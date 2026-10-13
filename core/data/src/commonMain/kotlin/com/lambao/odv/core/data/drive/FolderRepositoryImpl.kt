package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.getOrElse
import com.lambao.odv.core.common.result.map
import com.lambao.odv.core.data.DriveConstants
import com.lambao.odv.core.data.account.AccountDirectory
import com.lambao.odv.core.data.account.ScopedAccounts
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.database.DriveItemEntity
import com.lambao.odv.core.database.ItemRef
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.model.ItemKey
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SearchResult
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.repository.AccountRepository
import com.lambao.odv.core.domain.repository.FolderRepository
import com.lambao.odv.core.network.graph.GraphApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class FolderRepositoryImpl(
    private val api: GraphApi,
    private val dao: DriveDao,
    private val dispatchers: DispatcherProvider,
    private val accounts: AccountRepository,
    private val scoped: ScopedAccounts,
    private val directory: AccountDirectory,
) : FolderRepository {

    override suspend fun listChildren(folderId: String?): AppResult<List<DriveItem>> {
        // Gốc (null) là gốc của tài khoản đang xem. Phạm vi Tất cả không có gốc chung (gốc là danh sách tài khoản, TA-05) nên rỗng.
        val accountId: String
        val upn: String
        val graphId: String?
        if (folderId == null) {
            accountId = (accounts.observeScope().first() as? AccountScope.One)?.accountId ?: return AppResult.Success(emptyList())
            upn = directory.upnOf(accountId).getOrElse { return AppResult.Failure(it) }
            graphId = null
        } else {
            accountId = ItemKey.accountIdOf(folderId) ?: return AppResult.Failure(AppError.Http(status = 404, code = "accountNotFound"))
            val target = directory.resolve(folderId).getOrElse { return AppResult.Failure(it) }
            upn = target.upn
            graphId = target.graphId
        }
        return api.listChildren(upn, graphId).map { items -> items.mapNotNull { it.toDomain(accountId) } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChildren(folderId: String?, sort: SortOrder): Flow<List<DriveItem>> {
        // Thư mục gốc của UI (folderId null) là mục có id lưu ở sync_state.rootId **của tài khoản đang xem**; chưa quét xong thì chưa
        // biết. Phạm vi Tất cả không có gốc chung: gốc là danh sách tài khoản do tầng trình bày dựng từ danh sách tài khoản (TA-05).
        val parentIds: Flow<String?> =
            if (folderId != null) {
                flowOf(folderId)
            } else {
                accounts.observeScope().flatMapLatest { scope ->
                    when (scope) {
                        AccountScope.All -> flowOf(null)
                        is AccountScope.One -> dao.observeSyncState(scope.accountId).map { it?.rootId }
                    }
                }.distinctUntilChanged()
            }
        return parentIds
            .flatMapLatest { parentId ->
                if (parentId == null) {
                    flowOf(emptyList())
                } else {
                    // Mỗi trang delta làm Room phát lại cả bảng: conflate bỏ các lần phát dồn khi đang sắp xếp lần trước,
                    // distinctUntilChanged bỏ lần phát mà thư mục này không đổi, để UI không vẽ lại vô ích trong lúc đồng bộ.
                    dao.observeChildren(parentId)
                        .conflate()
                        .map { rows -> rows.sortedFor(sort).map { it.toDomain() } }
                        .distinctUntilChanged()
                }
            }
            // Sắp xếp và ánh xạ thư mục lớn không được chạy trên luồng chính.
            .flowOn(dispatchers.default)
    }

    override suspend fun folderPathOf(itemId: String): List<FolderRef> = withContext(dispatchers.io) {
        val parentId = dao.refs(listOf(itemId)).firstOrNull()?.parentId ?: return@withContext emptyList()
        val rootId = ItemKey.accountIdOf(itemId)?.let { dao.getSyncState(it)?.rootId }
        folderPaths(setOf(parentId), setOfNotNull(rootId))[parentId].orEmpty()
    }

    override suspend fun search(query: String, kinds: Set<MediaKind>, limit: Int): List<SearchResult> {
        val key = searchKey(query).trim()
        if (key.isEmpty()) return emptyList()
        // Chỉ tìm trong các tài khoản thuộc phạm vi xem (DS-03, TA-05).
        val accountIds = scoped.accountIds().first()
        return withContext(dispatchers.io) {
            val rows = dao.search(key, kinds.map { it.name }, accountIds, limit)
            if (rows.isEmpty()) return@withContext emptyList()
            val states = dao.getSyncStates().associateBy { it.accountId }
            val roots = states.values.mapNotNullTo(HashSet()) { it.rootId }
            val paths = folderPaths(rows.mapNotNull { it.parentId }.toSet(), roots)
            // Phạm vi Tất cả: đường dẫn bắt đầu bằng tên tài khoản (D4, TA-05); nhấn vào đó là mở gốc của tài khoản.
            val names = if (accounts.observeScope().first() == AccountScope.All) {
                accounts.observeAccounts().first().associate { it.id to it.displayName }
            } else {
                emptyMap()
            }
            rows.map { row ->
                val root = states[row.accountId]?.rootId
                val owner = names[row.accountId]
                val prefix = if (root != null && owner != null) listOf(FolderRef(root, owner)) else emptyList()
                SearchResult(row.toDomain(), prefix + paths[row.parentId].orEmpty())
            }
        }
    }

    /**
     * Tên các thư mục từ cấp dưới gốc xuống từng thư mục trong [folderIds]. Lấy theo từng tầng (một truy vấn mỗi tầng
     * cho cả tập) thay vì mỗi kết quả một chuỗi truy vấn. [rootIds] là gốc của các tài khoản liên quan (mỗi tài khoản một gốc).
     */
    private suspend fun folderPaths(folderIds: Set<String>, rootIds: Set<String>): Map<String, List<FolderRef>> {
        val refs = HashMap<String, ItemRef>()
        var pending = folderIds.filterTo(HashSet()) { it !in rootIds }
        var depth = 0
        while (pending.isNotEmpty() && depth++ < DriveConstants.MAX_PATH_DEPTH) {
            val found = pending.chunked(DriveConstants.REF_CHUNK).flatMap { dao.refs(it) }
            found.forEach { refs[it.id] = it }
            pending = found.mapNotNullTo(HashSet()) { it.parentId }.filterTo(HashSet()) { it !in rootIds && it !in refs }
        }
        return folderIds.associateWith { id ->
            val path = ArrayDeque<FolderRef>()
            var current: String? = id
            var guard = 0
            while (current != null && current !in rootIds && guard++ < DriveConstants.MAX_PATH_DEPTH) {
                val ref = refs[current] ?: break
                path.addFirst(FolderRef(ref.id, ref.name))
                current = ref.parentId
            }
            path.toList()
        }
    }

    /** Thư mục trước tệp (TM-02), rồi theo trường đã chọn (TM-05); hòa thì theo tên để thứ tự ổn định. */
    private fun List<DriveItemEntity>.sortedFor(order: SortOrder): List<DriveItemEntity> {
        val byField: Comparator<DriveItemEntity> = when (order.field) {
            SortField.Name -> compareBy { it.nameKey }
            SortField.Modified -> compareBy { it.modifiedAt ?: 0L }
            SortField.Size -> compareBy { it.sizeBytes }
        }
        val directed = if (order.direction == SortDirection.Descending) byField.reversed() else byField
        return sortedWith(compareByDescending<DriveItemEntity> { it.isFolder }.then(directed).thenBy { it.nameKey })
    }
}
