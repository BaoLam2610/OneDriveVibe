package com.lambao.odv.core.domain.usecase.library

import androidx.paging.PagingData
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow

/** Ảnh và video của cả drive theo [LibraryFilter], mới nhất trước, đọc từ Room theo trang (TV-01, TV-02). */
class ObserveLibraryPagesUseCase(
    private val library: LibraryRepository,
) {
    operator fun invoke(filter: LibraryFilter): Flow<PagingData<DriveItem>> = library.libraryPages(filter)
}
