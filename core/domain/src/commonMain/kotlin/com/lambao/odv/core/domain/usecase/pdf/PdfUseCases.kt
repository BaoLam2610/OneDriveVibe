package com.lambao.odv.core.domain.usecase.pdf

import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.model.PdfFile
import com.lambao.odv.core.domain.model.ReadingProgress
import com.lambao.odv.core.domain.repository.PdfRepository
import com.lambao.odv.core.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/** Tệp PDF của [CachedFileRef] (PD-01): có sẵn thì `Ready` ngay, chưa thì tải kèm tiến trình và tải tiếp phần dở (BN-03). */
class OpenPdfUseCase(
    private val pdfs: PdfRepository,
) {
    operator fun invoke(ref: CachedFileRef): Flow<CachedFileState> = pdfs.open(ref)
}

/** Bỏ bản PDF đã cache của [CachedFileRef] vì không mở được (hỏng, PD-07), để "Thử lại" tải bản mới. */
class DiscardCachedPdfUseCase(
    private val pdfs: PdfRepository,
) {
    suspend operator fun invoke(ref: CachedFileRef) = pdfs.discard(ref)
}

/** Bản ghi và thư mục chứa của một PDF (tên, cTag cho khóa cache, thông tin ở PD-08). Null nếu không còn trong Room. */
class GetPdfFileUseCase(
    private val pdfs: PdfRepository,
) {
    suspend operator fun invoke(itemId: String): PdfFile? = pdfs.getFile(itemId)
}

/** Trang đã lưu của PDF để mở lại đúng chỗ (PD-05). Null nếu chưa đọc lần nào hoặc tệp đã đổi nội dung. */
class GetReadingProgressUseCase(
    private val progress: ReadingProgressRepository,
) {
    suspend operator fun invoke(itemId: String, cTag: String?): ReadingProgress? = progress.get(itemId, cTag)
}

/** Lưu trang đang đọc (PD-05). `finished` do đây tính: trang cuối của tệp thì coi là đã đọc xong. */
class SaveReadingProgressUseCase(
    private val progress: ReadingProgressRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(itemId: String, cTag: String?, page: Int, pageCount: Int) {
        if (pageCount <= 0) return
        val clamped = page.coerceIn(0, pageCount - 1)
        progress.save(
            ReadingProgress(
                itemId = itemId,
                cTag = cTag,
                page = clamped,
                pageCount = pageCount,
                updatedAt = clock.now().toEpochMilliseconds(),
                finished = clamped == pageCount - 1,
            ),
        )
    }
}
