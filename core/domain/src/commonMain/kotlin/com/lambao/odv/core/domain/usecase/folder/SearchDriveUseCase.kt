package com.lambao.odv.core.domain.usecase.folder

import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SearchResult
import com.lambao.odv.core.domain.repository.FolderRepository

/** Tìm theo tên trên dữ liệu đã đồng bộ, không gọi API nên dùng được khi offline (DS-03). Chỉ trả loại tệp trong [kinds]. */
class SearchDriveUseCase(
    private val folders: FolderRepository,
) {
    suspend operator fun invoke(query: String, kinds: Set<MediaKind>): List<SearchResult> = folders.search(query, kinds)
}
