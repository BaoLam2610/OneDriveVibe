package com.lambao.odv.core.domain.repository

import androidx.paging.PagingData
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.LibraryDay
import com.lambao.odv.core.domain.model.LibraryFilter
import com.lambao.odv.core.domain.model.MediaKind
import kotlinx.coroutines.flow.Flow

/** Tab Thư viện: ảnh và video của cả drive nhóm theo ngày (TV-01 đến TV-06), đọc từ Room. */
interface LibraryRepository {

    /**
     * Mọi ảnh và video thuộc [filter] và nằm trong [enabledKinds] (loại tệp được bật, CD-01) trong drive (mọi thư mục), mới nhất trước theo `libraryDate` (TV-01, TV-02), đọc từ
     * Room theo trang nên drive lớn không nạp hết vào bộ nhớ. Tự làm mới khi đồng bộ đổi dữ liệu (DB-05).
     */
    fun libraryPages(filter: LibraryFilter, enabledKinds: Set<MediaKind>): Flow<PagingData<DriveItem>>

    /**
     * Số mục theo từng ngày của Thư viện, mới nhất trước, nhóm theo `dayNumberOf` với độ lệch [utcOffsetMs]. Dùng cho
     * số mục ở tiêu đề nhóm (TV-01) và cuộn nhanh (TV-04).
     */
    fun libraryDays(filter: LibraryFilter, enabledKinds: Set<MediaKind>, utcOffsetMs: Long): Flow<List<LibraryDay>>
}
