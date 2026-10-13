package com.lambao.odv.core.domain.usecase.library

import androidx.paging.PagingData
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Tìm ảnh và video trong Thư viện theo tên (TV-07): chỉ trong phạm vi Thư viện (ảnh/video theo [LibraryFilter] và loại tệp được bật, CD-01;
 * phạm vi tài khoản đang xem, TA-05), mới nhất trước, đọc Room theo trang nên dùng được khi offline.
 */
class SearchLibraryPagesUseCase(
    private val library: LibraryRepository,
) {
    operator fun invoke(query: String, filter: LibraryFilter, enabledKinds: Set<MediaKind>): Flow<PagingData<DriveItem>> =
        library.librarySearchPages(query, filter, enabledKinds)
}
