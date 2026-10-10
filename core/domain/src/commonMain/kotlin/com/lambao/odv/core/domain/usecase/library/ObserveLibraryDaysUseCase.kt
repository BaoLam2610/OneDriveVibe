package com.lambao.odv.core.domain.usecase.library

import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow

/** Số mục theo từng ngày của Thư viện (tiêu đề nhóm TV-01, cuộn nhanh TV-04), nhóm theo múi giờ [utcOffsetMs]. */
class ObserveLibraryDaysUseCase(
    private val library: LibraryRepository,
) {
    operator fun invoke(filter: LibraryFilter, enabledKinds: Set<MediaKind>, utcOffsetMs: Long): Flow<List<LibraryDay>> =
        library.libraryDays(filter, enabledKinds, utcOffsetMs)
}
