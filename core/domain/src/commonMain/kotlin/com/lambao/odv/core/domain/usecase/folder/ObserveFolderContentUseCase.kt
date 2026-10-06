package com.lambao.odv.core.domain.usecase.folder

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.FolderContent
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.sortedFor
import com.lambao.odv.core.domain.repository.FolderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Nội dung thư mục [folderId] (null = gốc) cho tab Thư mục, theo ADR-0007:
 * - [initialSyncDone]: đã quét xong lần đầu thì đọc Room (DB-05); chưa thì gọi thẳng API (TM-07).
 * - Luôn lọc theo [enabledKinds] (TM-03: thư mục và tệp thuộc loại được bật) và sắp xếp theo [sortOrders] (TM-02, TM-05).
 */
class ObserveFolderContentUseCase(
    private val folders: FolderRepository,
) {
    operator fun invoke(
        folderId: String?,
        initialSyncDone: Boolean,
        sortOrders: Flow<SortOrder>,
        enabledKinds: Set<MediaKind>,
    ): Flow<FolderContent> {
        val content = if (initialSyncDone) fromRoom(folderId, sortOrders) else fromApi(folderId, sortOrders)
        return content.map { if (it is FolderContent.Loaded) FolderContent.Loaded(it.items.visible(enabledKinds)) else it }
    }

    /** Đọc Room (DB-05): đổi cách sắp xếp thì truy vấn lại, đồng bộ làm đổi dữ liệu thì tự phát lại. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fromRoom(folderId: String?, sortOrders: Flow<SortOrder>): Flow<FolderContent> =
        sortOrders.flatMapLatest { sort -> folders.observeChildren(folderId, sort).map { FolderContent.Loaded(it) } }

    /**
     * TM-07: quét lần đầu chưa xong nên gọi thẳng API. Chỉ gọi mạng một lần cho mỗi thư mục; đổi cách sắp xếp chỉ sắp xếp
     * lại danh sách đã có, không gọi lại.
     */
    private fun fromApi(folderId: String?, sortOrders: Flow<SortOrder>): Flow<FolderContent> = flow {
        emit(FolderContent.Loading)
        when (val result = folders.listChildren(folderId)) {
            is AppResult.Success -> emitAll(sortOrders.map { FolderContent.Loaded(result.value.sortedFor(it)) })
            is AppResult.Failure -> emit(FolderContent.Failed(result.error))
        }
    }

    /** TM-03: chỉ giữ thư mục và tệp thuộc loại được bật. Thứ tự (TM-02, TM-05) đã do nguồn dữ liệu sắp xếp. */
    private fun List<DriveItem>.visible(enabledKinds: Set<MediaKind>): List<DriveItem> =
        filter { it.isFolder || it.mediaKind in enabledKinds }
}
