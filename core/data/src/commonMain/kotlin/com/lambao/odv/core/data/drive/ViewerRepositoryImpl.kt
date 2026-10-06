package com.lambao.odv.core.data.drive

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.map
import com.lambao.odv.core.database.DriveDao
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.ImageInfo
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.repository.FolderRepository
import com.lambao.odv.core.domain.repository.ViewerRepository
import com.lambao.odv.core.network.graph.GraphApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class ViewerRepositoryImpl(
    private val api: GraphApi,
    private val dao: DriveDao,
    private val folders: FolderRepository,
    private val dispatchers: DispatcherProvider,
) : ViewerRepository {

    override fun observeViewerItems(context: ViewerContext, kind: MediaKind): Flow<List<DriveItem>> = when (context) {
        is ViewerContext.Folder -> folders.observeChildren(context.folderId, context.sort)
            .map { items -> items.filter { it.mediaKind == kind } }
        is ViewerContext.Library -> {
            // Bộ lọc "Tất cả" gồm cả ảnh lẫn video; mỗi màn xem chỉ chuyển giữa các mục cùng loại với nó.
            val kinds = context.filter.kinds.filter { it == kind }.map { it.name }
            if (kinds.isEmpty()) {
                flowOf(emptyList())
            } else {
                dao.observeLibraryItems(kinds)
                    .conflate()
                    .map { rows -> rows.map { it.toDomain() } }
                    .distinctUntilChanged()
                    .flowOn(dispatchers.default)
            }
        }
    }

    override suspend fun getImageInfo(itemId: String): AppResult<ImageInfo> =
        api.getItemInfo(itemId).map { it.toImageInfo() }
}
