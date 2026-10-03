package com.lambao.odv.core.data.drive

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.map
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.database.DriveItemEntity
import com.lambao.odv.core.database.ItemRef
import com.lambao.odv.core.domain.model.ConnectionConfig
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SearchResult
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.network.GraphApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Giới hạn độ sâu khi dựng đường dẫn cha của kết quả tìm, chống vòng lặp nếu dữ liệu cha-con bị hỏng. */
private const val MAX_PATH_DEPTH = 64
private const val REF_CHUNK = 500

/** Số mục mỗi trang của Thư viện: bội của 4 cột để hàng cuối của trang không lẻ. */
private const val LIBRARY_PAGE_SIZE = 100

internal class DriveRepositoryImpl(
    private val api: GraphApi,
    private val configs: ConfigRepository,
    private val dao: DriveDao,
    private val dispatchers: DispatcherProvider,
) : DriveRepository {

    override suspend fun verifyConnection(config: ConnectionConfig): AppResult<Unit> =
        api.getDrive(config.toCredentials()).map { }

    override suspend fun listChildren(folderId: String?): AppResult<List<DriveItem>> {
        val config = when (val loaded = configs.load()) {
            is AppResult.Success -> loaded.value
            is AppResult.Failure -> return loaded
        }
        return api.listChildren(config.toCredentials(), folderId).map { items -> items.mapNotNull { it.toDomain() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChildren(folderId: String?, sort: SortOrder): Flow<List<DriveItem>> {
        // Thư mục gốc của UI (folderId null) là mục có id lưu ở sync_state.rootId; chưa quét xong thì chưa biết.
        val parentIds: Flow<String?> =
            if (folderId != null) flowOf(folderId) else dao.observeSyncState().map { it?.rootId }.distinctUntilChanged()
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

    override fun libraryPages(filter: LibraryFilter): Flow<PagingData<DriveItem>> =
        Pager(
            PagingConfig(
                pageSize = LIBRARY_PAGE_SIZE,
                initialLoadSize = LIBRARY_PAGE_SIZE * 2,
                prefetchDistance = LIBRARY_PAGE_SIZE / 2,
                // Cần chỗ giữ chỗ để kéo cuộn nhanh nhảy tới đoạn chưa nạp (TV-04) và để giao diện dựng đúng độ dài
                // từ số mục theo ngày (libraryDays) mà không chờ nạp hết.
                enablePlaceholders = true,
            ),
        ) { dao.pagedLibrary(filter.kinds.map { it.name }) }
            .flow
            .map { page -> page.map { it.toDomain() } }

    override fun libraryDays(filter: LibraryFilter, utcOffsetMs: Long): Flow<List<LibraryDay>> =
        dao.libraryDays(filter.kinds.map { it.name }, utcOffsetMs)
            .conflate()
            .map { rows -> rows.map { LibraryDay(it.dayNumber, it.count, it.videoCount) } }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)

    override suspend fun search(query: String, kinds: Set<MediaKind>, limit: Int): List<SearchResult> {
        val key = searchKey(query).trim()
        if (key.isEmpty()) return emptyList()
        return withContext(dispatchers.io) {
            val rows = dao.search(key, kinds.map { it.name }, limit)
            if (rows.isEmpty()) return@withContext emptyList()
            val rootId = dao.getSyncState()?.rootId
            val paths = folderPaths(rows.mapNotNull { it.parentId }.toSet(), rootId)
            rows.map { SearchResult(it.toDomain(), paths[it.parentId].orEmpty()) }
        }
    }

    /**
     * Tên các thư mục từ cấp dưới gốc xuống từng thư mục trong [folderIds]. Lấy theo từng tầng (một truy vấn mỗi tầng
     * cho cả tập) thay vì mỗi kết quả một chuỗi truy vấn.
     */
    private suspend fun folderPaths(folderIds: Set<String>, rootId: String?): Map<String, List<FolderRef>> {
        val refs = HashMap<String, ItemRef>()
        var pending = folderIds.filterTo(HashSet()) { it != rootId }
        var depth = 0
        while (pending.isNotEmpty() && depth++ < MAX_PATH_DEPTH) {
            val found = pending.chunked(REF_CHUNK).flatMap { dao.refs(it) }
            found.forEach { refs[it.id] = it }
            pending = found.mapNotNullTo(HashSet()) { it.parentId }.filterTo(HashSet()) { it != rootId && it !in refs }
        }
        return folderIds.associateWith { id ->
            val path = ArrayDeque<FolderRef>()
            var current: String? = id
            var guard = 0
            while (current != null && current != rootId && guard++ < MAX_PATH_DEPTH) {
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
