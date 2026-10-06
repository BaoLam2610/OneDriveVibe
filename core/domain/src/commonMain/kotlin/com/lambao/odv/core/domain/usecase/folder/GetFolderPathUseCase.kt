package com.lambao.odv.core.domain.usecase.folder

import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.repository.FolderRepository

/** Các thư mục từ cấp dưới gốc xuống thư mục chứa [itemId], đọc từ Room (bảng thông tin: đường dẫn thư mục, AN-05, VD-17). */
class GetFolderPathUseCase(
    private val folders: FolderRepository,
) {
    suspend operator fun invoke(itemId: String): List<FolderRef> = folders.folderPathOf(itemId)
}
