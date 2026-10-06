package com.lambao.odv.core.domain.model

import com.lambao.odv.core.common.error.AppError

/** Nội dung một thư mục cho tab Thư mục (TM-01, TM-07): đang tải, đã có danh sách (đã lọc loại tệp và sắp xếp), hoặc lỗi. */
sealed interface FolderContent {
    data object Loading : FolderContent
    data class Loaded(val items: List<DriveItem>) : FolderContent
    data class Failed(val error: AppError) : FolderContent
}
