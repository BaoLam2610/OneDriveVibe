package com.lambao.odv.core.data.drive

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.data.DriveConstants
import com.lambao.odv.core.data.account.ScopedAccounts
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class LibraryRepositoryImpl(
    private val dao: DriveDao,
    private val dispatchers: DispatcherProvider,
    private val scoped: ScopedAccounts,
) : LibraryRepository {

    // Phạm vi xem đổi (TA-06) thì dựng lại Pager với danh sách tài khoản mới: PagingData cũ bị bỏ, không còn mục của tài khoản khác.
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun libraryPages(filter: LibraryFilter, enabledKinds: Set<MediaKind>): Flow<PagingData<DriveItem>> =
        scoped.accountIds().flatMapLatest { accountIds ->
            pages(filter, enabledKinds, accountIds)
        }

    private fun pages(filter: LibraryFilter, enabledKinds: Set<MediaKind>, accountIds: List<String>): Flow<PagingData<DriveItem>> =
        Pager(
            PagingConfig(
                pageSize = DriveConstants.LIBRARY_PAGE_SIZE,
                initialLoadSize = DriveConstants.LIBRARY_PAGE_SIZE * 2,
                prefetchDistance = DriveConstants.LIBRARY_PAGE_SIZE / 2,
                // Cần chỗ giữ chỗ để kéo cuộn nhanh nhảy tới đoạn chưa nạp (TV-04) và để giao diện dựng đúng độ dài
                // từ số mục theo ngày (libraryDays) mà không chờ nạp hết.
                enablePlaceholders = true,
            ),
        ) { dao.pagedLibrary((filter.kinds intersect enabledKinds).map { it.name }, accountIds) }
            .flow
            .map { page -> page.map { it.toDomain() } }

    // Từ khóa rỗng thì không truy vấn: trả luồng rỗng ngay, đỡ tạo Pager vô ích.
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun librarySearchPages(query: String, filter: LibraryFilter, enabledKinds: Set<MediaKind>): Flow<PagingData<DriveItem>> {
        val key = searchKey(query).trim()
        if (key.isEmpty()) return flowOf(PagingData.empty())
        return scoped.accountIds().flatMapLatest { accountIds ->
            Pager(
                PagingConfig(
                    pageSize = DriveConstants.LIBRARY_PAGE_SIZE,
                    initialLoadSize = DriveConstants.LIBRARY_PAGE_SIZE * 2,
                    prefetchDistance = DriveConstants.LIBRARY_PAGE_SIZE / 2,
                    // Lưới kết quả không có cuộn nhanh nên không cần giữ chỗ; số kết quả đổi theo từ khóa.
                    enablePlaceholders = false,
                ),
            ) { dao.pagedLibrarySearch(key, (filter.kinds intersect enabledKinds).map { it.name }, accountIds) }
                .flow
                .map { page -> page.map { it.toDomain() } }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun libraryDays(filter: LibraryFilter, enabledKinds: Set<MediaKind>, utcOffsetMs: Long): Flow<List<LibraryDay>> =
        scoped.accountIds()
            .flatMapLatest { accountIds -> dao.libraryDays((filter.kinds intersect enabledKinds).map { it.name }, accountIds, utcOffsetMs) }
            .conflate()
            .map { rows -> rows.map { LibraryDay(it.dayNumber, it.count, it.videoCount) } }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
}
