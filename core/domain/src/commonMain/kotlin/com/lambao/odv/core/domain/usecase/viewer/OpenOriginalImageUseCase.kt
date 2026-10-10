package com.lambao.odv.core.domain.usecase.viewer

import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import kotlinx.coroutines.flow.Flow

/** Ảnh gốc của [CachedFileRef] (AN-01): có sẵn thì phát `Ready` ngay, chưa thì tải kèm tiến trình và tải tiếp phần dở (BN-03). */
class OpenOriginalImageUseCase(
    private val originals: OriginalImageRepository,
) {
    operator fun invoke(ref: CachedFileRef): Flow<CachedFileState> = originals.open(ref)
}
