package com.lambao.odv.core.data.drive

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.data.DriveConstants
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class LibraryRepositoryImpl(
    private val dao: DriveDao,
    private val dispatchers: DispatcherProvider,
) : LibraryRepository {

    override fun libraryPages(filter: LibraryFilter, enabledKinds: Set<MediaKind>): Flow<PagingData<DriveItem>> =
        Pager(
            PagingConfig(
                pageSize = DriveConstants.LIBRARY_PAGE_SIZE,
                initialLoadSize = DriveConstants.LIBRARY_PAGE_SIZE * 2,
                prefetchDistance = DriveConstants.LIBRARY_PAGE_SIZE / 2,
                // Cần chỗ giữ chỗ để kéo cuộn nhanh nhảy tới đoạn chưa nạp (TV-04) và để giao diện dựng đúng độ dài
                // từ số mục theo ngày (libraryDays) mà không chờ nạp hết.
                enablePlaceholders = true,
            ),
        ) { dao.pagedLibrary((filter.kinds intersect enabledKinds).map { it.name }) }
            .flow
            .map { page -> page.map { it.toDomain() } }

    override fun libraryDays(filter: LibraryFilter, enabledKinds: Set<MediaKind>, utcOffsetMs: Long): Flow<List<LibraryDay>> =
        dao.libraryDays((filter.kinds intersect enabledKinds).map { it.name }, utcOffsetMs)
            .conflate()
            .map { rows -> rows.map { LibraryDay(it.dayNumber, it.count, it.videoCount) } }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
}
